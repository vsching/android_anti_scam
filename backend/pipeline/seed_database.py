"""Scam domain database seed pipeline.

Orchestrates feed download, normalization, deduplication, versioned artifact
generation (JSON, SQLite, Bloom filter, manifest), R2 upload, KV allowlist
seeding, latest.json manifest update, and pending_discoveries processing.

Includes anti-poisoning validation gates before publishing.
"""

from __future__ import annotations

import json
import logging
import os
import sys
import tempfile
from datetime import datetime, timezone
from typing import Any

import click

from src.allowlist import get_all_allowlisted_domains, get_allowlist_domain_set
from src.bloom_filter import generate_bloom_filter
from src.cloudflare import D1Client, KVClient, R2Client
from src.discovery_processor import process_discoveries
from src.manifest import generate_latest_json, generate_manifest
from src.normalize import deduplicate, normalize_domain, validate_domain_format
from src.sources import FetchResult, fetch_all_sources, get_default_sources
from src.sqlite_exporter import export_to_sqlite

logger = logging.getLogger(__name__)

# Anti-poisoning thresholds
MAX_DOMAIN_DROP_PERCENT = 50  # reject if total drops >50% vs previous run
MAX_SINGLE_SOURCE_PERCENT = 80  # reject if any source contributes >80%


def _build_domain_records(
    normalized_domains: list[str],
    source_map: dict[str, str],
) -> list[dict[str, Any]]:
    """Build domain record dicts for JSON/SQLite export."""
    records = []
    for domain in normalized_domains:
        records.append(
            {
                "domain": domain,
                "verdict": "dangerous",
                "reason": "Listed in scam/phishing feed",
                "source": source_map.get(domain, "unknown"),
                "confidence": 1.0,
            }
        )
    return records


def _validate_anti_poisoning(
    results: list[FetchResult],
    total_domains: int,
    previous_count: int | None,
    force: bool,
) -> bool:
    """Run anti-poisoning validation gates.

    Returns True if publishing should proceed, False to abort.
    """
    warnings: list[str] = []

    # Gate (a): reject if total domain count drops >50% vs previous run
    if previous_count is not None and previous_count > 0:
        drop_percent = ((previous_count - total_domains) / previous_count) * 100
        if drop_percent > MAX_DOMAIN_DROP_PERCENT:
            warnings.append(
                f"Domain count dropped {drop_percent:.1f}% "
                f"(from {previous_count} to {total_domains}). "
                f"Possible source compromise."
            )

    # Gate (b): reject if any single source contributes >80% of new domains
    # Only applies when there are multiple successful sources
    successful_results = [r for r in results if r.success and r.domains]
    if len(successful_results) > 1 and total_domains > 0:
        for result in successful_results:
            source_pct = (len(result.domains) / total_domains) * 100
            if source_pct > MAX_SINGLE_SOURCE_PERCENT:
                warnings.append(
                    f"Source '{result.source}' contributes {source_pct:.1f}% "
                    f"of total domains ({len(result.domains)}/{total_domains}). "
                    f"Anomaly detected."
                )

    if warnings:
        for w in warnings:
            logger.warning("ANTI-POISONING: %s", w)

        if force:
            logger.warning("--force flag set, proceeding despite warnings")
            return True
        else:
            logger.error(
                "Anti-poisoning gates breached. "
                "Use --force to override. Aborting publish."
            )
            return False

    return True


def run_pipeline(
    r2_client: R2Client | None = None,
    kv_client: KVClient | None = None,
    d1_client: D1Client | None = None,
    sources: list[Any] | None = None,
    force: bool = False,
    output_dir: str | None = None,
    skip_upload: bool = False,
) -> dict[str, Any]:
    """Run the full seed pipeline.

    Args:
        r2_client: R2 client for uploads (None to skip uploads).
        kv_client: KV client for allowlist seeding (None to skip).
        d1_client: D1 client for discovery processing (None to skip).
        sources: Source adapters to use (None for defaults).
        force: Override anti-poisoning gates.
        output_dir: Directory for output artifacts (None for temp dir).
        skip_upload: If True, skip R2/KV uploads (useful for testing).

    Returns:
        Dict with pipeline results (domain_count, artifacts, etc.).
    """
    version = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    if output_dir is None:
        output_dir = tempfile.mkdtemp(prefix="safeanot-pipeline-")

    logger.info("Starting pipeline run for version %s", version)

    # Step 1: Fetch from all sources
    if sources is None:
        sources = get_default_sources()
    results = fetch_all_sources(sources=sources)

    successful = [r for r in results if r.success]
    failed = [r for r in results if not r.success]

    if failed:
        for f in failed:
            logger.warning("Source %s failed: %s", f.source, f.error)

    if not successful:
        logger.error("All sources failed. Aborting.")
        return {"success": False, "error": "All sources failed"}

    # Step 2: Normalize and deduplicate
    source_map: dict[str, str] = {}
    all_raw_domains: list[str] = []
    for result in successful:
        for raw in result.domains:
            normalized = normalize_domain(raw)
            if normalized and validate_domain_format(normalized):
                all_raw_domains.append(normalized)
                if normalized not in source_map:
                    source_map[normalized] = result.source

    # Deduplicate
    unique_domains = deduplicate(all_raw_domains)

    # Remove allowlisted domains from scam list
    allowlist_set = get_allowlist_domain_set()
    unique_domains = [d for d in unique_domains if d not in allowlist_set]

    logger.info("Normalized and deduplicated: %d unique domains", len(unique_domains))

    # Step 3: Process pending discoveries from D1
    discovered_domains: list[dict[str, Any]] = []
    known_set = set(unique_domains)
    if d1_client is not None:
        discovered_domains = process_discoveries(d1_client, known_set)
        # Add confirmed discoveries to the domain list
        for disc in discovered_domains:
            domain = disc["domain"]
            if domain not in known_set:
                unique_domains.append(domain)
                known_set.add(domain)
                source_map[domain] = "community_discovery"

    # Step 4: Anti-poisoning validation
    previous_count: int | None = None
    if r2_client is not None and not skip_upload:
        try:
            latest = r2_client.download_json("latest.json")
            if latest and "domain_count" in latest:
                previous_count = latest["domain_count"]
        except Exception:
            pass

    if not _validate_anti_poisoning(
        results, len(unique_domains), previous_count, force
    ):
        return {
            "success": False,
            "error": "Anti-poisoning gates breached",
            "domain_count": len(unique_domains),
        }

    # Step 5: Generate artifacts
    domain_records = _build_domain_records(unique_domains, source_map)

    # Full JSON
    full_json_path = os.path.join(output_dir, f"domains-full-{version}.json")
    with open(full_json_path, "w") as f:
        json.dump(domain_records, f, indent=2)

    # Delta JSON (for initial seed, delta = full)
    delta_json_path = os.path.join(output_dir, f"domains-delta-{version}.json")
    with open(delta_json_path, "w") as f:
        json.dump(domain_records, f, indent=2)

    # SQLite export
    sqlite_path = os.path.join(output_dir, f"domains-full-{version}.sqlite")
    export_to_sqlite(domain_records, sqlite_path, version=version)

    # Bloom filter
    bloom = generate_bloom_filter(unique_domains)
    bloom_path = os.path.join(output_dir, f"bloom-{version}.bin")
    with open(bloom_path, "wb") as f:
        bloom.write_to_file(f)

    # Manifest
    artifacts = {
        "full_json": full_json_path,
        "delta_json": delta_json_path,
        "sqlite": sqlite_path,
        "bloom": bloom_path,
    }
    bloom_params = {
        "size_bits": bloom.size_bits,
        "num_hashes": bloom.num_hashes,
        "domain_count": bloom.count,
    }
    manifest = generate_manifest(version, artifacts, len(unique_domains), bloom_params)
    manifest_path = os.path.join(output_dir, f"manifest-{version}.json")
    with open(manifest_path, "w") as f:
        json.dump(manifest, f, indent=2)

    logger.info(
        "Generated artifacts in %s: %d domains, %d bytes bloom",
        output_dir,
        len(unique_domains),
        bloom.size_bytes,
    )

    # Step 6: Upload to R2
    if r2_client is not None and not skip_upload:
        for artifact_type, local_path in artifacts.items():
            r2_key = manifest["artifacts"][artifact_type]["filename"]
            r2_client.upload_file(local_path, r2_key)

        r2_client.upload_file(manifest_path, f"manifest-{version}.json")

        # Update latest.json
        latest_json = generate_latest_json(manifest)
        r2_client.upload_json(latest_json, "latest.json")

        logger.info("Uploaded all artifacts to R2")

    # Step 7: Seed KV with allowlist
    if kv_client is not None and not skip_upload:
        allowlist_entries = get_all_allowlisted_domains()
        kv_bulk = [
            {
                "key": f"allowlist:{entry['domain']}",
                "value": json.dumps(
                    {
                        "domain": entry["domain"],
                        "verdict": "safe",
                        "category": entry["category"],
                        "entity": entry["entity"],
                    }
                ),
            }
            for entry in allowlist_entries
        ]
        kv_client.write_bulk(kv_bulk)
        logger.info("Seeded KV with %d allowlist entries", len(kv_bulk))

    return {
        "success": True,
        "version": version,
        "domain_count": len(unique_domains),
        "sources_succeeded": len(successful),
        "sources_failed": len(failed),
        "discoveries_promoted": len(discovered_domains),
        "artifacts": {k: os.path.basename(v) for k, v in artifacts.items()},
        "output_dir": output_dir,
    }


@click.command()
@click.option("--force", is_flag=True, help="Override anti-poisoning gates")
@click.option("--output-dir", type=str, default=None, help="Output directory")
@click.option(
    "--skip-upload", is_flag=True, help="Skip R2/KV uploads (local run)"
)
def main(force: bool, output_dir: str | None, skip_upload: bool) -> None:
    """Run the Safe Anot? scam domain seed pipeline."""
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s: %(message)s",
    )

    r2_client = None
    kv_client = None
    d1_client = None

    if not skip_upload:
        # Build real clients from environment variables
        from src.cloudflare import (
            CloudflareD1Client,
            CloudflareKVClient,
            CloudflareR2Client,
        )

        api_token = os.environ.get("CLOUDFLARE_API_TOKEN", "")
        account_id = os.environ.get("CLOUDFLARE_ACCOUNT_ID", "")

        if api_token and account_id:
            r2_bucket = os.environ.get("CLOUDFLARE_R2_BUCKET", "safeanot-data")
            r2_access_key = os.environ.get("CLOUDFLARE_R2_ACCESS_KEY_ID", "")
            r2_secret_key = os.environ.get("CLOUDFLARE_R2_SECRET_ACCESS_KEY", "")
            if r2_access_key and r2_secret_key:
                r2_client = CloudflareR2Client(
                    account_id, r2_access_key, r2_secret_key, r2_bucket
                )

            kv_namespace_id = os.environ.get("CLOUDFLARE_KV_NAMESPACE_ID", "")
            if kv_namespace_id:
                kv_client = CloudflareKVClient(
                    api_token, account_id, kv_namespace_id
                )

            d1_database_id = os.environ.get("CLOUDFLARE_D1_DATABASE_ID", "")
            if d1_database_id:
                d1_client = CloudflareD1Client(
                    api_token, account_id, d1_database_id
                )

    result = run_pipeline(
        r2_client=r2_client,
        kv_client=kv_client,
        d1_client=d1_client,
        force=force,
        output_dir=output_dir,
        skip_upload=skip_upload,
    )

    if result.get("success"):
        logger.info("Pipeline completed successfully: %s", result)
    else:
        logger.error("Pipeline failed: %s", result)
        sys.exit(1)


if __name__ == "__main__":
    main()

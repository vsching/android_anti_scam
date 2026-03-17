"""Manifest generation for versioned pipeline artifacts."""

from __future__ import annotations

import hashlib
import json
import os
from datetime import datetime, timezone
from typing import Any


def file_sha256(path: str) -> str:
    """Compute SHA-256 hex digest of a file."""
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            h.update(chunk)
    return h.hexdigest()


def file_size_bytes(path: str) -> int:
    """Return file size in bytes."""
    return os.path.getsize(path)


def generate_manifest(
    version: str,
    artifacts: dict[str, str],
    domain_count: int,
    bloom_params: dict[str, Any] | None = None,
) -> dict[str, Any]:
    """Generate a manifest JSON describing all pipeline artifacts.

    Args:
        version: Version string (e.g., "2026-03-17").
        artifacts: Dict mapping artifact type to local file path.
            Expected keys: "full_json", "delta_json", "sqlite", "bloom".
        domain_count: Total number of domains in the dataset.
        bloom_params: Bloom filter parameters (size_bits, num_hashes, etc.).

    Returns:
        Manifest dict ready for JSON serialization.
    """
    build_timestamp = datetime.now(timezone.utc).isoformat()

    manifest: dict[str, Any] = {
        "version": version,
        "build_timestamp": build_timestamp,
        "domain_count": domain_count,
        "artifacts": {},
    }

    artifact_key_map = {
        "full_json": f"domains-full-{version}.json",
        "delta_json": f"domains-delta-{version}.json",
        "sqlite": f"domains-full-{version}.sqlite",
        "bloom": f"bloom-{version}.bin",
    }

    for artifact_type, local_path in artifacts.items():
        if not os.path.exists(local_path):
            continue
        r2_key = artifact_key_map.get(artifact_type, os.path.basename(local_path))
        size = file_size_bytes(local_path)
        checksum = file_sha256(local_path)
        manifest["artifacts"][artifact_type] = {
            "filename": r2_key,
            "size_bytes": size,
            "size_kb": round(size / 1024, 1),
            "sha256": checksum,
        }

    if bloom_params:
        manifest["bloom_params"] = bloom_params

    return manifest


def generate_latest_json(manifest: dict[str, Any]) -> dict[str, Any]:
    """Generate the latest.json content from a manifest.

    This is the metadata file that the Worker reads to serve /api/data/latest.
    """
    latest: dict[str, Any] = {
        "version": manifest["version"],
        "build_timestamp": manifest["build_timestamp"],
        "domain_count": manifest["domain_count"],
    }

    for artifact_type in ("full_json", "delta_json", "sqlite", "bloom"):
        artifact = manifest.get("artifacts", {}).get(artifact_type)
        if artifact:
            latest[f"{artifact_type}_size_kb"] = artifact["size_kb"]
            latest[f"{artifact_type}_filename"] = artifact["filename"]

    if "bloom_params" in manifest:
        latest["bloom_params"] = manifest["bloom_params"]

    return latest

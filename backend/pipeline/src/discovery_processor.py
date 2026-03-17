"""Process pending domain discoveries from D1.

Reads pending_discoveries from D1 via Cloudflare API, cross-references
with fetched sources, promotes confirmed domains, and marks rows processed.
"""

from __future__ import annotations

import logging
from datetime import datetime, timezone
from typing import Any

from src.cloudflare import D1Client

logger = logging.getLogger(__name__)


def fetch_pending_discoveries(d1_client: D1Client) -> list[dict[str, Any]]:
    """Fetch unprocessed pending discoveries from D1.

    Returns list of dicts with at least: domain, verdict, reason, check_count.
    """
    sql = """
        SELECT domain, verdict, reason, check_count, confidence,
               created_at, last_seen_at
        FROM pending_discoveries
        WHERE processed = 0
        ORDER BY check_count DESC, created_at ASC
    """
    try:
        rows = d1_client.query(sql)
        logger.info("Fetched %d pending discoveries from D1", len(rows))
        return rows
    except Exception as exc:
        logger.error("Failed to fetch pending discoveries: %s", exc)
        return []


def cross_reference_discoveries(
    pending: list[dict[str, Any]],
    known_domains: set[str],
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    """Cross-reference pending discoveries against known scam domains.

    Args:
        pending: List of pending discovery rows from D1.
        known_domains: Set of domains confirmed by external sources.

    Returns:
        Tuple of (confirmed, unconfirmed) discovery lists.
        Confirmed means the domain appears in at least one external source.
    """
    confirmed: list[dict[str, Any]] = []
    unconfirmed: list[dict[str, Any]] = []

    for discovery in pending:
        domain = discovery.get("domain", "")
        if domain in known_domains:
            confirmed.append(discovery)
        else:
            unconfirmed.append(discovery)

    logger.info(
        "Cross-reference: %d confirmed, %d unconfirmed",
        len(confirmed),
        len(unconfirmed),
    )
    return confirmed, unconfirmed


def mark_discoveries_processed(
    d1_client: D1Client,
    domains: list[str],
) -> None:
    """Mark discoveries as processed in D1."""
    if not domains:
        return

    for domain in domains:
        try:
            d1_client.execute(
                "UPDATE pending_discoveries SET processed = 1 WHERE domain = ?",
                [domain],
            )
        except Exception as exc:
            logger.error(
                "Failed to mark discovery processed for %s: %s", domain, exc
            )


def cleanup_stale_discoveries(
    d1_client: D1Client,
    max_age_days: int = 30,
) -> int:
    """Remove or archive stale pending entries older than max_age_days.

    Returns the number of entries cleaned up.
    """
    cutoff = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
    sql = f"""
        DELETE FROM pending_discoveries
        WHERE processed = 1
          AND created_at < datetime('now', '-{max_age_days} days')
    """
    try:
        d1_client.execute(sql)
        # We can't easily get affected row count from the API,
        # so just log the action
        logger.info(
            "Cleaned up stale discoveries older than %d days", max_age_days
        )
        return 0  # actual count not available from API
    except Exception as exc:
        logger.error("Failed to cleanup stale discoveries: %s", exc)
        return 0


def process_discoveries(
    d1_client: D1Client,
    known_domains: set[str],
) -> list[dict[str, Any]]:
    """Full discovery processing pipeline.

    1. Fetch pending discoveries from D1.
    2. Cross-reference with known scam domains.
    3. Mark all pending (confirmed + unconfirmed) as processed.
    4. Cleanup stale entries.

    Returns list of confirmed discoveries to be added to bulk database.
    """
    pending = fetch_pending_discoveries(d1_client)
    if not pending:
        logger.info("No pending discoveries to process")
        return []

    confirmed, unconfirmed = cross_reference_discoveries(pending, known_domains)

    # Mark all as processed
    all_domains = [d["domain"] for d in pending]
    mark_discoveries_processed(d1_client, all_domains)

    # Cleanup stale entries
    cleanup_stale_discoveries(d1_client)

    return confirmed

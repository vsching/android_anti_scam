"""Weekly report CLI — selects top alert and triggers FCM notification.

Queries D1 for alerts from the last 7 days, selects the highest-severity
alert (breaking ties by report_count), and calls POST /api/alerts/notify.

Usage:
    python -m src.weekly_report [--dry-run]
"""

from __future__ import annotations

import asyncio
import logging
from datetime import datetime, timezone
from typing import Any

import click
import httpx

from src.cloudflare import CloudflareD1Client, D1Client
from src.config import PipelineConfig
from src.utils.retry import retry_async

logger = logging.getLogger(__name__)

# Severity ranking: higher number = more severe
SEVERITY_RANK = {
    "critical": 4,
    "high": 3,
    "medium": 2,
    "low": 1,
}


def fetch_recent_alerts(d1_client: D1Client, days: int = 7) -> list[dict[str, Any]]:
    """Fetch alerts created within the last `days` days from D1."""
    sql = """
        SELECT id, title, description, scam_type, severity, region,
               state, report_count, source_url, created_at, updated_at
        FROM alerts
        WHERE created_at >= datetime('now', ?)
        ORDER BY created_at DESC
    """
    try:
        rows = d1_client.query(sql, [f"-{days} days"])
        logger.info("Fetched %d alerts from last %d days", len(rows), days)
        return rows
    except Exception as exc:
        logger.error("Failed to fetch recent alerts: %s", exc)
        raise RuntimeError(f"D1 query failed: {exc}") from exc


def select_top_alert(alerts: list[dict[str, Any]]) -> dict[str, Any] | None:
    """Select the alert with the highest severity, then highest report_count.

    Returns None if the list is empty.
    """
    if not alerts:
        return None

    def sort_key(alert: dict[str, Any]) -> tuple[int, int]:
        severity = (alert.get("severity") or "low").lower()
        rank = SEVERITY_RANK.get(severity, 0)
        count = alert.get("report_count") or 0
        return (rank, count)

    return max(alerts, key=sort_key)


@retry_async(max_attempts=3, base_delay=1.0)
async def notify_alert(
    api_base_url: str,
    alert_id: str,
    pipeline_secret: str,
) -> dict[str, Any]:
    """Call POST /api/alerts/notify with the selected alert_id.

    Retries on 5xx and connection errors (via decorator).
    """
    url = f"{api_base_url.rstrip('/')}/api/alerts/notify"
    async with httpx.AsyncClient(timeout=30.0) as client:
        response = await client.post(
            url,
            json={"alert_id": alert_id},
            headers={"X-Pipeline-Key": pipeline_secret},
        )
        response.raise_for_status()
        return response.json()


async def run_weekly_report(
    d1_client: D1Client,
    config: PipelineConfig,
    dry_run: bool = False,
) -> dict[str, Any] | None:
    """Execute the weekly report pipeline.

    1. Query alerts from last 7 days.
    2. Select the top alert.
    3. Call POST /api/alerts/notify (unless dry-run).

    Returns the notification response or alert info on dry-run.
    """
    alerts = fetch_recent_alerts(d1_client)
    if not alerts:
        logger.info("No alerts found in the last 7 days — skipping notification")
        return None

    top_alert = select_top_alert(alerts)
    if top_alert is None:
        return None

    logger.info(
        "Selected alert: id=%s title=%s severity=%s report_count=%s",
        top_alert.get("id"),
        top_alert.get("title"),
        top_alert.get("severity"),
        top_alert.get("report_count"),
    )

    if dry_run:
        logger.info("[DRY RUN] Would notify for alert_id=%s", top_alert["id"])
        return {"dry_run": True, "alert": top_alert}

    result = await notify_alert(
        config.api_base_url,
        top_alert["id"],
        config.pipeline_secret,
    )
    logger.info("Notification sent: %s", result)
    return result


@click.command()
@click.option("--dry-run", is_flag=True, help="Print selected alert without sending notification.")
def main(dry_run: bool) -> None:
    """Weekly report: select top alert and trigger FCM notification."""
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s: %(message)s",
    )

    config = PipelineConfig.from_env()
    d1_client = CloudflareD1Client(
        api_token=config.cloudflare_api_token,
        account_id=config.cloudflare_account_id,
        database_id=config.cloudflare_d1_database_id,
    )

    result = asyncio.run(run_weekly_report(d1_client, config, dry_run=dry_run))
    if result:
        click.echo(f"Result: {result}")
    else:
        click.echo("No alerts to report.")


if __name__ == "__main__":
    main()

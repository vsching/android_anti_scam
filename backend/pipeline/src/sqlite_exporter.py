"""Export scam domains to a SQLite database file."""

from __future__ import annotations

import sqlite3
from datetime import datetime, timezone
from typing import Any


def export_to_sqlite(
    domains: list[dict[str, Any]],
    output_path: str,
    version: str | None = None,
) -> str:
    """Export domain records to a SQLite database file.

    Args:
        domains: List of dicts with keys: domain, verdict, reason, source, confidence.
        output_path: File path for the output SQLite file.
        version: Version string (e.g., "2026-03-17"). Defaults to today's date.

    Returns:
        The output_path for convenience.
    """
    if version is None:
        version = datetime.now(timezone.utc).strftime("%Y-%m-%d")

    conn = sqlite3.connect(output_path)
    try:
        cursor = conn.cursor()

        # Create domains table
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS domains (
                domain TEXT PRIMARY KEY,
                verdict TEXT NOT NULL,
                reason TEXT,
                source TEXT,
                confidence REAL
            )
        """)

        # Create metadata table
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS metadata (
                key TEXT PRIMARY KEY,
                value TEXT
            )
        """)

        # Insert domains
        cursor.executemany(
            """
            INSERT OR REPLACE INTO domains (domain, verdict, reason, source, confidence)
            VALUES (?, ?, ?, ?, ?)
            """,
            [
                (
                    d["domain"],
                    d.get("verdict", "dangerous"),
                    d.get("reason", ""),
                    d.get("source", ""),
                    d.get("confidence", 1.0),
                )
                for d in domains
            ],
        )

        # Insert metadata
        build_time = datetime.now(timezone.utc).isoformat()
        cursor.executemany(
            "INSERT OR REPLACE INTO metadata (key, value) VALUES (?, ?)",
            [
                ("version", version),
                ("build_timestamp", build_time),
                ("domain_count", str(len(domains))),
            ],
        )

        conn.commit()
    finally:
        conn.close()

    return output_path

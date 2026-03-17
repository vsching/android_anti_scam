"""Tests for SQLite export functionality."""

import os
import sqlite3
import tempfile

import pytest

from src.sqlite_exporter import export_to_sqlite


@pytest.fixture
def sample_domains():
    return [
        {
            "domain": "phishing-site.com",
            "verdict": "dangerous",
            "reason": "Listed in phishing feed",
            "source": "phishing_database",
            "confidence": 1.0,
        },
        {
            "domain": "scam-bank.xyz",
            "verdict": "dangerous",
            "reason": "Bank impersonation",
            "source": "scam_blocklist",
            "confidence": 0.95,
        },
        {
            "domain": "fake-login.top",
            "verdict": "dangerous",
            "reason": "Credential harvesting",
            "source": "phishing_army",
            "confidence": 0.9,
        },
    ]


@pytest.fixture
def sqlite_path():
    fd, path = tempfile.mkstemp(suffix=".sqlite")
    os.close(fd)
    yield path
    if os.path.exists(path):
        os.unlink(path)


class TestSQLiteExporter:
    def test_creates_valid_sqlite(self, sample_domains, sqlite_path):
        export_to_sqlite(sample_domains, sqlite_path, version="2026-03-17")
        conn = sqlite3.connect(sqlite_path)
        cursor = conn.cursor()
        cursor.execute("SELECT count(*) FROM domains")
        assert cursor.fetchone()[0] == 3
        conn.close()

    def test_correct_schema(self, sample_domains, sqlite_path):
        export_to_sqlite(sample_domains, sqlite_path)
        conn = sqlite3.connect(sqlite_path)
        cursor = conn.cursor()
        cursor.execute("PRAGMA table_info(domains)")
        columns = {row[1]: row[2] for row in cursor.fetchall()}
        assert "domain" in columns
        assert "verdict" in columns
        assert "reason" in columns
        assert "source" in columns
        assert "confidence" in columns
        conn.close()

    def test_queryable_by_domain(self, sample_domains, sqlite_path):
        export_to_sqlite(sample_domains, sqlite_path)
        conn = sqlite3.connect(sqlite_path)
        cursor = conn.cursor()
        cursor.execute(
            "SELECT * FROM domains WHERE domain = ?", ("phishing-site.com",)
        )
        row = cursor.fetchone()
        assert row is not None
        assert row[0] == "phishing-site.com"
        assert row[1] == "dangerous"
        conn.close()

    def test_metadata_table_exists(self, sample_domains, sqlite_path):
        export_to_sqlite(sample_domains, sqlite_path, version="2026-03-17")
        conn = sqlite3.connect(sqlite_path)
        cursor = conn.cursor()
        cursor.execute("SELECT value FROM metadata WHERE key = 'version'")
        assert cursor.fetchone()[0] == "2026-03-17"
        cursor.execute("SELECT value FROM metadata WHERE key = 'domain_count'")
        assert cursor.fetchone()[0] == "3"
        cursor.execute(
            "SELECT value FROM metadata WHERE key = 'build_timestamp'"
        )
        assert cursor.fetchone()[0] is not None
        conn.close()

    def test_idempotent_rerun(self, sample_domains, sqlite_path):
        export_to_sqlite(sample_domains, sqlite_path, version="2026-03-17")
        export_to_sqlite(sample_domains, sqlite_path, version="2026-03-17")
        conn = sqlite3.connect(sqlite_path)
        cursor = conn.cursor()
        cursor.execute("SELECT count(*) FROM domains")
        assert cursor.fetchone()[0] == 3
        conn.close()

    def test_empty_domains(self, sqlite_path):
        export_to_sqlite([], sqlite_path, version="2026-03-17")
        conn = sqlite3.connect(sqlite_path)
        cursor = conn.cursor()
        cursor.execute("SELECT count(*) FROM domains")
        assert cursor.fetchone()[0] == 0
        cursor.execute("SELECT value FROM metadata WHERE key = 'domain_count'")
        assert cursor.fetchone()[0] == "0"
        conn.close()

    def test_returns_output_path(self, sample_domains, sqlite_path):
        result = export_to_sqlite(sample_domains, sqlite_path)
        assert result == sqlite_path

    def test_domain_is_primary_key(self, sample_domains, sqlite_path):
        export_to_sqlite(sample_domains, sqlite_path)
        conn = sqlite3.connect(sqlite_path)
        cursor = conn.cursor()
        # Duplicate should be handled by INSERT OR REPLACE
        cursor.execute(
            "INSERT OR REPLACE INTO domains VALUES (?, ?, ?, ?, ?)",
            ("phishing-site.com", "safe", "updated", "manual", 0.5),
        )
        conn.commit()
        cursor.execute("SELECT count(*) FROM domains")
        assert cursor.fetchone()[0] == 3  # still 3, not 4
        conn.close()

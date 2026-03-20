"""Tests for weekly report alert selection, dry-run, and notification."""

from __future__ import annotations

import asyncio
from dataclasses import dataclass
from unittest.mock import AsyncMock, MagicMock, patch

import httpx
import pytest

from src.weekly_report import (
    fetch_recent_alerts,
    notify_alert,
    run_weekly_report,
    select_top_alert,
)


class MockD1Client:
    """Mock D1 client for testing."""

    def __init__(self, query_results=None, should_fail=False):
        self._query_results = query_results or []
        self._should_fail = should_fail

    def query(self, sql, params=None):
        if self._should_fail:
            raise Exception("D1 API error")
        return self._query_results

    def execute(self, sql, params=None):
        if self._should_fail:
            raise Exception("D1 API error")


@dataclass
class FakeConfig:
    """Minimal config for testing."""

    pipeline_secret: str = "test-secret"
    cloudflare_account_id: str = "test-account"
    cloudflare_d1_database_id: str = "test-db"
    cloudflare_api_token: str = "test-token"
    api_base_url: str = "https://api.example.com"


# -- Alert fixtures --

ALERT_HIGH = {
    "id": "alert-1",
    "title": "Banking Scam Alert",
    "description": "Fake banking SMS",
    "scam_type": "phishing",
    "severity": "high",
    "region": "MY",
    "state": None,
    "report_count": 50,
    "source_url": None,
    "created_at": "2026-03-15T00:00:00Z",
    "updated_at": "2026-03-15T00:00:00Z",
}

ALERT_CRITICAL = {
    "id": "alert-2",
    "title": "Critical Investment Fraud",
    "description": "Ponzi scheme targeting seniors",
    "scam_type": "investment",
    "severity": "critical",
    "region": "MY",
    "state": None,
    "report_count": 10,
    "source_url": None,
    "created_at": "2026-03-14T00:00:00Z",
    "updated_at": "2026-03-14T00:00:00Z",
}

ALERT_LOW = {
    "id": "alert-3",
    "title": "Minor Spam",
    "description": "Low priority",
    "scam_type": "spam",
    "severity": "low",
    "region": "SG",
    "state": None,
    "report_count": 5,
    "source_url": None,
    "created_at": "2026-03-13T00:00:00Z",
    "updated_at": "2026-03-13T00:00:00Z",
}

ALERT_HIGH_MORE_REPORTS = {
    "id": "alert-4",
    "title": "Another High Severity",
    "description": "Same severity, more reports",
    "scam_type": "phishing",
    "severity": "high",
    "region": "MY",
    "state": None,
    "report_count": 100,
    "source_url": None,
    "created_at": "2026-03-15T00:00:00Z",
    "updated_at": "2026-03-15T00:00:00Z",
}


class TestSelectTopAlert:
    def test_empty_list_returns_none(self):
        assert select_top_alert([]) is None

    def test_single_alert(self):
        result = select_top_alert([ALERT_HIGH])
        assert result["id"] == "alert-1"

    def test_highest_severity_wins(self):
        result = select_top_alert([ALERT_HIGH, ALERT_CRITICAL, ALERT_LOW])
        assert result["id"] == "alert-2"  # critical > high > low

    def test_same_severity_highest_report_count_wins(self):
        result = select_top_alert([ALERT_HIGH, ALERT_HIGH_MORE_REPORTS])
        assert result["id"] == "alert-4"  # same severity, more reports

    def test_missing_severity_treated_as_low(self):
        alert_no_severity = {**ALERT_HIGH, "id": "no-sev", "severity": None}
        result = select_top_alert([alert_no_severity, ALERT_LOW])
        # Both effectively "low" rank, but ALERT_LOW has report_count=5
        # and alert_no_severity has report_count=50
        assert result["id"] == "no-sev"

    def test_unknown_severity_ranked_lowest(self):
        alert_unknown = {**ALERT_HIGH, "id": "unk", "severity": "unknown"}
        result = select_top_alert([alert_unknown, ALERT_LOW])
        # "unknown" maps to rank 0, "low" maps to rank 1
        assert result["id"] == "alert-3"


class TestFetchRecentAlerts:
    def test_returns_alerts(self):
        client = MockD1Client(query_results=[ALERT_HIGH, ALERT_LOW])
        result = fetch_recent_alerts(client)
        assert len(result) == 2

    def test_empty_results(self):
        client = MockD1Client(query_results=[])
        result = fetch_recent_alerts(client)
        assert result == []

    def test_api_failure_returns_empty(self):
        client = MockD1Client(should_fail=True)
        result = fetch_recent_alerts(client)
        assert result == []


class TestRunWeeklyReportDryRun:
    def test_dry_run_does_not_call_notify(self):
        client = MockD1Client(query_results=[ALERT_CRITICAL])
        config = FakeConfig()

        with patch("src.weekly_report.notify_alert") as mock_notify:
            result = asyncio.run(
                run_weekly_report(client, config, dry_run=True)
            )
            mock_notify.assert_not_called()

        assert result is not None
        assert result["dry_run"] is True
        assert result["alert"]["id"] == "alert-2"

    def test_no_alerts_returns_none(self):
        client = MockD1Client(query_results=[])
        config = FakeConfig()

        result = asyncio.run(run_weekly_report(client, config, dry_run=True))
        assert result is None


class TestRunWeeklyReport:
    def test_calls_notify_with_top_alert(self):
        client = MockD1Client(query_results=[ALERT_HIGH, ALERT_CRITICAL])
        config = FakeConfig()

        with patch("src.weekly_report.notify_alert", new_callable=AsyncMock) as mock_notify:
            mock_notify.return_value = {"sent": True, "topics": ["scam_alerts_MY"]}
            result = asyncio.run(
                run_weekly_report(client, config, dry_run=False)
            )
            mock_notify.assert_called_once_with(
                "https://api.example.com",
                "alert-2",  # critical alert selected
                "test-secret",
            )

        assert result == {"sent": True, "topics": ["scam_alerts_MY"]}


class TestPipelineSecretHeader:
    def test_notify_sends_pipeline_key_header(self):
        """Verify the X-Pipeline-Key header is sent in the POST request."""
        with patch("src.weekly_report.httpx.AsyncClient") as MockClient:
            mock_client = AsyncMock()
            mock_response = MagicMock()
            mock_response.status_code = 200
            mock_response.raise_for_status = MagicMock()
            mock_response.json.return_value = {"sent": True}
            mock_client.post.return_value = mock_response
            mock_client.__aenter__ = AsyncMock(return_value=mock_client)
            mock_client.__aexit__ = AsyncMock(return_value=False)
            MockClient.return_value = mock_client

            result = asyncio.run(
                notify_alert(
                    "https://api.example.com",
                    "alert-123",
                    "my-secret-key",
                )
            )

            mock_client.post.assert_called_once_with(
                "https://api.example.com/api/alerts/notify",
                json={"alert_id": "alert-123"},
                headers={"X-Pipeline-Key": "my-secret-key"},
            )
            assert result == {"sent": True}

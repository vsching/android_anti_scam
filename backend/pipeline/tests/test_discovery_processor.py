"""Tests for discovery processor D1 integration."""

from unittest.mock import MagicMock, call

import pytest

from src.discovery_processor import (
    cleanup_stale_discoveries,
    cross_reference_discoveries,
    fetch_pending_discoveries,
    mark_discoveries_processed,
    process_discoveries,
)


class MockD1Client:
    """Mock D1 client for testing."""

    def __init__(self, query_results=None, should_fail=False):
        self._query_results = query_results or []
        self._should_fail = should_fail
        self.executed_queries: list[tuple[str, list]] = []

    def query(self, sql, params=None):
        if self._should_fail:
            raise Exception("D1 API error")
        return self._query_results

    def execute(self, sql, params=None):
        if self._should_fail:
            raise Exception("D1 API error")
        self.executed_queries.append((sql, params or []))


class TestFetchPendingDiscoveries:
    def test_returns_rows(self):
        rows = [
            {
                "domain": "suspicious.com",
                "verdict": "suspicious",
                "reason": "Typosquatting",
                "check_count": 5,
                "confidence": 0.8,
                "created_at": "2026-03-16",
                "last_seen_at": "2026-03-17",
            }
        ]
        client = MockD1Client(query_results=rows)
        result = fetch_pending_discoveries(client)
        assert len(result) == 1
        assert result[0]["domain"] == "suspicious.com"

    def test_empty_results(self):
        client = MockD1Client(query_results=[])
        result = fetch_pending_discoveries(client)
        assert result == []

    def test_api_failure_returns_empty(self):
        client = MockD1Client(should_fail=True)
        result = fetch_pending_discoveries(client)
        assert result == []


class TestCrossReferenceDiscoveries:
    def test_confirmed_in_sources(self):
        pending = [
            {"domain": "known-scam.com", "check_count": 3},
            {"domain": "unknown.com", "check_count": 1},
        ]
        known = {"known-scam.com", "other-scam.com"}
        confirmed, unconfirmed = cross_reference_discoveries(pending, known)
        assert len(confirmed) == 1
        assert confirmed[0]["domain"] == "known-scam.com"
        assert len(unconfirmed) == 1
        assert unconfirmed[0]["domain"] == "unknown.com"

    def test_all_confirmed(self):
        pending = [{"domain": "a.com"}, {"domain": "b.com"}]
        known = {"a.com", "b.com"}
        confirmed, unconfirmed = cross_reference_discoveries(pending, known)
        assert len(confirmed) == 2
        assert len(unconfirmed) == 0

    def test_none_confirmed(self):
        pending = [{"domain": "x.com"}]
        known = {"y.com"}
        confirmed, unconfirmed = cross_reference_discoveries(pending, known)
        assert len(confirmed) == 0
        assert len(unconfirmed) == 1

    def test_empty_pending(self):
        confirmed, unconfirmed = cross_reference_discoveries([], {"a.com"})
        assert confirmed == []
        assert unconfirmed == []


class TestMarkDiscoveriesProcessed:
    def test_marks_domains(self):
        client = MockD1Client()
        mark_discoveries_processed(client, ["a.com", "b.com"])
        assert len(client.executed_queries) == 2

    def test_empty_list_no_queries(self):
        client = MockD1Client()
        mark_discoveries_processed(client, [])
        assert len(client.executed_queries) == 0

    def test_failure_continues(self):
        """Should log error but not raise on individual failures."""
        client = MockD1Client(should_fail=True)
        # Should not raise
        mark_discoveries_processed(client, ["a.com"])


class TestCleanupStaleDiscoveries:
    def test_executes_cleanup(self):
        client = MockD1Client()
        cleanup_stale_discoveries(client, max_age_days=30)
        assert len(client.executed_queries) == 1

    def test_failure_returns_zero(self):
        client = MockD1Client(should_fail=True)
        result = cleanup_stale_discoveries(client)
        assert result == 0


class TestProcessDiscoveries:
    def test_full_pipeline(self):
        pending = [
            {"domain": "confirmed.com", "check_count": 5},
            {"domain": "not-confirmed.com", "check_count": 1},
        ]
        client = MockD1Client(query_results=pending)
        known = {"confirmed.com"}
        result = process_discoveries(client, known)
        assert len(result) == 1
        assert result[0]["domain"] == "confirmed.com"
        # Should have executed mark-processed and cleanup queries
        assert len(client.executed_queries) >= 2

    def test_no_pending(self):
        client = MockD1Client(query_results=[])
        result = process_discoveries(client, {"a.com"})
        assert result == []

    def test_idempotent(self):
        """Running twice with same data produces same result."""
        pending = [{"domain": "x.com", "check_count": 1}]
        known = {"x.com"}
        client1 = MockD1Client(query_results=pending)
        result1 = process_discoveries(client1, known)
        client2 = MockD1Client(query_results=pending)
        result2 = process_discoveries(client2, known)
        assert len(result1) == len(result2)

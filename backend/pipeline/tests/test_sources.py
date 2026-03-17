"""Tests for source fetch adapters and failure handling."""

from unittest.mock import MagicMock, patch

import httpx
import pytest

from src.sources import (
    FetchResult,
    PhishingArmySource,
    PhishingDatabaseSource,
    ScamBlocklistSource,
    _fetch_text_list,
    fetch_all_sources,
)


class MockTransport(httpx.BaseTransport):
    """Mock transport for httpx testing."""

    def __init__(self, response_text="", status_code=200, error=None):
        self._response_text = response_text
        self._status_code = status_code
        self._error = error

    def handle_request(self, request):
        if self._error:
            raise self._error
        return httpx.Response(
            status_code=self._status_code,
            text=self._response_text,
            request=request,
        )


class TestFetchTextList:
    def test_successful_fetch(self):
        client = httpx.Client(
            transport=MockTransport("domain1.com\ndomain2.com\ndomain3.com")
        )
        result = _fetch_text_list(
            "http://test.example.com/list.txt", "test", client=client, max_retries=1
        )
        assert result.success is True
        assert len(result.domains) == 3
        assert result.source == "test"
        client.close()

    def test_filters_comments(self):
        client = httpx.Client(
            transport=MockTransport("# comment\ndomain1.com\n# another\ndomain2.com")
        )
        result = _fetch_text_list(
            "http://test.example.com/list.txt", "test", client=client, max_retries=1
        )
        assert result.success is True
        assert len(result.domains) == 2
        client.close()

    def test_filters_empty_lines(self):
        client = httpx.Client(
            transport=MockTransport("domain1.com\n\n\ndomain2.com\n")
        )
        result = _fetch_text_list(
            "http://test.example.com/list.txt", "test", client=client, max_retries=1
        )
        assert len(result.domains) == 2
        client.close()

    def test_http_404_failure(self):
        client = httpx.Client(
            transport=MockTransport(status_code=404)
        )
        result = _fetch_text_list(
            "http://test.example.com/list.txt", "test", client=client, max_retries=1
        )
        assert result.success is False
        assert result.error is not None
        client.close()

    def test_network_error(self):
        client = httpx.Client(
            transport=MockTransport(error=httpx.ConnectError("Connection refused"))
        )
        result = _fetch_text_list(
            "http://test.example.com/list.txt", "test", client=client, max_retries=1
        )
        assert result.success is False
        assert "Connection refused" in result.error
        client.close()

    def test_malformed_data_still_returns(self):
        # Malformed but parseable - just returns raw lines
        client = httpx.Client(
            transport=MockTransport("not a domain\n<html>bad</html>")
        )
        result = _fetch_text_list(
            "http://test.example.com/list.txt", "test", client=client, max_retries=1
        )
        assert result.success is True
        assert len(result.domains) == 2  # raw lines returned; normalize later
        client.close()


class TestScamBlocklistSource:
    def test_adguard_format_parsing(self):
        content = "||scam-domain.com^\n||another-scam.xyz^\n||plain-domain.net"
        client = httpx.Client(transport=MockTransport(content))
        source = ScamBlocklistSource()
        result = source.fetch(client=client)
        assert result.success is True
        assert "scam-domain.com" in result.domains
        assert "another-scam.xyz" in result.domains
        assert "plain-domain.net" in result.domains
        client.close()


class TestPhishingDatabaseSource:
    def test_source_name(self):
        source = PhishingDatabaseSource()
        assert source.name == "phishing_database"


class TestPhishingArmySource:
    def test_source_name(self):
        source = PhishingArmySource()
        assert source.name == "phishing_army"


class TestFetchAllSources:
    def test_graceful_degradation(self):
        """Pipeline continues even when some sources fail."""

        class SuccessSource:
            @property
            def name(self):
                return "success_source"

            def fetch(self, client=None):
                return FetchResult(
                    source="success_source",
                    domains=["good-domain.com"],
                    success=True,
                )

        class FailSource:
            @property
            def name(self):
                return "fail_source"

            def fetch(self, client=None):
                raise Exception("Network error")

        results = fetch_all_sources(
            sources=[SuccessSource(), FailSource()],
        )
        assert len(results) == 2

        success_results = [r for r in results if r.success]
        fail_results = [r for r in results if not r.success]
        assert len(success_results) == 1
        assert len(fail_results) == 1
        assert success_results[0].domains == ["good-domain.com"]

    def test_all_sources_fail(self):
        class FailSource:
            @property
            def name(self):
                return "fail"

            def fetch(self, client=None):
                return FetchResult(source="fail", success=False, error="down")

        results = fetch_all_sources(sources=[FailSource(), FailSource()])
        assert all(not r.success for r in results)

    def test_empty_source_list(self):
        results = fetch_all_sources(sources=[])
        assert results == []

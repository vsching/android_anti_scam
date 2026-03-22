"""Tests for source fetch adapters and failure handling."""

from unittest.mock import MagicMock, patch

import httpx
import pytest

from src.sources import (
    FetchResult,
    OpenPhishSource,
    PhishingArmySource,
    PhishingDatabaseSource,
    ScamBlocklistSource,
    URLhausSource,
    _apply_domain_extraction,
    _extract_domain,
    _fetch_text_list,
    fetch_all_sources,
    get_default_sources,
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


    def test_adguard_format_without_trailing_caret(self):
        """The ||domain format (without ^) should also be parsed correctly."""
        content = "||no-caret-domain.com\n||with-caret.org^"
        client = httpx.Client(transport=MockTransport(content))
        source = ScamBlocklistSource()
        result = source.fetch(client=client)
        assert result.success is True
        assert "no-caret-domain.com" in result.domains
        assert "with-caret.org" in result.domains
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


class TestFetchTextListFallback:
    """Tests for multi-URL fallback support in _fetch_text_list."""

    def test_primary_succeeds(self):
        """When primary URL works, no fallback needed."""
        client = httpx.Client(
            transport=MockTransport("domain1.com\ndomain2.com")
        )
        result = _fetch_text_list(
            ["http://primary.example.com", "http://fallback.example.com"],
            "test",
            client=client,
            max_retries=1,
        )
        assert result.success is True
        assert len(result.domains) == 2
        assert result.error is None  # no fallback warning
        client.close()

    def test_fallback_used_on_primary_failure(self):
        """When primary fails, fallback URL is tried."""
        call_count = 0

        class FallbackTransport(httpx.BaseTransport):
            def handle_request(self, request):
                nonlocal call_count
                call_count += 1
                if "primary" in str(request.url):
                    raise httpx.ConnectError("Connection refused")
                return httpx.Response(
                    status_code=200,
                    text="fallback-domain.com",
                    request=request,
                )

        client = httpx.Client(transport=FallbackTransport())
        result = _fetch_text_list(
            ["http://primary.example.com", "http://fallback.example.com"],
            "test",
            client=client,
            max_retries=1,
        )
        assert result.success is True
        assert result.domains == ["fallback-domain.com"]
        assert result.error is not None  # fallback warning
        assert "fallback" in result.error.lower()
        client.close()

    def test_all_urls_fail(self):
        """When all URLs fail, result is failure."""
        client = httpx.Client(
            transport=MockTransport(error=httpx.ConnectError("down"))
        )
        result = _fetch_text_list(
            ["http://url1.example.com", "http://url2.example.com"],
            "test",
            client=client,
            max_retries=1,
        )
        assert result.success is False
        client.close()


class TestOpenPhishSource:
    def test_source_name(self):
        source = OpenPhishSource()
        assert source.name == "openphish"

    def test_url_to_domain_parsing(self):
        """Full URLs should be parsed down to just the domain."""
        content = (
            "https://evil.com/phish/page\n"
            "http://www.bad-site.org/login.php\n"
            "https://scam.example.net/path?q=1\n"
        )
        client = httpx.Client(transport=MockTransport(content))
        source = OpenPhishSource()
        result = source.fetch(client=client)
        assert result.success is True
        assert "evil.com" in result.domains
        assert "bad-site.org" in result.domains  # www. stripped
        assert "scam.example.net" in result.domains
        client.close()

    def test_empty_lines_ignored(self):
        content = "https://evil.com/page\n\n\nhttps://bad.com/x\n"
        client = httpx.Client(transport=MockTransport(content))
        source = OpenPhishSource()
        result = source.fetch(client=client)
        assert result.success is True
        assert len(result.domains) == 2
        client.close()


class TestURLhausSource:
    def test_source_name(self):
        source = URLhausSource()
        assert source.name == "urlhaus"

    def test_comment_filtering(self):
        """Lines starting with # should be skipped."""
        content = (
            "# URLhaus database dump\n"
            "# Last updated: 2026-01-01\n"
            "http://malware.example.com/payload.exe\n"
            "https://dropper.bad.net/dl\n"
        )
        client = httpx.Client(transport=MockTransport(content))
        source = URLhausSource()
        result = source.fetch(client=client)
        assert result.success is True
        assert len(result.domains) == 2
        assert "malware.example.com" in result.domains
        assert "dropper.bad.net" in result.domains
        client.close()

    def test_url_to_domain_parsing(self):
        content = "http://evil.com:8080/malware\nhttps://www.bad.org/payload\n"
        client = httpx.Client(transport=MockTransport(content))
        source = URLhausSource()
        result = source.fetch(client=client)
        assert result.success is True
        assert "evil.com" in result.domains
        assert "bad.org" in result.domains  # www. stripped
        client.close()


class TestExtractDomain:
    def test_simple_url(self):
        assert _extract_domain("https://evil.com/phish/page") == "evil.com"

    def test_www_stripped(self):
        assert _extract_domain("http://www.example.com/path") == "example.com"

    def test_port_stripped(self):
        assert _extract_domain("http://evil.com:8080/path") == "evil.com"

    def test_empty_string(self):
        assert _extract_domain("") is None

    def test_bare_domain(self):
        assert _extract_domain("evil.com") == "evil.com"


class TestGetDefaultSources:
    def test_returns_five_sources(self):
        sources = get_default_sources()
        assert len(sources) == 5

    def test_source_names(self):
        sources = get_default_sources()
        names = {s.name for s in sources}
        assert names == {
            "phishing_database",
            "scam_blocklist",
            "phishing_army",
            "openphish",
            "urlhaus",
        }

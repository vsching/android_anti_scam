"""Tests for domain normalization and validation."""

import pytest

from src.normalize import deduplicate, normalize_domain, validate_domain_format


class TestNormalizeDomain:
    """Tests for normalize_domain function."""

    def test_simple_domain(self):
        assert normalize_domain("example.com") == "example.com"

    def test_uppercase(self):
        assert normalize_domain("EXAMPLE.COM") == "example.com"

    def test_mixed_case(self):
        assert normalize_domain("Example.Com") == "example.com"

    def test_strip_http(self):
        assert normalize_domain("http://example.com") == "example.com"

    def test_strip_https(self):
        assert normalize_domain("https://example.com") == "example.com"

    def test_strip_path(self):
        assert normalize_domain("https://example.com/path/to/page") == "example.com"

    def test_strip_query(self):
        assert normalize_domain("https://example.com?foo=bar") == "example.com"

    def test_strip_fragment(self):
        assert normalize_domain("https://example.com#section") == "example.com"

    def test_strip_www(self):
        assert normalize_domain("www.example.com") == "example.com"

    def test_strip_www_with_protocol(self):
        assert normalize_domain("https://www.example.com") == "example.com"

    def test_strip_path_query_fragment(self):
        assert (
            normalize_domain("https://www.example.com/p?q=1#s") == "example.com"
        )

    def test_domain_with_subdomain(self):
        assert normalize_domain("sub.example.com") == "sub.example.com"

    def test_reject_ipv4(self):
        assert normalize_domain("192.168.1.1") is None

    def test_reject_ipv4_url(self):
        assert normalize_domain("http://192.168.1.1/path") is None

    def test_reject_localhost(self):
        assert normalize_domain("localhost") is None

    def test_reject_localhost_url(self):
        assert normalize_domain("http://localhost:8080/path") is None

    def test_empty_string(self):
        assert normalize_domain("") is None

    def test_whitespace(self):
        assert normalize_domain("   ") is None

    def test_strip_trailing_dots(self):
        assert normalize_domain("example.com.") == "example.com"

    def test_reject_single_label(self):
        assert normalize_domain("example") is None

    def test_reject_invalid_tld_numbers(self):
        assert normalize_domain("example.123") is None

    def test_domain_with_port(self):
        assert normalize_domain("example.com:8080") == "example.com"

    def test_url_with_port(self):
        assert normalize_domain("https://example.com:443/path") == "example.com"

    def test_domain_with_path_no_protocol(self):
        assert normalize_domain("example.com/login") == "example.com"

    def test_malaysian_domain(self):
        assert normalize_domain("maybank2u.com.my") == "maybank2u.com.my"

    def test_gov_domain(self):
        assert normalize_domain("hasil.gov.my") == "hasil.gov.my"

    def test_multiple_subdomains(self):
        assert (
            normalize_domain("a.b.c.example.com") == "a.b.c.example.com"
        )

    def test_hyphenated_domain(self):
        assert normalize_domain("my-site.example.com") == "my-site.example.com"

    def test_punycode(self):
        # xn-- domains should be accepted as valid
        assert normalize_domain("xn--nxasmq6b.com") == "xn--nxasmq6b.com"


class TestValidateDomainFormat:
    """Tests for validate_domain_format function."""

    def test_valid_domain(self):
        assert validate_domain_format("example.com") is True

    def test_valid_subdomain(self):
        assert validate_domain_format("sub.example.com") is True

    def test_reject_ip(self):
        assert validate_domain_format("192.168.1.1") is False

    def test_reject_localhost(self):
        assert validate_domain_format("localhost") is False

    def test_reject_empty(self):
        assert validate_domain_format("") is False

    def test_reject_no_tld(self):
        assert validate_domain_format("example") is False


class TestDeduplicate:
    """Tests for deduplicate function."""

    def test_no_duplicates(self):
        assert deduplicate(["a.com", "b.com"]) == ["a.com", "b.com"]

    def test_with_duplicates(self):
        assert deduplicate(["a.com", "b.com", "a.com"]) == ["a.com", "b.com"]

    def test_preserves_order(self):
        result = deduplicate(["c.com", "a.com", "b.com", "a.com"])
        assert result == ["c.com", "a.com", "b.com"]

    def test_empty_list(self):
        assert deduplicate([]) == []

    def test_all_same(self):
        assert deduplicate(["a.com", "a.com", "a.com"]) == ["a.com"]

"""Adapters for external scam domain feed sources.

Each source returns a list of raw domain/URL strings and a source tag.
Sources handle retry, timeout, and graceful failure (log and continue).
"""

from __future__ import annotations

import logging
import time
from dataclasses import dataclass, field
from typing import Protocol
from urllib.parse import urlparse

import httpx

logger = logging.getLogger(__name__)

DEFAULT_TIMEOUT = 30.0  # seconds
MAX_RETRIES = 3
RETRY_BACKOFF = 2.0  # seconds


@dataclass
class FetchResult:
    """Result from fetching a domain source."""

    source: str
    domains: list[str] = field(default_factory=list)
    success: bool = True
    error: str | None = None


class SourceAdapter(Protocol):
    """Protocol for domain source adapters."""

    @property
    def name(self) -> str: ...

    def fetch(self, client: httpx.Client | None = None) -> FetchResult: ...


def _fetch_text_list(
    urls: str | list[str],
    source_name: str,
    client: httpx.Client | None = None,
    timeout: float = DEFAULT_TIMEOUT,
    max_retries: int = MAX_RETRIES,
) -> FetchResult:
    """Fetch a text file containing one domain per line.

    Accepts a single URL string or a list of URLs (tried in order as fallbacks).
    Handles retry with exponential backoff, timeout, and graceful failure.
    """
    if isinstance(urls, str):
        urls = [urls]

    should_close = False
    if client is None:
        client = httpx.Client(timeout=timeout)
        should_close = True

    try:
        used_fallback = False
        for url_index, url in enumerate(urls):
            last_error: str | None = None
            for attempt in range(max_retries):
                try:
                    response = client.get(url, timeout=timeout)
                    response.raise_for_status()
                    lines = response.text.strip().splitlines()
                    # Filter out comments and empty lines
                    domains = [
                        line.strip()
                        for line in lines
                        if line.strip() and not line.strip().startswith("#")
                    ]
                    logger.info(
                        "Fetched %d domains from %s", len(domains), source_name
                    )
                    warning = None
                    if used_fallback:
                        warning = (
                            f"Primary URL failed; used fallback URL index {url_index}"
                        )
                        logger.warning(
                            "%s: %s", source_name, warning
                        )
                    return FetchResult(
                        source=source_name, domains=domains, error=warning
                    )
                except (httpx.HTTPStatusError, httpx.RequestError) as exc:
                    last_error = str(exc)
                    logger.warning(
                        "Attempt %d/%d failed for %s (URL %d): %s",
                        attempt + 1,
                        max_retries,
                        source_name,
                        url_index,
                        last_error,
                    )
                    if attempt < max_retries - 1:
                        time.sleep(RETRY_BACKOFF * (attempt + 1))

            # All retries exhausted for this URL, try next
            logger.warning(
                "All retries exhausted for %s URL %d, trying next fallback",
                source_name,
                url_index,
            )
            used_fallback = True

        logger.error("All URLs exhausted for %s: %s", source_name, last_error)
        return FetchResult(
            source=source_name, success=False, error=last_error
        )
    finally:
        if should_close:
            client.close()


def _extract_domain(url_string: str) -> str | None:
    """Extract domain from a full URL, stripping protocol, path, and www prefix."""
    url_string = url_string.strip()
    if not url_string:
        return None
    # Ensure there's a scheme for urlparse
    if not url_string.startswith(("http://", "https://")):
        url_string = "http://" + url_string
    try:
        parsed = urlparse(url_string)
        hostname = parsed.hostname
        if not hostname:
            return None
        # Strip www. prefix
        if hostname.startswith("www."):
            hostname = hostname[4:]
        return hostname if hostname else None
    except Exception:
        return None


class PhishingDatabaseSource:
    """Adapter for github.com/Phishing-Database/Phishing.Database."""

    URLS = [
        "https://raw.githubusercontent.com/"
        "Phishing-Database/Phishing.Database/master/phishing-domains-ACTIVE.txt"
    ]

    @property
    def name(self) -> str:
        return "phishing_database"

    def fetch(self, client: httpx.Client | None = None) -> FetchResult:
        return _fetch_text_list(self.URLS, self.name, client=client)


class ScamBlocklistSource:
    """Adapter for github.com/jarelllama/Scam-Blocklist."""

    URLS = [
        "https://raw.githubusercontent.com/"
        "jarelllama/Scam-Blocklist/main/lists/wildcard_domains/scams.txt"
    ]

    @property
    def name(self) -> str:
        return "scam_blocklist"

    def fetch(self, client: httpx.Client | None = None) -> FetchResult:
        result = _fetch_text_list(self.URLS, self.name, client=client)
        if result.success:
            # AdGuard format: lines like ||domain.com^
            cleaned: list[str] = []
            for line in result.domains:
                if line.startswith("||") and line.endswith("^"):
                    cleaned.append(line[2:-1])
                elif line.startswith("||"):
                    cleaned.append(line[2:])
                else:
                    cleaned.append(line)
            result.domains = cleaned
        return result


class PhishingArmySource:
    """Adapter for phishing.army blocklist."""

    URLS = ["https://phishing.army/download/phishing_army_blocklist.txt"]

    @property
    def name(self) -> str:
        return "phishing_army"

    def fetch(self, client: httpx.Client | None = None) -> FetchResult:
        return _fetch_text_list(self.URLS, self.name, client=client)


class OpenPhishSource:
    """Adapter for OpenPhish phishing feed.

    OpenPhish returns full URLs; we extract the domain from each line.
    """

    URLS = ["https://openphish.com/feed.txt"]

    @property
    def name(self) -> str:
        return "openphish"

    def fetch(self, client: httpx.Client | None = None) -> FetchResult:
        result = _fetch_text_list(self.URLS, self.name, client=client)
        if result.success:
            domains: list[str] = []
            for line in result.domains:
                domain = _extract_domain(line)
                if domain:
                    domains.append(domain)
            result.domains = domains
        return result


class URLhausSource:
    """Adapter for URLhaus recent malicious URL feed.

    URLhaus returns full URLs with comment lines starting with #.
    Comments are already filtered by _fetch_text_list; we extract domains.
    """

    URLS = ["https://urlhaus.abuse.ch/downloads/text_recent/"]

    @property
    def name(self) -> str:
        return "urlhaus"

    def fetch(self, client: httpx.Client | None = None) -> FetchResult:
        result = _fetch_text_list(self.URLS, self.name, client=client)
        if result.success:
            domains: list[str] = []
            for line in result.domains:
                domain = _extract_domain(line)
                if domain:
                    domains.append(domain)
            result.domains = domains
        return result


def get_default_sources() -> list[SourceAdapter]:
    """Return the default set of source adapters (5 sources)."""
    return [
        PhishingDatabaseSource(),
        ScamBlocklistSource(),
        PhishingArmySource(),
        OpenPhishSource(),
        URLhausSource(),
    ]


def fetch_all_sources(
    sources: list[SourceAdapter] | None = None,
    client: httpx.Client | None = None,
) -> list[FetchResult]:
    """Fetch domains from all sources, continuing on individual failures.

    Returns a list of FetchResults (some may have success=False).
    """
    if sources is None:
        sources = get_default_sources()

    results: list[FetchResult] = []
    for source in sources:
        try:
            result = source.fetch(client=client)
            results.append(result)
        except Exception as exc:
            logger.error(
                "Unexpected error fetching %s: %s", source.name, exc
            )
            results.append(
                FetchResult(source=source.name, success=False, error=str(exc))
            )

    return results

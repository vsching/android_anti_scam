"""Adapters for external scam domain feed sources.

Each source returns a list of raw domain/URL strings and a source tag.
Sources handle retry, timeout, and graceful failure (log and continue).
"""

from __future__ import annotations

import logging
import time
from dataclasses import dataclass, field
from typing import Protocol

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
    url: str,
    source_name: str,
    client: httpx.Client | None = None,
    timeout: float = DEFAULT_TIMEOUT,
    max_retries: int = MAX_RETRIES,
) -> FetchResult:
    """Fetch a text file containing one domain per line.

    Handles retry with exponential backoff, timeout, and graceful failure.
    """
    should_close = False
    if client is None:
        client = httpx.Client(timeout=timeout)
        should_close = True

    try:
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
                return FetchResult(source=source_name, domains=domains)
            except (httpx.HTTPStatusError, httpx.RequestError) as exc:
                last_error = str(exc)
                logger.warning(
                    "Attempt %d/%d failed for %s: %s",
                    attempt + 1,
                    max_retries,
                    source_name,
                    last_error,
                )
                if attempt < max_retries - 1:
                    time.sleep(RETRY_BACKOFF * (attempt + 1))

        logger.error("All retries exhausted for %s: %s", source_name, last_error)
        return FetchResult(
            source=source_name, success=False, error=last_error
        )
    finally:
        if should_close:
            client.close()


class PhishingDatabaseSource:
    """Adapter for github.com/Phishing-Database/Phishing.Database."""

    URL = (
        "https://raw.githubusercontent.com/"
        "Phishing-Database/Phishing.Database/master/phishing-domains-ACTIVE.txt"
    )

    @property
    def name(self) -> str:
        return "phishing_database"

    def fetch(self, client: httpx.Client | None = None) -> FetchResult:
        return _fetch_text_list(self.URL, self.name, client=client)


class ScamBlocklistSource:
    """Adapter for github.com/jarelllama/Scam-Blocklist."""

    URL = (
        "https://raw.githubusercontent.com/"
        "jarelllama/Scam-Blocklist/main/lists/adguard/scams.txt"
    )

    @property
    def name(self) -> str:
        return "scam_blocklist"

    def fetch(self, client: httpx.Client | None = None) -> FetchResult:
        result = _fetch_text_list(self.URL, self.name, client=client)
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

    URL = "https://phishing.army/download/phishing_army_blocklist.txt"

    @property
    def name(self) -> str:
        return "phishing_army"

    def fetch(self, client: httpx.Client | None = None) -> FetchResult:
        return _fetch_text_list(self.URL, self.name, client=client)


def get_default_sources() -> list[SourceAdapter]:
    """Return the default set of source adapters."""
    return [
        PhishingDatabaseSource(),
        ScamBlocklistSource(),
        PhishingArmySource(),
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

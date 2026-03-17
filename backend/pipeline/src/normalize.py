"""Domain normalization and validation utilities."""

from __future__ import annotations

import re
from urllib.parse import urlparse

# Valid TLD pattern: at least 2 alpha chars
_TLD_RE = re.compile(r"^[a-z]{2,}$")
# Basic hostname shape: labels separated by dots, each label alphanumeric + hyphens
_HOSTNAME_RE = re.compile(
    r"^(?:[a-z0-9](?:[a-z0-9\-]{0,61}[a-z0-9])?\.)+[a-z]{2,}$"
)
# IPv4 pattern
_IPV4_RE = re.compile(r"^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$")
# IPv6 bracket pattern (as seen in URLs)
_IPV6_BRACKET_RE = re.compile(r"^\[.*\]$")


def normalize_domain(raw: str) -> str | None:
    """Normalize a raw URL or domain string to a canonical domain.

    Returns None if the input is not a valid domain (IPs, localhost, etc.).
    """
    raw = raw.strip()
    if not raw:
        return None

    # If it looks like a URL (has ://), parse it
    if "://" in raw:
        try:
            parsed = urlparse(raw)
            hostname = parsed.hostname
        except Exception:
            return None
    else:
        # Could be domain or domain/path
        # Strip any path/query/fragment
        hostname = raw.split("/")[0].split("?")[0].split("#")[0]

    if not hostname:
        return None

    # Lowercase
    hostname = hostname.lower().strip(".")

    # Strip www prefix
    if hostname.startswith("www."):
        hostname = hostname[4:]

    # Strip port if present
    if ":" in hostname:
        hostname = hostname.rsplit(":", 1)[0]

    # Reject IPs
    if _IPV4_RE.match(hostname):
        return None
    if _IPV6_BRACKET_RE.match(hostname):
        return None

    # Reject localhost
    if hostname in ("localhost", "localhost.localdomain"):
        return None

    # Validate hostname shape
    if not _HOSTNAME_RE.match(hostname):
        return None

    # Validate TLD (last label must be alphabetic, >= 2 chars)
    tld = hostname.rsplit(".", 1)[-1]
    if not _TLD_RE.match(tld):
        return None

    return hostname


def validate_domain_format(domain: str) -> bool:
    """Check if a domain string has valid format (already normalized)."""
    if not domain:
        return False
    if _IPV4_RE.match(domain):
        return False
    if domain in ("localhost", "localhost.localdomain"):
        return False
    if not _HOSTNAME_RE.match(domain):
        return False
    tld = domain.rsplit(".", 1)[-1]
    return bool(_TLD_RE.match(tld))


def deduplicate(domains: list[str]) -> list[str]:
    """Remove duplicate domains preserving first-seen order."""
    seen: set[str] = set()
    result: list[str] = []
    for d in domains:
        if d not in seen:
            seen.add(d)
            result.append(d)
    return result

"""Pipeline configuration from environment variables.

Reads and validates required env vars for the weekly report
and other pipeline tasks.
"""

from __future__ import annotations

import os


class ConfigError(Exception):
    """Raised when a required environment variable is missing."""


def _require_env(name: str) -> str:
    """Return env var value or raise ConfigError."""
    value = os.environ.get(name, "").strip()
    if not value:
        raise ConfigError(f"Required environment variable {name} is not set")
    return value


class PipelineConfig:
    """Pipeline configuration loaded from environment variables."""

    def __init__(self) -> None:
        self.pipeline_secret = _require_env("PIPELINE_SECRET")
        self.cloudflare_account_id = _require_env("CLOUDFLARE_ACCOUNT_ID")
        self.cloudflare_d1_database_id = _require_env("CLOUDFLARE_D1_DATABASE_ID")
        self.cloudflare_api_token = _require_env("CLOUDFLARE_API_TOKEN")
        self.api_base_url = _require_env("API_BASE_URL")
        self.cloudflare_r2_access_key_id = _require_env("CLOUDFLARE_R2_ACCESS_KEY_ID")
        self.cloudflare_r2_secret_access_key = _require_env(
            "CLOUDFLARE_R2_SECRET_ACCESS_KEY"
        )
        self.cloudflare_r2_bucket = _require_env("CLOUDFLARE_R2_BUCKET")
        self.cloudflare_kv_namespace_id = _require_env("CLOUDFLARE_KV_NAMESPACE_ID")

    @classmethod
    def from_env(cls) -> PipelineConfig:
        """Create config from current environment variables."""
        return cls()

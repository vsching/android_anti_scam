"""Cloudflare R2 and KV adapter for pipeline uploads.

Uses dependency injection so tests can mock without real credentials.
"""

from __future__ import annotations

import json
import logging
from typing import Any, Protocol

import boto3
import httpx
from botocore.config import Config as BotoConfig

logger = logging.getLogger(__name__)


class R2Client(Protocol):
    """Protocol for R2 upload operations."""

    def upload_file(self, local_path: str, key: str) -> None: ...

    def upload_bytes(self, data: bytes, key: str) -> None: ...

    def upload_json(self, obj: Any, key: str) -> None: ...

    def download_json(self, key: str) -> Any | None: ...


class KVClient(Protocol):
    """Protocol for KV write operations."""

    def write(self, key: str, value: str) -> None: ...

    def write_bulk(self, entries: list[dict[str, str]]) -> None: ...


class D1Client(Protocol):
    """Protocol for D1 query operations via Cloudflare API."""

    def query(self, sql: str, params: list[Any] | None = None) -> list[dict]: ...

    def execute(self, sql: str, params: list[Any] | None = None) -> None: ...


class CloudflareR2Client:
    """Real R2 client using S3-compatible API via boto3."""

    def __init__(
        self,
        account_id: str,
        access_key_id: str,
        secret_access_key: str,
        bucket_name: str,
    ):
        self._bucket_name = bucket_name
        self._s3 = boto3.client(
            "s3",
            endpoint_url=f"https://{account_id}.r2.cloudflarestorage.com",
            aws_access_key_id=access_key_id,
            aws_secret_access_key=secret_access_key,
            config=BotoConfig(
                retries={"max_attempts": 3, "mode": "adaptive"},
            ),
            region_name="auto",
        )

    def upload_file(self, local_path: str, key: str) -> None:
        logger.info("Uploading file %s to R2 key %s", local_path, key)
        self._s3.upload_file(local_path, self._bucket_name, key)

    def upload_bytes(self, data: bytes, key: str) -> None:
        logger.info("Uploading %d bytes to R2 key %s", len(data), key)
        self._s3.put_object(Bucket=self._bucket_name, Key=key, Body=data)

    def upload_json(self, obj: Any, key: str) -> None:
        data = json.dumps(obj, indent=2).encode("utf-8")
        self.upload_bytes(data, key)

    def download_json(self, key: str) -> Any | None:
        try:
            response = self._s3.get_object(Bucket=self._bucket_name, Key=key)
            return json.loads(response["Body"].read().decode("utf-8"))
        except Exception:
            logger.warning("Failed to download %s from R2", key)
            return None


class CloudflareKVClient:
    """Real KV client using Cloudflare API."""

    def __init__(
        self,
        api_token: str,
        account_id: str,
        namespace_id: str,
    ):
        self._api_token = api_token
        self._account_id = account_id
        self._namespace_id = namespace_id
        self._base_url = (
            f"https://api.cloudflare.com/client/v4/accounts/{account_id}"
            f"/storage/kv/namespaces/{namespace_id}"
        )

    def _headers(self) -> dict[str, str]:
        return {"Authorization": f"Bearer {self._api_token}"}

    def write(self, key: str, value: str) -> None:

        url = f"{self._base_url}/values/{key}"
        response = httpx.put(url, headers=self._headers(), content=value)
        response.raise_for_status()

    def write_bulk(self, entries: list[dict[str, str]]) -> None:

        url = f"{self._base_url}/bulk"
        response = httpx.put(url, headers=self._headers(), json=entries)
        response.raise_for_status()


class CloudflareD1Client:
    """Real D1 client using Cloudflare API."""

    def __init__(
        self,
        api_token: str,
        account_id: str,
        database_id: str,
    ):
        self._api_token = api_token
        self._account_id = account_id
        self._database_id = database_id
        self._base_url = (
            f"https://api.cloudflare.com/client/v4/accounts/{account_id}"
            f"/d1/database/{database_id}"
        )

    def _headers(self) -> dict[str, str]:
        return {
            "Authorization": f"Bearer {self._api_token}",
            "Content-Type": "application/json",
        }

    def query(self, sql: str, params: list[Any] | None = None) -> list[dict]:

        url = f"{self._base_url}/query"
        body: dict[str, Any] = {"sql": sql}
        if params:
            body["params"] = params
        response = httpx.post(url, headers=self._headers(), json=body)
        response.raise_for_status()
        data = response.json()
        if data.get("result") and len(data["result"]) > 0:
            return data["result"][0].get("results", [])
        return []

    def execute(self, sql: str, params: list[Any] | None = None) -> None:

        url = f"{self._base_url}/query"
        body: dict[str, Any] = {"sql": sql}
        if params:
            body["params"] = params
        response = httpx.post(url, headers=self._headers(), json=body)
        response.raise_for_status()

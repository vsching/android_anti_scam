"""Tests for Cloudflare R2/KV adapter error paths and retry behavior."""

import json
from unittest.mock import MagicMock, patch

import pytest

from src.cloudflare import (
    CloudflareD1Client,
    CloudflareKVClient,
    CloudflareR2Client,
)


class TestCloudflareR2Client:
    """Test R2 client behavior (mocked boto3)."""

    @patch("src.cloudflare.boto3")
    def test_upload_file(self, mock_boto3):
        mock_s3 = MagicMock()
        mock_boto3.client.return_value = mock_s3

        client = CloudflareR2Client(
            account_id="test-account",
            access_key_id="test-key",
            secret_access_key="test-secret",
            bucket_name="test-bucket",
        )
        client.upload_file("/tmp/test.json", "artifacts/test.json")
        mock_s3.upload_file.assert_called_once_with(
            "/tmp/test.json", "test-bucket", "artifacts/test.json"
        )

    @patch("src.cloudflare.boto3")
    def test_upload_bytes(self, mock_boto3):
        mock_s3 = MagicMock()
        mock_boto3.client.return_value = mock_s3

        client = CloudflareR2Client(
            account_id="test-account",
            access_key_id="test-key",
            secret_access_key="test-secret",
            bucket_name="test-bucket",
        )
        client.upload_bytes(b"hello", "test-key")
        mock_s3.put_object.assert_called_once_with(
            Bucket="test-bucket", Key="test-key", Body=b"hello"
        )

    @patch("src.cloudflare.boto3")
    def test_upload_json(self, mock_boto3):
        mock_s3 = MagicMock()
        mock_boto3.client.return_value = mock_s3

        client = CloudflareR2Client(
            account_id="test-account",
            access_key_id="test-key",
            secret_access_key="test-secret",
            bucket_name="test-bucket",
        )
        client.upload_json({"version": "1.0"}, "latest.json")
        mock_s3.put_object.assert_called_once()
        call_kwargs = mock_s3.put_object.call_args
        assert call_kwargs[1]["Key"] == "latest.json"
        body = call_kwargs[1]["Body"]
        parsed = json.loads(body)
        assert parsed["version"] == "1.0"

    @patch("src.cloudflare.boto3")
    def test_download_json_success(self, mock_boto3):
        mock_s3 = MagicMock()
        mock_boto3.client.return_value = mock_s3
        mock_body = MagicMock()
        mock_body.read.return_value = b'{"version": "2026-03-17"}'
        mock_s3.get_object.return_value = {"Body": mock_body}

        client = CloudflareR2Client(
            account_id="test-account",
            access_key_id="test-key",
            secret_access_key="test-secret",
            bucket_name="test-bucket",
        )
        result = client.download_json("latest.json")
        assert result["version"] == "2026-03-17"

    @patch("src.cloudflare.boto3")
    def test_download_json_not_found(self, mock_boto3):
        mock_s3 = MagicMock()
        mock_boto3.client.return_value = mock_s3
        from botocore.exceptions import ClientError
        mock_s3.get_object.side_effect = ClientError(
            {"Error": {"Code": "NoSuchKey", "Message": "Not found"}},
            "GetObject",
        )

        client = CloudflareR2Client(
            account_id="test-account",
            access_key_id="test-key",
            secret_access_key="test-secret",
            bucket_name="test-bucket",
        )
        result = client.download_json("nonexistent.json")
        assert result is None

    @patch("src.cloudflare.boto3")
    def test_upload_failure_raises(self, mock_boto3):
        mock_s3 = MagicMock()
        mock_boto3.client.return_value = mock_s3
        mock_s3.upload_file.side_effect = Exception("Network timeout")

        client = CloudflareR2Client(
            account_id="test-account",
            access_key_id="test-key",
            secret_access_key="test-secret",
            bucket_name="test-bucket",
        )
        with pytest.raises(Exception, match="Network timeout"):
            client.upload_file("/tmp/test.json", "key")

    @patch("src.cloudflare.boto3")
    def test_credentials_not_logged(self, mock_boto3, caplog):
        """Verify credentials don't appear in log output."""
        mock_s3 = MagicMock()
        mock_boto3.client.return_value = mock_s3

        import logging

        with caplog.at_level(logging.DEBUG):
            client = CloudflareR2Client(
                account_id="SENSITIVE_ACCOUNT",
                access_key_id="SENSITIVE_KEY_ID",
                secret_access_key="SUPER_SECRET_KEY",
                bucket_name="test-bucket",
            )
            client.upload_bytes(b"test", "test-key")

        log_text = caplog.text
        assert "SENSITIVE_KEY_ID" not in log_text
        assert "SUPER_SECRET_KEY" not in log_text


class TestCloudflareKVClient:
    """Test KV client behavior (mocked httpx)."""

    def test_write(self):
        mock_response = MagicMock()
        mock_response.raise_for_status = MagicMock()

        client = CloudflareKVClient(
            api_token="test-token",
            account_id="test-account",
            namespace_id="test-ns",
        )
        with patch("httpx.put", return_value=mock_response) as mock_put:
            client.write("key1", "value1")
            mock_put.assert_called_once()

    def test_write_bulk(self):
        mock_response = MagicMock()
        mock_response.raise_for_status = MagicMock()

        client = CloudflareKVClient(
            api_token="test-token",
            account_id="test-account",
            namespace_id="test-ns",
        )
        entries = [
            {"key": "k1", "value": "v1"},
            {"key": "k2", "value": "v2"},
        ]
        with patch("httpx.put", return_value=mock_response) as mock_put:
            client.write_bulk(entries)
            mock_put.assert_called_once()

    def test_auth_error(self):
        mock_response = MagicMock()
        mock_response.raise_for_status.side_effect = Exception("401 Unauthorized")

        client = CloudflareKVClient(
            api_token="bad-token",
            account_id="test-account",
            namespace_id="test-ns",
        )
        with patch("httpx.put", return_value=mock_response):
            with pytest.raises(Exception, match="401"):
                client.write("key", "value")


class TestCloudflareD1Client:
    """Test D1 client behavior (mocked httpx)."""

    def test_query(self):
        mock_response = MagicMock()
        mock_response.raise_for_status = MagicMock()
        mock_response.json.return_value = {
            "result": [{"results": [{"domain": "test.com"}]}]
        }

        client = CloudflareD1Client(
            api_token="test-token",
            account_id="test-account",
            database_id="test-db",
        )
        with patch("httpx.post", return_value=mock_response):
            results = client.query("SELECT * FROM pending_discoveries")
        assert len(results) == 1
        assert results[0]["domain"] == "test.com"

    def test_execute(self):
        mock_response = MagicMock()
        mock_response.raise_for_status = MagicMock()
        mock_response.json.return_value = {"result": []}

        client = CloudflareD1Client(
            api_token="test-token",
            account_id="test-account",
            database_id="test-db",
        )
        with patch("httpx.post", return_value=mock_response):
            # Should not raise
            client.execute("UPDATE pending_discoveries SET processed = 1")

    def test_network_timeout(self):
        client = CloudflareD1Client(
            api_token="test-token",
            account_id="test-account",
            database_id="test-db",
        )
        with patch("httpx.post", side_effect=Exception("Connection timeout")):
            with pytest.raises(Exception, match="timeout"):
                client.query("SELECT 1")

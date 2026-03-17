"""Tests for the seed database pipeline orchestration."""

import json
import os
import sqlite3
import tempfile
from unittest.mock import MagicMock

import pytest

from seed_database import _validate_anti_poisoning, run_pipeline
from src.sources import FetchResult


class MockSource:
    """Mock source adapter for testing."""

    def __init__(self, name: str, domains: list[str], should_fail: bool = False):
        self._name = name
        self._domains = domains
        self._should_fail = should_fail

    @property
    def name(self):
        return self._name

    def fetch(self, client=None):
        if self._should_fail:
            return FetchResult(source=self._name, success=False, error="mock failure")
        return FetchResult(source=self._name, domains=self._domains)


class MockR2Client:
    """Mock R2 client."""

    def __init__(self, existing_latest=None):
        self.uploaded_files: dict[str, str] = {}
        self.uploaded_bytes: dict[str, bytes] = {}
        self.uploaded_json: dict[str, object] = {}
        self._existing_latest = existing_latest

    def upload_file(self, local_path, key):
        self.uploaded_files[key] = local_path

    def upload_bytes(self, data, key):
        self.uploaded_bytes[key] = data

    def upload_json(self, obj, key):
        self.uploaded_json[key] = obj

    def download_json(self, key):
        if key == "latest.json" and self._existing_latest:
            return self._existing_latest
        return None


class MockKVClient:
    """Mock KV client."""

    def __init__(self):
        self.written: dict[str, str] = {}
        self.bulk_entries: list[dict] = []

    def write(self, key, value):
        self.written[key] = value

    def write_bulk(self, entries):
        self.bulk_entries.extend(entries)


@pytest.fixture
def output_dir():
    d = tempfile.mkdtemp(prefix="test-pipeline-")
    yield d
    # Cleanup
    import shutil
    shutil.rmtree(d, ignore_errors=True)


@pytest.fixture
def sample_sources():
    return [
        MockSource("source_a", [
            "phishing-site.com",
            "scam-bank.xyz",
            "fake-login.top",
        ]),
        MockSource("source_b", [
            "another-scam.net",
            "phishing-site.com",  # duplicate
        ]),
    ]


class TestRunPipeline:
    def test_basic_run(self, sample_sources, output_dir):
        result = run_pipeline(
            sources=sample_sources,
            output_dir=output_dir,
            skip_upload=True,
        )
        assert result["success"] is True
        assert result["domain_count"] == 4  # 5 raw - 1 duplicate
        assert result["sources_succeeded"] == 2

    def test_output_json_schema(self, sample_sources, output_dir):
        result = run_pipeline(
            sources=sample_sources,
            output_dir=output_dir,
            skip_upload=True,
        )
        full_json_path = os.path.join(
            output_dir, result["artifacts"]["full_json"]
        )
        with open(full_json_path) as f:
            data = json.load(f)

        assert isinstance(data, list)
        assert len(data) > 0
        for record in data:
            assert "domain" in record
            assert "verdict" in record
            assert record["verdict"] == "dangerous"
            assert "reason" in record
            assert "source" in record

    def test_deduplication(self, sample_sources, output_dir):
        result = run_pipeline(
            sources=sample_sources,
            output_dir=output_dir,
            skip_upload=True,
        )
        # phishing-site.com appears in both sources but should be counted once
        full_json_path = os.path.join(
            output_dir, result["artifacts"]["full_json"]
        )
        with open(full_json_path) as f:
            data = json.load(f)
        domains = [r["domain"] for r in data]
        assert len(domains) == len(set(domains))

    def test_sqlite_artifact_valid(self, sample_sources, output_dir):
        result = run_pipeline(
            sources=sample_sources,
            output_dir=output_dir,
            skip_upload=True,
        )
        sqlite_path = os.path.join(
            output_dir, result["artifacts"]["sqlite"]
        )
        conn = sqlite3.connect(sqlite_path)
        cursor = conn.cursor()
        cursor.execute("SELECT count(*) FROM domains")
        count = cursor.fetchone()[0]
        assert count == result["domain_count"]
        # Test queryability
        cursor.execute(
            "SELECT * FROM domains WHERE domain = ?", ("phishing-site.com",)
        )
        row = cursor.fetchone()
        assert row is not None
        conn.close()

    def test_bloom_artifact_exists(self, sample_sources, output_dir):
        result = run_pipeline(
            sources=sample_sources,
            output_dir=output_dir,
            skip_upload=True,
        )
        bloom_path = os.path.join(
            output_dir, result["artifacts"]["bloom"]
        )
        assert os.path.exists(bloom_path)
        assert os.path.getsize(bloom_path) > 0

    def test_manifest_artifact(self, sample_sources, output_dir):
        result = run_pipeline(
            sources=sample_sources,
            output_dir=output_dir,
            skip_upload=True,
        )
        version = result["version"]
        manifest_path = os.path.join(output_dir, f"manifest-{version}.json")
        with open(manifest_path) as f:
            manifest = json.load(f)
        assert manifest["version"] == version
        assert manifest["domain_count"] == result["domain_count"]
        assert "artifacts" in manifest
        for artifact_type in ("full_json", "delta_json", "sqlite", "bloom"):
            assert artifact_type in manifest["artifacts"]
            artifact = manifest["artifacts"][artifact_type]
            assert "sha256" in artifact
            assert "size_bytes" in artifact

    def test_allowlist_not_in_scam_list(self, output_dir):
        """Allowlisted domains should not appear in scam output."""
        sources = [
            MockSource("test", [
                "maybank2u.com.my",  # allowlisted
                "real-scam.com",
            ]),
        ]
        result = run_pipeline(
            sources=sources,
            output_dir=output_dir,
            skip_upload=True,
        )
        full_json_path = os.path.join(
            output_dir, result["artifacts"]["full_json"]
        )
        with open(full_json_path) as f:
            data = json.load(f)
        domains = {r["domain"] for r in data}
        assert "maybank2u.com.my" not in domains
        assert "real-scam.com" in domains

    def test_allowlist_kv_seeding(self, sample_sources, output_dir):
        kv_client = MockKVClient()
        result = run_pipeline(
            kv_client=kv_client,
            sources=sample_sources,
            output_dir=output_dir,
        )
        assert result["success"] is True
        assert len(kv_client.bulk_entries) > 0
        # Check all entries have allowlist prefix
        for entry in kv_client.bulk_entries:
            assert entry["key"].startswith("allowlist:")
            value = json.loads(entry["value"])
            assert value["verdict"] == "safe"

    def test_r2_upload(self, sample_sources, output_dir):
        r2_client = MockR2Client()
        result = run_pipeline(
            r2_client=r2_client,
            sources=sample_sources,
            output_dir=output_dir,
        )
        assert result["success"] is True
        assert len(r2_client.uploaded_files) > 0
        assert "latest.json" in r2_client.uploaded_json

    def test_all_sources_fail(self, output_dir):
        sources = [
            MockSource("fail1", [], should_fail=True),
            MockSource("fail2", [], should_fail=True),
        ]
        result = run_pipeline(
            sources=sources,
            output_dir=output_dir,
            skip_upload=True,
        )
        assert result["success"] is False

    def test_partial_source_failure(self, output_dir):
        sources = [
            MockSource("good", ["valid-domain.com"]),
            MockSource("bad", [], should_fail=True),
        ]
        result = run_pipeline(
            sources=sources,
            output_dir=output_dir,
            skip_upload=True,
        )
        assert result["success"] is True
        assert result["sources_succeeded"] == 1
        assert result["sources_failed"] == 1

    def test_idempotent_rerun(self, sample_sources, output_dir):
        result1 = run_pipeline(
            sources=sample_sources,
            output_dir=output_dir,
            skip_upload=True,
        )
        result2 = run_pipeline(
            sources=sample_sources,
            output_dir=output_dir,
            skip_upload=True,
        )
        assert result1["domain_count"] == result2["domain_count"]


class TestAntiPoisoningValidation:
    def test_pass_when_no_previous(self):
        results = [
            FetchResult(source="a", domains=["d1.com", "d2.com"]),
        ]
        assert _validate_anti_poisoning(results, 2, None, False) is True

    def test_fail_on_large_drop(self):
        results = [
            FetchResult(source="a", domains=["d1.com"]),
        ]
        # Previous had 100, now only 10 (90% drop)
        assert _validate_anti_poisoning(results, 10, 100, False) is False

    def test_pass_on_acceptable_drop(self):
        results = [
            FetchResult(source="a", domains=["d1.com"] * 60),
        ]
        # Previous had 100, now 60 (40% drop, below 50% threshold)
        assert _validate_anti_poisoning(results, 60, 100, False) is True

    def test_fail_on_single_source_domination(self):
        # Use unique domains per source — source "a" has 90 unique domains not in "b"
        a_domains = [f"a-{i}.com" for i in range(90)]
        b_domains = [f"b-{i}.com" for i in range(10)]
        results = [
            FetchResult(source="a", domains=a_domains),
            FetchResult(source="b", domains=b_domains),
        ]
        # Source "a" has 90% unique contribution of 100 total
        assert _validate_anti_poisoning(results, 100, None, False) is False

    def test_pass_with_balanced_sources(self):
        a_domains = [f"a-{i}.com" for i in range(50)]
        b_domains = [f"b-{i}.com" for i in range(50)]
        results = [
            FetchResult(source="a", domains=a_domains),
            FetchResult(source="b", domains=b_domains),
        ]
        assert _validate_anti_poisoning(results, 100, None, False) is True

    def test_pass_single_source_only(self):
        """Single source should not trigger domination check."""
        results = [
            FetchResult(source="a", domains=["d1.com"] * 100),
        ]
        assert _validate_anti_poisoning(results, 100, None, False) is True

    def test_force_overrides_gates(self):
        results = [
            FetchResult(source="a", domains=["d1.com"]),
        ]
        # Large drop but force=True
        assert _validate_anti_poisoning(results, 10, 100, True) is True

    def test_invalid_domain_format_rejected_in_pipeline(self, tmp_path):
        """IPs and localhost should be filtered out during normalization."""
        sources = [
            MockSource("test", [
                "192.168.1.1",
                "localhost",
                "valid-scam.com",
            ]),
        ]
        result = run_pipeline(
            sources=sources,
            output_dir=str(tmp_path),
            skip_upload=True,
        )
        assert result["success"] is True
        assert result["domain_count"] == 1  # only valid-scam.com

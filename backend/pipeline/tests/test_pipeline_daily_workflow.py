"""Tests for the pipeline-daily.yml GitHub Actions workflow."""

from __future__ import annotations

import os

import yaml


WORKFLOW_PATH = os.path.join(
    os.path.dirname(__file__),
    "..",
    "..",
    "..",
    ".github",
    "workflows",
    "pipeline-daily.yml",
)


def _load_workflow() -> dict:
    with open(WORKFLOW_PATH) as f:
        wf = yaml.safe_load(f)
    # PyYAML parses bare `on` as boolean True; normalize key access
    if True in wf and "on" not in wf:
        wf["on"] = wf.pop(True)
    return wf


class TestPipelineDailyWorkflow:
    def test_cron_schedule(self):
        wf = _load_workflow()
        schedules = wf["on"]["schedule"]
        crons = [s["cron"] for s in schedules]
        assert "0 19 * * *" in crons

    def test_workflow_dispatch_present(self):
        wf = _load_workflow()
        assert "workflow_dispatch" in wf["on"]

    def test_python_313_setup(self):
        wf = _load_workflow()
        steps = wf["jobs"]["pipeline"]["steps"]
        python_steps = [
            s for s in steps
            if s.get("uses", "").startswith("actions/setup-python@")
        ]
        assert len(python_steps) >= 1
        assert python_steps[0]["with"]["python-version"] == "3.13"

    def test_pip_install_step_present(self):
        wf = _load_workflow()
        steps = wf["jobs"]["pipeline"]["steps"]
        pip_steps = [
            s for s in steps
            if "pip install" in s.get("run", "")
        ]
        assert len(pip_steps) >= 1
        assert "requirements.txt" in pip_steps[0]["run"]

    def test_seed_database_run_step_present(self):
        wf = _load_workflow()
        steps = wf["jobs"]["pipeline"]["steps"]
        seed_steps = [
            s for s in steps
            if "seed_database.py" in s.get("run", "")
        ]
        assert len(seed_steps) >= 1

    def test_required_secrets_wired(self):
        wf = _load_workflow()
        steps = wf["jobs"]["pipeline"]["steps"]
        seed_steps = [
            s for s in steps
            if "seed_database.py" in s.get("run", "")
        ]
        assert len(seed_steps) >= 1
        env = seed_steps[0].get("env", {})
        required_secrets = [
            "CLOUDFLARE_API_TOKEN",
            "CLOUDFLARE_ACCOUNT_ID",
            "CLOUDFLARE_R2_ACCESS_KEY_ID",
            "CLOUDFLARE_R2_SECRET_ACCESS_KEY",
            "CLOUDFLARE_R2_BUCKET",
            "CLOUDFLARE_KV_NAMESPACE_ID",
            "CLOUDFLARE_D1_DATABASE_ID",
        ]
        for secret_name in required_secrets:
            assert secret_name in env, f"Missing secret: {secret_name}"

    def test_upload_artifact_step_present(self):
        wf = _load_workflow()
        steps = wf["jobs"]["pipeline"]["steps"]
        upload_steps = [
            s for s in steps
            if s.get("uses", "").startswith("actions/upload-artifact@")
        ]
        assert len(upload_steps) >= 1

    def test_runs_on_ubuntu_latest(self):
        wf = _load_workflow()
        assert wf["jobs"]["pipeline"]["runs-on"] == "ubuntu-latest"

    def test_smoke_test_step_present(self):
        wf = _load_workflow()
        steps = wf["jobs"]["pipeline"]["steps"]
        smoke_steps = [
            s for s in steps
            if "smoke" in s.get("name", "").lower()
            or "smoke-test" in s.get("run", "")
        ]
        assert len(smoke_steps) >= 1, "Missing smoke-test step in workflow"

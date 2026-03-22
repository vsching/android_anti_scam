"""Tests for pipeline configuration — source URL env var support."""

import os
from unittest.mock import patch

from src.config import get_sources_json


class TestGetSourcesJson:
    def test_returns_none_when_unset(self):
        with patch.dict(os.environ, {}, clear=True):
            # Ensure the var is not set
            os.environ.pop("PIPELINE_SOURCES_JSON", None)
            assert get_sources_json() is None

    def test_returns_none_for_empty_string(self):
        with patch.dict(os.environ, {"PIPELINE_SOURCES_JSON": ""}):
            assert get_sources_json() is None

    def test_returns_none_for_whitespace_only(self):
        with patch.dict(os.environ, {"PIPELINE_SOURCES_JSON": "   "}):
            assert get_sources_json() is None

    def test_returns_value_when_set(self):
        json_value = '[{"name": "custom", "urls": ["https://example.com/feed.txt"]}]'
        with patch.dict(os.environ, {"PIPELINE_SOURCES_JSON": json_value}):
            result = get_sources_json()
            assert result == json_value

    def test_strips_whitespace(self):
        json_value = '  [{"name": "x", "urls": ["http://a.com"]}]  '
        with patch.dict(os.environ, {"PIPELINE_SOURCES_JSON": json_value}):
            result = get_sources_json()
            assert result == json_value.strip()

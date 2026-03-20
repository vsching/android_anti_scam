"""Tests for the async retry decorator."""

from __future__ import annotations

import asyncio
from unittest.mock import AsyncMock, patch

import httpx
import pytest

from src.utils.retry import retry_async


@pytest.fixture
def mock_response_500():
    """Create a mock 500 HTTP response."""
    response = httpx.Response(500, request=httpx.Request("GET", "http://test"))
    return response


@pytest.fixture
def mock_response_400():
    """Create a mock 400 HTTP response."""
    response = httpx.Response(400, request=httpx.Request("GET", "http://test"))
    return response


class TestRetryAsync:
    def test_succeeds_on_first_attempt(self):
        call_count = 0

        @retry_async(max_attempts=3, base_delay=0.01)
        async def succeed():
            nonlocal call_count
            call_count += 1
            return "ok"

        result = asyncio.run(succeed())
        assert result == "ok"
        assert call_count == 1

    def test_retries_on_5xx(self, mock_response_500):
        call_count = 0

        @retry_async(max_attempts=3, base_delay=0.01)
        async def fail_then_succeed():
            nonlocal call_count
            call_count += 1
            if call_count < 3:
                raise httpx.HTTPStatusError(
                    "Server Error",
                    request=mock_response_500.request,
                    response=mock_response_500,
                )
            return "ok"

        result = asyncio.run(fail_then_succeed())
        assert result == "ok"
        assert call_count == 3

    def test_does_not_retry_on_4xx(self, mock_response_400):
        call_count = 0

        @retry_async(max_attempts=3, base_delay=0.01)
        async def fail_400():
            nonlocal call_count
            call_count += 1
            raise httpx.HTTPStatusError(
                "Bad Request",
                request=mock_response_400.request,
                response=mock_response_400,
            )

        with pytest.raises(httpx.HTTPStatusError):
            asyncio.run(fail_400())
        assert call_count == 1

    def test_retries_on_connect_error(self):
        call_count = 0

        @retry_async(max_attempts=3, base_delay=0.01)
        async def fail_connect():
            nonlocal call_count
            call_count += 1
            if call_count < 2:
                raise httpx.ConnectError("Connection refused")
            return "connected"

        result = asyncio.run(fail_connect())
        assert result == "connected"
        assert call_count == 2

    def test_retries_on_connect_timeout(self):
        call_count = 0

        @retry_async(max_attempts=3, base_delay=0.01)
        async def fail_timeout():
            nonlocal call_count
            call_count += 1
            if call_count < 2:
                raise httpx.ConnectTimeout("Timed out")
            return "done"

        result = asyncio.run(fail_timeout())
        assert result == "done"
        assert call_count == 2

    def test_raises_after_all_attempts_exhausted(self, mock_response_500):
        call_count = 0

        @retry_async(max_attempts=3, base_delay=0.01)
        async def always_fail():
            nonlocal call_count
            call_count += 1
            raise httpx.HTTPStatusError(
                "Server Error",
                request=mock_response_500.request,
                response=mock_response_500,
            )

        with pytest.raises(httpx.HTTPStatusError):
            asyncio.run(always_fail())
        assert call_count == 3

    def test_preserves_function_name(self):
        @retry_async()
        async def my_function():
            pass

        assert my_function.__name__ == "my_function"

    def test_exponential_backoff_timing(self):
        """Verify that delays increase exponentially."""
        delays: list[float] = []
        original_sleep = asyncio.sleep

        async def mock_sleep(delay):
            delays.append(delay)

        @retry_async(max_attempts=4, base_delay=1.0)
        async def always_fail():
            raise httpx.ConnectError("fail")

        with patch("src.utils.retry.asyncio.sleep", side_effect=mock_sleep):
            with pytest.raises(httpx.ConnectError):
                asyncio.run(always_fail())

        # 3 retries (4 attempts - 1 initial), delays: 1.0, 2.0, 4.0
        assert len(delays) == 3
        assert delays[0] == pytest.approx(1.0)
        assert delays[1] == pytest.approx(2.0)
        assert delays[2] == pytest.approx(4.0)

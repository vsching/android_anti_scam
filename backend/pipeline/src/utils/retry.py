"""Async retry decorator with exponential backoff.

Retries on 5xx HTTP responses and connection errors.
"""

from __future__ import annotations

import asyncio
import functools
import logging
from typing import Any, Callable, TypeVar

import httpx

logger = logging.getLogger(__name__)

F = TypeVar("F", bound=Callable[..., Any])

# Defaults
DEFAULT_MAX_ATTEMPTS = 3
DEFAULT_BASE_DELAY = 1.0  # seconds


def retry_async(
    max_attempts: int = DEFAULT_MAX_ATTEMPTS,
    base_delay: float = DEFAULT_BASE_DELAY,
) -> Callable[[F], F]:
    """Decorator that retries an async function with exponential backoff.

    Retries on:
      - httpx.HTTPStatusError with 5xx status codes
      - httpx.ConnectError / httpx.ConnectTimeout

    Args:
        max_attempts: Maximum number of attempts (including the first).
        base_delay: Base delay in seconds; doubles each retry.
    """

    def decorator(fn: F) -> F:
        @functools.wraps(fn)
        async def wrapper(*args: Any, **kwargs: Any) -> Any:
            last_exception: Exception | None = None
            for attempt in range(1, max_attempts + 1):
                try:
                    return await fn(*args, **kwargs)
                except httpx.HTTPStatusError as exc:
                    if exc.response.status_code < 500:
                        raise  # Don't retry 4xx errors
                    last_exception = exc
                except (httpx.ConnectError, httpx.ConnectTimeout) as exc:
                    last_exception = exc

                if attempt < max_attempts:
                    delay = base_delay * (2 ** (attempt - 1))
                    logger.warning(
                        "Attempt %d/%d for %s failed (%s), retrying in %.1fs",
                        attempt,
                        max_attempts,
                        fn.__name__,
                        last_exception,
                        delay,
                    )
                    await asyncio.sleep(delay)

            logger.error(
                "All %d attempts for %s exhausted", max_attempts, fn.__name__
            )
            raise last_exception  # type: ignore[misc]

        return wrapper  # type: ignore[return-value]

    return decorator

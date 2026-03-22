// Reusable cache helpers.
// KV cache: serialized JSON strings with metadata-based TTL tracking.
// Cache API key builder: shared between check and admin-cache routes.

/**
 * Build a Cache API key URL for a given domain.
 * Cache API requires a valid URL as key.
 */
export function cacheApiKey(domain: string): string {
  return `https://cache.safeanot.internal/check/${encodeURIComponent(domain)}`;
}

/** Default cache TTL in seconds (15 minutes). */
export const DEFAULT_CACHE_TTL = 900;

/** Metadata cache TTL in seconds (1 hour) — for latest version / size metadata. */
export const METADATA_CACHE_TTL = 3600;

/** Metadata stored alongside cached values in KV. */
interface CacheMetadata {
  cachedAt: number; // epoch ms
  ttl: number; // seconds
}

/**
 * Build a cache key for a filtered list endpoint.
 * Pattern: `{prefix}:{filter || 'all'}`
 */
export function cacheKey(prefix: string, filter?: string): string {
  return `${prefix}:${filter || 'all'}`;
}

/**
 * Read a cached value from KV.
 * Returns the parsed value if found and not expired, otherwise null.
 */
export async function cacheGet<T>(
  kv: KVNamespace,
  key: string,
): Promise<T | null> {
  const { value, metadata } = await kv.getWithMetadata<CacheMetadata>(key, 'text');

  if (value === null || metadata === null) {
    return null;
  }

  const age = Date.now() - metadata.cachedAt;
  if (age > metadata.ttl * 1000) {
    // Expired — treat as miss (KV will clean up via expirationTtl)
    return null;
  }

  try {
    return JSON.parse(value) as T;
  } catch {
    return null;
  }
}

/**
 * Write a value to KV cache with TTL metadata.
 * Uses both metadata-based TTL (for soft checks) and KV expirationTtl (for hard cleanup).
 */
export async function cachePut<T>(
  kv: KVNamespace,
  key: string,
  value: T,
  ttl: number = DEFAULT_CACHE_TTL,
): Promise<void> {
  const metadata: CacheMetadata = {
    cachedAt: Date.now(),
    ttl,
  };

  await kv.put(key, JSON.stringify(value), {
    metadata,
    expirationTtl: ttl,
  });
}

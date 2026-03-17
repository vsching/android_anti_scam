// R2 manifest helpers for reading latest.json and resolving artifact keys.
// latest.json is stored at the root of the DATA_BUCKET R2 bucket.

import { cacheGet, cachePut } from './cache';

/** Shape of latest.json stored in R2. */
export interface LatestManifest {
  version: string; // e.g. "2026-03-16"
  full_key: string;
  delta_key: string;
  bloom_key: string;
  full_size_kb: number;
  delta_size_kb: number;
  bloom_size_kb: number;
  sqlite_size_kb: number;
  domain_count: number;
  build_timestamp: string;
}

/** 1-hour cache TTL for manifest metadata. */
export const METADATA_CACHE_TTL = 3600;

const MANIFEST_CACHE_KEY = 'data:manifest:latest';

/**
 * Fetch the latest.json manifest from R2, with KV caching.
 * Returns null if the manifest does not exist in R2.
 */
export async function getManifest(
  bucket: R2Bucket,
  kv: KVNamespace,
): Promise<LatestManifest | null> {
  // Check KV cache first
  const cached = await cacheGet<LatestManifest>(kv, MANIFEST_CACHE_KEY);
  if (cached !== null) {
    return cached;
  }

  // Fetch from R2
  const obj = await bucket.get('latest.json');
  if (obj === null) {
    return null;
  }

  const text = await obj.text();
  const manifest = JSON.parse(text) as LatestManifest;

  // Cache in KV for 1 hour
  await cachePut(kv, MANIFEST_CACHE_KEY, manifest, METADATA_CACHE_TTL);

  return manifest;
}

/** Date format regex: YYYY-MM-DD */
const DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Validate that a string is a valid YYYY-MM-DD date.
 */
export function isValidDate(dateStr: string): boolean {
  if (!DATE_REGEX.test(dateStr)) {
    return false;
  }
  const d = new Date(dateStr + 'T00:00:00Z');
  return !isNaN(d.getTime());
}

/**
 * Calculate the number of days between two YYYY-MM-DD date strings.
 * Returns positive if date2 is after date1.
 */
export function daysBetween(date1: string, date2: string): number {
  const d1 = new Date(date1 + 'T00:00:00Z');
  const d2 = new Date(date2 + 'T00:00:00Z');
  return Math.round((d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
}

/** Maximum age in days for which a delta is considered valid. */
export const MAX_DELTA_AGE_DAYS = 7;

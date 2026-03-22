// Tests for GET /api/health endpoint.
// Covers: fresh data (ok), stale data (stale), missing manifest (unknown).

import { SELF, env } from 'cloudflare:test';
import { describe, it, expect, beforeEach } from 'vitest';

interface HealthResponse {
  version?: string;
  build_timestamp?: string;
  freshness_hours?: number;
  status: string;
  error?: string;
}

/** Build a manifest with a specific build_timestamp. */
function makeManifest(buildTimestamp: string) {
  return {
    version: '2026-03-20',
    full_key: 'domains-full-2026-03-20.sqlite',
    delta_key: 'domains-delta-2026-03-20.json',
    bloom_key: 'bloom-2026-03-20.bin',
    full_size_kb: 12000,
    delta_size_kb: 150,
    bloom_size_kb: 600,
    sqlite_size_kb: 15000,
    domain_count: 500000,
    build_timestamp: buildTimestamp,
  };
}

async function clearKVCache() {
  await env.VERDICTS.delete('data:manifest:latest');
}

describe('GET /api/health', () => {
  beforeEach(async () => {
    await clearKVCache();
  });

  it('returns status "ok" when data is fresh (< 48h)', async () => {
    // Build timestamp 1 hour ago
    const oneHourAgo = new Date(Date.now() - 1 * 3600 * 1000).toISOString();
    const manifest = makeManifest(oneHourAgo);
    await env.DATA_BUCKET.put('latest.json', JSON.stringify(manifest));

    const response = await SELF.fetch('http://localhost/api/health');
    expect(response.status).toBe(200);

    const body = await response.json<HealthResponse>();
    expect(body.status).toBe('ok');
    expect(body.version).toBe('2026-03-20');
    expect(body.build_timestamp).toBe(oneHourAgo);
    expect(body.freshness_hours).toBeDefined();
    expect(body.freshness_hours).toBeLessThanOrEqual(2);
  });

  it('returns status "stale" when data is old (> 48h)', async () => {
    // Build timestamp 72 hours ago
    const threeeDaysAgo = new Date(Date.now() - 72 * 3600 * 1000).toISOString();
    const manifest = makeManifest(threeeDaysAgo);
    await env.DATA_BUCKET.put('latest.json', JSON.stringify(manifest));

    const response = await SELF.fetch('http://localhost/api/health');
    expect(response.status).toBe(200);

    const body = await response.json<HealthResponse>();
    expect(body.status).toBe('stale');
    expect(body.freshness_hours).toBeGreaterThanOrEqual(71);
  });

  it('returns status "unknown" when manifest is missing', async () => {
    await env.DATA_BUCKET.delete('latest.json');

    const response = await SELF.fetch('http://localhost/api/health');
    expect(response.status).toBe(200);

    const body = await response.json<HealthResponse>();
    expect(body.status).toBe('unknown');
    expect(body.error).toBe('No manifest found');
  });

  it('returns JSON content type', async () => {
    const manifest = makeManifest(new Date().toISOString());
    await env.DATA_BUCKET.put('latest.json', JSON.stringify(manifest));

    const response = await SELF.fetch('http://localhost/api/health');
    expect(response.headers.get('Content-Type')).toBe('application/json');
    await response.text(); // consume body
  });
});

// Tests for GET /api/data/* endpoints.
// Covers latest metadata, full file streaming, delta streaming,
// bloom filter streaming, edge cases, and error handling.

import { SELF, env } from 'cloudflare:test';
import { describe, it, expect, beforeAll, beforeEach } from 'vitest';

/** Sample latest.json manifest for tests. */
const MANIFEST = {
  version: '2026-03-16',
  full_key: 'domains-full-2026-03-16.sqlite',
  delta_key: 'domains-delta-2026-03-16.json',
  bloom_key: 'bloom-2026-03-16.bin',
  full_size_kb: 12000,
  delta_size_kb: 150,
  bloom_size_kb: 600,
  sqlite_size_kb: 15000,
  domain_count: 500000,
  build_timestamp: '2026-03-16T05:00:00Z',
};

/** Sample delta JSON content. */
const DELTA_CONTENT = JSON.stringify({
  domains_added: ['evil.com', 'scam.net'],
  domains_removed: ['old-scam.com'],
  version: '2026-03-16',
});

/** Sample SQLite content (just bytes for testing). */
const FULL_DB_BYTES = [0x53, 0x51, 0x4c, 0x69, 0x74, 0x65];

/** Sample Bloom filter content. */
const BLOOM_BYTES = [0xde, 0xad, 0xbe, 0xef, 0x01, 0x02];

async function seedR2() {
  await env.DATA_BUCKET.put('latest.json', JSON.stringify(MANIFEST));
  await env.DATA_BUCKET.put(MANIFEST.full_key, new Uint8Array(FULL_DB_BYTES));
  await env.DATA_BUCKET.put(MANIFEST.delta_key, DELTA_CONTENT);
  await env.DATA_BUCKET.put(MANIFEST.bloom_key, new Uint8Array(BLOOM_BYTES));
}

async function clearKVCache() {
  await env.VERDICTS.delete('data:manifest:latest');
}

describe('GET /api/data/*', () => {
  beforeAll(async () => {
    await seedR2();
  });

  beforeEach(async () => {
    await clearKVCache();
  });

  describe('GET /api/data/latest', () => {
    it('returns metadata about the latest build', async () => {
      const response = await SELF.fetch('http://localhost/api/data/latest');
      expect(response.status).toBe(200);

      const data = await response.json<Record<string, unknown>>();
      expect(data.version).toBe('2026-03-16');
      expect(data.domain_count).toBe(500000);
      expect(data.full_size_kb).toBe(12000);
      expect(data.delta_size_kb).toBe(150);
      expect(data.bloom_size_kb).toBe(600);
      expect(data.build_timestamp).toBe('2026-03-16T05:00:00Z');
    });

    it('returns JSON content type', async () => {
      const response = await SELF.fetch('http://localhost/api/data/latest');
      const data = await response.text(); // consume body
      expect(response.headers.get('Content-Type')).toBe('application/json');
    });

    it('returns 404 when no manifest exists', async () => {
      // Remove manifest from R2
      await env.DATA_BUCKET.delete('latest.json');

      const response = await SELF.fetch('http://localhost/api/data/latest');
      expect(response.status).toBe(404);

      const data = await response.json<{ error: string }>();
      expect(data.error).toBe('No data available');

      // Restore
      await env.DATA_BUCKET.put('latest.json', JSON.stringify(MANIFEST));
    });
  });

  describe('GET /api/data/full', () => {
    it('streams the full SQLite database', async () => {
      const response = await SELF.fetch('http://localhost/api/data/full');
      expect(response.status).toBe(200);

      const buf = new Uint8Array(await response.arrayBuffer());
      expect(Array.from(buf)).toEqual(FULL_DB_BYTES);
    });

    it('sets correct content type and Content-Length for SQLite', async () => {
      const response = await SELF.fetch('http://localhost/api/data/full');
      expect(response.headers.get('Content-Type')).toBe('application/x-sqlite3');
      expect(response.headers.get('Content-Length')).toBe(
        FULL_DB_BYTES.length.toString(),
      );
      // Consume the body to avoid storage isolation issues
      await response.arrayBuffer();
    });

    it('returns 404 when R2 object is missing', async () => {
      await env.DATA_BUCKET.delete(MANIFEST.full_key);

      const response = await SELF.fetch('http://localhost/api/data/full');
      expect(response.status).toBe(404);
      await response.text(); // consume body

      // Restore
      await env.DATA_BUCKET.put(MANIFEST.full_key, new Uint8Array(FULL_DB_BYTES));
    });
  });

  describe('GET /api/data/delta', () => {
    it('streams the delta JSON for a valid since date', async () => {
      const response = await SELF.fetch(
        'http://localhost/api/data/delta?since=2026-03-15',
      );
      expect(response.status).toBe(200);

      const data = await response.json<Record<string, unknown>>();
      expect(data.domains_added).toBeDefined();
    });

    it('returns 400 when since parameter is missing', async () => {
      const response = await SELF.fetch('http://localhost/api/data/delta');
      expect(response.status).toBe(400);

      const data = await response.json<{ error: string }>();
      expect(data.error).toContain('Missing');
    });

    it('returns 400 for invalid date format', async () => {
      const response = await SELF.fetch(
        'http://localhost/api/data/delta?since=not-a-date',
      );
      expect(response.status).toBe(400);

      const data = await response.json<{ error: string }>();
      expect(data.error).toContain('Invalid date format');
    });

    it('returns 400 for bad date like 2026-13-45', async () => {
      const response = await SELF.fetch(
        'http://localhost/api/data/delta?since=2026-13-45',
      );
      expect(response.status).toBe(400);
      await response.text(); // consume body
    });

    it('returns empty delta for future since date', async () => {
      const response = await SELF.fetch(
        'http://localhost/api/data/delta?since=2026-03-17',
      );
      expect(response.status).toBe(200);

      const data = await response.json<{
        domains_added: string[];
        domains_removed: string[];
      }>();
      expect(data.domains_added).toEqual([]);
      expect(data.domains_removed).toEqual([]);
    });

    it('returns empty delta when since equals current version', async () => {
      const response = await SELF.fetch(
        'http://localhost/api/data/delta?since=2026-03-16',
      );
      expect(response.status).toBe(200);

      const data = await response.json<{
        domains_added: string[];
        domains_removed: string[];
      }>();
      expect(data.domains_added).toEqual([]);
      expect(data.domains_removed).toEqual([]);
    });

    it('returns 410 for very old since date (>7 days)', async () => {
      const response = await SELF.fetch(
        'http://localhost/api/data/delta?since=2026-03-01',
      );
      expect(response.status).toBe(410);

      const data = await response.json<{ error: string; redirect: string }>();
      expect(data.error).toContain('full download');
      expect(data.redirect).toBe('/api/data/full');
    });
  });

  describe('GET /api/data/bloom', () => {
    it('streams the Bloom filter binary', async () => {
      const response = await SELF.fetch('http://localhost/api/data/bloom');
      expect(response.status).toBe(200);

      const buf = new Uint8Array(await response.arrayBuffer());
      expect(Array.from(buf)).toEqual(BLOOM_BYTES);
    });

    it('sets correct content type and Content-Length for binary', async () => {
      const response = await SELF.fetch('http://localhost/api/data/bloom');
      expect(response.headers.get('Content-Type')).toBe(
        'application/octet-stream',
      );
      expect(response.headers.get('Content-Length')).toBe(
        BLOOM_BYTES.length.toString(),
      );
      await response.arrayBuffer(); // consume body
    });
  });

  describe('stale manifest handling', () => {
    it('uses cached manifest when not expired', async () => {
      // Prime KV with a still-valid cached manifest pointing to a non-existent file
      const cachedManifest = {
        ...MANIFEST,
        version: '2026-03-10',
        full_key: 'domains-full-2026-03-10.sqlite',
      };
      await env.VERDICTS.put(
        'data:manifest:latest',
        JSON.stringify(cachedManifest),
        {
          metadata: { cachedAt: Date.now(), ttl: 3600 },
          expirationTtl: 3600,
        },
      );

      // The cached manifest's full_key doesn't exist in R2 → should 404
      const response = await SELF.fetch('http://localhost/api/data/full');
      expect(response.status).toBe(404);
      await response.text(); // consume body
    });

    it('fetches fresh manifest after cache expires', async () => {
      // Prime KV with an expired cached manifest
      const staleManifest = {
        ...MANIFEST,
        version: '2026-03-10',
        full_key: 'domains-full-2026-03-10.sqlite',
      };
      await env.VERDICTS.put(
        'data:manifest:latest',
        JSON.stringify(staleManifest),
        {
          metadata: { cachedAt: Date.now() - 4000 * 1000, ttl: 3600 },
          expirationTtl: 86400,
        },
      );

      // Expired cache → falls through to R2, gets real manifest → should succeed
      const response = await SELF.fetch('http://localhost/api/data/full');
      expect(response.status).toBe(200);
      await response.arrayBuffer(); // consume body
    });
  });
});

// Tests for GET /api/alerts endpoint.
// Covers uncached reads, cached reads, recency ordering, region filtering,
// empty-region fallback, invalid region fallback, and cache invalidation after TTL.

import { SELF, env } from 'cloudflare:test';
import { describe, it, expect, beforeAll, beforeEach } from 'vitest';

// Schema + seed SQL executed against the test D1 instance.
const SCHEMA_SQL = `
CREATE TABLE IF NOT EXISTS alerts (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  description TEXT,
  scam_type TEXT,
  severity TEXT,
  region TEXT,
  state TEXT,
  report_count INTEGER DEFAULT 0,
  source_url TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_alerts_created_at ON alerts(created_at);
`;

const SEED_SQL = `
INSERT OR IGNORE INTO alerts (id, title, description, scam_type, severity, region, report_count, created_at)
VALUES
  ('alert-001', 'Fake Maybank TAC SMS',
   'Scammers sending SMS claiming your TAC has been requested.',
   'phishing', 'high', 'MY', 1247, '2026-03-01T00:00:00Z'),
  ('alert-002', 'LHDN Tax Refund Scam',
   'Fake messages from LHDN offering tax refunds.',
   'phishing', 'high', 'MY', 892, '2026-03-05T00:00:00Z'),
  ('alert-003', 'Shopee Lucky Draw Scam',
   'WhatsApp messages claiming you won a Shopee lucky draw.',
   'phishing', 'medium', 'both', 634, '2026-03-08T00:00:00Z'),
  ('alert-004', 'DBS OTP Phishing',
   'SMS pretending to be from DBS Bank requesting OTP verification.',
   'phishing', 'high', 'SG', 521, '2026-03-10T00:00:00Z'),
  ('alert-005', 'Parcel Delivery Fee Scam',
   'Messages claiming a parcel is held and requires a small fee.',
   'phishing', 'medium', 'both', 403, '2026-03-12T00:00:00Z');
`;

interface Alert {
  id: string;
  title: string;
  description: string | null;
  scam_type: string | null;
  severity: string | null;
  region: string | null;
  report_count: number;
  created_at: string;
}

async function applyMigrations() {
  // Run schema then seed as separate statements
  const schemaStatements = SCHEMA_SQL.split(';')
    .map((s) => s.trim())
    .filter(Boolean);
  for (const sql of schemaStatements) {
    await env.DB.prepare(sql).run();
  }

  const seedStatements = SEED_SQL.split(';')
    .map((s) => s.trim())
    .filter(Boolean);
  for (const sql of seedStatements) {
    await env.DB.prepare(sql).run();
  }
}

async function clearKVCache() {
  // Delete known cache keys
  const keys = ['alerts:all', 'alerts:MY', 'alerts:SG', 'alerts:both'];
  for (const key of keys) {
    await env.VERDICTS.delete(key);
  }
}

describe('GET /api/alerts', () => {
  beforeAll(async () => {
    await applyMigrations();
  });

  beforeEach(async () => {
    await clearKVCache();
  });

  it('returns all alerts when no region filter is specified', async () => {
    const response = await SELF.fetch('http://localhost/api/alerts');
    expect(response.status).toBe(200);

    const alerts = await response.json<Alert[]>();
    expect(alerts.length).toBe(5);
  });

  it('returns alerts ordered by recency (newest first)', async () => {
    const response = await SELF.fetch('http://localhost/api/alerts');
    const alerts = await response.json<Alert[]>();

    // Verify descending order by created_at
    for (let i = 1; i < alerts.length; i++) {
      const prev = new Date(alerts[i - 1].created_at).getTime();
      const curr = new Date(alerts[i].created_at).getTime();
      expect(prev).toBeGreaterThanOrEqual(curr);
    }
  });

  it('returns JSON content type', async () => {
    const response = await SELF.fetch('http://localhost/api/alerts');
    expect(response.headers.get('Content-Type')).toBe('application/json');
  });

  describe('region filtering', () => {
    it('filters by region=MY', async () => {
      const response = await SELF.fetch('http://localhost/api/alerts?region=MY');
      const alerts = await response.json<Alert[]>();

      expect(alerts.length).toBe(2);
      for (const alert of alerts) {
        expect(alert.region).toBe('MY');
      }
    });

    it('filters by region=SG', async () => {
      const response = await SELF.fetch('http://localhost/api/alerts?region=SG');
      const alerts = await response.json<Alert[]>();

      expect(alerts.length).toBe(1);
      expect(alerts[0].region).toBe('SG');
      expect(alerts[0].id).toBe('alert-004');
    });

    it('filters by region=both', async () => {
      const response = await SELF.fetch('http://localhost/api/alerts?region=both');
      const alerts = await response.json<Alert[]>();

      expect(alerts.length).toBe(2);
      for (const alert of alerts) {
        expect(alert.region).toBe('both');
      }
    });

    it('returns all alerts for empty region parameter', async () => {
      const response = await SELF.fetch('http://localhost/api/alerts?region=');
      const alerts = await response.json<Alert[]>();

      expect(alerts.length).toBe(5);
    });

    it('returns all alerts for invalid region parameter', async () => {
      const response = await SELF.fetch(
        'http://localhost/api/alerts?region=INVALID',
      );
      expect(response.status).toBe(200);

      const alerts = await response.json<Alert[]>();
      expect(alerts.length).toBe(5);
    });
  });

  describe('KV caching', () => {
    it('caches results in KV after first request', async () => {
      // First request — populates cache
      await SELF.fetch('http://localhost/api/alerts');

      // Check that KV now has the cached value
      const cached = await env.VERDICTS.get('alerts:all');
      expect(cached).not.toBeNull();

      const parsed = JSON.parse(cached!) as Alert[];
      expect(parsed.length).toBe(5);
    });

    it('serves cached results on subsequent requests', async () => {
      // Prime the cache with a known value
      const fakeAlerts = [{ id: 'cached-001', title: 'Cached Alert' }];
      await env.VERDICTS.put('alerts:all', JSON.stringify(fakeAlerts), {
        metadata: { cachedAt: Date.now(), ttl: 900 },
        expirationTtl: 900,
      });

      const response = await SELF.fetch('http://localhost/api/alerts');
      const alerts = await response.json<{ id: string; title: string }[]>();

      expect(alerts.length).toBe(1);
      expect(alerts[0].id).toBe('cached-001');
      expect(alerts[0].title).toBe('Cached Alert');
    });

    it('caches region-filtered results separately', async () => {
      await SELF.fetch('http://localhost/api/alerts?region=MY');

      const cached = await env.VERDICTS.get('alerts:MY');
      expect(cached).not.toBeNull();

      const parsed = JSON.parse(cached!) as Alert[];
      expect(parsed.length).toBe(2);
    });

    it('does not serve expired cache entries', async () => {
      // Store a cache entry with a cachedAt time well in the past
      const fakeAlerts = [{ id: 'stale-001', title: 'Stale Alert' }];
      await env.VERDICTS.put('alerts:all', JSON.stringify(fakeAlerts), {
        metadata: { cachedAt: Date.now() - 1000 * 1000, ttl: 900 },
        expirationTtl: 86400, // Keep the key alive so we test soft TTL
      });

      const response = await SELF.fetch('http://localhost/api/alerts');
      const alerts = await response.json<Alert[]>();

      // Should have gotten fresh data from D1, not the stale cache
      expect(alerts.length).toBe(5);
      expect(alerts[0].id).not.toBe('stale-001');
    });
  });
});

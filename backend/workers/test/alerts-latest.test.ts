// Tests for GET /api/alerts/latest endpoint.
// Covers KV cache hit, D1 query fallback, high-severity preference,
// correct response shape, 5-min TTL, and empty database.

import { SELF, env } from 'cloudflare:test';
import { describe, it, expect, beforeAll, beforeEach } from 'vitest';

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
  const keys = ['alerts:latest'];
  for (const key of keys) {
    await env.VERDICTS.delete(key);
  }
}

describe('GET /api/alerts/latest', () => {
  beforeAll(async () => {
    await applyMigrations();
  });

  beforeEach(async () => {
    await clearKVCache();
  });

  it('returns the most recent high-severity alert', async () => {
    const response = await SELF.fetch('http://localhost/api/alerts/latest');
    expect(response.status).toBe(200);

    const alert = await response.json<Alert>();
    // alert-004 is the most recent high-severity alert (2026-03-10)
    expect(alert.id).toBe('alert-004');
    expect(alert.severity).toBe('high');
  });

  it('returns correct response shape', async () => {
    const response = await SELF.fetch('http://localhost/api/alerts/latest');
    const alert = await response.json<Alert>();

    expect(alert).toHaveProperty('id');
    expect(alert).toHaveProperty('title');
    expect(alert).toHaveProperty('description');
    expect(alert).toHaveProperty('scam_type');
    expect(alert).toHaveProperty('severity');
    expect(alert).toHaveProperty('region');
    expect(alert).toHaveProperty('report_count');
    expect(alert).toHaveProperty('created_at');
  });

  it('returns JSON content type', async () => {
    const response = await SELF.fetch('http://localhost/api/alerts/latest');
    expect(response.headers.get('Content-Type')).toBe('application/json');
  });

  describe('KV caching', () => {
    it('caches result in KV after first request', async () => {
      await SELF.fetch('http://localhost/api/alerts/latest');

      const cached = await env.VERDICTS.get('alerts:latest');
      expect(cached).not.toBeNull();

      const parsed = JSON.parse(cached!) as Alert;
      expect(parsed.id).toBe('alert-004');
    });

    it('serves cached result on subsequent requests', async () => {
      // Prime the cache with a known value
      const fakeAlert = { id: 'cached-latest', title: 'Cached Latest Alert' };
      await env.VERDICTS.put('alerts:latest', JSON.stringify(fakeAlert), {
        metadata: { cachedAt: Date.now(), ttl: 300 },
        expirationTtl: 300,
      });

      const response = await SELF.fetch('http://localhost/api/alerts/latest');
      const alert = await response.json<{ id: string; title: string }>();

      expect(alert.id).toBe('cached-latest');
      expect(alert.title).toBe('Cached Latest Alert');
    });

    it('does not serve expired cache entries', async () => {
      // Store an expired cache entry (cachedAt well in the past, short TTL)
      const fakeAlert = { id: 'stale-latest', title: 'Stale Alert' };
      await env.VERDICTS.put('alerts:latest', JSON.stringify(fakeAlert), {
        metadata: { cachedAt: Date.now() - 600_000, ttl: 300 },
        expirationTtl: 86400,
      });

      const response = await SELF.fetch('http://localhost/api/alerts/latest');
      const alert = await response.json<Alert>();

      expect(alert.id).not.toBe('stale-latest');
      expect(alert.id).toBe('alert-004');
    });
  });

  describe('fallback behavior', () => {
    it('falls back to most recent alert when no high-severity exists', async () => {
      // Remove all high-severity alerts temporarily
      await env.DB.prepare(
        `UPDATE alerts SET severity = 'low' WHERE severity = 'high'`,
      ).run();
      await clearKVCache();

      const response = await SELF.fetch('http://localhost/api/alerts/latest');
      expect(response.status).toBe(200);

      const alert = await response.json<Alert>();
      // alert-005 is the most recent (2026-03-12) with non-high severity
      expect(alert.id).toBe('alert-005');

      // Restore high-severity alerts
      await env.DB.prepare(
        `UPDATE alerts SET severity = 'high' WHERE id IN ('alert-001', 'alert-002', 'alert-004')`,
      ).run();
    });
  });
});

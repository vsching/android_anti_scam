// Tests for POST /api/alerts/notify endpoint.
// Covers authentication (timing-safe comparison), rate limiting, alert lookup,
// region-to-topic mapping, and FCM notification triggering.
//
// Note: FCM sending is tested at the integration level — sendFcmTopicMessage
// will fail in tests (no real Firebase credentials) but the endpoint logic
// (auth, rate limiting, D1 queries) is fully testable.

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
  ('alert-002', 'DBS OTP Phishing',
   'SMS pretending to be from DBS Bank requesting OTP verification.',
   'phishing', 'high', 'SG', 521, '2026-03-10T00:00:00Z'),
  ('alert-003', 'Shopee Lucky Draw Scam',
   'WhatsApp messages claiming you won a Shopee lucky draw.',
   'phishing', 'medium', 'both', 634, '2026-03-08T00:00:00Z');
`;

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

async function clearRateLimitKeys() {
  // Clean up any rate limit keys
  const keys = ['ratelimit:notify:127.0.0.1', 'ratelimit:notify:unknown'];
  for (const key of keys) {
    await env.VERDICTS.delete(key);
  }
}

function notifyRequest(
  body: Record<string, unknown>,
  headers: Record<string, string> = {},
): Request {
  return new Request('http://localhost/api/alerts/notify', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...headers,
    },
    body: JSON.stringify(body),
  });
}

describe('POST /api/alerts/notify', () => {
  beforeAll(async () => {
    await applyMigrations();
  });

  beforeEach(async () => {
    await clearRateLimitKeys();
  });

  describe('authentication', () => {
    it('returns 401 when X-Pipeline-Key header is missing', async () => {
      const response = await SELF.fetch(
        notifyRequest({ alert_id: 'alert-001' }),
      );
      expect(response.status).toBe(401);

      const body = await response.json<{ error: string }>();
      expect(body.error).toBe('Missing authentication');
    });

    it('returns 401 when X-Pipeline-Key is invalid', async () => {
      const response = await SELF.fetch(
        notifyRequest({ alert_id: 'alert-001' }, { 'X-Pipeline-Key': 'wrong-key' }),
      );
      expect(response.status).toBe(401);

      const body = await response.json<{ error: string }>();
      expect(body.error).toBe('Invalid authentication');
    });

    it('accepts valid X-Pipeline-Key', async () => {
      const response = await SELF.fetch(
        notifyRequest({ alert_id: 'nonexistent' }, { 'X-Pipeline-Key': 'test-pipeline-secret' }),
      );
      // Should pass auth — will get 404 for nonexistent alert (not 401)
      expect(response.status).toBe(404);
    });
  });

  describe('request validation', () => {
    it('returns 400 when body is not valid JSON', async () => {
      const request = new Request('http://localhost/api/alerts/notify', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Pipeline-Key': 'test-pipeline-secret',
        },
        body: 'not json',
      });

      const response = await SELF.fetch(request);
      expect(response.status).toBe(400);
    });

    it('returns 400 when alert_id is missing', async () => {
      const response = await SELF.fetch(
        notifyRequest({}, { 'X-Pipeline-Key': 'test-pipeline-secret' }),
      );
      expect(response.status).toBe(400);

      const body = await response.json<{ error: string }>();
      expect(body.error).toBe('Missing or invalid alert_id');
    });
  });

  describe('alert lookup', () => {
    it('returns 404 when alert does not exist', async () => {
      const response = await SELF.fetch(
        notifyRequest(
          { alert_id: 'nonexistent' },
          { 'X-Pipeline-Key': 'test-pipeline-secret' },
        ),
      );
      expect(response.status).toBe(404);

      const body = await response.json<{ error: string }>();
      expect(body.error).toBe('Alert not found');
    });
  });

  describe('region-to-topic mapping', () => {
    it('maps MY region to scam_alerts_MY topic', async () => {
      // FCM sending fails in tests (no real credentials), so the endpoint
      // returns 502 with sent: false. We verify the topic mapping is correct.
      const response = await SELF.fetch(
        notifyRequest(
          { alert_id: 'alert-001' },
          { 'X-Pipeline-Key': 'test-pipeline-secret' },
        ),
      );

      const body = await response.json<{ sent: boolean; topics: string[]; results: boolean[] }>();
      expect(body.topics).toEqual(['scam_alerts_MY']);
      // FCM fails in test env — verify failure is reported correctly
      expect(response.status).toBe(502);
      expect(body.sent).toBe(false);
      expect(body.results).toEqual([false]);
    });

    it('maps SG region to scam_alerts_SG topic', async () => {
      const response = await SELF.fetch(
        notifyRequest(
          { alert_id: 'alert-002' },
          { 'X-Pipeline-Key': 'test-pipeline-secret' },
        ),
      );

      const body = await response.json<{ sent: boolean; topics: string[]; results: boolean[] }>();
      expect(body.topics).toEqual(['scam_alerts_SG']);
      expect(response.status).toBe(502);
      expect(body.sent).toBe(false);
      expect(body.results).toEqual([false]);
    });

    it('maps both region to both topics', async () => {
      const response = await SELF.fetch(
        notifyRequest(
          { alert_id: 'alert-003' },
          { 'X-Pipeline-Key': 'test-pipeline-secret' },
        ),
      );

      const body = await response.json<{ sent: boolean; topics: string[]; results: boolean[] }>();
      expect(body.topics).toEqual(['scam_alerts_MY', 'scam_alerts_SG']);
      expect(response.status).toBe(502);
      expect(body.sent).toBe(false);
      expect(body.results).toEqual([false, false]);
    });
  });

  describe('rate limiting', () => {
    it('returns 429 when rate limit is exceeded', async () => {
      // Send 10 requests (the limit)
      for (let i = 0; i < 10; i++) {
        await SELF.fetch(
          notifyRequest(
            { alert_id: 'alert-001' },
            { 'X-Pipeline-Key': 'test-pipeline-secret' },
          ),
        );
      }

      // 11th request should be rate limited
      const response = await SELF.fetch(
        notifyRequest(
          { alert_id: 'alert-001' },
          { 'X-Pipeline-Key': 'test-pipeline-secret' },
        ),
      );
      expect(response.status).toBe(429);

      const body = await response.json<{ error: string }>();
      expect(body.error).toBe('Rate limit exceeded');
    });
  });
});

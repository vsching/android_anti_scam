// Tests for POST /api/check endpoint.
// Covers: known safe domain (KV hit), typosquatting, suspicious TLD,
// URL shortener, unknown domain, invalid payload (400), oversized payload (413),
// cache hit behavior, subdomain handling, Punycode handling, domain extraction from URL.

import { SELF, env } from 'cloudflare:test';
import { describe, it, expect, beforeAll, beforeEach } from 'vitest';

// D1 schema for pending_discoveries (inline, not readFileSync)
const SCHEMA_SQL = `
CREATE TABLE IF NOT EXISTS pending_discoveries (
  id TEXT PRIMARY KEY,
  domain TEXT NOT NULL UNIQUE,
  verdict TEXT,
  reason TEXT,
  source TEXT,
  check_count INTEGER DEFAULT 1,
  last_seen_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  processed BOOLEAN DEFAULT FALSE,
  processed_at DATETIME
);
CREATE INDEX IF NOT EXISTS idx_pending_unprocessed ON pending_discoveries(processed, created_at);
`;

interface CheckResponse {
  domain: string;
  verdict: string;
  reason: string;
  confidence: number;
  details: Record<string, unknown>;
}

interface ErrorResponse {
  error: string;
}

async function applyMigrations() {
  const statements = SCHEMA_SQL.split(';')
    .map((s) => s.trim())
    .filter(Boolean);
  for (const sql of statements) {
    await env.DB.prepare(sql).run();
  }
}

async function postCheck(body: unknown): Promise<Response> {
  return SELF.fetch('http://localhost/api/check', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
}

describe('POST /api/check', () => {
  beforeAll(async () => {
    await applyMigrations();
  });

  beforeEach(async () => {
    // Clean up pending_discoveries between tests
    await env.DB.prepare('DELETE FROM pending_discoveries').run();
  });

  describe('input validation', () => {
    it('returns 400 for empty body', async () => {
      const response = await SELF.fetch('http://localhost/api/check', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: '',
      });
      expect(response.status).toBe(400);
    });

    it('returns 400 for invalid JSON', async () => {
      const response = await SELF.fetch('http://localhost/api/check', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: 'not json',
      });
      expect(response.status).toBe(400);
      const body = await response.json<ErrorResponse>();
      expect(body.error).toBe('Invalid JSON');
    });

    it('returns 400 when neither url nor domain is provided', async () => {
      const response = await postCheck({ foo: 'bar' });
      expect(response.status).toBe(400);
      const body = await response.json<ErrorResponse>();
      expect(body.error).toContain('url');
    });

    it('returns 400 for invalid URL', async () => {
      const response = await postCheck({ url: '://invalid' });
      expect(response.status).toBe(400);
    });

    it('returns 413 for oversized payload', async () => {
      const largeBody = JSON.stringify({ url: 'x'.repeat(15000) });
      const response = await SELF.fetch('http://localhost/api/check', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: largeBody,
      });
      expect(response.status).toBe(413);
    });
  });

  describe('domain extraction from URL', () => {
    it('extracts domain from full URL', async () => {
      const response = await postCheck({ url: 'https://maybank2u.com.my/login?ref=123' });
      expect(response.status).toBe(200);
      const body = await response.json<CheckResponse>();
      expect(body.domain).toBe('maybank2u.com.my');
    });

    it('accepts bare domain input', async () => {
      const response = await postCheck({ domain: 'maybank2u.com.my' });
      expect(response.status).toBe(200);
      const body = await response.json<CheckResponse>();
      expect(body.domain).toBe('maybank2u.com.my');
    });

    it('strips www prefix', async () => {
      const response = await postCheck({ url: 'https://www.maybank2u.com.my' });
      expect(response.status).toBe(200);
      const body = await response.json<CheckResponse>();
      expect(body.domain).toBe('maybank2u.com.my');
    });
  });

  describe('known safe domain (KV hit)', () => {
    it('returns safe verdict for KV-listed domain', async () => {
      // Seed KV with a known domain
      await env.VERDICTS.put('example-safe.com.my', JSON.stringify({ verdict: 'safe' }));

      const response = await postCheck({ domain: 'example-safe.com.my' });
      expect(response.status).toBe(200);
      const body = await response.json<CheckResponse>();
      expect(body.verdict).toBe('safe');
      expect(body.details.source).toBe('kv');
      expect(body.confidence).toBe(1.0);

      // Clean up
      await env.VERDICTS.delete('example-safe.com.my');
    });
  });

  describe('heuristic checks', () => {
    it('detects typosquatting domain', async () => {
      const response = await postCheck({ domain: 'maybank2u.xyz' });
      expect(response.status).toBe(200);
      const body = await response.json<CheckResponse>();
      // Should flag as dangerous due to brand pattern + suspicious TLD
      expect(['dangerous', 'suspicious']).toContain(body.verdict);
      expect(body.confidence).toBeGreaterThan(0.5);
    });

    it('detects suspicious TLD', async () => {
      const response = await postCheck({ domain: 'random-site.xyz' });
      expect(response.status).toBe(200);
      const body = await response.json<CheckResponse>();
      expect(['suspicious', 'dangerous']).toContain(body.verdict);
      expect(body.details.check_type).toBeDefined();
    });

    it('detects URL shortener', async () => {
      const response = await postCheck({ domain: 'bit.ly' });
      expect(response.status).toBe(200);
      const body = await response.json<CheckResponse>();
      expect(body.verdict).toBe('suspicious');
      expect(body.details.check_type).toBe('url_shortener');
    });

    it('returns safe for unknown harmless domain', async () => {
      const response = await postCheck({ domain: 'totally-unique-domain-abc123.com' });
      expect(response.status).toBe(200);
      const body = await response.json<CheckResponse>();
      expect(body.verdict).toBe('safe');
    });

    it('detects bank name pattern with suspicious TLD', async () => {
      const response = await postCheck({ domain: 'maybank-secure-update.xyz' });
      expect(response.status).toBe(200);
      const body = await response.json<CheckResponse>();
      expect(body.verdict).toBe('dangerous');
      expect(body.confidence).toBeGreaterThanOrEqual(0.85);
    });
  });

  describe('subdomain handling', () => {
    it('flags subdomain brand abuse', async () => {
      const response = await postCheck({ domain: 'maybank.evil.xyz' });
      expect(response.status).toBe(200);
      const body = await response.json<CheckResponse>();
      expect(body.verdict).toBe('dangerous');
      expect(body.details.check_type).toBe('subdomain_abuse');
    });
  });

  describe('Punycode handling', () => {
    it('flags Punycode/IDN domains', async () => {
      const response = await postCheck({ domain: 'xn--mybnk-mqa.com' });
      expect(response.status).toBe(200);
      const body = await response.json<CheckResponse>();
      expect(['suspicious', 'dangerous']).toContain(body.verdict);
    });
  });

  describe('D1 discovery writes', () => {
    it('writes suspicious domain to pending_discoveries', async () => {
      await postCheck({ domain: 'maybank-login-verify.xyz' });

      const result = await env.DB.prepare(
        'SELECT * FROM pending_discoveries WHERE domain = ?',
      )
        .bind('maybank-login-verify.xyz')
        .first();

      expect(result).not.toBeNull();
      expect(result!.source).toBe('heuristic');
      expect(result!.check_count).toBe(1);
    });

    it('increments check_count on repeated checks', async () => {
      await postCheck({ domain: 'cimb-secure-login.xyz' });

      // Delete from Cache API so the second request goes through heuristics again
      const cache = caches.default;
      const cacheReq = new Request(
        `https://cache.safeanot.internal/check/${encodeURIComponent('cimb-secure-login.xyz')}`,
      );
      await cache.delete(cacheReq);

      await postCheck({ domain: 'cimb-secure-login.xyz' });

      const result = await env.DB.prepare(
        'SELECT * FROM pending_discoveries WHERE domain = ?',
      )
        .bind('cimb-secure-login.xyz')
        .first();

      expect(result).not.toBeNull();
      expect(result!.check_count).toBe(2);
    });

    it('does not write safe domains to pending_discoveries', async () => {
      await postCheck({ domain: 'totally-unique-harmless-domain.com' });

      const result = await env.DB.prepare(
        'SELECT * FROM pending_discoveries WHERE domain = ?',
      )
        .bind('totally-unique-harmless-domain.com')
        .first();

      expect(result).toBeNull();
    });
  });

  describe('response format', () => {
    it('returns JSON content type', async () => {
      const response = await postCheck({ domain: 'example.com' });
      expect(response.headers.get('Content-Type')).toBe('application/json');
    });

    it('includes all expected fields', async () => {
      const response = await postCheck({ domain: 'maybank-secure.xyz' });
      const body = await response.json<CheckResponse>();

      expect(body).toHaveProperty('domain');
      expect(body).toHaveProperty('verdict');
      expect(body).toHaveProperty('reason');
      expect(body).toHaveProperty('confidence');
      expect(body).toHaveProperty('details');
      expect(typeof body.confidence).toBe('number');
    });
  });
});

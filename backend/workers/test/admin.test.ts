// Tests for admin API endpoints.
// Covers: auth validation, allowlist CRUD, blocklist CRUD, cache purge,
// discoveries list/dismiss, malformed body handling.

import { SELF, env } from 'cloudflare:test';
import { describe, it, expect, beforeAll, beforeEach } from 'vitest';

const ADMIN_SECRET = 'test-admin-secret';

// D1 schemas needed for admin tests
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

CREATE TABLE IF NOT EXISTS admin_audit_logs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  domain TEXT NOT NULL,
  action TEXT NOT NULL,
  payload_json TEXT,
  admin_ip TEXT,
  request_id TEXT,
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_admin_audit_created_at ON admin_audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_admin_audit_domain ON admin_audit_logs(domain);
`;

async function applyMigrations() {
  const statements = SCHEMA_SQL.split(';')
    .map((s) => s.trim())
    .filter(Boolean);
  for (const sql of statements) {
    await env.DB.prepare(sql).run();
  }
}

function adminHeaders(): Record<string, string> {
  return {
    'Content-Type': 'application/json',
    'X-Admin-Key': ADMIN_SECRET,
  };
}

function noAuthHeaders(): Record<string, string> {
  return {
    'Content-Type': 'application/json',
  };
}

describe('Admin API', () => {
  beforeAll(async () => {
    await applyMigrations();
  });

  beforeEach(async () => {
    // Clean up KV entries
    const allKeys = await env.VERDICTS.list();
    for (const key of allKeys.keys) {
      await env.VERDICTS.delete(key.name);
    }
    // Clean up D1 tables
    await env.DB.prepare('DELETE FROM pending_discoveries').run();
    await env.DB.prepare('DELETE FROM admin_audit_logs').run();
  });

  describe('authentication', () => {
    it('returns 401 for unauthenticated allowlist GET', async () => {
      const response = await SELF.fetch('http://localhost/api/admin/allowlist', {
        method: 'GET',
        headers: noAuthHeaders(),
      });
      expect(response.status).toBe(401);
    });

    it('returns 401 for wrong admin key', async () => {
      const response = await SELF.fetch('http://localhost/api/admin/allowlist', {
        method: 'GET',
        headers: { ...noAuthHeaders(), 'X-Admin-Key': 'wrong-key' },
      });
      expect(response.status).toBe(401);
    });

    it('returns 401 for unauthenticated blocklist POST', async () => {
      const response = await SELF.fetch('http://localhost/api/admin/blocklist', {
        method: 'POST',
        headers: noAuthHeaders(),
        body: JSON.stringify({ domain: 'evil.com', reason: 'scam' }),
      });
      expect(response.status).toBe(401);
    });

    it('returns 401 for unauthenticated cache purge', async () => {
      const response = await SELF.fetch('http://localhost/api/admin/cache/example.com', {
        method: 'DELETE',
        headers: noAuthHeaders(),
      });
      expect(response.status).toBe(401);
    });

    it('returns 401 for unauthenticated discoveries GET', async () => {
      const response = await SELF.fetch('http://localhost/api/admin/discoveries', {
        method: 'GET',
        headers: noAuthHeaders(),
      });
      expect(response.status).toBe(401);
    });

    it('returns 401 for correct-length but wrong admin key', async () => {
      // Use a key with the same length as the real secret to test timing-safe comparison
      const wrongKey = 'x'.repeat(ADMIN_SECRET.length);
      const response = await SELF.fetch('http://localhost/api/admin/allowlist', {
        method: 'GET',
        headers: { ...noAuthHeaders(), 'X-Admin-Key': wrongKey },
      });
      expect(response.status).toBe(401);
    });
  });

  describe('POST /api/admin/allowlist', () => {
    it('adds an allowlist entry and returns 200', async () => {
      const response = await SELF.fetch('http://localhost/api/admin/allowlist', {
        method: 'POST',
        headers: adminHeaders(),
        body: JSON.stringify({ domain: 'maybank2u.com.my', entity: 'Maybank', category: 'banking' }),
      });
      expect(response.status).toBe(200);
      const body = await response.json<{ ok: boolean; domain: string }>();
      expect(body.ok).toBe(true);
      expect(body.domain).toBe('maybank2u.com.my');

      // Verify KV entry
      const kvValue = await env.VERDICTS.get('allowlist:maybank2u.com.my');
      expect(kvValue).not.toBeNull();
      const parsed = JSON.parse(kvValue!);
      expect(parsed.verdict).toBe('safe');
      expect(parsed.entity).toBe('Maybank');

      // Verify audit log entry was written
      const auditRow = await env.DB.prepare(
        'SELECT * FROM admin_audit_logs WHERE domain = ? AND action = ?',
      )
        .bind('maybank2u.com.my', 'allowlist_add')
        .first<{ domain: string; action: string; payload_json: string }>();
      expect(auditRow).not.toBeNull();
      expect(auditRow!.domain).toBe('maybank2u.com.my');
      expect(auditRow!.action).toBe('allowlist_add');
    });

    it('returns 400 for missing fields', async () => {
      const response = await SELF.fetch('http://localhost/api/admin/allowlist', {
        method: 'POST',
        headers: adminHeaders(),
        body: JSON.stringify({ domain: 'example.com' }),
      });
      expect(response.status).toBe(400);
    });

    it('returns 400 for invalid JSON', async () => {
      const response = await SELF.fetch('http://localhost/api/admin/allowlist', {
        method: 'POST',
        headers: adminHeaders(),
        body: 'not-json',
      });
      expect(response.status).toBe(400);
    });
  });

  describe('DELETE /api/admin/allowlist/:domain', () => {
    it('removes an allowlist entry', async () => {
      // Seed
      await env.VERDICTS.put('allowlist:test.com', JSON.stringify({ verdict: 'safe' }));

      const response = await SELF.fetch('http://localhost/api/admin/allowlist/test.com', {
        method: 'DELETE',
        headers: adminHeaders(),
      });
      expect(response.status).toBe(200);

      const kvValue = await env.VERDICTS.get('allowlist:test.com');
      expect(kvValue).toBeNull();
    });
  });

  describe('GET /api/admin/allowlist', () => {
    it('returns an array of entries', async () => {
      await env.VERDICTS.put(
        'allowlist:bank.com',
        JSON.stringify({ verdict: 'safe', entity: 'Bank', category: 'banking' }),
      );

      const response = await SELF.fetch('http://localhost/api/admin/allowlist', {
        method: 'GET',
        headers: adminHeaders(),
      });
      expect(response.status).toBe(200);
      const body = await response.json<{ entries: unknown[] }>();
      expect(Array.isArray(body.entries)).toBe(true);
      expect(body.entries.length).toBeGreaterThanOrEqual(1);
    });
  });

  describe('POST /api/admin/blocklist', () => {
    it('adds a blocklist entry and returns 200', async () => {
      const response = await SELF.fetch('http://localhost/api/admin/blocklist', {
        method: 'POST',
        headers: adminHeaders(),
        body: JSON.stringify({ domain: 'scam-site.xyz', reason: 'confirmed scam' }),
      });
      expect(response.status).toBe(200);
      const body = await response.json<{ ok: boolean; domain: string }>();
      expect(body.ok).toBe(true);

      const kvValue = await env.VERDICTS.get('scam-site.xyz');
      expect(kvValue).not.toBeNull();
      const parsed = JSON.parse(kvValue!);
      expect(parsed.verdict).toBe('dangerous');
    });

    it('returns 400 for invalid JSON body', async () => {
      const response = await SELF.fetch('http://localhost/api/admin/blocklist', {
        method: 'POST',
        headers: adminHeaders(),
        body: 'not-json',
      });
      expect(response.status).toBe(400);
    });

    it('returns 400 for missing reason', async () => {
      const response = await SELF.fetch('http://localhost/api/admin/blocklist', {
        method: 'POST',
        headers: adminHeaders(),
        body: JSON.stringify({ domain: 'scam.com' }),
      });
      expect(response.status).toBe(400);
    });
  });

  describe('DELETE /api/admin/blocklist/:domain', () => {
    it('removes a blocklist entry', async () => {
      await env.VERDICTS.put('evil.com', JSON.stringify({ verdict: 'dangerous' }));

      const response = await SELF.fetch('http://localhost/api/admin/blocklist/evil.com', {
        method: 'DELETE',
        headers: adminHeaders(),
      });
      expect(response.status).toBe(200);

      const kvValue = await env.VERDICTS.get('evil.com');
      expect(kvValue).toBeNull();
    });
  });

  describe('GET /api/admin/blocklist', () => {
    it('returns an array of entries', async () => {
      await env.VERDICTS.put('scam.com', JSON.stringify({ verdict: 'dangerous' }));

      const response = await SELF.fetch('http://localhost/api/admin/blocklist', {
        method: 'GET',
        headers: adminHeaders(),
      });
      expect(response.status).toBe(200);
      const body = await response.json<{ entries: unknown[] }>();
      expect(Array.isArray(body.entries)).toBe(true);
    });
  });

  describe('DELETE /api/admin/cache/:domain', () => {
    it('returns purged: true and removes cached entry', async () => {
      // First, populate the Cache API with a known entry
      const domain = 'cache-test.com';
      const cacheKey = `https://cache.safeanot.internal/check/${encodeURIComponent(domain)}`;
      const cache = caches.default;
      const cacheRequest = new Request(cacheKey);
      await cache.put(
        cacheRequest,
        new Response(JSON.stringify({ domain, verdict: 'safe' }), {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
            'Cache-Control': 's-maxage=3600',
          },
        }),
      );

      // Verify it exists
      const beforePurge = await cache.match(new Request(cacheKey));
      expect(beforePurge).not.toBeUndefined();

      // Call purge
      const response = await SELF.fetch(`http://localhost/api/admin/cache/${domain}`, {
        method: 'DELETE',
        headers: adminHeaders(),
      });
      expect(response.status).toBe(200);
      const body = await response.json<{ purged: boolean; domain: string }>();
      expect(body.purged).toBe(true);
      expect(body.domain).toBe(domain);

      // Verify the cache entry is gone
      const afterPurge = await cache.match(new Request(cacheKey));
      expect(afterPurge).toBeUndefined();
    });
  });

  describe('GET /api/admin/discoveries', () => {
    it('returns an array of discoveries', async () => {
      // Seed a discovery
      await env.DB.prepare(
        `INSERT INTO pending_discoveries (id, domain, verdict, reason, source, check_count)
         VALUES (?, ?, ?, ?, ?, ?)`,
      ).bind('test-id', 'suspicious.xyz', 'suspicious', 'heuristic flag', 'heuristic', 1).run();

      const response = await SELF.fetch('http://localhost/api/admin/discoveries', {
        method: 'GET',
        headers: adminHeaders(),
      });
      expect(response.status).toBe(200);
      const body = await response.json<{ entries: unknown[] }>();
      expect(Array.isArray(body.entries)).toBe(true);
      expect(body.entries.length).toBe(1);
    });
  });

  describe('DELETE /api/admin/discoveries/:id', () => {
    it('dismisses a discovery entry', async () => {
      await env.DB.prepare(
        `INSERT INTO pending_discoveries (id, domain, verdict, reason, source, check_count)
         VALUES (?, ?, ?, ?, ?, ?)`,
      ).bind('dismiss-id', 'bad.xyz', 'dangerous', 'test', 'heuristic', 1).run();

      const response = await SELF.fetch('http://localhost/api/admin/discoveries/dismiss-id', {
        method: 'DELETE',
        headers: adminHeaders(),
      });
      expect(response.status).toBe(200);
      const body = await response.json<{ ok: boolean; id: string }>();
      expect(body.ok).toBe(true);

      const result = await env.DB.prepare(
        'SELECT * FROM pending_discoveries WHERE id = ?',
      ).bind('dismiss-id').first();
      expect(result).toBeNull();
    });

    it('returns 404 for non-existent discovery', async () => {
      const response = await SELF.fetch('http://localhost/api/admin/discoveries/nonexistent', {
        method: 'DELETE',
        headers: adminHeaders(),
      });
      expect(response.status).toBe(404);
    });
  });
});

// Tests for guardian pairing API endpoints.
// Covers: code generation, claiming, expiry, max guardians, deletion,
// rate limiting, get wards/guardians, HMAC auth.

import { SELF, env } from 'cloudflare:test';
import { describe, it, expect, beforeAll, beforeEach } from 'vitest';

const SCHEMA_SQL = `
CREATE TABLE IF NOT EXISTS guardian_pairing_codes (
    code TEXT PRIMARY KEY,
    ward_device_id TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    claimed INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS guardian_pairings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ward_device_id TEXT NOT NULL,
    guardian_device_id TEXT NOT NULL,
    ward_display_name TEXT DEFAULT '',
    guardian_display_name TEXT DEFAULT '',
    created_at INTEGER NOT NULL,
    UNIQUE(ward_device_id, guardian_device_id)
);
CREATE TABLE IF NOT EXISTS guardian_heartbeats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ward_device_id TEXT NOT NULL,
    security_score INTEGER NOT NULL,
    secured_items INTEGER NOT NULL,
    total_items INTEGER NOT NULL,
    play_protect_enabled INTEGER NOT NULL DEFAULT 1,
    timestamp INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_guardian_heartbeats_ward_ts
    ON guardian_heartbeats (ward_device_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_guardian_pairings_ward
    ON guardian_pairings (ward_device_id);
CREATE INDEX IF NOT EXISTS idx_guardian_pairings_guardian
    ON guardian_pairings (guardian_device_id);
`;

// Shared HMAC secret for tests — must match env.GUARDIAN_HMAC_SECRET
const HMAC_SECRET = 'test-hmac-secret';

async function computeHmac(body: string): Promise<string> {
  const encoder = new TextEncoder();
  const key = await crypto.subtle.importKey(
    'raw',
    encoder.encode(HMAC_SECRET),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign'],
  );
  const signature = await crypto.subtle.sign('HMAC', key, encoder.encode(body));
  const bytes = new Uint8Array(signature);
  let hex = '';
  for (let i = 0; i < bytes.length; i++) {
    hex += bytes[i].toString(16).padStart(2, '0');
  }
  return hex;
}

async function applyMigrations() {
  const statements = SCHEMA_SQL.split(';')
    .map((s) => s.trim())
    .filter(Boolean);
  for (const sql of statements) {
    await env.DB.prepare(sql).run();
  }
}

async function postWithHmac(
  url: string,
  body: Record<string, unknown>,
): Promise<Response> {
  const bodyText = JSON.stringify(body);
  const hmac = await computeHmac(bodyText);
  return SELF.fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Device-HMAC': hmac,
    },
    body: bodyText,
  });
}

async function deleteWithHmac(
  url: string,
  body: Record<string, unknown>,
): Promise<Response> {
  const bodyText = JSON.stringify(body);
  const hmac = await computeHmac(bodyText);
  return SELF.fetch(url, {
    method: 'DELETE',
    headers: {
      'Content-Type': 'application/json',
      'X-Device-HMAC': hmac,
    },
    body: bodyText,
  });
}

async function getWithHmac(url: string, deviceId: string): Promise<Response> {
  const hmac = await computeHmac(deviceId);
  return SELF.fetch(url, {
    method: 'GET',
    headers: {
      'X-Device-HMAC': hmac,
    },
  });
}

async function generateCode(deviceId: string): Promise<string> {
  const res = await postWithHmac('http://localhost/api/guardian/pair/generate', {
    device_id: deviceId,
  });
  const body = await res.json<{ code: string }>();
  return body.code;
}

describe('Guardian Pairing API', () => {
  beforeAll(async () => {
    await applyMigrations();
  });

  beforeEach(async () => {
    // Clean tables between tests
    await env.DB.prepare('DELETE FROM guardian_pairing_codes').run();
    await env.DB.prepare('DELETE FROM guardian_pairings').run();
    await env.DB.prepare('DELETE FROM guardian_heartbeats').run();
  });

  describe('POST /api/guardian/pair/generate', () => {
    it('returns a 6-character alphanumeric code', async () => {
      const res = await postWithHmac(
        'http://localhost/api/guardian/pair/generate',
        { device_id: 'ward-device-001' },
      );
      expect(res.status).toBe(200);

      const body = await res.json<{ code: string; expires_at: number }>();
      expect(body.code).toHaveLength(6);
      // Should only contain safe characters (no 0, O, 1, I, L)
      expect(body.code).toMatch(/^[ABCDEFGHJKMNPQRSTUVWXYZ23456789]+$/);
      expect(body.expires_at).toBeGreaterThan(Math.floor(Date.now() / 1000));
    });

    it('rejects requests without HMAC header', async () => {
      const res = await SELF.fetch(
        'http://localhost/api/guardian/pair/generate',
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ device_id: 'ward-device-001' }),
        },
      );
      expect(res.status).toBe(401);
    });

    it('rejects requests with invalid HMAC', async () => {
      const res = await SELF.fetch(
        'http://localhost/api/guardian/pair/generate',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'X-Device-HMAC': 'invalid-hmac-value',
          },
          body: JSON.stringify({ device_id: 'ward-device-001' }),
        },
      );
      expect(res.status).toBe(401);
    });

    it('rejects missing device_id', async () => {
      const res = await postWithHmac(
        'http://localhost/api/guardian/pair/generate',
        {},
      );
      expect(res.status).toBe(400);
    });

    it('enforces rate limit of 10 codes per device per hour', async () => {
      const now = Math.floor(Date.now() / 1000);
      // Insert 10 codes for this device in the current hour
      const stmt = env.DB.prepare(
        'INSERT INTO guardian_pairing_codes (code, ward_device_id, created_at, expires_at, claimed) VALUES (?, ?, ?, ?, 0)',
      );
      const batch = Array.from({ length: 10 }, (_, i) =>
        stmt.bind(`CODE${i.toString().padStart(2, '0')}`, 'rate-limit-device', now - 60, now + 840),
      );
      await env.DB.batch(batch);

      const res = await postWithHmac(
        'http://localhost/api/guardian/pair/generate',
        { device_id: 'rate-limit-device' },
      );
      expect(res.status).toBe(429);
      const body = await res.json<{ error: string }>();
      expect(body.error).toContain('Rate limit');
    });
  });

  describe('POST /api/guardian/pair/claim', () => {
    it('claims a valid pairing code and creates a pairing', async () => {
      // Generate a code as the ward
      const code = await generateCode('ward-001');

      // Claim as guardian
      const res = await postWithHmac(
        'http://localhost/api/guardian/pair/claim',
        {
          device_id: 'guardian-001',
          code,
          display_name: 'Mom',
        },
      );
      expect(res.status).toBe(200);
      const body = await res.json<{ status: string; ward_device_id: string }>();
      expect(body.status).toBe('paired');
      expect(body.ward_device_id).toBe('ward-001');

      // Verify pairing exists in DB
      const pairing = await env.DB.prepare(
        'SELECT * FROM guardian_pairings WHERE ward_device_id = ? AND guardian_device_id = ?',
      )
        .bind('ward-001', 'guardian-001')
        .first();
      expect(pairing).not.toBeNull();
    });

    it('returns 410 for expired code', async () => {
      // Insert an expired code directly
      const now = Math.floor(Date.now() / 1000);
      await env.DB.prepare(
        'INSERT INTO guardian_pairing_codes (code, ward_device_id, created_at, expires_at, claimed) VALUES (?, ?, ?, ?, 0)',
      )
        .bind('EXPRD1', 'ward-expired', now - 1000, now - 1)
        .run();

      const res = await postWithHmac(
        'http://localhost/api/guardian/pair/claim',
        { device_id: 'guardian-001', code: 'EXPRD1' },
      );
      expect(res.status).toBe(410);
    });

    it('returns 410 for already claimed code', async () => {
      const now = Math.floor(Date.now() / 1000);
      await env.DB.prepare(
        'INSERT INTO guardian_pairing_codes (code, ward_device_id, created_at, expires_at, claimed) VALUES (?, ?, ?, ?, 1)',
      )
        .bind('USED01', 'ward-used', now - 60, now + 840)
        .run();

      const res = await postWithHmac(
        'http://localhost/api/guardian/pair/claim',
        { device_id: 'guardian-001', code: 'USED01' },
      );
      expect(res.status).toBe(410);
    });

    it('returns 404 for invalid code', async () => {
      const res = await postWithHmac(
        'http://localhost/api/guardian/pair/claim',
        { device_id: 'guardian-001', code: 'BADCOD' },
      );
      expect(res.status).toBe(404);
    });

    it('enforces maximum 3 guardians per ward', async () => {
      const now = Math.floor(Date.now() / 1000);
      // Create 3 existing pairings for ward
      const stmt = env.DB.prepare(
        'INSERT INTO guardian_pairings (ward_device_id, guardian_device_id, created_at) VALUES (?, ?, ?)',
      );
      await env.DB.batch([
        stmt.bind('ward-full', 'g1', now),
        stmt.bind('ward-full', 'g2', now),
        stmt.bind('ward-full', 'g3', now),
      ]);

      // Create a valid code for this ward
      await env.DB.prepare(
        'INSERT INTO guardian_pairing_codes (code, ward_device_id, created_at, expires_at, claimed) VALUES (?, ?, ?, ?, 0)',
      )
        .bind('FULL01', 'ward-full', now, now + 900)
        .run();

      const res = await postWithHmac(
        'http://localhost/api/guardian/pair/claim',
        { device_id: 'g4', code: 'FULL01' },
      );
      expect(res.status).toBe(409);
      const body = await res.json<{ error: string }>();
      expect(body.error).toContain('3');
    });

    it('prevents self-pairing', async () => {
      const code = await generateCode('ward-self');

      const res = await postWithHmac(
        'http://localhost/api/guardian/pair/claim',
        { device_id: 'ward-self', code },
      );
      expect(res.status).toBe(400);
      const body = await res.json<{ error: string }>();
      expect(body.error).toContain('yourself');
    });
  });

  describe('GET /api/guardian/wards', () => {
    it('returns ward list with latest heartbeat for a guardian', async () => {
      const now = Math.floor(Date.now() / 1000);

      // Create pairing
      await env.DB.prepare(
        'INSERT INTO guardian_pairings (ward_device_id, guardian_device_id, ward_display_name, created_at) VALUES (?, ?, ?, ?)',
      )
        .bind('ward-hb', 'guardian-hb', 'Grandma', now)
        .run();

      // Insert heartbeats
      await env.DB.batch([
        env.DB.prepare(
          'INSERT INTO guardian_heartbeats (ward_device_id, security_score, secured_items, total_items, play_protect_enabled, timestamp) VALUES (?, ?, ?, ?, ?, ?)',
        ).bind('ward-hb', 70, 5, 8, 1, now - 100),
        env.DB.prepare(
          'INSERT INTO guardian_heartbeats (ward_device_id, security_score, secured_items, total_items, play_protect_enabled, timestamp) VALUES (?, ?, ?, ?, ?, ?)',
        ).bind('ward-hb', 85, 7, 8, 1, now),
      ]);

      const res = await getWithHmac(
        `http://localhost/api/guardian/wards?device_id=guardian-hb`,
        'guardian-hb',
      );
      expect(res.status).toBe(200);
      const body = await res.json<{
        wards: Array<{
          ward_device_id: string;
          security_score: number;
          heartbeat_timestamp: number;
        }>;
      }>();
      expect(body.wards).toHaveLength(1);
      expect(body.wards[0].ward_device_id).toBe('ward-hb');
      // Should return the latest heartbeat
      expect(body.wards[0].security_score).toBe(85);
      expect(body.wards[0].heartbeat_timestamp).toBe(now);
    });

    it('returns empty array when guardian has no wards', async () => {
      const res = await getWithHmac(
        'http://localhost/api/guardian/wards?device_id=no-wards',
        'no-wards',
      );
      expect(res.status).toBe(200);
      const body = await res.json<{ wards: unknown[] }>();
      expect(body.wards).toHaveLength(0);
    });

    it('rejects missing device_id', async () => {
      const res = await SELF.fetch('http://localhost/api/guardian/wards', {
        method: 'GET',
      });
      expect(res.status).toBe(400);
    });
  });

  describe('GET /api/guardian/guardians', () => {
    it('returns guardian list for a ward', async () => {
      const now = Math.floor(Date.now() / 1000);

      await env.DB.batch([
        env.DB.prepare(
          'INSERT INTO guardian_pairings (ward_device_id, guardian_device_id, guardian_display_name, created_at) VALUES (?, ?, ?, ?)',
        ).bind('ward-g', 'guardian-a', 'Mom', now),
        env.DB.prepare(
          'INSERT INTO guardian_pairings (ward_device_id, guardian_device_id, guardian_display_name, created_at) VALUES (?, ?, ?, ?)',
        ).bind('ward-g', 'guardian-b', 'Dad', now),
      ]);

      const res = await getWithHmac(
        'http://localhost/api/guardian/guardians?device_id=ward-g',
        'ward-g',
      );
      expect(res.status).toBe(200);
      const body = await res.json<{
        guardians: Array<{ guardian_device_id: string; guardian_display_name: string }>;
      }>();
      expect(body.guardians).toHaveLength(2);
    });
  });

  describe('DELETE /api/guardian/pair/:pairingId', () => {
    it('allows ward to delete a pairing', async () => {
      const now = Math.floor(Date.now() / 1000);
      await env.DB.prepare(
        'INSERT INTO guardian_pairings (ward_device_id, guardian_device_id, created_at) VALUES (?, ?, ?)',
      )
        .bind('ward-del', 'guardian-del', now)
        .run();

      const pairing = await env.DB.prepare(
        'SELECT id FROM guardian_pairings WHERE ward_device_id = ?',
      )
        .bind('ward-del')
        .first<{ id: number }>();

      const res = await deleteWithHmac(
        `http://localhost/api/guardian/pair/${pairing!.id}`,
        { device_id: 'ward-del' },
      );
      expect(res.status).toBe(200);
      const body = await res.json<{ status: string }>();
      expect(body.status).toBe('deleted');

      // Verify deleted
      const check = await env.DB.prepare(
        'SELECT * FROM guardian_pairings WHERE id = ?',
      )
        .bind(pairing!.id)
        .first();
      expect(check).toBeNull();
    });

    it('allows guardian to delete a pairing', async () => {
      const now = Math.floor(Date.now() / 1000);
      await env.DB.prepare(
        'INSERT INTO guardian_pairings (ward_device_id, guardian_device_id, created_at) VALUES (?, ?, ?)',
      )
        .bind('ward-del2', 'guardian-del2', now)
        .run();

      const pairing = await env.DB.prepare(
        'SELECT id FROM guardian_pairings WHERE guardian_device_id = ?',
      )
        .bind('guardian-del2')
        .first<{ id: number }>();

      const res = await deleteWithHmac(
        `http://localhost/api/guardian/pair/${pairing!.id}`,
        { device_id: 'guardian-del2' },
      );
      expect(res.status).toBe(200);
    });

    it('rejects delete from unauthorized device', async () => {
      const now = Math.floor(Date.now() / 1000);
      await env.DB.prepare(
        'INSERT INTO guardian_pairings (ward_device_id, guardian_device_id, created_at) VALUES (?, ?, ?)',
      )
        .bind('ward-unauth', 'guardian-unauth', now)
        .run();

      const pairing = await env.DB.prepare(
        'SELECT id FROM guardian_pairings WHERE ward_device_id = ?',
      )
        .bind('ward-unauth')
        .first<{ id: number }>();

      const res = await deleteWithHmac(
        `http://localhost/api/guardian/pair/${pairing!.id}`,
        { device_id: 'stranger-device' },
      );
      expect(res.status).toBe(403);
    });

    it('returns 404 for non-existent pairing', async () => {
      const res = await deleteWithHmac(
        'http://localhost/api/guardian/pair/99999',
        { device_id: 'some-device' },
      );
      expect(res.status).toBe(404);
    });
  });

  describe('POST /api/guardian/heartbeat', () => {
    it('stores heartbeat data for a ward with pairings', async () => {
      const now = Math.floor(Date.now() / 1000);

      // Create a pairing so the ward has guardians
      await env.DB.prepare(
        'INSERT INTO guardian_pairings (ward_device_id, guardian_device_id, created_at) VALUES (?, ?, ?)',
      )
        .bind('ward-hb-post', 'guardian-hb-post', now)
        .run();

      const res = await postWithHmac('http://localhost/api/guardian/heartbeat', {
        device_id: 'ward-hb-post',
        security_score: 85,
        secured_items: 7,
        total_items: 8,
        play_protect_enabled: true,
        timestamp: now,
      });
      expect(res.status).toBe(200);
      const body = await res.json<{ status: string }>();
      expect(body.status).toBe('ok');

      // Verify heartbeat stored in DB
      const heartbeat = await env.DB.prepare(
        'SELECT * FROM guardian_heartbeats WHERE ward_device_id = ?',
      )
        .bind('ward-hb-post')
        .first<{ security_score: number; secured_items: number; total_items: number }>();
      expect(heartbeat).not.toBeNull();
      expect(heartbeat!.security_score).toBe(85);
      expect(heartbeat!.secured_items).toBe(7);
      expect(heartbeat!.total_items).toBe(8);
    });

    it('rejects heartbeat from device with no pairings', async () => {
      const now = Math.floor(Date.now() / 1000);

      const res = await postWithHmac('http://localhost/api/guardian/heartbeat', {
        device_id: 'unlinked-device',
        security_score: 90,
        secured_items: 5,
        total_items: 5,
        play_protect_enabled: true,
        timestamp: now,
      });
      expect(res.status).toBe(403);
      const body = await res.json<{ error: string }>();
      expect(body.error).toContain('no guardian pairings');
    });

    it('cleans up heartbeats older than 30 days', async () => {
      const now = Math.floor(Date.now() / 1000);
      const thirtyOneDaysAgo = now - 31 * 24 * 60 * 60;

      // Create a pairing
      await env.DB.prepare(
        'INSERT INTO guardian_pairings (ward_device_id, guardian_device_id, created_at) VALUES (?, ?, ?)',
      )
        .bind('ward-cleanup', 'guardian-cleanup', now)
        .run();

      // Insert an old heartbeat
      await env.DB.prepare(
        'INSERT INTO guardian_heartbeats (ward_device_id, security_score, secured_items, total_items, play_protect_enabled, timestamp) VALUES (?, ?, ?, ?, ?, ?)',
      )
        .bind('ward-cleanup', 50, 3, 8, 1, thirtyOneDaysAgo)
        .run();

      // Post a new heartbeat (triggers cleanup)
      await postWithHmac('http://localhost/api/guardian/heartbeat', {
        device_id: 'ward-cleanup',
        security_score: 90,
        secured_items: 7,
        total_items: 8,
        play_protect_enabled: true,
        timestamp: now,
      });

      // Verify old heartbeat was cleaned up
      const oldHeartbeat = await env.DB.prepare(
        'SELECT * FROM guardian_heartbeats WHERE ward_device_id = ? AND timestamp = ?',
      )
        .bind('ward-cleanup', thirtyOneDaysAgo)
        .first();
      expect(oldHeartbeat).toBeNull();

      // Verify new heartbeat exists
      const newHeartbeat = await env.DB.prepare(
        'SELECT * FROM guardian_heartbeats WHERE ward_device_id = ? AND timestamp = ?',
      )
        .bind('ward-cleanup', now)
        .first();
      expect(newHeartbeat).not.toBeNull();
    });

    it('rejects heartbeat with missing fields', async () => {
      const res = await postWithHmac('http://localhost/api/guardian/heartbeat', {
        device_id: 'some-device',
      });
      expect(res.status).toBe(400);
    });

    it('rejects heartbeat with invalid security_score', async () => {
      const now = Math.floor(Date.now() / 1000);
      const res = await postWithHmac('http://localhost/api/guardian/heartbeat', {
        device_id: 'some-device',
        security_score: 150,
        secured_items: 5,
        total_items: 5,
        play_protect_enabled: true,
        timestamp: now,
      });
      expect(res.status).toBe(400);
    });
  });

  describe('GET /api/guardian/wards/:deviceId/heartbeats', () => {
    it('returns heartbeat history for a ward', async () => {
      const now = Math.floor(Date.now() / 1000);

      // Insert heartbeats
      await env.DB.batch([
        env.DB.prepare(
          'INSERT INTO guardian_heartbeats (ward_device_id, security_score, secured_items, total_items, play_protect_enabled, timestamp) VALUES (?, ?, ?, ?, ?, ?)',
        ).bind('ward-history', 70, 5, 8, 1, now - 3600),
        env.DB.prepare(
          'INSERT INTO guardian_heartbeats (ward_device_id, security_score, secured_items, total_items, play_protect_enabled, timestamp) VALUES (?, ?, ?, ?, ?, ?)',
        ).bind('ward-history', 85, 7, 8, 1, now),
      ]);

      const res = await getWithHmac(
        'http://localhost/api/guardian/wards/ward-history/heartbeats',
        'ward-history',
      );
      expect(res.status).toBe(200);
      const body = await res.json<{
        heartbeats: Array<{ security_score: number; timestamp: number }>;
      }>();
      expect(body.heartbeats).toHaveLength(2);
      // Should be ordered by timestamp DESC
      expect(body.heartbeats[0].security_score).toBe(85);
      expect(body.heartbeats[1].security_score).toBe(70);
    });

    it('respects days query parameter', async () => {
      const now = Math.floor(Date.now() / 1000);
      const twoDaysAgo = now - 2 * 24 * 60 * 60;
      const eightDaysAgo = now - 8 * 24 * 60 * 60;

      await env.DB.batch([
        env.DB.prepare(
          'INSERT INTO guardian_heartbeats (ward_device_id, security_score, secured_items, total_items, play_protect_enabled, timestamp) VALUES (?, ?, ?, ?, ?, ?)',
        ).bind('ward-days', 70, 5, 8, 1, eightDaysAgo),
        env.DB.prepare(
          'INSERT INTO guardian_heartbeats (ward_device_id, security_score, secured_items, total_items, play_protect_enabled, timestamp) VALUES (?, ?, ?, ?, ?, ?)',
        ).bind('ward-days', 90, 7, 8, 1, twoDaysAgo),
      ]);

      // Default 7 days - should only return the recent one
      const res = await getWithHmac(
        'http://localhost/api/guardian/wards/ward-days/heartbeats',
        'ward-days',
      );
      expect(res.status).toBe(200);
      const body = await res.json<{
        heartbeats: Array<{ security_score: number }>;
      }>();
      expect(body.heartbeats).toHaveLength(1);
      expect(body.heartbeats[0].security_score).toBe(90);

      // 30 days - should return both
      const res30 = await getWithHmac(
        'http://localhost/api/guardian/wards/ward-days/heartbeats?days=30',
        'ward-days',
      );
      const body30 = await res30.json<{
        heartbeats: Array<{ security_score: number }>;
      }>();
      expect(body30.heartbeats).toHaveLength(2);
    });

    it('returns empty array when no heartbeats exist', async () => {
      const res = await getWithHmac(
        'http://localhost/api/guardian/wards/no-heartbeats/heartbeats',
        'no-heartbeats',
      );
      expect(res.status).toBe(200);
      const body = await res.json<{ heartbeats: unknown[] }>();
      expect(body.heartbeats).toHaveLength(0);
    });
  });
});

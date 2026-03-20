// Tests for guardian alert notification logic (E08-005).
// Covers: score drop triggers, Play Protect disabled, RED band entry,
// daily alert cap, FCM token registration.

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
CREATE TABLE IF NOT EXISTS guardian_fcm_tokens (
    device_id TEXT PRIMARY KEY,
    fcm_token TEXT NOT NULL,
    updated_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS guardian_daily_alerts (
    ward_device_id TEXT NOT NULL,
    alert_date TEXT NOT NULL,
    alert_count INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (ward_device_id, alert_date)
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

/**
 * Helper: set up a ward with a guardian pairing and previous heartbeat.
 */
async function setupWardWithGuardian(
  wardId: string,
  guardianId: string,
  prevScore: number,
  prevPlayProtect: boolean,
  prevTimestamp: number,
) {
  await env.DB.batch([
    env.DB.prepare(
      'INSERT INTO guardian_pairings (ward_device_id, guardian_device_id, ward_display_name, created_at) VALUES (?, ?, ?, ?)',
    ).bind(wardId, guardianId, 'Test Ward', prevTimestamp),
    env.DB.prepare(
      'INSERT INTO guardian_heartbeats (ward_device_id, security_score, secured_items, total_items, play_protect_enabled, timestamp) VALUES (?, ?, ?, ?, ?, ?)',
    ).bind(wardId, prevScore, 5, 8, prevPlayProtect ? 1 : 0, prevTimestamp),
  ]);
}

describe('Guardian Alert Notifications', () => {
  beforeAll(async () => {
    await applyMigrations();
  });

  beforeEach(async () => {
    await env.DB.prepare('DELETE FROM guardian_pairing_codes').run();
    await env.DB.prepare('DELETE FROM guardian_pairings').run();
    await env.DB.prepare('DELETE FROM guardian_heartbeats').run();
    await env.DB.prepare('DELETE FROM guardian_fcm_tokens').run();
    await env.DB.prepare('DELETE FROM guardian_daily_alerts').run();
  });

  describe('Heartbeat alert triggers', () => {
    it('triggers alert on score drop of 20+ points', async () => {
      const now = Math.floor(Date.now() / 1000);
      await setupWardWithGuardian('ward-drop', 'guardian-drop', 80, true, now - 3600);

      const res = await postWithHmac('http://localhost/api/guardian/heartbeat', {
        device_id: 'ward-drop',
        security_score: 55,
        secured_items: 3,
        total_items: 8,
        play_protect_enabled: true,
        timestamp: now,
      });
      expect(res.status).toBe(200);

      // Verify alert counter was incremented
      const alertRow = await env.DB.prepare(
        'SELECT alert_count FROM guardian_daily_alerts WHERE ward_device_id = ?',
      )
        .bind('ward-drop')
        .first<{ alert_count: number }>();
      expect(alertRow).not.toBeNull();
      expect(alertRow!.alert_count).toBe(1);
    });

    it('does not trigger alert for small score drop (< 20)', async () => {
      const now = Math.floor(Date.now() / 1000);
      await setupWardWithGuardian('ward-small', 'guardian-small', 80, true, now - 3600);

      const res = await postWithHmac('http://localhost/api/guardian/heartbeat', {
        device_id: 'ward-small',
        security_score: 65,
        secured_items: 5,
        total_items: 8,
        play_protect_enabled: true,
        timestamp: now,
      });
      expect(res.status).toBe(200);

      // No alert counter should exist
      const alertRow = await env.DB.prepare(
        'SELECT alert_count FROM guardian_daily_alerts WHERE ward_device_id = ?',
      )
        .bind('ward-small')
        .first();
      expect(alertRow).toBeNull();
    });

    it('triggers alert when Play Protect is disabled', async () => {
      const now = Math.floor(Date.now() / 1000);
      await setupWardWithGuardian('ward-pp', 'guardian-pp', 80, true, now - 3600);

      const res = await postWithHmac('http://localhost/api/guardian/heartbeat', {
        device_id: 'ward-pp',
        security_score: 80,
        secured_items: 5,
        total_items: 8,
        play_protect_enabled: false,
        timestamp: now,
      });
      expect(res.status).toBe(200);

      const alertRow = await env.DB.prepare(
        'SELECT alert_count FROM guardian_daily_alerts WHERE ward_device_id = ?',
      )
        .bind('ward-pp')
        .first<{ alert_count: number }>();
      expect(alertRow).not.toBeNull();
      expect(alertRow!.alert_count).toBe(1);
    });

    it('triggers alert when score enters RED band (< 50)', async () => {
      const now = Math.floor(Date.now() / 1000);
      await setupWardWithGuardian('ward-red', 'guardian-red', 55, true, now - 3600);

      const res = await postWithHmac('http://localhost/api/guardian/heartbeat', {
        device_id: 'ward-red',
        security_score: 45,
        secured_items: 3,
        total_items: 8,
        play_protect_enabled: true,
        timestamp: now,
      });
      expect(res.status).toBe(200);

      const alertRow = await env.DB.prepare(
        'SELECT alert_count FROM guardian_daily_alerts WHERE ward_device_id = ?',
      )
        .bind('ward-red')
        .first<{ alert_count: number }>();
      expect(alertRow).not.toBeNull();
      expect(alertRow!.alert_count).toBe(1);
    });

    it('does not trigger alert when score stays in RED band', async () => {
      const now = Math.floor(Date.now() / 1000);
      // Previous score already below 50
      await setupWardWithGuardian('ward-stay-red', 'guardian-stay-red', 40, true, now - 3600);

      const res = await postWithHmac('http://localhost/api/guardian/heartbeat', {
        device_id: 'ward-stay-red',
        security_score: 38,
        secured_items: 3,
        total_items: 8,
        play_protect_enabled: true,
        timestamp: now,
      });
      expect(res.status).toBe(200);

      // No alert (score drop is only 2, not >=20; already in RED band)
      const alertRow = await env.DB.prepare(
        'SELECT alert_count FROM guardian_daily_alerts WHERE ward_device_id = ?',
      )
        .bind('ward-stay-red')
        .first();
      expect(alertRow).toBeNull();
    });

    it('does not trigger alert on first heartbeat (no previous)', async () => {
      const now = Math.floor(Date.now() / 1000);

      // Create pairing but no previous heartbeat
      await env.DB.prepare(
        'INSERT INTO guardian_pairings (ward_device_id, guardian_device_id, ward_display_name, created_at) VALUES (?, ?, ?, ?)',
      )
        .bind('ward-first', 'guardian-first', 'Test', now)
        .run();

      const res = await postWithHmac('http://localhost/api/guardian/heartbeat', {
        device_id: 'ward-first',
        security_score: 30,
        secured_items: 2,
        total_items: 8,
        play_protect_enabled: false,
        timestamp: now,
      });
      expect(res.status).toBe(200);

      // No alert — first heartbeat has no comparison point
      const alertRow = await env.DB.prepare(
        'SELECT alert_count FROM guardian_daily_alerts WHERE ward_device_id = ?',
      )
        .bind('ward-first')
        .first();
      expect(alertRow).toBeNull();
    });
  });

  describe('Daily alert cap (anti-spam)', () => {
    it('enforces max 3 alerts per ward per day', async () => {
      const now = Math.floor(Date.now() / 1000);
      const today = new Date().toISOString().slice(0, 10);

      // Set up ward with guardian
      await env.DB.prepare(
        'INSERT INTO guardian_pairings (ward_device_id, guardian_device_id, ward_display_name, created_at) VALUES (?, ?, ?, ?)',
      )
        .bind('ward-cap', 'guardian-cap', 'Capped Ward', now)
        .run();

      // Pre-fill 3 alerts for today
      await env.DB.prepare(
        'INSERT INTO guardian_daily_alerts (ward_device_id, alert_date, alert_count) VALUES (?, ?, ?)',
      )
        .bind('ward-cap', today, 3)
        .run();

      // Insert a previous heartbeat with high score
      await env.DB.prepare(
        'INSERT INTO guardian_heartbeats (ward_device_id, security_score, secured_items, total_items, play_protect_enabled, timestamp) VALUES (?, ?, ?, ?, ?, ?)',
      )
        .bind('ward-cap', 90, 7, 8, 1, now - 3600)
        .run();

      // Send a heartbeat with a big drop — should NOT increment alert count
      const res = await postWithHmac('http://localhost/api/guardian/heartbeat', {
        device_id: 'ward-cap',
        security_score: 30,
        secured_items: 2,
        total_items: 8,
        play_protect_enabled: false,
        timestamp: now,
      });
      expect(res.status).toBe(200);

      // Alert count should still be 3 (not 4)
      const alertRow = await env.DB.prepare(
        'SELECT alert_count FROM guardian_daily_alerts WHERE ward_device_id = ? AND alert_date = ?',
      )
        .bind('ward-cap', today)
        .first<{ alert_count: number }>();
      expect(alertRow!.alert_count).toBe(3);
    });
  });

  describe('POST /api/guardian/fcm-token', () => {
    it('stores FCM token for a device', async () => {
      const res = await postWithHmac('http://localhost/api/guardian/fcm-token', {
        device_id: 'device-fcm-001',
        fcm_token: 'test-fcm-token-abc123',
      });
      expect(res.status).toBe(200);
      const body = await res.json<{ status: string }>();
      expect(body.status).toBe('ok');

      // Verify token stored
      const token = await env.DB.prepare(
        'SELECT fcm_token FROM guardian_fcm_tokens WHERE device_id = ?',
      )
        .bind('device-fcm-001')
        .first<{ fcm_token: string }>();
      expect(token).not.toBeNull();
      expect(token!.fcm_token).toBe('test-fcm-token-abc123');
    });

    it('updates FCM token on re-registration', async () => {
      // Register initial token
      await postWithHmac('http://localhost/api/guardian/fcm-token', {
        device_id: 'device-fcm-002',
        fcm_token: 'old-token',
      });

      // Re-register with new token
      const res = await postWithHmac('http://localhost/api/guardian/fcm-token', {
        device_id: 'device-fcm-002',
        fcm_token: 'new-token',
      });
      expect(res.status).toBe(200);

      const token = await env.DB.prepare(
        'SELECT fcm_token FROM guardian_fcm_tokens WHERE device_id = ?',
      )
        .bind('device-fcm-002')
        .first<{ fcm_token: string }>();
      expect(token!.fcm_token).toBe('new-token');
    });

    it('rejects missing device_id', async () => {
      const res = await postWithHmac('http://localhost/api/guardian/fcm-token', {
        fcm_token: 'some-token',
      });
      expect(res.status).toBe(400);
    });

    it('rejects missing fcm_token', async () => {
      const res = await postWithHmac('http://localhost/api/guardian/fcm-token', {
        device_id: 'some-device',
      });
      expect(res.status).toBe(400);
    });

    it('rejects requests without HMAC header', async () => {
      const res = await SELF.fetch('http://localhost/api/guardian/fcm-token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ device_id: 'x', fcm_token: 'y' }),
      });
      expect(res.status).toBe(401);
    });
  });
});

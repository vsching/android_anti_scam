// Tests for guardian "Help Me Fix This" request (E08-006).
// Covers: push to all guardians, rate limit 1/hour, rejected without guardians.

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
CREATE TABLE IF NOT EXISTS guardian_help_requests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ward_device_id TEXT NOT NULL,
    security_score INTEGER NOT NULL,
    unfixed_items TEXT NOT NULL,
    created_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_guardian_heartbeats_ward_ts
    ON guardian_heartbeats (ward_device_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_guardian_pairings_ward
    ON guardian_pairings (ward_device_id);
CREATE INDEX IF NOT EXISTS idx_guardian_pairings_guardian
    ON guardian_pairings (guardian_device_id);
CREATE INDEX IF NOT EXISTS idx_guardian_help_requests_ward_ts
    ON guardian_help_requests (ward_device_id, created_at);
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

describe('Guardian Help Request', () => {
  beforeAll(async () => {
    await applyMigrations();
  });

  beforeEach(async () => {
    await env.DB.prepare('DELETE FROM guardian_pairing_codes').run();
    await env.DB.prepare('DELETE FROM guardian_pairings').run();
    await env.DB.prepare('DELETE FROM guardian_heartbeats').run();
    await env.DB.prepare('DELETE FROM guardian_fcm_tokens').run();
    await env.DB.prepare('DELETE FROM guardian_daily_alerts').run();
    await env.DB.prepare('DELETE FROM guardian_help_requests').run();
  });

  it('sends help request successfully and records it', async () => {
    const now = Math.floor(Date.now() / 1000);

    // Set up ward with guardian
    await env.DB.prepare(
      'INSERT INTO guardian_pairings (ward_device_id, guardian_device_id, ward_display_name, created_at) VALUES (?, ?, ?, ?)',
    )
      .bind('ward-help', 'guardian-help', 'Test Ward', now)
      .run();

    const res = await postWithHmac('http://localhost/api/guardian/help-request', {
      device_id: 'ward-help',
      security_score: 45,
      unfixed_items: ['WhatsApp', 'Telegram'],
    });

    expect(res.status).toBe(200);
    const body = await res.json<{ status: string }>();
    expect(body.status).toBe('ok');

    // Verify help request was recorded
    const record = await env.DB.prepare(
      'SELECT * FROM guardian_help_requests WHERE ward_device_id = ?',
    )
      .bind('ward-help')
      .first<{ security_score: number; unfixed_items: string }>();
    expect(record).not.toBeNull();
    expect(record!.security_score).toBe(45);
    expect(JSON.parse(record!.unfixed_items)).toEqual(['WhatsApp', 'Telegram']);
  });

  it('sends push to all guardians with FCM tokens', async () => {
    const now = Math.floor(Date.now() / 1000);

    // Set up ward with two guardians
    await env.DB.batch([
      env.DB.prepare(
        'INSERT INTO guardian_pairings (ward_device_id, guardian_device_id, ward_display_name, created_at) VALUES (?, ?, ?, ?)',
      ).bind('ward-multi', 'guardian-1', 'Test Ward', now),
      env.DB.prepare(
        'INSERT INTO guardian_pairings (ward_device_id, guardian_device_id, ward_display_name, created_at) VALUES (?, ?, ?, ?)',
      ).bind('ward-multi', 'guardian-2', 'Test Ward', now),
      env.DB.prepare(
        'INSERT INTO guardian_fcm_tokens (device_id, fcm_token, updated_at) VALUES (?, ?, ?)',
      ).bind('guardian-1', 'fcm-token-1', now),
      env.DB.prepare(
        'INSERT INTO guardian_fcm_tokens (device_id, fcm_token, updated_at) VALUES (?, ?, ?)',
      ).bind('guardian-2', 'fcm-token-2', now),
    ]);

    // The actual FCM send will fail (no real Firebase) but the request should still succeed
    const res = await postWithHmac('http://localhost/api/guardian/help-request', {
      device_id: 'ward-multi',
      security_score: 30,
      unfixed_items: ['Chrome'],
    });

    expect(res.status).toBe(200);

    // Verify request was recorded
    const record = await env.DB.prepare(
      'SELECT * FROM guardian_help_requests WHERE ward_device_id = ?',
    )
      .bind('ward-multi')
      .first();
    expect(record).not.toBeNull();
  });

  it('rate limits to 1 request per hour', async () => {
    const now = Math.floor(Date.now() / 1000);

    // Set up ward with guardian
    await env.DB.prepare(
      'INSERT INTO guardian_pairings (ward_device_id, guardian_device_id, ward_display_name, created_at) VALUES (?, ?, ?, ?)',
    )
      .bind('ward-rate', 'guardian-rate', 'Test Ward', now)
      .run();

    // First request should succeed
    const res1 = await postWithHmac('http://localhost/api/guardian/help-request', {
      device_id: 'ward-rate',
      security_score: 40,
      unfixed_items: ['App1'],
    });
    expect(res1.status).toBe(200);

    // Second request within the hour should be rate limited
    const res2 = await postWithHmac('http://localhost/api/guardian/help-request', {
      device_id: 'ward-rate',
      security_score: 35,
      unfixed_items: ['App1', 'App2'],
    });
    expect(res2.status).toBe(429);
    const body = await res2.json<{ error: string }>();
    expect(body.error).toContain('Rate limit');
  });

  it('allows request after rate limit window expires', async () => {
    const now = Math.floor(Date.now() / 1000);

    // Set up ward with guardian
    await env.DB.prepare(
      'INSERT INTO guardian_pairings (ward_device_id, guardian_device_id, ward_display_name, created_at) VALUES (?, ?, ?, ?)',
    )
      .bind('ward-expired', 'guardian-expired', 'Test Ward', now)
      .run();

    // Insert an old help request from more than 1 hour ago
    const oldTimestamp = now - 3601;
    await env.DB.prepare(
      'INSERT INTO guardian_help_requests (ward_device_id, security_score, unfixed_items, created_at) VALUES (?, ?, ?, ?)',
    )
      .bind('ward-expired', 50, '["OldApp"]', oldTimestamp)
      .run();

    // New request should succeed
    const res = await postWithHmac('http://localhost/api/guardian/help-request', {
      device_id: 'ward-expired',
      security_score: 40,
      unfixed_items: ['NewApp'],
    });
    expect(res.status).toBe(200);
  });

  it('rejects request from device without guardians', async () => {
    const res = await postWithHmac('http://localhost/api/guardian/help-request', {
      device_id: 'ward-no-guardian',
      security_score: 30,
      unfixed_items: ['App1'],
    });

    expect(res.status).toBe(403);
    const body = await res.json<{ error: string }>();
    expect(body.error).toContain('no guardian pairings');
  });

  it('rejects request with missing device_id', async () => {
    const res = await postWithHmac('http://localhost/api/guardian/help-request', {
      security_score: 30,
      unfixed_items: ['App1'],
    });
    expect(res.status).toBe(400);
  });

  it('rejects request with invalid security_score', async () => {
    const res = await postWithHmac('http://localhost/api/guardian/help-request', {
      device_id: 'ward-1',
      security_score: 150,
      unfixed_items: ['App1'],
    });
    expect(res.status).toBe(400);
  });

  it('rejects request with missing unfixed_items', async () => {
    const res = await postWithHmac('http://localhost/api/guardian/help-request', {
      device_id: 'ward-1',
      security_score: 30,
    });
    expect(res.status).toBe(400);
  });

  it('rejects request without HMAC header', async () => {
    const res = await SELF.fetch('http://localhost/api/guardian/help-request', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        device_id: 'ward-1',
        security_score: 30,
        unfixed_items: ['App1'],
      }),
    });
    expect(res.status).toBe(401);
  });
});

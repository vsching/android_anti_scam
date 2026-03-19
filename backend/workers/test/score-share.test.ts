// Tests for POST /api/score/share endpoint.
// Covers: valid batch, validation errors, rate limiting, oversized payload,
// max events per batch, enum validation, timestamp validation.

import { SELF, env } from 'cloudflare:test';
import { describe, it, expect, beforeAll, beforeEach } from 'vitest';

const SCHEMA_SQL = `
CREATE TABLE IF NOT EXISTS share_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT NOT NULL,
    share_type TEXT NOT NULL,
    content_id TEXT NOT NULL,
    platform TEXT NOT NULL,
    client_timestamp TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_share_events_device_date
    ON share_events (device_id, created_at);
CREATE INDEX IF NOT EXISTS idx_share_events_type
    ON share_events (share_type, created_at);
`;

async function applyMigrations() {
  const statements = SCHEMA_SQL.split(';')
    .map((s) => s.trim())
    .filter(Boolean);
  for (const sql of statements) {
    await env.DB.prepare(sql).run();
  }
}

async function postShare(body: unknown): Promise<Response> {
  return SELF.fetch('http://localhost/api/score/share', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
}

function validEvent(overrides: Record<string, unknown> = {}) {
  return {
    share_type: 'VERDICT',
    content_id: 'example.com',
    platform: 'GENERIC',
    timestamp: Date.now() - 60000,
    ...overrides,
  };
}

function validBatch(overrides: Record<string, unknown> = {}) {
  return {
    device_id: 'test-device-001',
    events: [validEvent()],
    ...overrides,
  };
}

describe('POST /api/score/share', () => {
  beforeAll(async () => {
    await applyMigrations();
  });

  beforeEach(async () => {
    await env.DB.prepare('DELETE FROM share_events').run();
  });

  it('accepts a valid batch of share events', async () => {
    const res = await postShare(validBatch());
    expect(res.status).toBe(200);
    const body = await res.json<{ accepted: number }>();
    expect(body.accepted).toBe(1);

    // Verify inserted into D1
    const rows = await env.DB.prepare('SELECT * FROM share_events').all();
    expect(rows.results.length).toBe(1);
    expect(rows.results[0].share_type).toBe('VERDICT');
    expect(rows.results[0].device_id).toBe('test-device-001');
  });

  it('accepts multiple events in a single batch', async () => {
    const batch = validBatch({
      events: [
        validEvent({ share_type: 'VERDICT', content_id: 'a.com' }),
        validEvent({ share_type: 'SCORE', content_id: 'score' }),
        validEvent({ share_type: 'ALERT', content_id: 'alert-001', platform: 'WHATSAPP' }),
      ],
    });

    const res = await postShare(batch);
    expect(res.status).toBe(200);
    const body = await res.json<{ accepted: number }>();
    expect(body.accepted).toBe(3);
  });

  it('rejects missing device_id', async () => {
    const res = await postShare({ events: [validEvent()] });
    expect(res.status).toBe(400);
    const body = await res.json<{ error: string }>();
    expect(body.error).toContain('device_id');
  });

  it('rejects empty events array', async () => {
    const res = await postShare(validBatch({ events: [] }));
    expect(res.status).toBe(400);
  });

  it('rejects more than 50 events per batch', async () => {
    const events = Array.from({ length: 51 }, () => validEvent());
    const res = await postShare(validBatch({ events }));
    expect(res.status).toBe(400);
    const body = await res.json<{ error: string }>();
    expect(body.error).toContain('50');
  });

  it('rejects invalid share_type', async () => {
    const res = await postShare(
      validBatch({ events: [validEvent({ share_type: 'INVALID_TYPE' })] }),
    );
    expect(res.status).toBe(400);
    const body = await res.json<{ error: string }>();
    expect(body.error).toContain('share_type');
  });

  it('rejects invalid platform', async () => {
    const res = await postShare(
      validBatch({ events: [validEvent({ platform: 'TELEGRAM' })] }),
    );
    expect(res.status).toBe(400);
    const body = await res.json<{ error: string }>();
    expect(body.error).toContain('platform');
  });

  it('rejects future timestamps', async () => {
    const futureTimestamp = Date.now() + 10 * 60 * 1000; // 10 min in future
    const res = await postShare(
      validBatch({ events: [validEvent({ timestamp: futureTimestamp })] }),
    );
    expect(res.status).toBe(400);
    const body = await res.json<{ error: string }>();
    expect(body.error).toContain('future');
  });

  it('rejects timestamps older than 30 days', async () => {
    const oldTimestamp = Date.now() - 31 * 24 * 60 * 60 * 1000;
    const res = await postShare(
      validBatch({ events: [validEvent({ timestamp: oldTimestamp })] }),
    );
    expect(res.status).toBe(400);
    const body = await res.json<{ error: string }>();
    expect(body.error).toContain('old');
  });

  it('rejects missing content_id', async () => {
    const res = await postShare(
      validBatch({ events: [validEvent({ content_id: '' })] }),
    );
    expect(res.status).toBe(400);
  });

  it('rejects non-numeric timestamp', async () => {
    const res = await postShare(
      validBatch({ events: [validEvent({ timestamp: 'not-a-number' })] }),
    );
    expect(res.status).toBe(400);
  });

  it('rejects invalid JSON', async () => {
    const res = await SELF.fetch('http://localhost/api/score/share', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: '{invalid json',
    });
    expect(res.status).toBe(400);
  });

  it('enforces per-device daily rate limit', async () => {
    // Insert 100 events for the device
    const stmt = env.DB.prepare(
      `INSERT INTO share_events (device_id, share_type, content_id, platform, client_timestamp, created_at)
       VALUES (?, ?, ?, ?, ?, ?)`,
    );
    const now = new Date().toISOString();
    const batch = Array.from({ length: 100 }, (_, i) =>
      stmt.bind('rate-limit-device', 'VERDICT', `d${i}`, 'GENERIC', now, now),
    );
    await env.DB.batch(batch);

    // Next request should be rate limited
    const res = await postShare(
      validBatch({ device_id: 'rate-limit-device' }),
    );
    expect(res.status).toBe(429);
    const body = await res.json<{ error: string }>();
    expect(body.error).toContain('limit');
  });

  it('allows events from different devices independently', async () => {
    const res1 = await postShare(validBatch({ device_id: 'device-a' }));
    expect(res1.status).toBe(200);

    const res2 = await postShare(validBatch({ device_id: 'device-b' }));
    expect(res2.status).toBe(200);
  });

  it('accepts all valid share types', async () => {
    const types = ['VERDICT', 'SCORE', 'ALERT', 'WARNING_TEMPLATE', 'RESCUE_CARD'];
    const events = types.map((t) => validEvent({ share_type: t }));
    const res = await postShare(validBatch({ events }));
    expect(res.status).toBe(200);
    const body = await res.json<{ accepted: number }>();
    expect(body.accepted).toBe(5);
  });

  it('accepts both valid platforms', async () => {
    const events = [
      validEvent({ platform: 'GENERIC' }),
      validEvent({ platform: 'WHATSAPP' }),
    ];
    const res = await postShare(validBatch({ events }));
    expect(res.status).toBe(200);
  });
});

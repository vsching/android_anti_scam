// Tests for the retention cron handler.
// Verifies old reports/discoveries are deleted while recent data is preserved.

import { env } from 'cloudflare:test';
import { describe, it, expect, beforeAll } from 'vitest';
import { handleRetention } from '../src/lib/retention';

const SCHEMA_SQL = [
  `CREATE TABLE IF NOT EXISTS reports (
    id TEXT PRIMARY KEY, domain TEXT, phone_number TEXT, message_text TEXT,
    source_app TEXT, reporter_region TEXT, reporter_state TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP, verified BOOLEAN DEFAULT FALSE,
    report_count INTEGER DEFAULT 1)`,
  `CREATE TABLE IF NOT EXISTS pending_discoveries (
    id TEXT PRIMARY KEY, domain TEXT NOT NULL UNIQUE, verdict TEXT, reason TEXT,
    source TEXT, check_count INTEGER DEFAULT 1,
    last_seen_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN DEFAULT FALSE, processed_at DATETIME)`,
];

async function applySchema(db: D1Database): Promise<void> {
  for (const sql of SCHEMA_SQL) {
    await db.prepare(sql).run();
  }
}

describe('Retention handler', () => {
  beforeAll(async () => {
    await applySchema(env.DB);
  });

  it('deletes reports older than 90 days', async () => {
    const oldDate = new Date(Date.now() - 91 * 24 * 60 * 60 * 1000).toISOString();
    const recentDate = new Date().toISOString();

    await env.DB.batch([
      env.DB.prepare(
        'INSERT OR IGNORE INTO reports (id, domain, created_at) VALUES (?, ?, ?)',
      ).bind('old-report', 'old.example.com', oldDate),
      env.DB.prepare(
        'INSERT OR IGNORE INTO reports (id, domain, created_at) VALUES (?, ?, ?)',
      ).bind('recent-report', 'new.example.com', recentDate),
    ]);

    await handleRetention(env);

    const oldRow = await env.DB.prepare('SELECT id FROM reports WHERE id = ?')
      .bind('old-report')
      .first();
    const recentRow = await env.DB.prepare('SELECT id FROM reports WHERE id = ?')
      .bind('recent-report')
      .first();

    expect(oldRow).toBeNull();
    expect(recentRow).not.toBeNull();
  });

  it('deletes pending_discoveries older than 30 days', async () => {
    const oldDate = new Date(Date.now() - 31 * 24 * 60 * 60 * 1000).toISOString();
    const recentDate = new Date().toISOString();

    await env.DB.batch([
      env.DB.prepare(
        'INSERT OR IGNORE INTO pending_discoveries (id, domain, created_at) VALUES (?, ?, ?)',
      ).bind('old-pd', 'old-discovery.xyz', oldDate),
      env.DB.prepare(
        'INSERT OR IGNORE INTO pending_discoveries (id, domain, created_at) VALUES (?, ?, ?)',
      ).bind('recent-pd', 'new-discovery.xyz', recentDate),
    ]);

    await handleRetention(env);

    const oldRow = await env.DB.prepare('SELECT id FROM pending_discoveries WHERE id = ?')
      .bind('old-pd')
      .first();
    const recentRow = await env.DB.prepare('SELECT id FROM pending_discoveries WHERE id = ?')
      .bind('recent-pd')
      .first();

    expect(oldRow).toBeNull();
    expect(recentRow).not.toBeNull();
  });

  it('is idempotent — running twice with no old data is a no-op', async () => {
    // Insert fresh recent data
    const recentDate = new Date().toISOString();
    await env.DB.prepare(
      'INSERT OR IGNORE INTO reports (id, domain, created_at) VALUES (?, ?, ?)',
    ).bind('idempotent-report', 'fresh.example.com', recentDate).run();

    // Run retention twice
    await expect(handleRetention(env)).resolves.not.toThrow();
    await expect(handleRetention(env)).resolves.not.toThrow();

    // Recent data still exists
    const recentReport = await env.DB.prepare('SELECT id FROM reports WHERE id = ?')
      .bind('idempotent-report')
      .first();
    expect(recentReport).not.toBeNull();
  });
});

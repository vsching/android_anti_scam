// Tests for D1 schema migrations and seed data.
// Validates table existence, CRUD, seed alerts, unique constraints, and idempotent migration.

import { env } from 'cloudflare:test';
import { describe, it, expect, beforeAll } from 'vitest';
import { SEED_ALERTS } from '../src/data/seed-alerts';

// Inline migration SQL since readFileSync is not available in Workers runtime.
// These must stay in sync with the actual migration files.
const SCHEMA_SQL = [
  `CREATE TABLE IF NOT EXISTS reports (
    id TEXT PRIMARY KEY, domain TEXT, phone_number TEXT, message_text TEXT,
    source_app TEXT, reporter_region TEXT, reporter_state TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP, verified BOOLEAN DEFAULT FALSE,
    report_count INTEGER DEFAULT 1)`,
  `CREATE INDEX IF NOT EXISTS idx_reports_created_at ON reports(created_at)`,
  `CREATE TABLE IF NOT EXISTS alerts (
    id TEXT PRIMARY KEY, title TEXT NOT NULL, description TEXT, scam_type TEXT,
    severity TEXT, region TEXT, state TEXT, report_count INTEGER DEFAULT 0,
    source_url TEXT, created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP)`,
  `CREATE INDEX IF NOT EXISTS idx_alerts_created_at ON alerts(created_at)`,
  `CREATE TABLE IF NOT EXISTS guardian_pairs (
    id TEXT PRIMARY KEY, pair_code TEXT UNIQUE NOT NULL, parent_device_token TEXT,
    guardian_device_token TEXT, parent_score INTEGER DEFAULT 0,
    parent_last_heartbeat DATETIME, created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE)`,
  `CREATE INDEX IF NOT EXISTS idx_guardian_pairs_created_at ON guardian_pairs(created_at)`,
  `CREATE TABLE IF NOT EXISTS shared_scores (
    id TEXT PRIMARY KEY, score_percent INTEGER, secured_count INTEGER,
    total_count INTEGER, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)`,
  `CREATE INDEX IF NOT EXISTS idx_shared_scores_created_at ON shared_scores(created_at)`,
  `CREATE TABLE IF NOT EXISTS pending_discoveries (
    id TEXT PRIMARY KEY, domain TEXT NOT NULL UNIQUE, verdict TEXT, reason TEXT,
    source TEXT, check_count INTEGER DEFAULT 1,
    last_seen_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN DEFAULT FALSE, processed_at DATETIME)`,
  `CREATE INDEX IF NOT EXISTS idx_pending_unprocessed ON pending_discoveries(processed, created_at)`,
  `CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    domain TEXT NOT NULL,
    action TEXT NOT NULL,
    payload_json TEXT,
    admin_ip TEXT,
    request_id TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')))`,
  `CREATE INDEX IF NOT EXISTS idx_admin_audit_created_at ON admin_audit_logs(created_at)`,
  `CREATE INDEX IF NOT EXISTS idx_admin_audit_domain ON admin_audit_logs(domain)`,
];

const SEED_SQL = [
  `INSERT OR IGNORE INTO alerts (id, title, description, scam_type, severity, region, report_count, created_at) VALUES
    ('alert-001', 'Fake Maybank TAC SMS', 'Scammers sending SMS claiming your TAC has been requested. Links lead to phishing sites that steal banking credentials.', 'phishing', 'high', 'MY', 1247, '2026-03-01T00:00:00Z'),
    ('alert-002', 'LHDN Tax Refund Scam', 'Fake messages from "LHDN" offering tax refunds. Asks victims to install a malicious APK to "process" the refund.', 'phishing', 'high', 'MY', 892, '2026-03-05T00:00:00Z'),
    ('alert-003', 'Shopee Lucky Draw Scam', 'WhatsApp messages claiming you won a Shopee lucky draw. Link leads to a fake site that harvests personal data.', 'phishing', 'medium', 'both', 634, '2026-03-08T00:00:00Z'),
    ('alert-004', 'DBS OTP Phishing', 'SMS pretending to be from DBS Bank requesting OTP verification. Links to a cloned banking portal.', 'phishing', 'high', 'SG', 521, '2026-03-10T00:00:00Z'),
    ('alert-005', 'Parcel Delivery Fee Scam', 'Messages claiming a parcel is held and requires a small fee. Payment page steals credit card details.', 'phishing', 'medium', 'both', 403, '2026-03-12T00:00:00Z')`,
];

async function applyMigrations(db: D1Database): Promise<void> {
  for (const sql of SCHEMA_SQL) {
    await db.prepare(sql).run();
  }
  for (const sql of SEED_SQL) {
    await db.prepare(sql).run();
  }
}

describe('D1 Migrations', () => {
  beforeAll(async () => {
    await applyMigrations(env.DB);
  });

  describe('Table existence', () => {
    const tables = ['reports', 'alerts', 'guardian_pairs', 'shared_scores', 'pending_discoveries', 'admin_audit_logs'];

    for (const table of tables) {
      it(`table "${table}" exists`, async () => {
        const result = await env.DB.prepare(
          `SELECT name FROM sqlite_master WHERE type='table' AND name=?`,
        )
          .bind(table)
          .first<{ name: string }>();
        expect(result?.name).toBe(table);
      });
    }
  });

  describe('CRUD smoke tests', () => {
    it('can INSERT and SELECT from reports', async () => {
      await env.DB.prepare(
        `INSERT INTO reports (id, domain, source_app, reporter_region) VALUES (?, ?, ?, ?)`,
      )
        .bind('test-report-1', 'scam.example.com', 'whatsapp', 'MY')
        .run();

      const row = await env.DB.prepare('SELECT * FROM reports WHERE id = ?')
        .bind('test-report-1')
        .first<{ id: string; domain: string }>();
      expect(row?.domain).toBe('scam.example.com');
    });

    it('can INSERT and SELECT from guardian_pairs', async () => {
      await env.DB.prepare(
        `INSERT INTO guardian_pairs (id, pair_code) VALUES (?, ?)`,
      )
        .bind('gp-1', 'ABC123')
        .run();

      const row = await env.DB.prepare('SELECT * FROM guardian_pairs WHERE id = ?')
        .bind('gp-1')
        .first<{ pair_code: string }>();
      expect(row?.pair_code).toBe('ABC123');
    });

    it('can INSERT and SELECT from shared_scores', async () => {
      await env.DB.prepare(
        `INSERT INTO shared_scores (id, score_percent, secured_count, total_count) VALUES (?, ?, ?, ?)`,
      )
        .bind('ss-1', 85, 6, 7)
        .run();

      const row = await env.DB.prepare('SELECT * FROM shared_scores WHERE id = ?')
        .bind('ss-1')
        .first<{ score_percent: number }>();
      expect(row?.score_percent).toBe(85);
    });

    it('can INSERT and SELECT from admin_audit_logs', async () => {
      await env.DB.prepare(
        `INSERT INTO admin_audit_logs (domain, action, payload_json, admin_ip, request_id)
         VALUES (?, ?, ?, ?, ?)`,
      )
        .bind('audit-test.com', 'allowlist_add', '{"entity":"Test"}', '127.0.0.1', 'req-001')
        .run();

      const row = await env.DB.prepare('SELECT * FROM admin_audit_logs WHERE domain = ?')
        .bind('audit-test.com')
        .first<{ domain: string; action: string; admin_ip: string }>();
      expect(row?.domain).toBe('audit-test.com');
      expect(row?.action).toBe('allowlist_add');
      expect(row?.admin_ip).toBe('127.0.0.1');
    });

    it('can INSERT and SELECT from pending_discoveries', async () => {
      await env.DB.prepare(
        `INSERT INTO pending_discoveries (id, domain, verdict, reason, source) VALUES (?, ?, ?, ?, ?)`,
      )
        .bind('pd-1', 'suspicious.xyz', 'suspicious', 'TLD check', 'heuristic')
        .run();

      const row = await env.DB.prepare('SELECT * FROM pending_discoveries WHERE id = ?')
        .bind('pd-1')
        .first<{ domain: string; verdict: string }>();
      expect(row?.domain).toBe('suspicious.xyz');
      expect(row?.verdict).toBe('suspicious');
    });
  });

  describe('Seed alerts', () => {
    it('has 5 seeded alerts', async () => {
      const result = await env.DB.prepare('SELECT COUNT(*) as count FROM alerts').first<{
        count: number;
      }>();
      expect(result?.count).toBe(5);
    });

    it('seed alert IDs match expected values', async () => {
      const rows = await env.DB.prepare('SELECT id FROM alerts ORDER BY id')
        .all<{ id: string }>();
      const ids = rows.results.map((r) => r.id);
      expect(ids).toEqual([
        'alert-001', 'alert-002', 'alert-003', 'alert-004', 'alert-005',
      ]);
    });

    it('TS seed data matches SQL seed data', async () => {
      const rows = await env.DB.prepare(
        'SELECT id, title, scam_type, severity, region, report_count FROM alerts ORDER BY id',
      ).all<{
        id: string; title: string; scam_type: string;
        severity: string; region: string; report_count: number;
      }>();

      for (let i = 0; i < SEED_ALERTS.length; i++) {
        const dbRow = rows.results[i];
        const tsRow = SEED_ALERTS[i];
        expect(dbRow.id).toBe(tsRow.id);
        expect(dbRow.title).toBe(tsRow.title);
        expect(dbRow.scam_type).toBe(tsRow.scam_type);
        expect(dbRow.severity).toBe(tsRow.severity);
        expect(dbRow.region).toBe(tsRow.region);
        expect(dbRow.report_count).toBe(tsRow.report_count);
      }
    });
  });

  describe('Unique constraints', () => {
    it('rejects duplicate pending_discoveries domain', async () => {
      await env.DB.prepare(
        `INSERT OR IGNORE INTO pending_discoveries (id, domain, verdict, source) VALUES (?, ?, ?, ?)`,
      )
        .bind('pd-dup-1', 'unique-test.xyz', 'suspicious', 'heuristic')
        .run();

      await expect(
        env.DB.prepare(
          `INSERT INTO pending_discoveries (id, domain, verdict, source) VALUES (?, ?, ?, ?)`,
        )
          .bind('pd-dup-2', 'unique-test.xyz', 'dangerous', 'heuristic')
          .run(),
      ).rejects.toThrow();
    });

    it('rejects duplicate guardian_pairs pair_code', async () => {
      await env.DB.prepare(
        `INSERT OR IGNORE INTO guardian_pairs (id, pair_code) VALUES (?, ?)`,
      )
        .bind('gp-dup-1', 'UNIQ01')
        .run();

      await expect(
        env.DB.prepare(
          `INSERT INTO guardian_pairs (id, pair_code) VALUES (?, ?)`,
        )
          .bind('gp-dup-2', 'UNIQ01')
          .run(),
      ).rejects.toThrow();
    });
  });

  describe('Idempotent migration', () => {
    it('running migrations twice does not error', async () => {
      await expect(applyMigrations(env.DB)).resolves.not.toThrow();
    });

    it('still has 5 alerts after re-migration', async () => {
      const result = await env.DB.prepare('SELECT COUNT(*) as count FROM alerts').first<{
        count: number;
      }>();
      expect(result?.count).toBe(5);
    });
  });

  describe('Indexes', () => {
    it('has created_at indexes on all tables', async () => {
      const indexes = await env.DB.prepare(
        `SELECT name, tbl_name FROM sqlite_master WHERE type='index' AND name LIKE 'idx_%'`,
      ).all<{ name: string; tbl_name: string }>();

      const indexNames = indexes.results.map((r) => r.name);
      expect(indexNames).toContain('idx_reports_created_at');
      expect(indexNames).toContain('idx_alerts_created_at');
      expect(indexNames).toContain('idx_guardian_pairs_created_at');
      expect(indexNames).toContain('idx_shared_scores_created_at');
      expect(indexNames).toContain('idx_pending_unprocessed');
    });
  });
});

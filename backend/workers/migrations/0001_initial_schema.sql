-- E01-005: Initial schema for Safe Anot? D1 database
-- Tables: reports, alerts, guardian_pairs, shared_scores, pending_discoveries

-- User-submitted scam reports
CREATE TABLE IF NOT EXISTS reports (
  id TEXT PRIMARY KEY,
  domain TEXT,
  phone_number TEXT,
  message_text TEXT,
  source_app TEXT,
  reporter_region TEXT,
  reporter_state TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  verified BOOLEAN DEFAULT FALSE,
  report_count INTEGER DEFAULT 1
);
CREATE INDEX IF NOT EXISTS idx_reports_created_at ON reports(created_at);

-- Scam alerts (curated by pipeline)
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

-- Family Guardian pairings (schema only — logic in E08)
CREATE TABLE IF NOT EXISTS guardian_pairs (
  id TEXT PRIMARY KEY,
  pair_code TEXT UNIQUE NOT NULL,
  parent_device_token TEXT,
  guardian_device_token TEXT,
  parent_score INTEGER DEFAULT 0,
  parent_last_heartbeat DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  active BOOLEAN DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_guardian_pairs_created_at ON guardian_pairs(created_at);

-- Shared score cards (for viral sharing)
CREATE TABLE IF NOT EXISTS shared_scores (
  id TEXT PRIMARY KEY,
  score_percent INTEGER,
  secured_count INTEGER,
  total_count INTEGER,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_shared_scores_created_at ON shared_scores(created_at);

-- Suspicious domains queued for pipeline review (async discovery)
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

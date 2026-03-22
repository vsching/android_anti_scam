CREATE TABLE IF NOT EXISTS admin_audit_logs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  domain TEXT NOT NULL,
  action TEXT NOT NULL,
  payload_json TEXT,
  admin_ip TEXT,
  request_id TEXT,
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX idx_admin_audit_created_at ON admin_audit_logs(created_at);
CREATE INDEX idx_admin_audit_domain ON admin_audit_logs(domain);

-- Share event analytics table.
-- Stores anonymised share events for tracking feature adoption.
CREATE TABLE IF NOT EXISTS share_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT NOT NULL,
    share_type TEXT NOT NULL,
    content_id TEXT NOT NULL,
    platform TEXT NOT NULL,
    client_timestamp TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Index for per-device rate limiting queries.
CREATE INDEX IF NOT EXISTS idx_share_events_device_date
    ON share_events (device_id, created_at);

-- Index for analytics queries by share type.
CREATE INDEX IF NOT EXISTS idx_share_events_type
    ON share_events (share_type, created_at);

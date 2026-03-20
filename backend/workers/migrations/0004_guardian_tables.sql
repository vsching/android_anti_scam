-- Guardian pairing tables for Phone Shield feature.
-- Allows guardians to monitor ward devices.

-- Pairing codes: short-lived codes for initiating a guardian pairing.
CREATE TABLE IF NOT EXISTS guardian_pairing_codes (
    code TEXT PRIMARY KEY,
    ward_device_id TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    claimed INTEGER NOT NULL DEFAULT 0
);

-- Guardian pairings: active ward-guardian relationships.
CREATE TABLE IF NOT EXISTS guardian_pairings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ward_device_id TEXT NOT NULL,
    guardian_device_id TEXT NOT NULL,
    ward_display_name TEXT DEFAULT '',
    guardian_display_name TEXT DEFAULT '',
    created_at INTEGER NOT NULL,
    UNIQUE(ward_device_id, guardian_device_id)
);

-- Guardian heartbeats: periodic security status reports from ward devices.
CREATE TABLE IF NOT EXISTS guardian_heartbeats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ward_device_id TEXT NOT NULL,
    security_score INTEGER NOT NULL,
    secured_items INTEGER NOT NULL,
    total_items INTEGER NOT NULL,
    play_protect_enabled INTEGER NOT NULL DEFAULT 1,
    timestamp INTEGER NOT NULL
);

-- Indexes for efficient queries.
CREATE INDEX IF NOT EXISTS idx_guardian_heartbeats_ward_ts
    ON guardian_heartbeats (ward_device_id, timestamp);

CREATE INDEX IF NOT EXISTS idx_guardian_pairings_ward
    ON guardian_pairings (ward_device_id);

CREATE INDEX IF NOT EXISTS idx_guardian_pairings_guardian
    ON guardian_pairings (guardian_device_id);

-- FCM tokens for push notifications to guardian devices.
CREATE TABLE IF NOT EXISTS guardian_fcm_tokens (
    device_id TEXT PRIMARY KEY,
    fcm_token TEXT NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Daily alert counters for anti-spam (max 3 alerts per ward per day).
CREATE TABLE IF NOT EXISTS guardian_daily_alerts (
    ward_device_id TEXT NOT NULL,
    alert_date TEXT NOT NULL,
    alert_count INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (ward_device_id, alert_date)
);

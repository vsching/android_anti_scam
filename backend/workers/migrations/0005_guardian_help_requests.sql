-- Help request tracking for "Help Me Fix This" feature.
-- Stores help requests sent by wards to their guardians (rate limited to 1/hour).

CREATE TABLE IF NOT EXISTS guardian_help_requests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ward_device_id TEXT NOT NULL,
    security_score INTEGER NOT NULL,
    unfixed_items TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_guardian_help_requests_ward_ts
    ON guardian_help_requests (ward_device_id, created_at);

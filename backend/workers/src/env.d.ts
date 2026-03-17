// Type definitions for Cloudflare Worker bindings.
// Kept in sync with wrangler.toml bindings.

interface Env {
  // KV — allowlist + new domain discoveries only (NOT bulk domain store)
  VERDICTS: KVNamespace;

  // R2 — bulk scam database (SQLite, Bloom filter, delta files)
  DATA_BUCKET: R2Bucket;

  // D1 — reports, alerts, guardian pairings, pending discoveries
  DB: D1Database;

  // Environment
  ENVIRONMENT: string;

  // Firebase (for FCM push notifications)
  FIREBASE_API_KEY: string;
  FIREBASE_PROJECT_ID: string;
}

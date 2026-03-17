# E01: Backend API + Scam Database

> **Phase:** 1 (MVP)
> **Priority:** P0 — Foundation for all other features
> **Depends On:** None
> **Estimated Effort:** 1-2 weeks

---

## Overview

Set up the Cloudflare Workers backend API, seed the scam domain database in R2 (bulk) and KV (allowlist + new discoveries only), and create the core `/api/check` endpoint that powers the link checker. Generate device-first artifacts (SQLite export, Bloom filter) so the Android app can handle 95%+ of checks locally. This is the foundation everything else builds on.

## Technical Specs

- Architecture: [docs/INFRASTRUCTURE_ARCHITECTURE.md](../INFRASTRUCTURE_ARCHITECTURE.md)
- Technical Requirements: [docs/TECHNICAL_REQUIREMENTS.md](../TECHNICAL_REQUIREMENTS.md)
- Data Sources: [docs/GROWTH_AND_DATA_STRATEGY.md](../GROWTH_AND_DATA_STRATEGY.md) (Part 4)

## Tech Stack

- Cloudflare Workers (TypeScript)
- Cloudflare KV (allowlist + new domain discoveries only — NOT bulk domain store)
- Cloudflare R2 (bulk scam database, SQLite exports, Bloom filters)
- Cloudflare D1 (reports, alerts, guardian pairings, pending discoveries)
- Cloudflare Rate Limiting (built-in Rulesets via dashboard/API/Terraform — NOT wrangler.toml)
- Wrangler CLI for deployment

---

## Issues

### E01-001: Project Scaffold + Wrangler Setup

Set up the Cloudflare Workers project with Wrangler, TypeScript, local development environment, security middleware, and pipeline scaffold.

**Acceptance Criteria:**
- `wrangler dev` runs locally and serves a hello-world Worker
- TypeScript configured with strict mode
- KV, R2, and D1 bindings declared in `wrangler.toml`
- Cloudflare rate limiting rules provisioned via Rulesets IaC (documented as comments in `wrangler.toml`)
- `.dev.vars` for local secrets (Firebase key placeholder)
- `.env.example` documenting all required secrets
- CORS middleware allowing safeanot.com origin + app requests
- Security headers middleware (X-Content-Type-Options, X-Frame-Options, Permissions-Policy, etc.)
- CI/CD: GitHub Actions workflow deploys to Cloudflare on push to main
- Pipeline scaffold with `pyproject.toml` and README
- Logging redaction policy: never log full URLs checked, phone numbers, or message text
- Project lives in `/backend/workers/` (Workers) and `/backend/pipeline/` (Python pipeline)

**Test Cases:**
- Worker responds to GET / with 200 and version info
- Worker responds to unknown routes with 404
- CORS headers present on responses
- Security headers present on responses

---

### E01-005: D1 Database Schema + Migrations

Create the D1 database with all required tables for v1, including the `pending_discoveries` table for the async discovery pipeline.

**Acceptance Criteria:**
- D1 database `safeanot-db` created via Wrangler
- Tables created:
  - `reports` (scam reports from users)
  - `alerts` (curated scam alerts)
  - `guardian_pairs` (family guardian pairings -- schema only, logic in E08)
  - `shared_scores` (viral share card data)
  - `pending_discoveries` (suspicious domains queued for pipeline review)
- All tables include `created_at` timestamps and retention-friendly indexes
- Migration script in `/backend/workers/migrations/`
- Schema matches INFRASTRUCTURE_ARCHITECTURE.md (corrected version)
- Seed data: 5 initial alerts
- Retention policy: scheduled function deletes reports older than 90 days
- Note: use least-privilege Cloudflare API tokens (separate read-only for Workers, write for pipeline)

**Test Cases:**
- All tables exist after migration
- Can INSERT and SELECT from each table
- Seed alerts are present
- Unique constraints enforced correctly
- Idempotent migration (running twice doesn't error)
- `pending_discoveries` table accepts and returns discovery rows
- `created_at` indexes exist on all tables

---

### E01-002: Scam Domain Database Seed

Download open-source scam domain feeds, process them, and upload to R2 as versioned JSON files plus device-first artifacts (SQLite export, Bloom filter). Seed KV with allowlisted safe domains only. Process any pending discoveries from D1.

**Acceptance Criteria:**
- Python script (`/backend/pipeline/seed_database.py`) that:
  - Downloads from Phishing.Database, Scam-Blocklist, Phishing Army (3 sources minimum)
  - Deduplicates and normalizes domains (lowercase, strip protocol/path/www)
  - Generates `domains-full-YYYY-MM-DD.json` and `domains-delta-YYYY-MM-DD.json`
  - Generates `domains-full-YYYY-MM-DD.sqlite` (for app bundling and R2 download)
  - Generates `bloom-YYYY-MM-DD.bin` (~600KB Bloom filter for 500K domains, <1% false positive rate; ~200-300KB compressed)
  - Generates `manifest-YYYY-MM-DD.json` describing all artifacts, sizes, and checksums
  - Uploads all artifacts to R2 bucket `safeanot-data`
  - Updates `latest.json` manifest in R2
  - Seeds KV namespace with allowlisted safe domains (Malaysian banks, SG banks, gov domains, e-commerce)
  - Reads `pending_discoveries` from D1, promotes confirmed domains to bulk DB
- R2 contains at least 100K+ scam domains after initial seed
- Allowlist contains all domains from GROWTH_AND_DATA_STRATEGY.md Section 3.3

**Test Cases:**
- Script completes without error
- R2 file is valid JSON with expected schema: `[{ "domain": "...", "verdict": "dangerous", "reason": "...", "source": "..." }]`
- Allowlist domains return "safe" verdict
- Known scam domain from Phishing.Database returns "dangerous"
- Source fetch failure handling (network error, 404, malformed data)
- Idempotent rerun produces same output
- SQLite export is valid and queryable (`SELECT * FROM domains WHERE domain = ?`)
- Bloom filter has expected false positive rate (<1% tested against random domains)
- Manifest JSON contains correct checksums and sizes

---

### E01-006: R2 Download Endpoints

Serve the scam database files from R2 for the Android app to download daily. Worker streams R2 objects directly (no presigned URLs).

**Acceptance Criteria:**
- GET `/api/data/latest` returns metadata: `{ "version": "2026-03-16", "full_size_kb": 12000, "delta_size_kb": 150, "bloom_size_kb": 600, "sqlite_size_kb": 15000 }`
- GET `/api/data/full` streams the full SQLite database directly from R2 via `env.R2_BUCKET.get(key)`
- GET `/api/data/delta?since=2026-03-15` streams the delta file from R2
- GET `/api/data/bloom` streams the Bloom filter from R2
- Response includes `Content-Length` from R2 object metadata for download progress
- Cached metadata in KV (1-hour TTL)
- Worker IS the download endpoint -- no redirects or presigned URLs needed

**Test Cases:**
- /api/data/latest returns valid metadata JSON
- /api/data/full streams R2 object with correct Content-Length
- /api/data/delta with valid date returns delta file
- /api/data/delta with future date returns empty delta
- /api/data/delta with very old date returns full database
- Bad date format returns 400
- Missing R2 object returns 404 gracefully
- Stale manifest handling (manifest points to deleted object)

---

### E01-003: /api/check Endpoint (Link Checker)

The core domain-check endpoint. Receives a URL/domain, checks against KV (new discoveries) and runs heuristic engine for unknowns. Most checks will be handled on-device; this handles the ~5% that aren't. Writes suspicious unknown domains to D1 for pipeline review.

**Acceptance Criteria:**
- POST `/api/check` accepts `{ "url": "https://maybank-secure-update.xyz/login" }` or `{ "domain": "maybank-secure-update.xyz" }`
- Extracts domain from URL (strips protocol, path, query params, www)
- Check flow: Cache API -> KV -> heuristic engine
- Returns `{ "domain": "...", "verdict": "dangerous|safe|suspicious|unknown", "reason": "...", "confidence": 0.0-1.0, "details": { ... } }`
- Heuristic engine checks:
  - Typosquatting (Levenshtein distance from allowlisted bank/gov domains)
  - Suspicious TLD (.xyz, .top, .buzz, .click, .loan, .win, .gq, .ml, .cf, .tk, .ga)
  - Bank-name pattern matching (maybank, cimb, rhb, etc. + random suffix)
  - URL shortener detection (bit.ly, tinyurl, etc.)
- Results cached in Cache API only (1-hour TTL). KV is NOT written by /api/check — only pipeline writes confirmed discoveries to KV
- Miss coalescing: concurrent requests for the same unknown domain don't duplicate heuristic work
- Rate limited: 100 req/min per IP via Cloudflare rate limiting rules
- Suspicious unknown domains written to D1 `pending_discoveries` for pipeline review
- Discovery module writes to D1 (not a full queue -- simplified for v1)

**Test Cases:**
- Known scam domain -> "dangerous"
- Known safe domain -> "safe"
- Typosquatting domain (e.g., "maybannk2u.com") -> "suspicious" with reason
- Suspicious TLD (e.g., "maybank.xyz") -> "suspicious"
- URL shortener -> "suspicious" with reason
- Completely unknown domain -> "unknown"
- Rate limit exceeded -> 429 response
- Invalid input -> 400 response
- Cache API hit returns without KV read
- KV takes precedence over heuristic
- Punycode/IDN domain handling (e.g., xn-- encoded domains)
- Subdomain handling (sub.maybank.xyz should still flag maybank.xyz pattern)
- Concurrent miss coalescing (same domain, concurrent requests share one evaluation)
- Oversized payload rejection (>10KB body returns 413)

---

### E01-004: /api/alerts Endpoint (Scam Feed)

Serve trending scam alerts from D1. Cached in KV with 15-min TTL.

**Acceptance Criteria:**
- GET `/api/alerts` returns list of scam alerts sorted by recency
- GET `/api/alerts?region=MY` filters by region (MY/SG/both)
- Response: `[{ "id": "...", "title": "...", "description": "...", "scam_type": "...", "severity": "high|medium|low", "region": "...", "report_count": 0, "created_at": "..." }]`
- Results cached in KV (15-min TTL)
- D1 table `alerts` seeded with 5 initial alerts (from prototype data)

**Test Cases:**
- GET /api/alerts returns 200 with array
- Region filter works
- Cached response matches fresh response within TTL
- Empty region returns all alerts
- Invalid region value returns all alerts (not error)
- Cache invalidation after TTL (stale cache is replaced)

---

## Implementation Order

1. **E01-001** -- Scaffold first (everything depends on this)
2. **E01-005** -- D1 schema (needed by pipeline for pending_discoveries and by endpoints)
3. **E01-002** -- Seed the scam database with device-first artifacts (needed by /api/check and R2 downloads)
4. **E01-006** -- R2 download endpoints (foundational for device-first architecture)
5. **E01-003** -- Link checker endpoint (residual path after device checks, ~5% of traffic)
6. **E01-004** -- Alerts endpoint (quick, independent)

# E01: Backend API + Scam Database

> **Phase:** 1 (MVP)
> **Priority:** P0 — Foundation for all other features
> **Depends On:** None
> **Estimated Effort:** 1-2 weeks

---

## Overview

Set up the Cloudflare Workers backend API, seed the scam domain database in R2 + KV, and create the core `/api/check` endpoint that powers the link checker. This is the foundation everything else builds on.

## Technical Specs

- Architecture: [docs/INFRASTRUCTURE_ARCHITECTURE.md](../INFRASTRUCTURE_ARCHITECTURE.md)
- Technical Requirements: [docs/TECHNICAL_REQUIREMENTS.md](../TECHNICAL_REQUIREMENTS.md)
- Data Sources: [docs/GROWTH_AND_DATA_STRATEGY.md](../GROWTH_AND_DATA_STRATEGY.md) (Part 4)

## Tech Stack

- Cloudflare Workers (TypeScript)
- Cloudflare KV (new domain discoveries)
- Cloudflare R2 (bulk scam database)
- Cloudflare D1 (reports, alerts, guardian pairings)
- Wrangler CLI for deployment

---

## Issues

### E01-001: Project Scaffold + Wrangler Setup

Set up the Cloudflare Workers project with Wrangler, TypeScript, and local development environment.

**Acceptance Criteria:**
- `wrangler dev` runs locally and serves a hello-world Worker
- TypeScript configured with strict mode
- KV, R2, and D1 bindings declared in `wrangler.toml`
- `.dev.vars` for local secrets (Firebase key placeholder)
- CI/CD: GitHub Actions workflow deploys to Cloudflare on push to main
- Project lives in `/backend/workers/`

**Test Cases:**
- Worker responds to GET / with 200 and version info
- Worker responds to unknown routes with 404

---

### E01-002: Scam Domain Database Seed

Download open-source scam domain feeds, process them, and upload to R2 as versioned JSON/SQLite files. Seed KV with allowlisted safe domains.

**Acceptance Criteria:**
- Python script (`/backend/pipeline/seed_database.py`) that:
  - Downloads from Phishing.Database, Scam-Blocklist, Phishing Army (3 sources minimum)
  - Deduplicates and normalizes domains (lowercase, strip protocol/path/www)
  - Generates `domains-full-YYYY-MM-DD.json` and `domains-delta-YYYY-MM-DD.json`
  - Uploads to R2 bucket `safeanot-data`
  - Seeds KV namespace with allowlisted safe domains (Malaysian banks, SG banks, gov domains, e-commerce)
- R2 contains at least 100K+ scam domains after initial seed
- Allowlist contains all domains from GROWTH_AND_DATA_STRATEGY.md Section 3.3

**Test Cases:**
- Script completes without error
- R2 file is valid JSON with expected schema: `[{ "domain": "...", "verdict": "dangerous", "reason": "...", "source": "..." }]`
- Allowlist domains return "safe" verdict
- Known scam domain from Phishing.Database returns "dangerous"

---

### E01-003: /api/check Endpoint (Link Checker)

The core domain-check endpoint. Receives a URL/domain, checks against KV (new discoveries) and runs heuristic engine for unknowns. Most checks will be handled on-device; this handles the ~5% that aren't.

**Acceptance Criteria:**
- POST `/api/check` accepts `{ "url": "https://maybank-secure-update.xyz/login" }` or `{ "domain": "maybank-secure-update.xyz" }`
- Extracts domain from URL (strips protocol, path, query params, www)
- Check flow: Cache API → KV → heuristic engine
- Returns `{ "domain": "...", "verdict": "dangerous|safe|suspicious|unknown", "reason": "...", "confidence": 0.0-1.0, "details": { ... } }`
- Heuristic engine checks:
  - Typosquatting (Levenshtein distance from allowlisted bank/gov domains)
  - Suspicious TLD (.xyz, .top, .buzz, .click, .loan, .win, .gq, .ml, .cf, .tk, .ga)
  - Bank-name pattern matching (maybank, cimb, rhb, etc. + random suffix)
  - URL shortener detection (bit.ly, tinyurl, etc.)
- Results cached in Cache API (1-hour TTL) and KV (for confirmed discoveries)
- Miss coalescing: concurrent requests for the same unknown domain don't duplicate heuristic work
- Rate limited: 100 req/min per IP

**Test Cases:**
- Known scam domain → "dangerous"
- Known safe domain → "safe"
- Typosquatting domain (e.g., "maybannk2u.com") → "suspicious" with reason
- Suspicious TLD (e.g., "maybank.xyz") → "suspicious"
- URL shortener → "suspicious" with reason
- Completely unknown domain → "unknown"
- Rate limit exceeded → 429 response
- Invalid input → 400 response

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

---

### E01-005: D1 Database Schema + Migrations

Create the D1 database with all required tables for v1.

**Acceptance Criteria:**
- D1 database `safeanot-db` created via Wrangler
- Tables created:
  - `reports` (scam reports from users)
  - `alerts` (curated scam alerts)
  - `guardian_pairs` (family guardian pairings — schema only, logic in E08)
  - `shared_scores` (viral share card data)
- Migration script in `/backend/workers/migrations/`
- Schema matches INFRASTRUCTURE_ARCHITECTURE.md (corrected version)
- Seed data: 5 initial alerts

**Test Cases:**
- All tables exist after migration
- Can INSERT and SELECT from each table
- Seed alerts are present

---

### E01-006: R2 Delta Download Endpoint

Serve the scam database delta files from R2 for the Android app to download daily.

**Acceptance Criteria:**
- GET `/api/data/latest` returns metadata: `{ "version": "2026-03-16", "full_url": "...", "delta_url": "...", "delta_size_kb": 150, "full_size_kb": 12000 }`
- GET `/api/data/full` redirects to R2 presigned URL for full database
- GET `/api/data/delta?since=2026-03-15` redirects to R2 presigned URL for delta since that date
- Response includes `Content-Length` for download progress
- Cached metadata in KV (1-hour TTL)

**Test Cases:**
- /api/data/latest returns valid metadata
- /api/data/full returns valid redirect to R2
- /api/data/delta with valid date returns delta file
- /api/data/delta with future date returns empty delta
- /api/data/delta with very old date returns full database redirect

---

## Implementation Order

1. **E01-001** — Scaffold first (everything depends on this)
2. **E01-005** — D1 schema (needed by other endpoints)
3. **E01-002** — Seed the scam database (needed by /api/check)
4. **E01-003** — Link checker endpoint (core feature)
5. **E01-004** — Alerts endpoint (quick, depends on D1)
6. **E01-006** — R2 delta download (app needs this for device-first caching)

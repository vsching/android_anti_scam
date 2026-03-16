# E01 Implementation Plan: Backend API + Scam Database

> Generated from: docs/development-plan/E01-backend-api-scam-database.md
> Technical specs referenced: docs/INFRASTRUCTURE_ARCHITECTURE.md
> Date: 2026-03-16

## Pre-Implementation Checklist
- [ ] Dependencies complete: None
- [ ] Technical specs reviewed: INFRASTRUCTURE_ARCHITECTURE.md
- [ ] Plan reviewed by Codex
- [ ] Plan approved by user

---

## Issue E01-001: Project Scaffold + Wrangler Setup

### Tasks
1. File: `/backend/workers/package.json`  
   Action: Create  
   Details: Define the Workers package, Wrangler/Vitest/TypeScript scripts, and pinned dev dependencies for local development and CI.

2. File: `/backend/workers/tsconfig.json`  
   Action: Create  
   Details: Enable strict TypeScript, Workers-compatible module settings, path-safe compilation, and Vitest type support.

3. File: `/backend/workers/wrangler.toml`  
   Action: Create  
   Details: Configure Worker name, entrypoint, compatibility date, and placeholder bindings for KV, R2, and D1.

4. File: `/backend/workers/.dev.vars`  
   Action: Create  
   Details: Add local-only placeholders for Firebase credentials and any pipeline/admin secrets needed by `wrangler dev`.

5. File: `/backend/workers/.gitignore`  
   Action: Create  
   Details: Exclude `.dev.vars`, `.wrangler/`, coverage output, and local SQLite/D1 artifacts.

6. File: `/backend/workers/src/index.ts`  
   Action: Create  
   Details: Implement the initial Worker entrypoint with `GET /` version response and default 404 routing.

7. File: `/backend/workers/src/router.ts`  
   Action: Create  
   Details: Centralize route matching so later endpoints can be added without inflating the entrypoint.

8. File: `/backend/workers/src/env.d.ts`  
   Action: Create  
   Details: Declare typed bindings for KV, R2 bucket, D1 database, cache-related env vars, and Firebase placeholders.

9. File: `/.github/workflows/deploy-workers.yml`  
   Action: Create  
   Details: Add GitHub Actions workflow to install dependencies, run tests, and deploy with Wrangler on push to `main`.

10. File: `/backend/workers/vitest.config.ts`  
    Action: Create  
    Details: Configure Workers test environment and coverage defaults for route-level tests.

11. File: `/backend/workers/test/app.test.ts`  
    Action: Create  
    Details: Verify `GET /` returns 200 with version metadata and unknown routes return 404.

### Tests
- `/backend/workers/test/app.test.ts` — tests hello-world root route and 404 behavior
- `/.github/workflows/deploy-workers.yml` — runs backend test suite before deploy

### Acceptance Criteria
- `wrangler dev` runs locally and serves a hello-world Worker
- TypeScript configured with strict mode
- KV, R2, and D1 bindings declared in `wrangler.toml`
- `.dev.vars` for local secrets (Firebase key placeholder)
- CI/CD: GitHub Actions workflow deploys to Cloudflare on push to main
- Project lives in `/backend/workers/`

---

## Issue E01-002: Scam Domain Database Seed

### Tasks
1. File: `/backend/pipeline/pyproject.toml`  
   Action: Create  
   Details: Define Python runtime, CLI entrypoint, and dependencies for HTTP fetching, Cloudflare auth, and pytest.

2. File: `/backend/pipeline/seed_database.py`  
   Action: Create  
   Details: Implement orchestration for feed download, normalization, deduplication, versioned artifact generation, R2 upload, KV allowlist seeding, and latest-manifest generation.

3. File: `/backend/pipeline/src/sources.py`  
   Action: Create  
   Details: Add adapters for Phishing.Database, Scam-Blocklist, and Phishing Army with retry, timeout, and source tagging.

4. File: `/backend/pipeline/src/normalize.py`  
   Action: Create  
   Details: Normalize raw URLs/domains by lowercasing, stripping protocol/path/query/`www`, validating hostname shape, and deduplicating.

5. File: `/backend/pipeline/src/allowlist.py`  
   Action: Create  
   Details: Encode the Section 3.3 allowlist for Malaysian banks, Singapore banks, Malaysian government domains, and e-commerce domains as a canonical seed set.

6. File: `/backend/pipeline/src/cloudflare.py`  
   Action: Create  
   Details: Wrap R2 upload and KV write operations so pipeline logic stays testable and deployment credentials stay isolated.

7. File: `/backend/pipeline/tests/test_normalize.py`  
   Action: Create  
   Details: Cover normalization edge cases including protocols, paths, query strings, uppercase input, and `www` prefixes.

8. File: `/backend/pipeline/tests/test_seed_database.py`  
   Action: Create  
   Details: Validate output schema, deduplication, allowlist seeding, minimum-source merge behavior, and manifest artifact generation.

### Tests
- `/backend/pipeline/tests/test_normalize.py` — tests domain normalization and canonicalization
- `/backend/pipeline/tests/test_seed_database.py` — tests feed merge, JSON schema, allowlist seeding, and R2/KV upload orchestration

### Acceptance Criteria
- Python script (`/backend/pipeline/seed_database.py`) that:
  - Downloads from Phishing.Database, Scam-Blocklist, Phishing Army (3 sources minimum)
  - Deduplicates and normalizes domains (lowercase, strip protocol/path/www)
  - Generates `domains-full-YYYY-MM-DD.json` and `domains-delta-YYYY-MM-DD.json`
  - Uploads to R2 bucket `safeanot-data`
  - Seeds KV namespace with allowlisted safe domains (Malaysian banks, SG banks, gov domains, e-commerce)
- R2 contains at least 100K+ scam domains after initial seed
- Allowlist contains all domains from GROWTH_AND_DATA_STRATEGY.md Section 3.3

---

## Issue E01-003: /api/check Endpoint (Link Checker)

### Tasks
1. File: `/backend/workers/src/router.ts`  
   Action: Modify  
   Details: Register `POST /api/check` and keep route dispatch isolated from handler implementation.

2. File: `/backend/workers/src/routes/check.ts`  
   Action: Create  
   Details: Implement request parsing, input validation, domain extraction, cache lookup, KV lookup, heuristic fallback, response shaping, and 400/429 handling.

3. File: `/backend/workers/src/lib/domain.ts`  
   Action: Create  
   Details: Add URL/domain parsing helpers that strip protocol, path, query params, fragments, and `www`.

4. File: `/backend/workers/src/lib/heuristics.ts`  
   Action: Create  
   Details: Implement typosquatting detection against allowlisted bank/gov domains, suspicious TLD checks, bank-name pattern checks, and URL shortener detection.

5. File: `/backend/workers/src/lib/cache.ts`  
   Action: Create  
   Details: Encapsulate Cache API read/write behavior with 1-hour TTL headers and cache-key normalization.

6. File: `/backend/workers/src/lib/inflight.ts`  
   Action: Create  
   Details: Add per-domain in-memory miss coalescing so concurrent unknown-domain checks share one heuristic computation per isolate.

7. File: `/backend/workers/src/lib/rate-limit.ts`  
   Action: Create  
   Details: Implement 100 req/min per-IP rate limiting using KV counters with minute buckets.

8. File: `/backend/workers/src/env.d.ts`  
   Action: Modify  
   Details: Extend typed env bindings for rate-limit namespace keys, heuristic thresholds, and cache TTL constants.

9. File: `/backend/workers/test/check.test.ts`  
   Action: Create  
   Details: Cover known scam, known safe, typosquatting, suspicious TLD, URL shortener, unknown domain, invalid payload, and rate-limit cases.

### Tests
- `/backend/workers/test/check.test.ts` — tests `/api/check` request validation, lookup flow, heuristics, caching behavior, and rate limiting

### Acceptance Criteria
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

---

## Issue E01-004: /api/alerts Endpoint (Scam Feed)

### Tasks
1. File: `/backend/workers/src/router.ts`  
   Action: Modify  
   Details: Register `GET /api/alerts` in the shared router.

2. File: `/backend/workers/src/routes/alerts.ts`  
   Action: Create  
   Details: Implement D1 query path, optional `region` filter (`MY`, `SG`, `both`), response serialization, and KV-backed 15-minute caching.

3. File: `/backend/workers/src/lib/cache.ts`  
   Action: Modify  
   Details: Add reusable helpers for serialized KV cache entries and cache-key construction for filtered list endpoints.

4. File: `/backend/workers/src/data/seed-alerts.ts`  
   Action: Create  
   Details: Define the five initial curated alerts that seed D1 and establish the response schema expected by the endpoint.

5. File: `/backend/workers/test/alerts.test.ts`  
   Action: Create  
   Details: Verify uncached and cached reads, recency ordering, region filtering, and empty-region fallback.

### Tests
- `/backend/workers/test/alerts.test.ts` — tests `/api/alerts` response shape, region filtering, sort order, and KV cache parity

### Acceptance Criteria
- GET `/api/alerts` returns list of scam alerts sorted by recency
- GET `/api/alerts?region=MY` filters by region (MY/SG/both)
- Response: `[{ "id": "...", "title": "...", "description": "...", "scam_type": "...", "severity": "high|medium|low", "region": "...", "report_count": 0, "created_at": "..." }]`
- Results cached in KV (15-min TTL)
- D1 table `alerts` seeded with 5 initial alerts (from prototype data)

---

## Issue E01-005: D1 Database Schema + Migrations

### Tasks
1. File: `/backend/workers/wrangler.toml`  
   Action: Modify  
   Details: Add the concrete D1 binding name and migration directory configuration for local and deployed environments.

2. File: `/backend/workers/migrations/0001_initial_schema.sql`  
   Action: Create  
   Details: Create `reports`, `alerts`, `guardian_pairs`, and `shared_scores` tables matching the infrastructure spec and corrected naming/types.

3. File: `/backend/workers/migrations/0002_seed_alerts.sql`  
   Action: Create  
   Details: Insert the five initial alerts into `alerts` using stable IDs and timestamps suitable for local/dev/prod bootstrap.

4. File: `/backend/workers/src/data/seed-alerts.ts`  
   Action: Modify  
   Details: Keep the app-visible seed dataset aligned with the SQL seed payload to avoid divergence.

5. File: `/backend/workers/test/d1-migrations.test.ts`  
   Action: Create  
   Details: Validate table existence, insert/select viability for each table, and presence of seed alerts after migrations.

### Tests
- `/backend/workers/test/d1-migrations.test.ts` — tests schema creation, CRUD smoke checks, and seeded alert presence

### Acceptance Criteria
- D1 database `safeanot-db` created via Wrangler
- Tables created:
  - `reports` (scam reports from users)
  - `alerts` (curated scam alerts)
  - `guardian_pairs` (family guardian pairings — schema only, logic in E08)
  - `shared_scores` (viral share card data)
- Migration script in `/backend/workers/migrations/`
- Schema matches INFRASTRUCTURE_ARCHITECTURE.md (corrected version)
- Seed data: 5 initial alerts

---

## Issue E01-006: R2 Delta Download Endpoint

### Tasks
1. File: `/backend/workers/src/router.ts`  
   Action: Modify  
   Details: Register `GET /api/data/latest`, `GET /api/data/full`, and `GET /api/data/delta`.

2. File: `/backend/workers/src/routes/data.ts`  
   Action: Create  
   Details: Implement metadata lookup, date validation, old-date fallback to full database, future-date empty-delta behavior, and redirect responses.

3. File: `/backend/workers/src/lib/r2-manifest.ts`  
   Action: Create  
   Details: Read the latest manifest/version metadata from R2 or KV, calculate sizes, and build response payloads with consistent version selection.

4. File: `/backend/pipeline/seed_database.py`  
   Action: Modify  
   Details: Publish `latest.json` metadata alongside full/delta artifacts so the Worker can serve deterministic download metadata.

5. File: `/backend/workers/src/lib/cache.ts`  
   Action: Modify  
   Details: Add 1-hour KV metadata caching helpers for latest version and size metadata.

6. File: `/backend/workers/test/data.test.ts`  
   Action: Create  
   Details: Verify latest metadata response, full redirect, valid delta redirect, future-date empty delta, and very-old-date full redirect behavior.

### Tests
- `/backend/workers/test/data.test.ts` — tests `/api/data/latest`, `/api/data/full`, and `/api/data/delta` response behavior and redirect rules

### Acceptance Criteria
- GET `/api/data/latest` returns metadata: `{ "version": "2026-03-16", "full_url": "...", "delta_url": "...", "delta_size_kb": 150, "full_size_kb": 12000 }`
- GET `/api/data/full` redirects to R2 presigned URL for full database
- GET `/api/data/delta?since=2026-03-15` redirects to R2 presigned URL for delta since that date
- Response includes `Content-Length` for download progress
- Cached metadata in KV (1-hour TTL)

---

## Implementation Order
1. E01-001: Scaffold first because every later issue depends on a runnable Worker project, typed bindings, and CI baseline.
2. E01-005: Create D1 schema next so alert-related work and integration tests have a stable database contract early.
3. E01-002: Build the seed pipeline before `/api/check` so real allowlist/scam artifacts and R2 metadata exist instead of mocked assumptions.
4. E01-003: Implement `/api/check` after the scaffold and seed model are defined, since heuristics and KV keys depend on those contracts.
5. E01-004: Add `/api/alerts` once D1 migrations and seed alerts are in place; it is isolated and quick after schema setup.
6. E01-006: Finish with R2 download endpoints because they rely on pipeline-produced manifest/version artifacts and are downstream of the data publication format.

## Files Summary
| File | Action | Issues |
|---|---|---|
| `/backend/workers/package.json` | Create | E01-001 |
| `/backend/workers/tsconfig.json` | Create | E01-001 |
| `/backend/workers/wrangler.toml` | Create/Modify | E01-001, E01-005 |
| `/backend/workers/.dev.vars` | Create | E01-001 |
| `/backend/workers/.gitignore` | Create | E01-001 |
| `/backend/workers/src/index.ts` | Create | E01-001 |
| `/backend/workers/src/router.ts` | Create/Modify | E01-001, E01-003, E01-004, E01-006 |
| `/backend/workers/src/env.d.ts` | Create/Modify | E01-001, E01-003 |
| `/.github/workflows/deploy-workers.yml` | Create | E01-001 |
| `/backend/workers/vitest.config.ts` | Create | E01-001 |
| `/backend/workers/test/app.test.ts` | Create | E01-001 |
| `/backend/pipeline/pyproject.toml` | Create | E01-002 |
| `/backend/pipeline/seed_database.py` | Create/Modify | E01-002, E01-006 |
| `/backend/pipeline/src/sources.py` | Create | E01-002 |
| `/backend/pipeline/src/normalize.py` | Create | E01-002 |
| `/backend/pipeline/src/allowlist.py` | Create | E01-002 |
| `/backend/pipeline/src/cloudflare.py` | Create | E01-002 |
| `/backend/pipeline/tests/test_normalize.py` | Create | E01-002 |
| `/backend/pipeline/tests/test_seed_database.py` | Create | E01-002 |
| `/backend/workers/src/routes/check.ts` | Create | E01-003 |
| `/backend/workers/src/lib/domain.ts` | Create | E01-003 |
| `/backend/workers/src/lib/heuristics.ts` | Create | E01-003 |
| `/backend/workers/src/lib/cache.ts` | Create/Modify | E01-003, E01-004, E01-006 |
| `/backend/workers/src/lib/inflight.ts` | Create | E01-003 |
| `/backend/workers/src/lib/rate-limit.ts` | Create | E01-003 |
| `/backend/workers/test/check.test.ts` | Create | E01-003 |
| `/backend/workers/src/routes/alerts.ts` | Create | E01-004 |
| `/backend/workers/src/data/seed-alerts.ts` | Create/Modify | E01-004, E01-005 |
| `/backend/workers/test/alerts.test.ts` | Create | E01-004 |
| `/backend/workers/migrations/0001_initial_schema.sql` | Create | E01-005 |
| `/backend/workers/migrations/0002_seed_alerts.sql` | Create | E01-005 |
| `/backend/workers/test/d1-migrations.test.ts` | Create | E01-005 |
| `/backend/workers/src/routes/data.ts` | Create | E01-006 |
| `/backend/workers/src/lib/r2-manifest.ts` | Create | E01-006 |
| `/backend/workers/test/data.test.ts` | Create | E01-006 |

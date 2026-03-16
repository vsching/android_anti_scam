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
   Details: Configure Worker name, entrypoint, compatibility date, placeholder bindings for KV, R2, and D1, and Cloudflare rate limiting rules (100 req/min per IP on `/api/*` routes).

4. File: `/backend/workers/.dev.vars`
   Action: Create
   Details: Add local-only placeholders for Firebase credentials and any pipeline/admin secrets needed by `wrangler dev`.

5. File: `/backend/workers/.env.example`
   Action: Create
   Details: Document all required secrets and environment variables with descriptions. Include: `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID`, `R2_BUCKET_NAME`, `KV_NAMESPACE_ID`, `D1_DATABASE_ID`, `FIREBASE_PROJECT_ID`, `FIREBASE_PRIVATE_KEY`. Add comments noting which are read-only vs write tokens.

6. File: `/backend/workers/.gitignore`
   Action: Create
   Details: Exclude `.dev.vars`, `.wrangler/`, coverage output, local SQLite/D1 artifacts, and `node_modules/`.

7. File: `/backend/workers/src/index.ts`
   Action: Create
   Details: Implement the initial Worker entrypoint with `GET /` version response, middleware chain (CORS, security headers), and default 404 routing. Add logging redaction note: never log full URLs checked, phone numbers, or message text.

8. File: `/backend/workers/src/router.ts`
   Action: Create
   Details: Centralize route matching so later endpoints can be added without inflating the entrypoint.

9. File: `/backend/workers/src/env.d.ts`
   Action: Create
   Details: Declare typed bindings for KV, R2 bucket, D1 database, cache-related env vars, and Firebase placeholders.

10. File: `/backend/workers/src/middleware/cors.ts`
    Action: Create
    Details: CORS middleware allowing `https://safeanot.com` origin, app requests (via custom header or origin pattern), and preflight OPTIONS handling. Deny all other origins.

11. File: `/backend/workers/src/middleware/security-headers.ts`
    Action: Create
    Details: Add security headers to all responses: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `X-XSS-Protection: 1; mode=block`, `Strict-Transport-Security`, `Referrer-Policy: strict-origin-when-cross-origin`.

12. File: `/.github/workflows/deploy-workers.yml`
    Action: Create
    Details: Add GitHub Actions workflow to install dependencies, run tests, and deploy with Wrangler on push to `main`.

13. File: `/backend/workers/vitest.config.ts`
    Action: Create
    Details: Configure Workers test environment and coverage defaults for route-level tests.

14. File: `/backend/workers/test/app.test.ts`
    Action: Create
    Details: Verify `GET /` returns 200 with version metadata, unknown routes return 404, CORS headers are present, and security headers are present.

15. File: `/backend/workers/README.md`
    Action: Create
    Details: Setup instructions: how to install dependencies, run locally with `wrangler dev`, run tests, deploy, and configure environment variables.

16. File: `/backend/pipeline/requirements.txt`
    Action: Create
    Details: Pinned Python dependencies: `requests`, `cloudflare` (or `boto3` for R2 S3-compat), `pytest`, `pytest-cov`, `mmh3` (for Bloom filter), and any other pipeline dependencies.

17. File: `/backend/pipeline/README.md`
    Action: Create
    Details: Pipeline setup instructions: how to install Python dependencies, run the seed script, run tests, and configure Cloudflare credentials.

### Tests
- `/backend/workers/test/app.test.ts` -- tests hello-world root route, 404 behavior, CORS headers, and security headers
- `/.github/workflows/deploy-workers.yml` -- runs backend test suite before deploy

### Acceptance Criteria
- `wrangler dev` runs locally and serves a hello-world Worker
- TypeScript configured with strict mode
- KV, R2, and D1 bindings declared in `wrangler.toml`
- Cloudflare rate limiting rules configured in `wrangler.toml`
- `.dev.vars` for local secrets (Firebase key placeholder)
- `.env.example` with clearly documented required secrets
- CORS middleware allowing safeanot.com origin + app requests
- Security headers on all responses
- CI/CD: GitHub Actions workflow deploys to Cloudflare on push to main
- Logging redaction policy documented: never log full URLs, phone numbers, or message text
- Pipeline scaffold with `requirements.txt` and README
- Workers project lives in `/backend/workers/`, pipeline in `/backend/pipeline/`

---

## Issue E01-005: D1 Database Schema + Migrations

### Tasks
1. File: `/backend/workers/wrangler.toml`
   Action: Modify
   Details: Add the concrete D1 binding name and migration directory configuration for local and deployed environments.

2. File: `/backend/workers/migrations/0001_initial_schema.sql`
   Action: Create
   Details: Create `reports`, `alerts`, `guardian_pairs`, `shared_scores`, and `pending_discoveries` tables matching the infrastructure spec. All tables include `created_at DATETIME DEFAULT CURRENT_TIMESTAMP`. Add retention-friendly indexes on `created_at` for all tables. Add unique constraints where appropriate (e.g., `pending_discoveries.domain`).

3. File: `/backend/workers/migrations/0002_seed_alerts.sql`
   Action: Create
   Details: Insert the five initial alerts into `alerts` using stable IDs and timestamps suitable for local/dev/prod bootstrap.

4. File: `/backend/workers/src/data/seed-alerts.ts`
   Action: Create
   Details: Keep the app-visible seed dataset aligned with the SQL seed payload to avoid divergence.

5. File: `/backend/workers/src/lib/retention.ts`
   Action: Create
   Details: Scheduled function (Cron Trigger) to delete reports older than 90 days and pending_discoveries older than 30 days. Configured as a Workers scheduled handler.

6. File: `/backend/workers/test/d1-migrations.test.ts`
   Action: Create
   Details: Validate table existence, insert/select viability for each table, presence of seed alerts, unique constraint enforcement, and idempotent migration (running twice doesn't error).

### Tests
- `/backend/workers/test/d1-migrations.test.ts` -- tests schema creation, CRUD smoke checks, seeded alert presence, unique constraints, idempotent migration, and `pending_discoveries` table

### Acceptance Criteria
- D1 database `safeanot-db` created via Wrangler
- Tables created:
  - `reports` (scam reports from users)
  - `alerts` (curated scam alerts)
  - `guardian_pairs` (family guardian pairings -- schema only, logic in E08)
  - `shared_scores` (viral share card data)
  - `pending_discoveries` (suspicious domains queued for pipeline review)
- All tables include `created_at` column with retention-friendly indexes
- Migration script in `/backend/workers/migrations/`
- Schema matches INFRASTRUCTURE_ARCHITECTURE.md (corrected version)
- Seed data: 5 initial alerts
- Retention function: scheduled to delete reports older than 90 days
- Note: use least-privilege Cloudflare API tokens (separate read-only for Workers, write for pipeline)

---

## Issue E01-002: Scam Domain Database Seed

### Tasks
1. File: `/backend/pipeline/pyproject.toml`
   Action: Create
   Details: Define Python runtime, CLI entrypoint, and dependencies for HTTP fetching, Cloudflare auth, Bloom filter generation, SQLite export, and pytest.

2. File: `/backend/pipeline/seed_database.py`
   Action: Create
   Details: Implement orchestration for feed download, normalization, deduplication, versioned artifact generation (JSON, SQLite, Bloom filter, manifest), R2 upload, KV allowlist seeding, `latest.json` manifest update, and pending_discoveries processing.

3. File: `/backend/pipeline/src/sources.py`
   Action: Create
   Details: Add adapters for Phishing.Database, Scam-Blocklist, and Phishing Army with retry, timeout, source tagging, and graceful failure handling (log and continue if one source is down).

4. File: `/backend/pipeline/src/normalize.py`
   Action: Create
   Details: Normalize raw URLs/domains by lowercasing, stripping protocol/path/query/`www`, validating hostname shape, and deduplicating.

5. File: `/backend/pipeline/src/allowlist.py`
   Action: Create
   Details: Encode the Section 3.3 allowlist for Malaysian banks, Singapore banks, Malaysian government domains, and e-commerce domains as a canonical seed set.

6. File: `/backend/pipeline/src/cloudflare.py`
   Action: Create
   Details: Wrap R2 upload and KV write operations so pipeline logic stays testable and deployment credentials stay isolated.

7. File: `/backend/pipeline/src/sqlite_exporter.py`
   Action: Create
   Details: Export the deduplicated domain list to a SQLite file (`domains-full-YYYY-MM-DD.sqlite`). Schema: `CREATE TABLE domains (domain TEXT PRIMARY KEY, verdict TEXT, reason TEXT, source TEXT, confidence REAL)`. Add index on `domain` column. Include metadata table with version and build timestamp.

8. File: `/backend/pipeline/src/bloom_filter.py`
   Action: Create
   Details: Generate a Bloom filter binary file (`bloom-YYYY-MM-DD.bin`) from the full domain list. Target: ~100KB for 500K domains with <1% false positive rate. Use MurmurHash3 for hashing. Include a header with filter parameters (size, hash count, version) so the Android app can deserialize correctly.

9. File: `/backend/pipeline/src/manifest.py`
   Action: Create
   Details: Generate `manifest-YYYY-MM-DD.json` containing artifact filenames, sizes, SHA-256 checksums, domain count, bloom filter parameters, and build timestamp. Also update `latest.json` in R2 root.

10. File: `/backend/pipeline/src/discovery_processor.py`
    Action: Create
    Details: Read `pending_discoveries` from D1 via Cloudflare API, cross-reference with fetched sources, promote confirmed domains to the bulk database, and mark processed rows. Remove or archive stale pending entries older than 30 days.

11. File: `/backend/pipeline/tests/test_normalize.py`
    Action: Create
    Details: Cover normalization edge cases including protocols, paths, query strings, uppercase input, and `www` prefixes.

12. File: `/backend/pipeline/tests/test_seed_database.py`
    Action: Create
    Details: Validate output schema, deduplication, allowlist seeding, minimum-source merge behavior, manifest artifact generation, and idempotent rerun behavior.

13. File: `/backend/pipeline/tests/test_sqlite_exporter.py`
    Action: Create
    Details: Verify SQLite file is valid, queryable, contains expected domains, has correct schema, and includes metadata.

14. File: `/backend/pipeline/tests/test_bloom_filter.py`
    Action: Create
    Details: Verify Bloom filter has expected false positive rate (<1%), correctly identifies all inserted domains (zero false negatives), file size is within expected range (~100KB for 500K domains), and header is parseable.

15. File: `/backend/pipeline/tests/test_sources.py`
    Action: Create
    Details: Test source fetch failure handling: network errors, HTTP 404, malformed data. Verify graceful degradation (pipeline continues with remaining sources).

### Tests
- `/backend/pipeline/tests/test_normalize.py` -- tests domain normalization and canonicalization
- `/backend/pipeline/tests/test_seed_database.py` -- tests feed merge, JSON schema, allowlist seeding, R2/KV upload orchestration, and idempotent reruns
- `/backend/pipeline/tests/test_sqlite_exporter.py` -- tests SQLite export validity and queryability
- `/backend/pipeline/tests/test_bloom_filter.py` -- tests Bloom filter false positive rate and correctness
- `/backend/pipeline/tests/test_sources.py` -- tests source fetch failure handling and graceful degradation

### Acceptance Criteria
- Python script (`/backend/pipeline/seed_database.py`) that:
  - Downloads from Phishing.Database, Scam-Blocklist, Phishing Army (3 sources minimum)
  - Deduplicates and normalizes domains (lowercase, strip protocol/path/www)
  - Generates `domains-full-YYYY-MM-DD.json` and `domains-delta-YYYY-MM-DD.json`
  - Generates `domains-full-YYYY-MM-DD.sqlite` for device bundling and R2 download
  - Generates `bloom-YYYY-MM-DD.bin` (~100KB Bloom filter, <1% false positive)
  - Generates `manifest-YYYY-MM-DD.json` with checksums and sizes
  - Uploads all artifacts to R2 bucket `safeanot-data`
  - Updates `latest.json` in R2
  - Seeds KV namespace with allowlisted safe domains only (Malaysian banks, SG banks, gov domains, e-commerce)
  - Reads and processes `pending_discoveries` from D1
- R2 contains at least 100K+ scam domains after initial seed
- Allowlist contains all domains from GROWTH_AND_DATA_STRATEGY.md Section 3.3

---

## Issue E01-006: R2 Delta Download Endpoint

### Tasks
1. File: `/backend/workers/src/router.ts`
   Action: Modify
   Details: Register `GET /api/data/latest`, `GET /api/data/full`, `GET /api/data/delta`, and `GET /api/data/bloom`.

2. File: `/backend/workers/src/routes/data.ts`
   Action: Create
   Details: Implement metadata lookup from `latest.json` in R2, date validation for delta requests, and R2 object streaming. Use `env.R2_BUCKET.get(key)` to fetch objects and stream the response body directly to the client. Set `Content-Length` from R2 object metadata (`object.size`). For `/api/data/full`, stream the SQLite file. For `/api/data/delta`, stream the delta JSON. For `/api/data/bloom`, stream the Bloom filter binary. Handle missing objects with 404. Handle old dates by serving full database instead of delta.

3. File: `/backend/workers/src/lib/r2-manifest.ts`
   Action: Create
   Details: Read `latest.json` from R2, parse artifact metadata, resolve artifact keys for the requested version/date, and provide helpers for date validation and key construction.

4. File: `/backend/pipeline/seed_database.py`
   Action: Modify
   Details: Publish `latest.json` metadata alongside full/delta/SQLite/bloom artifacts so the Worker can serve deterministic download metadata.

5. File: `/backend/workers/src/lib/cache.ts`
   Action: Modify
   Details: Add 1-hour KV metadata caching helpers for latest version and size metadata.

6. File: `/backend/workers/test/data.test.ts`
   Action: Create
   Details: Verify latest metadata response, full file streaming, valid delta streaming, bloom filter streaming, future-date empty delta, very-old-date full database fallback, bad date format 400, missing R2 object 404, and stale manifest handling.

### Tests
- `/backend/workers/test/data.test.ts` -- tests `/api/data/latest`, `/api/data/full`, `/api/data/delta`, and `/api/data/bloom` response behavior, streaming, and error handling

### Acceptance Criteria
- GET `/api/data/latest` returns metadata: `{ "version": "2026-03-16", "full_size_kb": 12000, "delta_size_kb": 150, "bloom_size_kb": 100, "sqlite_size_kb": 15000 }`
- GET `/api/data/full` streams full SQLite database directly from R2 via `env.R2_BUCKET.get(key)`
- GET `/api/data/delta?since=2026-03-15` streams delta file from R2
- GET `/api/data/bloom` streams Bloom filter binary from R2
- Response includes `Content-Length` from R2 object metadata for download progress
- Cached metadata in KV (1-hour TTL)
- Worker IS the download endpoint -- no redirects or presigned URLs

---

## Issue E01-003: /api/check Endpoint (Link Checker)

### Tasks
1. File: `/backend/workers/src/router.ts`
   Action: Modify
   Details: Register `POST /api/check` and keep route dispatch isolated from handler implementation.

2. File: `/backend/workers/src/routes/check.ts`
   Action: Create
   Details: Implement request parsing (reject bodies >10KB), input validation, domain extraction, cache lookup, KV lookup, heuristic fallback, response shaping, and 400/413/429 handling. After heuristic evaluation, if verdict is "suspicious" or "dangerous", call discovery module to write to D1.

3. File: `/backend/workers/src/lib/domain.ts`
   Action: Create
   Details: Add URL/domain parsing helpers that strip protocol, path, query params, fragments, and `www`. Handle Punycode/IDN domains (xn-- encoded). Extract root domain for subdomain matching (e.g., sub.maybank.xyz -> check against maybank.xyz patterns).

4. File: `/backend/workers/src/lib/heuristics.ts`
   Action: Create
   Details: Implement typosquatting detection against allowlisted bank/gov domains, suspicious TLD checks, bank-name pattern checks, URL shortener detection, and subdomain-aware matching.

5. File: `/backend/workers/src/lib/cache.ts`
   Action: Create
   Details: Encapsulate Cache API read/write behavior with 1-hour TTL headers and cache-key normalization.

6. File: `/backend/workers/src/lib/inflight.ts`
   Action: Create
   Details: Add per-domain in-memory miss coalescing so concurrent unknown-domain checks share one heuristic computation per isolate. Uses a Map of domain -> Promise to deduplicate concurrent evaluations.

7. File: `/backend/workers/wrangler.toml`
   Action: Modify
   Details: Add Cloudflare rate limiting rules configuration: 100 req/min per IP for `/api/check`, 10 req/min for `/api/report`. Rate limiting is handled entirely by Cloudflare's built-in infrastructure — no application-level counters, no KV, no in-memory tracking. Worker returns 429 when Cloudflare enforces the limit.

8. File: `/backend/workers/src/lib/discovery.ts`
   Action: Create
   Details: Write suspicious unknown domains to D1 `pending_discoveries` table for pipeline review. Insert domain, verdict, confidence, heuristic reason, and timestamp. Use INSERT OR IGNORE to avoid duplicates. This is the v1 async discovery mechanism (no full queue needed).

9. File: `/backend/workers/src/env.d.ts`
   Action: Modify
   Details: Extend typed env bindings for heuristic thresholds, cache TTL constants, and D1 database binding.

10. File: `/backend/workers/test/check.test.ts`
    Action: Create
    Details: Cover known scam, known safe, typosquatting, suspicious TLD, URL shortener, unknown domain, invalid payload, rate-limit, cache hit behavior, KV precedence, Punycode handling, subdomain handling, concurrent miss coalescing, and oversized payload rejection.

### Tests
- `/backend/workers/test/check.test.ts` -- tests `/api/check` request validation, lookup flow, heuristics, caching, rate limiting, discovery writes, Punycode/IDN, subdomains, miss coalescing, and payload limits

### Acceptance Criteria
- POST `/api/check` accepts `{ "url": "https://maybank-secure-update.xyz/login" }` or `{ "domain": "maybank-secure-update.xyz" }`
- Extracts domain from URL (strips protocol, path, query params, www)
- Check flow: Cache API -> KV -> heuristic engine
- Returns `{ "domain": "...", "verdict": "dangerous|safe|suspicious|unknown", "reason": "...", "confidence": 0.0-1.0, "details": { ... } }`
- Heuristic engine checks:
  - Typosquatting (Levenshtein distance from allowlisted bank/gov domains)
  - Suspicious TLD (.xyz, .top, .buzz, .click, .loan, .win, .gq, .ml, .cf, .tk, .ga)
  - Bank-name pattern matching (maybank, cimb, rhb, etc. + random suffix)
  - URL shortener detection (bit.ly, tinyurl, etc.)
- Results cached in Cache API (1-hour TTL) and KV (for confirmed discoveries)
- Miss coalescing: concurrent requests for the same unknown domain don't duplicate heuristic work
- Rate limited: 100 req/min per IP via Cloudflare built-in rate limiting rules (no application-level counters)
- Suspicious/dangerous unknowns written to D1 `pending_discoveries` for pipeline review
- Punycode/IDN domains handled correctly
- Subdomain matching works (sub.maybank.xyz flags against maybank pattern)
- Bodies >10KB rejected with 413

---

## Issue E01-004: /api/alerts Endpoint (Scam Feed)

### Tasks
1. File: `/backend/workers/src/router.ts`
   Action: Modify
   Details: Register `GET /api/alerts` in the shared router.

2. File: `/backend/workers/src/routes/alerts.ts`
   Action: Create
   Details: Implement D1 query path, optional `region` filter (`MY`, `SG`, `both`). Invalid region values default to returning all alerts (not error). Response serialization and KV-backed 15-minute caching.

3. File: `/backend/workers/src/lib/cache.ts`
   Action: Modify
   Details: Add reusable helpers for serialized KV cache entries and cache-key construction for filtered list endpoints.

4. File: `/backend/workers/src/data/seed-alerts.ts`
   Action: Modify
   Details: Keep the app-visible seed dataset aligned with the SQL seed payload to avoid divergence.

5. File: `/backend/workers/test/alerts.test.ts`
   Action: Create
   Details: Verify uncached and cached reads, recency ordering, region filtering, empty-region fallback, invalid region fallback, and cache invalidation after TTL.

### Tests
- `/backend/workers/test/alerts.test.ts` -- tests `/api/alerts` response shape, region filtering, sort order, KV cache parity, invalid region fallback, and TTL invalidation

### Acceptance Criteria
- GET `/api/alerts` returns list of scam alerts sorted by recency
- GET `/api/alerts?region=MY` filters by region (MY/SG/both)
- Invalid region value returns all alerts (not error)
- Response: `[{ "id": "...", "title": "...", "description": "...", "scam_type": "...", "severity": "high|medium|low", "region": "...", "report_count": 0, "created_at": "..." }]`
- Results cached in KV (15-min TTL)
- D1 table `alerts` seeded with 5 initial alerts (from prototype data)

---

## Implementation Order
1. E01-001: Scaffold first because every later issue depends on a runnable Worker project, typed bindings, security middleware, and CI baseline.
2. E01-005: Create D1 schema next (including `pending_discoveries`) so pipeline, alert-related work, and integration tests have a stable database contract early.
3. E01-002: Build the seed pipeline with device-first artifacts (JSON, SQLite, Bloom filter, manifest) before endpoints so real data exists in R2.
4. E01-006: R2 download endpoints next because they are foundational for the device-first architecture -- the app needs these to download the bulk database.
5. E01-003: Implement `/api/check` after data and download endpoints exist, since it handles only the residual ~5% of checks not resolved on-device. Includes discovery writes to D1.
6. E01-004: Add `/api/alerts` last; it is isolated and quick after schema setup.

## Files Summary
| File | Action | Issues |
|---|---|---|
| `/backend/workers/package.json` | Create | E01-001 |
| `/backend/workers/tsconfig.json` | Create | E01-001 |
| `/backend/workers/wrangler.toml` | Create/Modify | E01-001, E01-005 |
| `/backend/workers/.dev.vars` | Create | E01-001 |
| `/backend/workers/.env.example` | Create | E01-001 |
| `/backend/workers/.gitignore` | Create | E01-001 |
| `/backend/workers/src/index.ts` | Create | E01-001 |
| `/backend/workers/src/router.ts` | Create/Modify | E01-001, E01-003, E01-004, E01-006 |
| `/backend/workers/src/env.d.ts` | Create/Modify | E01-001, E01-003 |
| `/backend/workers/src/middleware/cors.ts` | Create | E01-001 |
| `/backend/workers/src/middleware/security-headers.ts` | Create | E01-001 |
| `/.github/workflows/deploy-workers.yml` | Create | E01-001 |
| `/backend/workers/vitest.config.ts` | Create | E01-001 |
| `/backend/workers/test/app.test.ts` | Create | E01-001 |
| `/backend/workers/README.md` | Create | E01-001 |
| `/backend/pipeline/requirements.txt` | Create | E01-001 |
| `/backend/pipeline/README.md` | Create | E01-001 |
| `/backend/pipeline/pyproject.toml` | Create | E01-002 |
| `/backend/pipeline/seed_database.py` | Create/Modify | E01-002, E01-006 |
| `/backend/pipeline/src/sources.py` | Create | E01-002 |
| `/backend/pipeline/src/normalize.py` | Create | E01-002 |
| `/backend/pipeline/src/allowlist.py` | Create | E01-002 |
| `/backend/pipeline/src/cloudflare.py` | Create | E01-002 |
| `/backend/pipeline/src/sqlite_exporter.py` | Create | E01-002 |
| `/backend/pipeline/src/bloom_filter.py` | Create | E01-002 |
| `/backend/pipeline/src/manifest.py` | Create | E01-002 |
| `/backend/pipeline/src/discovery_processor.py` | Create | E01-002 |
| `/backend/pipeline/tests/test_normalize.py` | Create | E01-002 |
| `/backend/pipeline/tests/test_seed_database.py` | Create | E01-002 |
| `/backend/pipeline/tests/test_sqlite_exporter.py` | Create | E01-002 |
| `/backend/pipeline/tests/test_bloom_filter.py` | Create | E01-002 |
| `/backend/pipeline/tests/test_sources.py` | Create | E01-002 |
| `/backend/workers/src/routes/check.ts` | Create | E01-003 |
| `/backend/workers/src/lib/domain.ts` | Create | E01-003 |
| `/backend/workers/src/lib/heuristics.ts` | Create | E01-003 |
| `/backend/workers/src/lib/cache.ts` | Create/Modify | E01-003, E01-004, E01-006 |
| `/backend/workers/src/lib/inflight.ts` | Create | E01-003 |
| `/backend/workers/wrangler.toml` | Modify (rate limiting rules) | E01-003 |
| `/backend/workers/src/lib/discovery.ts` | Create | E01-003 |
| `/backend/workers/test/check.test.ts` | Create | E01-003 |
| `/backend/workers/src/routes/alerts.ts` | Create | E01-004 |
| `/backend/workers/src/data/seed-alerts.ts` | Create/Modify | E01-004, E01-005 |
| `/backend/workers/test/alerts.test.ts` | Create | E01-004 |
| `/backend/workers/migrations/0001_initial_schema.sql` | Create | E01-005 |
| `/backend/workers/migrations/0002_seed_alerts.sql` | Create | E01-005 |
| `/backend/workers/src/lib/retention.ts` | Create | E01-005 |
| `/backend/workers/test/d1-migrations.test.ts` | Create | E01-005 |
| `/backend/workers/src/routes/data.ts` | Create | E01-006 |
| `/backend/workers/src/lib/r2-manifest.ts` | Create | E01-006 |
| `/backend/workers/test/data.test.ts` | Create | E01-006 |

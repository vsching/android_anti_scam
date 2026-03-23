# E11: Data Pipeline Automation & Admin Management

> **Phase:** 1 — MVP Operations
> **Depends On:** E01B (Pipeline Deployment)
> **Status:** Not Started
> **Issues:** 5

---

## Overview

Automate the scam domain pipeline to run daily via GitHub Actions, build admin tools for managing the allowlist and manual blocklist, add pipeline health monitoring, and establish a sustainable long-term data management strategy.

Currently the pipeline runs manually, the allowlist requires code changes + redeploy, and there's no way to manually flag scam domains outside of feed data. This epic closes those operational gaps.

---

## E11-001: GitHub Actions Daily Pipeline Cron

**Description:** Set up automated daily pipeline runs via GitHub Actions to keep scam domain data fresh.

**Tasks:**
- Create `.github/workflows/pipeline-daily.yml` with daily cron at 3am MYT (7pm UTC previous day)
- Workflow: checkout → install Python 3.13 deps → run pipeline with R2/KV upload → verify artifacts
- Store pipeline credentials as GitHub secrets (CLOUDFLARE_API_TOKEN, CLOUDFLARE_ACCOUNT_ID, R2 access keys, KV namespace ID, D1 database ID)
- Add `workflow_dispatch` for manual trigger
- Log pipeline output (domain count, artifact sizes, sources fetched, failures)
- Send failure notification via GitHub Actions (email to repo owner)

**Acceptance Criteria:**
- [ ] Pipeline runs automatically daily at 3am MYT
- [ ] R2 artifacts updated with fresh data from 3 sources
- [ ] KV allowlist re-seeded on each run
- [ ] Manual trigger works via GitHub Actions UI
- [ ] Pipeline failures trigger email notification
- [ ] Pipeline is idempotent — running twice on same day is safe

**Test Cases:**
- Trigger manual run via `workflow_dispatch` and verify R2 artifacts updated
- Verify `GET /api/data/latest` returns today's version after cron run
- Simulate source failure and verify notification sent

---

## E11-002: Admin CLI for Allowlist & Blocklist Management

**Description:** Build a CLI script to add/remove domains from the KV allowlist and blocklist without code changes or redeployment.

**Tasks:**
- Create `scripts/admin.sh` with subcommands: `allowlist-add`, `allowlist-remove`, `blocklist-add`, `blocklist-remove`, `list`
- `allowlist-add <domain> <entity> <category>` — writes `allowlist:<domain>` to KV with safe verdict
- `blocklist-add <domain> <reason>` — writes `<domain>` to KV with dangerous verdict (manual override)
- `allowlist-remove <domain>` / `blocklist-remove <domain>` — deletes key from KV
- `list --allowlist` / `list --blocklist` — lists all entries from KV
- Validate domain format before writing
- Require confirmation for removals

**Acceptance Criteria:**
- [ ] Can add a new startup domain to allowlist via CLI in <10 seconds
- [ ] Can manually flag a scam domain via CLI immediately (no redeploy)
- [ ] Can list all allowlisted/blocklisted domains
- [ ] Changes take effect immediately (Worker reads KV on each request)
- [ ] Invalid domains are rejected with error message

**Test Cases:**
- Add `newstartup.com.my` to allowlist → `POST /api/check {"url":"newstartup.com.my"}` returns safe
- Add `scam-site.com` to blocklist → `POST /api/check {"url":"scam-site.com"}` returns dangerous
- Remove from allowlist → domain falls back to heuristic check
- Attempt to add invalid domain → error

---

## E11-003: Admin API Endpoints (Auth-Protected)

**Description:** Add authenticated admin API endpoints to the Worker for managing allowlist and blocklist remotely (e.g., from a future admin dashboard or mobile app).

**Tasks:**
- Add `POST /api/admin/allowlist` — add domain to allowlist (requires ADMIN_SECRET header)
- Add `DELETE /api/admin/allowlist/:domain` — remove from allowlist
- Add `POST /api/admin/blocklist` — add domain to blocklist (manual override)
- Add `DELETE /api/admin/blocklist/:domain` — remove from blocklist
- Add `GET /api/admin/allowlist` — list all allowlist entries
- Add `GET /api/admin/blocklist` — list all blocklist entries
- Auth via `X-Admin-Key` header matching `ADMIN_SECRET` Wrangler secret
- Rate limit admin endpoints to 10 req/min

**Acceptance Criteria:**
- [ ] Admin endpoints work with correct auth key
- [ ] Unauthenticated requests return 401
- [ ] Changes to KV take effect immediately
- [ ] Admin actions are logged (domain, action, timestamp) in D1

**Test Cases:**
- POST allowlist with valid key → 200, domain queryable as safe
- POST allowlist without key → 401
- POST blocklist → domain returns dangerous on check
- GET allowlist → returns all entries with category/entity

---

## E11-004: Pipeline Health Monitoring & Smoke Tests

**Description:** Add post-deploy smoke tests and pipeline health monitoring to catch failures early.

**Tasks:**
- Create `scripts/smoke-test.sh` that verifies all API endpoints after deploy
- Smoke tests: `GET /` → 200, `GET /api/data/latest` → domain_count > 0, `GET /api/alerts` → returns alerts, `POST /api/check` safe domain → safe, scam domain → dangerous
- Add smoke test step to GitHub Actions after each pipeline run
- Add `GET /api/health` endpoint returning pipeline freshness (hours since last R2 update)
- Alert if pipeline data is >48 hours stale

**Acceptance Criteria:**
- [ ] Smoke tests run after each pipeline cron
- [ ] Smoke test failure triggers notification
- [ ] `/api/health` returns data freshness
- [ ] Alert fires if data >48 hours stale
- [ ] README includes smoke test instructions

**Test Cases:**
- Run smoke tests against production → all pass
- Verify `/api/health` returns correct `last_updated` timestamp
- Simulate stale data → verify alert condition triggers

---

## E11-005: Scam Blocklist Source URL Resilience

**Description:** Make the pipeline resilient to feed URL changes and add fallback sources.

**Tasks:**
- Add URL validation + fallback for each source (try primary URL, fallback to mirror/alternative)
- Add OpenPhish as 4th feed source (`https://openphish.com/feed.txt`)
- Add URLhaus abuse.ch as 5th source (`https://urlhaus.abuse.ch/downloads/text_recent/`)
- Make source list configurable via environment variable (JSON list of URLs)
- Log warning when a source fails but others succeed
- Log error + alert when ALL sources fail

**Acceptance Criteria:**
- [ ] Pipeline continues if 1-2 sources fail (graceful degradation)
- [ ] At least 4 active feed sources
- [ ] Source URLs configurable without code change
- [ ] Anti-poisoning gates still apply across all sources

**Test Cases:**
- Mock one source failure → pipeline completes with remaining sources
- Verify OpenPhish and URLhaus sources fetch domains correctly
- Override source URLs via env var → pipeline uses custom URLs

---

## Implementation Order

1. **E11-001** — Daily cron (highest impact — keeps data fresh)
2. **E11-002** — Admin CLI (immediate operational need)
3. **E11-004** — Smoke tests (safety net)
4. **E11-003** — Admin API (enables future admin dashboard)
5. **E11-005** — Source resilience (hardening)

## Notes

- E11-001 requires R2 S3-compatible API keys (create in Cloudflare dashboard under R2 → Manage API Tokens)
- E11-002 uses wrangler CLI directly — no new infrastructure needed
- E11-003 requires setting `ADMIN_SECRET` via `wrangler secret put ADMIN_SECRET`
- The pipeline already handles anti-poisoning (50% drop gate, 80% single-source gate)

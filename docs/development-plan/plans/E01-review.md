# E01 Plan Review: Backend API + Scam Database

## Review Mode
Plan Review (pre-build)

## Review History

### Round 1 — 2026-03-17
**Status:** Complete
**Findings:** 7 issues found (3 HIGH, 3 MEDIUM, 1 LOW)
**Fixed:** 6 issues fixed, 1 dismissed
**Themes:** propagation-gap x2, classification-gap x1, fail-open x1, test-gap x1, other x2

**Details:**

| # | Severity | Finding | Theme | Resolution |
|---|----------|---------|-------|------------|
| 1 | HIGH | Rate limiting planned in wrangler.toml — insufficient for Cloudflare WAF enforcement | classification-gap | Fixed: changed to Cloudflare Rate Limiting Rulesets (IaC/dashboard), wrangler.toml has comments only |
| 2 | HIGH | KV used for metadata/alerts caching contradicts "allowlist + discoveries only" | propagation-gap | Dismissed: INFRASTRUCTURE_ARCHITECTURE.md lines 121-122 already list "Cached alert metadata (15-min TTL)" and "Cached R2 manifest metadata (1-hour TTL)" as valid KV uses |
| 3 | HIGH | TECHNICAL_REQUIREMENTS.md v1 says "no cloud integration" but E01 builds cloud API | propagation-gap | Fixed: clarified scope exclusions apply to Android app only, added note that backend API is in-scope for Phase 1 |
| 4 | MEDIUM | pending_discoveries INSERT OR IGNORE loses check_count signal | fail-open | Fixed: changed to UPSERT with check_count increment and last_seen_at tracking |
| 5 | MEDIUM | Missing tests: retention cron, discovery_processor, Cloudflare adapter errors | test-gap | Fixed: added retention.test.ts, test_discovery_processor.py, test_cloudflare.py |
| 6 | MEDIUM | Duplicate Python deps (requirements.txt + pyproject.toml) | other | Fixed: requirements.txt marked as remove/generate-only, pyproject.toml is single source |
| 7 | LOW | Deprecated X-XSS-Protection, missing CSP/Permissions-Policy | other | Fixed: removed X-XSS-Protection, added Permissions-Policy, noted CSP for E06 |

### Round 2 — 2026-03-17
**Status:** Complete
**Findings:** 6 issues found (1 HIGH recurring, 4 MEDIUM, 1 LOW)
**Fixed:** 6 issues fixed
**Themes:** propagation-gap x2 (RECURRING), classification-gap x2, fail-open x1, other x1

**Details:**

| # | Severity | Finding | Theme | Resolution |
|---|----------|---------|-------|------------|
| 1 | HIGH (RECURRING) | Epic source doc not synced with R1 fixes (rate limiting, requirements.txt, headers) | propagation-gap | Fixed: synced E01-backend-api-scam-database.md with all R1 corrections |
| 2 | MEDIUM (RECURRING) | Acceptance criteria still says requirements.txt | propagation-gap | Fixed: changed to pyproject.toml in both plan and epic source |
| 3 | MEDIUM | Retention cron has no wrangler.toml trigger config — will never run | classification-gap | Fixed: added wrangler.toml [triggers] crons task + acceptance criterion |
| 4 | MEDIUM | Rate limiting IaC has no concrete deliverable — not reproducible | classification-gap | Fixed: added /infra/rate-limiting.sh script task with idempotent API provisioning + CI step |
| 5 | MEDIUM | Feed ingestion missing anti-poisoning validation gates | fail-open | Fixed: added validation gates (50% drop rejection, 80% single-source anomaly, domain format checks, --force override) |
| 6 | LOW | Alert seed data duplicated in SQL and TS — drift risk | other | Fixed: SQL migration is single source of truth, TS file is derived + verified by test assertion |

## Recurring Issue Tracker

| Theme | Rounds Hit | Total Findings | Status |
|-------|-----------|----------------|--------|
| propagation-gap | R1, R2 | 4 | PATTERN |
| classification-gap | R1, R2 | 3 | PATTERN |
| fail-open | R1, R2 | 2 | PATTERN |
| test-gap | R1 | 1 | RESOLVED |
| other | R1, R2 | 3 | PATTERN |

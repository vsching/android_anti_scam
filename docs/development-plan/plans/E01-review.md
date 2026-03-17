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

## Recurring Issue Tracker

| Theme | Rounds Hit | Total Findings | Status |
|-------|-----------|----------------|--------|
| propagation-gap | R1 | 2 | RESOLVED |
| classification-gap | R1 | 1 | RESOLVED |
| fail-open | R1 | 1 | RESOLVED |
| test-gap | R1 | 1 | RESOLVED |
| other | R1 | 2 | RESOLVED |

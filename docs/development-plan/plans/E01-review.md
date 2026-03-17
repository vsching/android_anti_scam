# E01 Plan Review: Backend API + Scam Database

## Review Mode
Plan Review (pre-build)

## Review History

### Round 1 — 2026-03-17
**Status:** Complete
**Findings:** 7 issues found (3 HIGH, 3 MEDIUM, 1 LOW)
**Fixed:** 6 issues fixed, 1 dismissed
**Themes:** propagation-gap x2, classification-gap x1, fail-open x1, test-gap x1, other x2

### Round 2 — 2026-03-17
**Status:** Complete
**Findings:** 6 issues found (1 HIGH recurring, 4 MEDIUM, 1 LOW)
**Fixed:** 6 issues fixed
**Themes:** propagation-gap x2 (RECURRING), classification-gap x2, fail-open x1, other x1

### Round 3 — 2026-03-17
**Status:** Complete
**Findings:** 5 issues found (1 HIGH, 3 MEDIUM, 1 LOW)
**Fixed:** 5 issues fixed
**Themes:** bounds-missing x1, fail-open x1, propagation-gap x1, test-gap x1, other x1

**Details:**

| # | Severity | Finding | Theme | Resolution |
|---|----------|---------|-------|------------|
| 1 | HIGH | Bloom filter ~100KB for 500K domains at <1% FPR is mathematically impossible (~600KB needed) | bounds-missing | Fixed: corrected to ~600KB in plan, epic, and infra spec. Compressed transfer ~200-300KB. |
| 2 | MEDIUM | Worker .env.example includes pipeline publish secrets (CLOUDFLARE_API_TOKEN) | fail-open | Fixed: scoped to runtime bindings only, publish secrets in pipeline env only |
| 3 | MEDIUM | KV write policy ambiguous: /api/check caches in KV vs only confirmed discoveries | propagation-gap | Fixed: clarified /api/check uses Cache API only, KV written only by pipeline for confirmed discoveries |
| 4 | MEDIUM | Anti-poisoning gates have no explicit tests | test-gap | Fixed: added test cases for all gates + --force override to test_seed_database.py |
| 5 | LOW | SQLite exporter plans redundant index on domain (already PK) | other | Fixed: removed redundant index |

## Recurring Issue Tracker

| Theme | Rounds Hit | Total Findings | Status |
|-------|-----------|----------------|--------|
| propagation-gap | R1, R2, R3 | 5 | PATTERN (plan-level drift, all fixed — no architectural concern for plan review) |
| classification-gap | R1, R2 | 3 | PATTERN |
| fail-open | R1, R2, R3 | 3 | PATTERN |
| test-gap | R1, R3 | 2 | PATTERN |
| bounds-missing | R3 | 1 | RESOLVED |
| other | R1, R2, R3 | 4 | PATTERN |

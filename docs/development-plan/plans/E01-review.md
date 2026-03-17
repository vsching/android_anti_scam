# E01 Code Review: Backend API + Scam Database

## Review Mode
Code Review (post-build) — transitioned from plan review after build

## Plan Review History (pre-build)
- Round 1: 7 found, 6 fixed, 1 dismissed
- Round 2: 6 found, 6 fixed
- Round 3: 5 found, 5 fixed

## Code Review History

### Round 1 — 2026-03-17
**Status:** Complete
**Findings:** 5 issues found (4 P1, 1 P2)
**Fixed:** 5 issues fixed
**Themes:** propagation-gap x3, fail-open x1, classification-gap x1

### Round 2 — 2026-03-17
**Status:** Complete
**Findings:** 5 issues found (2 P1, 2 P2, 1 P3)
**Fixed:** 5 issues fixed
**Themes:** fail-open x2, propagation-gap x1, bounds-missing x1, other x1

**Details:**

| # | Severity | Finding | Theme | Resolution |
|---|----------|---------|-------|------------|
| 1 | P1 | Discovery UPSERT doesn't reset processed=FALSE — re-seen domains stay excluded from pipeline | fail-open | Fixed: added `processed = FALSE, processed_at = NULL` to UPSERT |
| 2 | P1 | KV parse errors fail open to verdict:safe — malformed KV suppresses scam detection | fail-open | Fixed: fall through to heuristics if KV parse fails (fail-closed) |
| 3 | P2 | Delta JSON written as full domain list, not delta semantics (added/removed) | propagation-gap | Fixed: delta now uses `{domains_added, domains_removed, version}` format |
| 4 | P2 | Unguarded JSON.parse on latest.json manifest → 500s on malformed data | fail-open | Fixed: try/catch + null return on parse failure or missing required fields |
| 5 | P3 | isValidDate accepts calendar-invalid dates (2026-02-30) via Date normalization | bounds-missing | Fixed: verify parsed date string matches input to catch normalization |

## Recurring Issue Tracker

| Theme | Rounds Hit | Total Findings | Files Affected | Status |
|-------|-----------|----------------|----------------|--------|
| propagation-gap | R1, R2 | 4 | check.ts, manifest.py, discovery_processor.py, seed_database.py | PATTERN |
| fail-open | R1, R2 | 4 | seed_database.py, discovery.ts, check.ts, r2-manifest.ts | PATTERN |
| classification-gap | R1 | 1 | discovery.ts | RESOLVED |
| bounds-missing | R2 | 1 | r2-manifest.ts | RESOLVED |

# E01 Code Review: Backend API + Scam Database

## Review Mode
Code Review (post-build)

## Plan Review History (pre-build)
- Round 1: 7 found, 6 fixed, 1 dismissed
- Round 2: 6 found, 6 fixed
- Round 3: 5 found, 5 fixed

## Code Review History

### Round 1 — 2026-03-17
**Status:** Complete
**Findings:** 5 (4 P1, 1 P2) — cross-component contract bugs
**Fixed:** 5
**Themes:** propagation-gap x3, fail-open x1, classification-gap x1

### Round 2 — 2026-03-17
**Status:** Complete
**Findings:** 5 (2 P1, 2 P2, 1 P3)
**Fixed:** 5
**Themes:** fail-open x2, propagation-gap x1, bounds-missing x1, other x1

### Round 3 — 2026-03-17
**Status:** Complete
**Findings:** 6 (3 P2, 3 P3) — hardening and edge cases
**Fixed:** 6
**Themes:** fail-open x2, bounds-missing x1, other x3

**Details:**

| # | Severity | Finding | Theme | Resolution |
|---|----------|---------|-------|------------|
| 1 | P2 | Body size enforced by string length, not byte length — multibyte bypass | bounds-missing | Fixed: use TextEncoder.encode().length |
| 2 | P2 | cacheGet unguarded JSON.parse — malformed cache → 500s | fail-open | Fixed: try/catch, return null |
| 3 | P2 | Anti-poisoning denominator uses post-dedup total, not raw source union | fail-open | Fixed: use raw union of source domains as denominator |
| 4 | P3 | recordDiscovery awaited, blocking response | other | Fixed: fire-and-forget with .catch() |
| 5 | P3 | streamR2Object reads full arrayBuffer, not streaming | other | Fixed: pass object.body ReadableStream directly |
| 6 | P3 | SIGPIPE in rate-limiting.sh dry-run | other | Fixed: use printf instead of echo\|head |

## Recurring Issue Tracker

| Theme | Rounds Hit | Total Findings | Files Affected | Status |
|-------|-----------|----------------|----------------|--------|
| propagation-gap | R1, R2 | 4 | check.ts, manifest.py, discovery_processor.py, seed_database.py | PATTERN |
| fail-open | R1, R2, R3 | 6 | seed_database.py, discovery.ts, check.ts, r2-manifest.ts, cache.ts | PATTERN (independent locations, not architectural) |
| classification-gap | R1 | 1 | discovery.ts | RESOLVED |
| bounds-missing | R2, R3 | 2 | r2-manifest.ts, check.ts | PATTERN |
| other | R2, R3 | 4 | check.ts, data.ts, rate-limiting.sh | PATTERN |

## Pattern Analysis (Round 3)
Finding rate: R1=5, R2=5, R3=6 — not converging yet but severity is dropping (P1→P2→P3).
"fail-open" hit 3 rounds but each instance is in a different file/context (unguarded parse in different locations). This is a defense-in-depth pattern, not an architectural issue — each parse site needs its own guard.

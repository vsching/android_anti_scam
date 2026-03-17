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
**Findings:** 5 issues found (4 P1, 1 P2) — all cross-component contract bugs
**Fixed:** 5 issues fixed
**Themes:** propagation-gap x3, fail-open x1, classification-gap x1

**Details:**

| # | Severity | Finding | Theme | Resolution |
|---|----------|---------|-------|------------|
| 1 | P1 | KV lookup uses bare domain but pipeline seeds with `allowlist:` prefix — allowlist never matches | propagation-gap | Fixed: check `allowlist:domain` first, then bare domain for discoveries |
| 2 | P1 | Pipeline latest.json emits `full_json_filename`/`full_json_size_kb` but Worker expects `full_key`/`full_size_kb` | propagation-gap | Fixed: generate_latest_json now outputs Worker-compatible keys |
| 3 | P1 | Anti-poisoning gate divides raw source count by deduplicated total — can false-abort on overlapping sources | fail-open | Fixed: compute unique contribution per source (domains not in any other source) |
| 4 | P2 | Discovery UPSERT compares verdicts lexicographically (`suspicious` > `dangerous`) — incorrect escalation | classification-gap | Fixed: use explicit severity rank mapping (unknown=0, safe=1, suspicious=2, dangerous=3) |
| 5 | P1 | Discovery processor queries `confidence` column that doesn't exist in schema — silently fails | propagation-gap | Fixed: removed nonexistent column from SELECT |

## Recurring Issue Tracker

| Theme | Rounds Hit | Total Findings | Files Affected | Status |
|-------|-----------|----------------|----------------|--------|
| propagation-gap | R1 | 3 | check.ts, manifest.py, discovery_processor.py | RESOLVED |
| fail-open | R1 | 1 | seed_database.py | RESOLVED |
| classification-gap | R1 | 1 | discovery.ts | RESOLVED |

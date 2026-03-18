# E05 Code Review: Profile + Settings

## Epic Scope
Wire real stats, region preferences, notification settings, emergency contacts, about section.

## Review History

### Pre-Review — 2026-03-18
**Status:** Complete
**Findings:** 15 issues found (0 critical, 3 high, 9 medium, 3 low)
**Fixed:** 10 issues fixed (3 high, 6 medium, 1 low)
**Deferred:** 5 low/opinion-based findings
**Themes:** propagation-gap x3, test-gap x5, fail-open x2, other x5

### Round 1 (Codex) — 2026-03-18
**Status:** Complete
**Findings:** 5 issues found (1 critical, 2 warnings, 2 info)
**Fixed:** 3 issues fixed (1 critical, 2 warnings)
**Deferred:** 2 info-level findings (silent ActivityNotFoundException, migration test)
**Themes:** race-condition x1, fail-open x1, propagation-gap x1, test-gap x1, other x1
**Categories:**
- Security: 1 (race condition on audit_count)
- Error handling: 1 (DataStore IOException)
- Performance: 1 (alerts initial fetch flicker)
- Code quality: 1 (silent error swallowing — deferred)
- Test coverage: 1 (migration test — deferred)

## Recurring Issue Tracker

| Theme | Rounds Hit | Total Findings | Files Affected | Status |
|-------|-----------|----------------|----------------|--------|
| test-gap | Pre, R1 | 6 | ProfileViewModelTest, MigrationTest | PATTERN |
| fail-open | Pre, R1 | 3 | UserPreferencesDataStore, DatabaseModule, ProfileScreen | PATTERN |
| propagation-gap | Pre, R1 | 4 | ProfileScreen, AlertsViewModel, AboutSection | PATTERN |
| race-condition | R1 | 1 | AuditRepositoryImpl | RESOLVED |
| other | Pre, R1 | 6 | various | — |

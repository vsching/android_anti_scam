# E05: Profile + Settings

> **Phase:** 1 (MVP)
> **Priority:** P1 — Polish and user trust
> **Depends On:** None
> **Estimated Effort:** 3-5 days

---

## Overview

The Profile screen currently shows placeholder data and a stub share button. Settings only covers audit reminders. For a scam protection app targeting Malaysia and Singapore, the Profile + Settings screen needs to display real user stats, allow region and notification preferences, provide a working share action, and surface help/emergency reporting channels with country-specific authority contacts.

## Technical Specs

- Product Context: `.claude/context/product-context.md`
- System Patterns: `.claude/context/system-patterns.md`
- Feature Brainstorm: `docs/BRAINSTORM_FEATURES.md` (Senior Mode, Guardian placeholder)

## Tech Stack

- Kotlin + Jetpack Compose + Material 3
- Room (user preferences persistence)
- Hilt (dependency injection)
- Android PackageManager API (app version)
- Android Share Intent (share action)
- DataStore (lightweight user preferences)

---

## Issues

### E05-001: Wire Real Stats, App Version, and Share Action

Replace all placeholder/hardcoded values in Profile with real data and implement the share functionality.

**Acceptance Criteria:**
- `totalAudits` counts the actual number of completed audit scans from persisted data
- `appVersion` reads from `BuildConfig.VERSION_NAME` or `PackageManager` metadata
- Share button launches Android share sheet with app promotion message and Play Store link
- Stats card shows additional useful info: last audit date, current security score
- All displayed data survives app restart (loaded from Room/persisted state)

**Test Cases:**
- ViewModel loads real totalAudits count from repository
- App version matches BuildConfig value
- Share intent fires with expected text content
- Stats display updates after a new audit completes
- UI renders correctly with zero audits (fresh install)

---

### E05-002: Region Preference and Notification Settings

Add a persistent region preference (Malaysia / Singapore) that controls alert feed defaults, and extend notification settings beyond just audit reminders.

**Acceptance Criteria:**
- Region picker (Malaysia / Singapore) persisted via DataStore
- Selected region overrides locale-based default in alerts feed
- Notification preference for scam alerts (enable/disable) separate from audit reminders
- All preferences persist across app restarts
- Region selection card shows country flag or label for clarity

**Test Cases:**
- Region preference persists and loads on restart
- Changing region updates alerts feed default filter
- Notification toggle for scam alerts persists independently from audit reminders
- DataStore read/write operations are tested
- UI renders correct selected state for region chips

---

### E05-003: About, Help, and Emergency Contacts Section

Add an informational section with app details, legal links, and country-specific emergency scam reporting contacts.

**Acceptance Criteria:**
- About section shows app version, build number, and "Made in Malaysia" branding
- Legal links: Privacy Policy, Terms of Service (open in browser via intent)
- Emergency contacts card with country-specific authority info:
  - Malaysia: MCMC (Suruhanjaya Komunikasi dan Multimedia Malaysia), PDRM Scam Response Centre, NSRC hotline 997
  - Singapore: ScamShield, SPF Anti-Scam Centre, hotline 1800-722-6688
- Emergency contacts adapt based on selected region preference
- Tapping a hotline number triggers a phone dialer intent
- Tapping an authority link opens the relevant website

**Test Cases:**
- About section displays correct app version
- Privacy Policy link opens browser intent
- Emergency contacts show Malaysia contacts when region is Malaysia
- Emergency contacts show Singapore contacts when region is Singapore
- Phone dialer intent triggered when tapping hotline number
- All links/intents verified with intent resolution checks

---

## Implementation Order

1. **E05-001** — Wire real stats and share (fixes existing placeholders, no new dependencies)
2. **E05-002** — Region preference and notification settings (introduces DataStore, needed by E05-003)
3. **E05-003** — About/help/emergency contacts (depends on region preference from E05-002)

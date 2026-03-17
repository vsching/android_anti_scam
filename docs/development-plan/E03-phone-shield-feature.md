# E03: Phone Shield Feature

> **Phase:** 1 (MVP)
> **Priority:** P0 — Core security audit feature
> **Depends On:** None
> **Estimated Effort:** 1-2 weeks

---

## Overview

The Phone Shield is the app's primary security audit feature. It detects which high-risk apps (messaging, browsers, file managers) are installed on the device, checks their "Install unknown apps" permission status via user confirmation, provides guided remediation flows, calculates a security score, and sends periodic reminders. This is the existing Shield/Audit functionality that needs completion and polish.

## Technical Specs

- Technical Requirements: [docs/TECHNICAL_REQUIREMENTS.md](../TECHNICAL_REQUIREMENTS.md) (FR-01 through FR-06)
- Package Query List: [docs/TECHNICAL_REQUIREMENTS.md](../TECHNICAL_REQUIREMENTS.md) (Section 3.5)

## Tech Stack

- Kotlin + Jetpack Compose + Material 3
- Room (audit state persistence)
- WorkManager (periodic audit reminders)
- PackageManager API (app detection)
- Settings deep-links (remediation flow)
- Hilt (dependency injection)

---

## Issues

### E03-001: Security Audit Dashboard Enhancement

Enhance the existing Shield screen with pull-to-refresh, proper category grouping, Play Protect status card, and polished UI.

**Acceptance Criteria:**
- Display all queried apps grouped by category (Messaging, Browser, File Manager)
- Each item shows: app icon (loaded from device), app name, risk description, status badge, action button
- Status badges: Secured (green), Needs Review (red), Not Installed (gray), Skipped (amber)
- Pull-to-refresh triggers a re-scan of all items
- Play Protect status card with "Check Play Protect" button
- Dashboard loads within 1 second on mid-range devices

**Test Cases:**
- Dashboard renders all audit items grouped by category
- Pull-to-refresh triggers re-scan
- Status badges show correct colors
- Play Protect card renders with action button

---

### E03-002: Install Source Detection

Implement reliable detection of all high-risk apps using `<queries>` manifest declarations.

**Acceptance Criteria:**
- Detect the presence of all apps in the Package Query List (Section 3.5 of TECHNICAL_REQUIREMENTS)
- Use `PackageManager.getPackageInfo()` with exception handling
- Handle Android 11+ package visibility restrictions via `<queries>` in AndroidManifest
- Correctly differentiate between "not installed" and "installed but not queryable"
- Detection completes in under 500ms for all packages
- Load actual app icons from device for installed apps

**Test Cases:**
- Detection returns correct status for installed apps
- Detection returns NOT_INSTALLED for missing apps
- Detection completes within 500ms
- App icons loaded for installed apps, fallback drawable for uninstalled

---

### E03-003: Guided Remediation Flow

Step-by-step flow to guide users through disabling "Install unknown apps" permission for each detected app.

**Acceptance Criteria:**
- Tapping "Fix" opens instruction screen specific to that app
- Instruction screen shows: app name, icon, risk explanation, numbered steps, "Open Settings" button, "I've Done It" button, "Skip" button
- Deep-link to `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` with package URI
- Fallback to `Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS` if deep-link unavailable
- After returning from settings, prompt for confirmation
- Status updates persist in Room database

**Test Cases:**
- Fix button navigates to remediation screen
- "Open Settings" launches correct intent
- "I've Done It" marks item as secured
- "Skip" marks item as skipped
- Status persists across app restarts

---

### E03-004: Security Score Calculation

Calculate and display security score based on remediation progress.

**Acceptance Criteria:**
- Score formula: `(securedItems / totalDetectedItems) * 100`
- Only installed apps count toward total (Not Installed excluded)
- Circular progress indicator with percentage on dashboard
- Color coding: 0-49% red, 50-79% amber, 80-100% green
- Score persists locally across sessions
- Recalculates on every audit refresh

**Test Cases:**
- Score calculates correctly with various secured/total ratios
- Not Installed items excluded from score
- Color transitions at correct thresholds
- Score persists after app restart

---

### E03-005: Periodic Audit Reminders

Background re-check via WorkManager to remind users if security posture has changed.

**Acceptance Criteria:**
- WorkManager periodic task runs every 7 days (configurable: 3/7/14/30 days)
- Worker re-scans all queried packages and compares against last audit state
- If new risky app detected or status regressed, post a notification
- Notification tap opens the dashboard
- Respects battery optimization and Doze mode
- User can enable/disable reminders in settings

**Test Cases:**
- Worker detects newly installed risky app
- Worker detects status regression
- Notification posted when change detected
- Notification tap opens dashboard
- Reminder interval configurable

---

## Implementation Order

1. **E03-002** — Install source detection (foundation for all audit features)
2. **E03-004** — Security score calculation (depends on detection)
3. **E03-001** — Dashboard enhancement (depends on detection + score)
4. **E03-003** — Guided remediation flow (depends on dashboard)
5. **E03-005** — Periodic reminders (depends on detection, independent of UI)

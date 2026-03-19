# E08: Family Guardian Mode

> **Phase:** 2 (Growth)
> **Priority:** P0 — Viral growth engine and key differentiator
> **Depends On:** E01 (Backend API), E03 (Phone Shield)
> **Estimated Effort:** 2-3 weeks

---

## Overview

Family Guardian Mode allows a child or family member (the "guardian") to remotely monitor an elderly relative's phone security posture. The parent's app sends periodic heartbeats with security score data to the backend. The guardian sees a dashboard of linked family members' security status. When a protected user's security posture degrades (e.g., re-enabling "Install unknown apps", Play Protect disabled, score drop), the guardian receives a push notification. This is the app's primary viral growth mechanism: every protected parent generates 1-3 guardian installs.

## Technical Specs

- Feature Brainstorm: `docs/BRAINSTORM_FEATURES.md` (Section 1: Family Guardian Mode)
- Product Context: `.claude/context/product-context.md`
- Backend API: E01 (Cloudflare Workers + D1 + FCM)
- Phone Shield: E03 (security score, audit items, WorkManager)

## Tech Stack

### Android
- Kotlin + Jetpack Compose + Material 3
- Room (guardian pairing persistence)
- WorkManager (periodic heartbeat)
- Hilt (dependency injection)
- Firebase Cloud Messaging (push notifications)

### Backend (Cloudflare Workers)
- D1 (guardian_pairings, heartbeats tables)
- FCM via Firebase Admin (push notifications to guardians)

---

## Issues

### E08-001: Guardian Pairing — Backend API

Backend endpoints for creating and managing guardian-ward pairings via short pairing codes.

**Acceptance Criteria:**
- `POST /api/guardian/pair/generate` — ward's device generates a 6-character alphanumeric pairing code, stored in D1 with 15-minute TTL and ward's device_id
- `POST /api/guardian/pair/claim` — guardian submits pairing code + their device_id, backend validates code, creates pairing in D1, returns ward info
- `GET /api/guardian/wards` — guardian retrieves list of wards they monitor (device_id, display_name, last_heartbeat, security_score)
- `GET /api/guardian/guardians` — ward retrieves list of guardians monitoring them
- `DELETE /api/guardian/pair/:pairingId` — either party can delete a pairing
- Pairing codes are single-use and expire after 15 minutes
- Maximum 3 guardians per ward
- Rate limiting: max 10 pairing code generations per device per hour

**Test Cases:**
- Generate returns a valid 6-char code
- Claim with valid code creates pairing and invalidates code
- Claim with expired code returns 410 Gone
- Claim with invalid code returns 404
- Ward cannot have more than 3 guardians
- Delete removes pairing for both parties
- Rate limiting enforced on code generation

---

### E08-002: Guardian Pairing — Android Client

Android UI and data layer for generating/claiming pairing codes and managing guardian relationships locally.

**Acceptance Criteria:**
- Ward flow: "Protect This Phone" button generates a 6-digit code displayed with countdown timer (15 min)
- Guardian flow: "Add Family Member" button opens code entry screen, validates and claims the code
- Pairings stored locally in Room for offline display
- Pairing screen accessible from Profile/Settings
- Display name editable after pairing (stored locally and synced)
- Unlink option with confirmation dialog

**Test Cases:**
- Generate code API called and code displayed with countdown
- Claim code with valid input navigates to success screen
- Claim code with invalid/expired input shows error
- Pairings persist in Room across app restarts
- Unlink removes pairing locally and via API
- Display name update persists

---

### E08-003: Heartbeat Worker — Security Score Reporting

WorkManager periodic task on the ward's device that reports security score and audit status to the backend.

**Acceptance Criteria:**
- `POST /api/guardian/heartbeat` backend endpoint accepts: device_id, security_score, scored_items, total_items, play_protect_enabled, timestamp
- WorkManager PeriodicWorkRequest runs every 6 hours (configurable)
- Heartbeat includes current security score, Play Protect status, and item counts
- Backend stores heartbeat in D1, updates ward's latest status
- Heartbeat only runs if device has at least one guardian pairing
- Battery-friendly: respects Doze mode and battery optimization
- Heartbeat data retained for 30 days

**Test Cases:**
- Worker sends heartbeat with correct payload
- Backend stores heartbeat and updates latest status
- Worker does not run when no pairings exist
- Heartbeat interval is configurable
- Old heartbeats cleaned up after 30 days

---

### E08-004: Guardian Dashboard Screen

Compose screen showing the guardian's linked family members and their security status at a glance.

**Acceptance Criteria:**
- Guardian Dashboard accessible as a new tab or from Profile screen
- Each ward card shows: display name, security score ring (colored), last heartbeat time ("2 hours ago"), Play Protect status indicator
- Pull-to-refresh fetches latest ward data from API
- "Went dark" indicator if last heartbeat is older than 24 hours
- Empty state with "Add Family Member" CTA
- Tap ward card to see detailed history (score trend over last 7 days)
- Loading skeleton while data is fetching

**Test Cases:**
- Dashboard renders ward cards with correct data
- Score ring color matches band thresholds
- "Went dark" indicator shows for stale heartbeats (>24h)
- Pull-to-refresh updates data
- Empty state shows when no wards linked
- Navigation to detail screen works

---

### E08-005: Guardian Alert Notifications

Push notifications to guardians when a ward's security posture degrades.

**Acceptance Criteria:**
- Backend compares incoming heartbeat with previous heartbeat for the same ward
- Alert triggers when: security score drops by 20+ points, Play Protect disabled, score enters RED band (<50%)
- Push notification sent via FCM to all guardians of the affected ward
- Notification includes: ward display name, alert reason, current score
- Guardian taps notification to open the Guardian Dashboard
- FCM token registration endpoint: `POST /api/guardian/fcm-token` (device_id, fcm_token)
- Maximum 3 alert notifications per ward per day (anti-spam)

**Test Cases:**
- Score drop of 20+ triggers notification
- Score drop of 10 does not trigger notification
- Play Protect disabled triggers notification
- Score entering RED band triggers notification
- FCM token stored and used for push delivery
- Notification tap opens Guardian Dashboard
- Anti-spam: 4th alert in same day suppressed

---

### E08-006: "Help Me Fix This" Request

One-tap button on the ward's device that sends their current security status to all guardians with a help request.

**Acceptance Criteria:**
- "Help Me Fix This" button visible on Shield screen when ward has guardians
- Tapping sends `POST /api/guardian/help-request` with device_id, current security score, list of unfixed items
- Backend sends push notification to all guardians: "{name} is asking for help with their phone security"
- Notification deep-links to ward's detail in Guardian Dashboard
- Rate limited: max 1 help request per hour
- Confirmation dialog before sending: "This will notify your family members"

**Test Cases:**
- Help button visible only when guardians exist
- Help request sends correct payload
- Push notification delivered to all guardians
- Rate limiting prevents spam (1/hour)
- Confirmation dialog shown before send
- Deep-link opens correct ward detail

---

## Implementation Order

1. **E08-001** — Backend pairing API (foundation for all guardian features)
2. **E08-002** — Android pairing client (depends on backend API)
3. **E08-003** — Heartbeat worker + backend endpoint (depends on pairing)
4. **E08-004** — Guardian dashboard screen (depends on pairing + heartbeat data)
5. **E08-005** — Alert notifications (depends on heartbeat comparison logic)
6. **E08-006** — Help request (depends on pairing + FCM, independent of dashboard)

# E09: Scam of the Week + Push Notifications

> **Phase:** 2 (Growth)
> **Priority:** P1 — Engagement and retention feature
> **Depends On:** E01 (Backend API), E04 (Scam Alerts Feed)
> **Estimated Effort:** 1-2 weeks

---

## Overview

Introduce a weekly "Scam of the Week" featured alert and a push notification delivery system using Firebase Cloud Messaging (FCM). The backend pipeline generates a weekly report highlighting the most impactful scam of the week. The Worker API serves the featured alert and triggers FCM push notifications. The Android app registers for FCM, handles incoming notifications, and displays featured alerts with a prominent UI treatment. The notification toggle in E05's UserPreferencesDataStore (`scam_alerts_notifications_enabled`) is wired to actually control FCM topic subscription.

## Technical Specs

- Architecture: [docs/INFRASTRUCTURE_ARCHITECTURE.md](../INFRASTRUCTURE_ARCHITECTURE.md) (Firebase FCM section, Cron jobs table)
- Backend: `/api/alerts/latest` endpoint (5-min TTL, serves latest featured alert)
- Existing: E04 alerts infrastructure (Room cache, AlertsRepository, AlertsDao)
- Existing: E05 notification preference (`UserPreferencesDataStore.scamAlertsNotificationsEnabledFlow`)
- Firebase: FCM topic-based messaging (`scam_alerts_MY`, `scam_alerts_SG`)

## Tech Stack

- **Android:** Kotlin, Jetpack Compose, Firebase Messaging SDK, WorkManager, Hilt
- **Backend:** Cloudflare Workers (TypeScript), D1, Firebase Admin SDK (HTTP v1 API)
- **Pipeline:** Python, httpx (for FCM HTTP v1 API calls via Worker proxy)

---

## Issues

### E09-001: Firebase Cloud Messaging Setup + FCM Service

Add Firebase Messaging SDK to the Android app. Create an `FcmTokenManager` that registers the device token, and a `SafeAnotMessagingService` that receives push notifications and displays them as system notifications. Wire the E05 notification preference toggle to subscribe/unsubscribe from FCM topics.

**Acceptance Criteria:**
- Firebase Messaging SDK added to `build.gradle.kts` (via Firebase BOM)
- `google-services.json` placeholder documented (not committed to repo)
- `SafeAnotMessagingService` extends `FirebaseMessagingService`, handles `onMessageReceived` and `onNewToken`
- Notification channel created for scam alerts: `scam_alerts` (separate from existing `audit_reminders`)
- `FcmTokenManager` class manages token refresh and topic subscription
- FCM topics: `scam_alerts_MY`, `scam_alerts_SG` based on user region preference
- When notification toggle is ON: subscribe to region topic. OFF: unsubscribe from all scam alert topics
- When region changes: unsubscribe old topic, subscribe new topic
- Tapping a notification deep-links to the alert detail screen (`safeanot.com/alert/{alertId}`)
- Service declared in `AndroidManifest.xml`

**Test Cases:**
- `FcmTokenManager` subscribes to correct topic based on region
- `FcmTokenManager` unsubscribes when notifications disabled
- Topic changes when region preference changes
- Notification payload parsed correctly in `SafeAnotMessagingService`
- Notification channel created with correct ID and name
- Deep link intent constructed correctly from notification data

---

### E09-002: Backend `/api/alerts/latest` Endpoint + FCM Trigger

Add a new `/api/alerts/latest` GET endpoint that returns the single most recent featured alert. Add a POST `/api/alerts/notify` endpoint (authenticated with a pipeline secret) that triggers an FCM push notification to the relevant region topic when a new "Scam of the Week" is published.

**Acceptance Criteria:**
- GET `/api/alerts/latest` returns the single most recent alert from D1 `alerts` table where `severity = 'high'` (or newest overall if no high-severity), cached in KV with 5-min TTL
- Response shape: `{ id, title, description, scam_type, severity, region, report_count, created_at }`
- POST `/api/alerts/notify` accepts `{ alert_id: string }`, authenticated via `X-Pipeline-Key` header matching `env.PIPELINE_SECRET`
- Notify endpoint reads alert from D1, sends FCM message to `scam_alerts_{region}` topic(s) via Firebase HTTP v1 API
- FCM payload includes: `title`, `body` (description truncated to 100 chars), `data.alert_id`, `data.deep_link`
- `FIREBASE_SERVICE_ACCOUNT_KEY` secret added to `wrangler.toml` as a secret binding
- `PIPELINE_SECRET` added to env for pipeline authentication
- Routes registered in `index.ts`

**Test Cases:**
- GET `/api/alerts/latest` returns single alert with correct shape
- KV cache hit returns cached response within TTL
- POST `/api/alerts/notify` with valid key triggers FCM send
- POST `/api/alerts/notify` with invalid/missing key returns 401
- POST `/api/alerts/notify` with non-existent alert_id returns 404
- FCM payload matches expected format
- Region-specific topic targeting works (MY alert -> `scam_alerts_MY` topic)

---

### E09-003: "Scam of the Week" Featured Alert UI

Add a prominent "Scam of the Week" card at the top of the Alerts feed screen. This card highlights the latest high-severity alert with a distinct visual treatment (larger card, warning icon, "Featured" badge). Tapping navigates to the alert detail screen.

**Acceptance Criteria:**
- `FeaturedAlertCard` composable with distinct styling: warning gradient background, "Scam of the Week" header badge, larger title text, severity indicator
- Card appears at top of alerts feed, above the region filter chips
- Data sourced from a new `GetFeaturedAlertUseCase` that queries the latest high-severity alert from the local Room cache
- Featured alert refreshed alongside normal alerts refresh
- Card is dismissible for the current session (state in ViewModel, not persisted)
- If no featured alert available, card is hidden (no empty state)
- Alert detail screen reused from E04-003 (no changes needed)

**Test Cases:**
- Featured card renders with correct alert data
- Featured card hidden when no high-severity alerts exist
- Tapping featured card navigates to alert detail
- Dismissing featured card hides it for the session
- Featured card reappears on fresh app launch
- Card displays correctly with long titles (truncation)

---

### E09-004: Pipeline Weekly Report + FCM Notification Trigger

Create the pipeline script that runs weekly (Monday 9am MYT) to select the "Scam of the Week" from D1 alert data and trigger the Worker's notify endpoint to send push notifications.

**Acceptance Criteria:**
- Python script `backend/pipeline/src/weekly_report.py`
- Queries D1 (via Cloudflare API) for alerts created in the past 7 days
- Selects the alert with highest `report_count` and `severity = 'high'` as "Scam of the Week"
- If no high-severity alerts, selects highest report_count overall
- Calls POST `/api/alerts/notify` with the selected alert ID and pipeline secret
- Logs the selected alert and notification result
- Cron entry documented for `Weekly Mon 9am MYT`
- Error handling: retries on transient failures (max 3 attempts)
- Dry-run mode via `--dry-run` CLI flag (selects alert but skips notification)

**Test Cases:**
- Script selects correct alert from mock D1 data
- Highest report_count with high severity wins
- Falls back to highest report_count if no high-severity alerts
- Pipeline secret sent in request header
- Retry logic on transient HTTP errors
- Dry-run mode skips POST call
- Empty alerts list handled gracefully (no notification sent)

---

## Implementation Order

1. **E09-001** -- FCM setup on Android (foundation: token management, notification service, topic subscription)
2. **E09-002** -- Backend endpoints (serves featured alert data, triggers FCM notifications)
3. **E09-003** -- Featured alert UI (depends on data layer from E09-001 awareness, reuses E04 alert infrastructure)
4. **E09-004** -- Pipeline script (depends on E09-002 notify endpoint being available)

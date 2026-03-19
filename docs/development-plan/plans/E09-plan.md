# E09 Implementation Plan: Scam of the Week + Push Notifications

> Generated from: docs/development-plan/E09-scam-of-the-week-push-notifications.md
> Technical specs referenced: docs/INFRASTRUCTURE_ARCHITECTURE.md (Firebase FCM, Cron jobs, /api/alerts/latest endpoint)
> Date: 2026-03-20

## Pre-Implementation Checklist

- [ ] Dependencies complete: E01 (Backend API) -- DONE, E04 (Scam Alerts Feed) -- DONE
- [ ] Technical specs reviewed: INFRASTRUCTURE_ARCHITECTURE.md, E04 epic, E05 epic (notification toggle)
- [ ] Plan reviewed by Codex
- [ ] Plan approved by user

---

## Issue E09-001: Firebase Cloud Messaging Setup + FCM Service

### Tasks

1. **Add Firebase dependencies to build.gradle.kts**
   - File: `android/app/build.gradle.kts`
   - Action: Modify
   - Details: Add `com.google.gms:google-services` plugin, Firebase BOM platform, `firebase-messaging-ktx` dependency. Add google-services plugin to project-level `build.gradle.kts` as well.

2. **Add google-services plugin to project-level build file**
   - File: `android/build.gradle.kts`
   - Action: Modify
   - Details: Add `id("com.google.gms.google-services") version "4.4.0" apply false` to plugins block.

3. **Document google-services.json requirement**
   - File: `android/app/google-services.json.example`
   - Action: Create
   - Details: Placeholder JSON showing expected structure. Real file must be obtained from Firebase Console. Add `google-services.json` to `.gitignore` if not already present.

4. **Create scam alerts notification channel constant**
   - File: `android/app/src/main/java/com/safeanot/app/util/Constants.kt`
   - Action: Modify
   - Details: Add `SCAM_ALERTS_CHANNEL_ID = "scam_alerts"`, `SCAM_ALERTS_CHANNEL_NAME = "Scam Alerts"`, and FCM topic constants `FCM_TOPIC_SCAM_ALERTS_MY = "scam_alerts_MY"`, `FCM_TOPIC_SCAM_ALERTS_SG = "scam_alerts_SG"`.

5. **Create FcmTokenManager for topic subscription management**
   - File: `android/app/src/main/java/com/safeanot/app/data/remote/FcmTokenManager.kt`
   - Action: Create
   - Details: Hilt `@Singleton` class, `@Inject constructor` with `UserPreferencesDataStore`. Methods: `subscribeToRegionTopic(region: String?)`, `unsubscribeFromAllTopics()`, `updateSubscription(enabled: Boolean, region: String?)`. The `region` parameter is `String?` (matching `regionFlow` emission type) and is mapped to topic names directly (e.g., `"MY"` -> `"scam_alerts_MY"`, `"SG"` -> `"scam_alerts_SG"`). If `region` is null, no subscription is made. Uses `FirebaseMessaging.getInstance().subscribeToTopic()` / `unsubscribeFromTopic()`. Observes `scamAlertsNotificationsEnabledFlow` and `regionFlow` to reactively manage subscriptions. No `Context` dependency -- uses FirebaseMessaging static instance.

6. **Create SafeAnotMessagingService**
   - File: `android/app/src/main/java/com/safeanot/app/service/SafeAnotMessagingService.kt`
   - Action: Create
   - Details: Extends `FirebaseMessagingService`. `onMessageReceived()`: parse `data["alert_id"]` and `data["title"]` / `data["body"]` from `RemoteMessage`, build notification with `NotificationCompat.Builder` using `scam_alerts` channel, set `PendingIntent` with deep link to `https://safeanot.com/alert/{alertId}`. `onNewToken()`: log new token (token storage for server-side sending is not needed since we use topic-based messaging). Does NOT create notification channel here -- channel is created solely in `SafeAnotApp.onCreate()` (see task 9).

7. **Register service in AndroidManifest.xml**
   - File: `android/app/src/main/AndroidManifest.xml`
   - Action: Modify
   - Details: Add `<service android:name=".service.SafeAnotMessagingService" android:exported="false">` with `<intent-filter><action android:name="com.google.firebase.MESSAGING_EVENT" /></intent-filter>` inside `<application>`.

8. **Wire notification preference to FCM subscription in SafeAnotApp**
   - File: `android/app/src/main/java/com/safeanot/app/SafeAnotApp.kt`
   - Action: Modify
   - Details: Inject `FcmTokenManager`. In `onCreate()`, add `initializeFcmSubscription()` method that launches a coroutine (using the existing `applicationScope` / `ProcessLifecycleOwner.get().lifecycleScope` pattern established in `SafeAnotApp`) to combine `scamAlertsNotificationsEnabledFlow` and `regionFlow` from `UserPreferencesDataStore`, calling `fcmTokenManager.updateSubscription()` on each emission. This ensures subscription state is always synced on app start and when preferences change.

10. **Request POST_NOTIFICATIONS runtime permission (Android 13+)**
    - File: `android/app/src/main/java/com/safeanot/app/feature/settings/SettingsScreen.kt`
    - Action: Modify
    - Details: When the user enables the scam alerts notification toggle on Android 13+ (SDK 33+), request `android.permission.POST_NOTIFICATIONS` via `rememberLauncherForActivityResult(RequestPermission())`. If permission is denied, revert the toggle to OFF and show a Snackbar explaining that notifications require permission. Also add `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` to `AndroidManifest.xml`.

9.  **Create notification channel at app startup**
    - File: `android/app/src/main/java/com/safeanot/app/SafeAnotApp.kt`
    - Action: Modify
    - Details: In `onCreate()`, create `NotificationChannel(SCAM_ALERTS_CHANNEL_ID, SCAM_ALERTS_CHANNEL_NAME, IMPORTANCE_HIGH)` via `NotificationManager`. This is idempotent on API 26+.

### Tests

- `android/app/src/test/java/com/safeanot/app/data/remote/FcmTokenManagerTest.kt` -- Tests that `updateSubscription(true, "MY")` subscribes to `scam_alerts_MY`, `updateSubscription(false, *)` unsubscribes from all, region change triggers unsubscribe-old + subscribe-new, `updateSubscription(true, null)` does not subscribe to any topic (null region is a no-op).
- `android/app/src/test/java/com/safeanot/app/service/SafeAnotMessagingServiceTest.kt` -- Tests notification payload parsing, deep link intent construction, channel creation.

### Acceptance Criteria

- Firebase Messaging SDK added to `build.gradle.kts` (via Firebase BOM)
- `google-services.json` placeholder documented (not committed to repo)
- `SafeAnotMessagingService` extends `FirebaseMessagingService`, handles `onMessageReceived` and `onNewToken`
- Notification channel created for scam alerts: `scam_alerts` (separate from existing `audit_reminders`)
- `FcmTokenManager` class manages token refresh and topic subscription
- FCM topics: `scam_alerts_MY`, `scam_alerts_SG` based on user region preference
- When notification toggle is ON: subscribe to region topic. OFF: unsubscribe from all scam alert topics
- When region changes: unsubscribe old topic, subscribe new topic
- Tapping a notification deep-links to the alert detail screen (`safeanot.com/alert/{alertId}`)
- Notification channel created in a single place (`SafeAnotApp.onCreate()`), not duplicated in `SafeAnotMessagingService`
- `POST_NOTIFICATIONS` runtime permission requested on Android 13+ when user enables notification toggle
- Service declared in `AndroidManifest.xml`

---

## Issue E09-002: Backend `/api/alerts/latest` Endpoint + FCM Trigger

### Tasks

1. **Add PIPELINE_SECRET, FIREBASE_SERVICE_ACCOUNT_KEY, and FIREBASE_PROJECT_ID to Env interface**
   - File: `backend/workers/src/env.d.ts`
   - Action: Modify
   - Details: Add `PIPELINE_SECRET: string;`, `FIREBASE_SERVICE_ACCOUNT_KEY: string;`, and `FIREBASE_PROJECT_ID: string;` to the `Env` interface. `FIREBASE_PROJECT_ID` is a separate env var used to construct the FCM HTTP v1 URL (`https://fcm.googleapis.com/v1/projects/{FIREBASE_PROJECT_ID}/messages:send`) -- it is NOT extracted from the service account JSON at runtime. All are secret bindings configured via `wrangler secret put`.

2. **Document new secrets in wrangler.toml comments**
   - File: `backend/workers/wrangler.toml`
   - Action: Modify
   - Details: Add comments documenting `PIPELINE_SECRET`, `FIREBASE_SERVICE_ACCOUNT_KEY`, and `FIREBASE_PROJECT_ID` secrets (actual values set via `wrangler secret put`, not in config file).

3. **Create Firebase messaging helper**
   - File: `backend/workers/src/lib/firebase-messaging.ts`
   - Action: Create
   - Details: Function `sendFcmTopicMessage(env: Env, topic: string, title: string, body: string, data: Record<string, string>): Promise<void>`. Uses Firebase HTTP v1 API (`https://fcm.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/messages:send`). Generates OAuth2 access token from service account key JWT (using Web Crypto API available in Workers). Constructs message payload with `notification` (title, body) and `data` (alert_id, deep_link) fields targeting the topic. Handles HTTP errors and throws on failure.

4. **Create JWT signing utility for Firebase auth**
   - File: `backend/workers/src/lib/jwt.ts`
   - Action: Create
   - Details: Function `getFirebaseAccessToken(serviceAccountKey: string): Promise<string>`. Parses the JSON service account key, creates a JWT with claims `{ iss, scope: "https://www.googleapis.com/auth/firebase.messaging", aud: "https://oauth2.googleapis.com/token", iat, exp }`. Signs with RS256 using Web Crypto API (`crypto.subtle.importKey` + `crypto.subtle.sign`). Exchanges JWT for access token via Google OAuth2 token endpoint. No module-level token caching -- generates a fresh token per request, which is acceptable given the low volume of this weekly endpoint (Workers module-level state is unreliable across isolate evictions).

5. **Create /api/alerts/latest route handler**
   - File: `backend/workers/src/routes/alerts-latest.ts`
   - Action: Create
   - Details: GET handler. Uses existing `cacheKey()` / `cacheGet()` / `cachePut()` utilities (from `src/lib/cache.ts`) with cache key `alerts:latest` (5-min TTL). On miss, queries D1: `SELECT * FROM alerts WHERE severity = 'high' ORDER BY created_at DESC LIMIT 1`. If no high-severity, falls back to `SELECT * FROM alerts ORDER BY created_at DESC LIMIT 1`. Caches result via `cachePut()`. Returns single alert JSON.

6. **Create /api/alerts/notify route handler**
   - File: `backend/workers/src/routes/alerts-notify.ts`
   - Action: Create
   - Details: POST handler. Authenticates via `X-Pipeline-Key` header against `env.PIPELINE_SECRET` using timing-safe comparison (`crypto.subtle.timingSafeEqual()` on UTF-8 encoded ArrayBuffers). Returns 401 if missing/mismatched. Rate-limited to 10 requests per minute per IP (using existing rate-limiting middleware or a simple KV-based counter with 60s TTL). Parses body `{ alert_id: string }`. Queries D1 for alert by ID. Returns 404 if not found. Determines target topics from alert region (`MY` -> `scam_alerts_MY`, `SG` -> `scam_alerts_SG`, `both` -> both topics). Calls `sendFcmTopicMessage()` for each topic. Returns 200 with `{ sent: true, topics: [...] }`.

7. **Register new routes in index.ts**
   - File: `backend/workers/src/index.ts`
   - Action: Modify
   - Details: Import `handleAlertsLatest` from `./routes/alerts-latest` and `handleAlertsNotify` from `./routes/alerts-notify`. Register `router.get('/api/alerts/latest', handleAlertsLatest)` and `router.post('/api/alerts/notify', handleAlertsNotify)`.

### Tests

- `backend/workers/test/routes/alerts-latest.test.ts` -- Tests KV cache hit, D1 query fallback, high-severity preference, correct response shape, 5-min TTL.
- `backend/workers/test/routes/alerts-notify.test.ts` -- Tests authentication via timing-safe comparison (valid key, invalid key, missing key), alert not found, successful FCM send, region-to-topic mapping, both-region targeting, rate limiting (429 response when exceeded).
- `backend/workers/test/lib/firebase-messaging.test.ts` -- Tests FCM payload construction, HTTP error handling.
- `backend/workers/test/lib/jwt.test.ts` -- Tests JWT creation and token exchange (mocked).

### Acceptance Criteria

- GET `/api/alerts/latest` returns the single most recent alert from D1 `alerts` table where `severity = 'high'`, cached in KV with 5-min TTL
- Response shape: `{ id, title, description, scam_type, severity, region, report_count, created_at }`
- POST `/api/alerts/notify` accepts `{ alert_id: string }`, authenticated via `X-Pipeline-Key` header
- Notify endpoint reads alert from D1, sends FCM message to `scam_alerts_{region}` topic(s)
- FCM payload includes: `title`, `body`, `data.alert_id`, `data.deep_link`
- `/api/alerts/notify` rate-limited to 10 requests per minute per IP
- Routes registered in `index.ts`

---

## Issue E09-003: "Scam of the Week" Featured Alert UI

### Tasks

1. **Create GetFeaturedAlertUseCase**
   - File: `android/app/src/main/java/com/safeanot/app/domain/usecase/GetFeaturedAlertUseCase.kt`
   - Action: Create
   - Details: Hilt `@Inject constructor(alertsRepository: AlertsRepository, userPreferencesDataStore: UserPreferencesDataStore)`. Operator function `operator fun invoke(): Flow<ScamAlert?>`. Combines `userPreferencesDataStore.regionFlow` with `AlertsRepository.observeAlerts()` using the user's region preference (mapped to `AlertRegionFilter`) so that only region-relevant alerts are considered. Maps to emit the first alert with `severity == HIGH`, or null if none exist. Uses `combine(regionFlow, alertsFlow) { region, alerts -> alerts.firstOrNull { it.severity == AlertSeverity.HIGH } }`.

2. **Add observeAllAlerts to AlertsRepository if not present**
   - File: `android/app/src/main/java/com/safeanot/app/domain/repository/AlertsRepository.kt`
   - Action: Modify (if needed)
   - Details: Verify `observeAlerts(AlertRegionFilter.ALL)` returns all alerts. The existing `observeAlerts(filter)` with `ALL` filter should work. If `GetFeaturedAlertUseCase` needs all alerts regardless of region, use `observeAlerts(AlertRegionFilter.ALL)`.

3. **Create FeaturedAlertCard composable**
   - File: `android/app/src/main/java/com/safeanot/app/feature/alerts/components/FeaturedAlertCard.kt`
   - Action: Create
   - Details: `@Composable fun FeaturedAlertCard(alert: ScamAlert, onDismiss: () -> Unit, onClick: () -> Unit)`. Card with `MaterialTheme.colorScheme.errorContainer` background gradient, "Scam of the Week" badge with warning icon (`Icons.Default.Warning`), alert title in `titleLarge` typography, severity chip, scam type, report count. Close/dismiss icon button in top-right corner. Entire card clickable via `onClick`. Respects Material 3 elevation and shape tokens.

4. **Add featured alert state to AlertsViewModel**
   - File: `android/app/src/main/java/com/safeanot/app/feature/alerts/AlertsViewModel.kt`
   - Action: Modify
   - Details: Add `featuredAlert: ScamAlert?` and `isFeaturedDismissed: Boolean` to `AlertsUiState`. Inject `GetFeaturedAlertUseCase`. In `init`, launch coroutine to collect featured alert flow and update state. Add `onDismissFeatured()` method that sets `isFeaturedDismissed = true` (session-only, not persisted). Featured alert shows when `featuredAlert != null && !isFeaturedDismissed`.

5. **Integrate FeaturedAlertCard into AlertsScreen**
   - File: `android/app/src/main/java/com/safeanot/app/feature/alerts/AlertsScreen.kt`
   - Action: Modify
   - Details: Above the region filter chips, conditionally render `FeaturedAlertCard` when `uiState.featuredAlert != null && !uiState.isFeaturedDismissed`. Wire `onDismiss` to `viewModel.onDismissFeatured()` and `onClick` to navigate to alert detail.

### Tests

- `android/app/src/test/java/com/safeanot/app/domain/usecase/GetFeaturedAlertUseCaseTest.kt` -- Tests that high-severity alert is returned, null when no high-severity, first high-severity by order.
- `android/app/src/test/java/com/safeanot/app/feature/alerts/AlertsViewModelFeaturedTest.kt` -- Tests featured alert state, dismiss behavior, reappears on fresh ViewModel init, null when no featured alert.

### Acceptance Criteria

- `FeaturedAlertCard` composable with distinct styling: warning gradient background, "Scam of the Week" header badge, larger title text, severity indicator
- Card appears at top of alerts feed, above the region filter chips
- Data sourced from `GetFeaturedAlertUseCase` that queries the latest high-severity alert
- Featured alert refreshed alongside normal alerts refresh
- Card is dismissible for the current session (state in ViewModel, not persisted)
- If no featured alert available, card is hidden
- Alert detail screen reused from E04-003

---

## Issue E09-004: Pipeline Weekly Report + FCM Notification Trigger

### Tasks

1. **Create weekly_report.py pipeline script**
   - File: `backend/pipeline/src/weekly_report.py`
   - Action: Create
   - Details: Click CLI with `--dry-run` flag. Reuses the existing `D1Client` class (from `src/clients/d1_client.py`) to query D1 via Cloudflare API, rather than making raw `httpx` calls. SQL: `SELECT * FROM alerts WHERE created_at >= datetime('now', '-7 days') ORDER BY CASE WHEN severity = 'high' THEN 0 ELSE 1 END, report_count DESC LIMIT 1`. If result found, calls `POST {API_BASE_URL}/api/alerts/notify` with `{ "alert_id": selected.id }` and `X-Pipeline-Key` header. Logs selection reasoning. In `--dry-run` mode, logs selected alert but skips POST.

2. **Add retry logic utility**
   - File: `backend/pipeline/src/utils/retry.py`
   - Action: Create (if not exists)
   - Details: Async retry decorator/function with exponential backoff. Max 3 attempts. Retries on `httpx.HTTPStatusError` for 5xx codes and `httpx.ConnectError`. Used by the notify POST call.

3. **Add configuration for pipeline secrets**
   - File: `backend/pipeline/src/config.py`
   - Action: Create (or modify if exists)
   - Details: Reads `PIPELINE_SECRET`, `CLOUDFLARE_ACCOUNT_ID`, `CLOUDFLARE_D1_DATABASE_ID`, `CLOUDFLARE_API_TOKEN`, `API_BASE_URL` from environment variables. Validates all required vars are present.

4. **Document cron schedule**
   - File: `backend/pipeline/README.md`
   - Action: Modify (or create if not exists)
   - Details: Add cron entry: `0 9 * * 1 cd /opt/pipeline && python -m src.weekly_report` (Monday 9am MYT = 1am UTC). Document environment variables needed.

### Tests

- `backend/pipeline/tests/test_weekly_report.py` -- Tests alert selection logic (highest report_count with high severity wins, fallback to any severity, empty list returns None), dry-run mode, retry on transient errors, pipeline secret header sent.
- `backend/pipeline/tests/test_retry.py` -- Tests retry decorator with mock failures, max attempts, exponential backoff timing.

### Acceptance Criteria

- Python script `backend/pipeline/src/weekly_report.py`
- Queries D1 via Cloudflare API for alerts created in the past 7 days
- Selects the alert with highest `report_count` and `severity = 'high'`
- Falls back to highest report_count if no high-severity alerts
- Calls POST `/api/alerts/notify` with selected alert ID and pipeline secret
- Retry logic on transient failures (max 3 attempts)
- Dry-run mode via `--dry-run` CLI flag
- Empty alerts list handled gracefully

---

## Implementation Order

Recommended sequence (respects internal dependencies):

1. **E09-001** -- FCM setup on Android. Foundation for everything: token management, notification service, topic subscription. No backend dependency needed to build and test locally.
2. **E09-002** -- Backend endpoints. The `/api/alerts/latest` endpoint provides data for the featured UI, and `/api/alerts/notify` is the trigger target for the pipeline. Can be built independently of Android work.
3. **E09-003** -- Featured alert UI. Depends on existing E04 alert data in Room. Uses `GetFeaturedAlertUseCase` that queries local cache. Can be built in parallel with E09-002.
4. **E09-004** -- Pipeline script. Depends on E09-002 `/api/alerts/notify` endpoint being deployed. Last to implement as it ties everything together.

## Files Summary

| File | Action | Issues |
|------|--------|--------|
| `android/app/build.gradle.kts` | Modify | E09-001 |
| `android/build.gradle.kts` | Modify | E09-001 |
| `android/app/google-services.json.example` | Create | E09-001 |
| `android/app/src/main/java/com/safeanot/app/util/Constants.kt` | Modify | E09-001 |
| `android/app/src/main/java/com/safeanot/app/data/remote/FcmTokenManager.kt` | Create | E09-001 |
| `android/app/src/main/java/com/safeanot/app/service/SafeAnotMessagingService.kt` | Create | E09-001 |
| `android/app/src/main/AndroidManifest.xml` | Modify | E09-001 |
| `android/app/src/main/java/com/safeanot/app/SafeAnotApp.kt` | Modify | E09-001 |
| `android/app/src/main/java/com/safeanot/app/feature/settings/SettingsScreen.kt` | Modify | E09-001 |
| `android/app/src/test/java/com/safeanot/app/data/remote/FcmTokenManagerTest.kt` | Create | E09-001 |
| `android/app/src/test/java/com/safeanot/app/service/SafeAnotMessagingServiceTest.kt` | Create | E09-001 |
| `backend/workers/src/env.d.ts` | Modify | E09-002 |
| `backend/workers/wrangler.toml` | Modify | E09-002 |
| `backend/workers/src/lib/firebase-messaging.ts` | Create | E09-002 |
| `backend/workers/src/lib/jwt.ts` | Create | E09-002 |
| `backend/workers/src/routes/alerts-latest.ts` | Create | E09-002 |
| `backend/workers/src/routes/alerts-notify.ts` | Create | E09-002 |
| `backend/workers/src/index.ts` | Modify | E09-002 |
| `backend/workers/test/routes/alerts-latest.test.ts` | Create | E09-002 |
| `backend/workers/test/routes/alerts-notify.test.ts` | Create | E09-002 |
| `backend/workers/test/lib/firebase-messaging.test.ts` | Create | E09-002 |
| `backend/workers/test/lib/jwt.test.ts` | Create | E09-002 |
| `android/app/src/main/java/com/safeanot/app/domain/usecase/GetFeaturedAlertUseCase.kt` | Create | E09-003 |
| `android/app/src/main/java/com/safeanot/app/feature/alerts/components/FeaturedAlertCard.kt` | Create | E09-003 |
| `android/app/src/main/java/com/safeanot/app/feature/alerts/AlertsViewModel.kt` | Modify | E09-003 |
| `android/app/src/main/java/com/safeanot/app/feature/alerts/AlertsScreen.kt` | Modify | E09-003 |
| `android/app/src/test/java/com/safeanot/app/domain/usecase/GetFeaturedAlertUseCaseTest.kt` | Create | E09-003 |
| `android/app/src/test/java/com/safeanot/app/feature/alerts/AlertsViewModelFeaturedTest.kt` | Create | E09-003 |
| `backend/pipeline/src/weekly_report.py` | Create | E09-004 |
| `backend/pipeline/src/utils/retry.py` | Create | E09-004 |
| `backend/pipeline/src/config.py` | Create | E09-004 |
| `backend/pipeline/README.md` | Modify | E09-004 |
| `backend/pipeline/tests/test_weekly_report.py` | Create | E09-004 |
| `backend/pipeline/tests/test_retry.py` | Create | E09-004 |

---

## Codex Review Trace

| # | Severity | Finding | Fix Applied | Issue |
|---|----------|---------|-------------|-------|
| 1 | HIGH | FcmTokenManager type mismatch -- `regionFlow` emits `String?` not `AlertRegionFilter` | Changed parameter type to `String?` and map to topic names directly (e.g., `"MY"` -> `"scam_alerts_MY"`) | E09-001 |
| 2 | HIGH | Pipeline secret timing-unsafe comparison | Specified `crypto.subtle.timingSafeEqual()` for `PIPELINE_SECRET` check on UTF-8 encoded ArrayBuffers | E09-002 |
| 3 | MEDIUM | Missing `POST_NOTIFICATIONS` runtime permission request (Android 13+) | Added task 10 to request permission when user enables notification toggle in SettingsScreen | E09-001 |
| 4 | MEDIUM | Env interface redundancy -- `FIREBASE_PROJECT_ID` unclear | Clarified `FIREBASE_PROJECT_ID` is a separate env var for FCM URL, not extracted from service account JSON | E09-002 |
| 5 | MEDIUM | `GetFeaturedAlertUseCase` shows global alerts regardless of region | Added `UserPreferencesDataStore` dependency; combines `regionFlow` to filter by user region | E09-003 |
| 6 | MEDIUM | JWT token caching unreliable in Workers (isolate eviction) | Removed module-level caching; generates fresh token per request (acceptable for low-volume weekly endpoint) | E09-002 |
| 7 | MEDIUM | Notification channel created in two places | Removed channel creation from `SafeAnotMessagingService`; kept only in `SafeAnotApp.onCreate()` | E09-001 |
| 8 | LOW | `/api/alerts/latest` should use existing `cacheKey`/`cacheGet`/`cachePut` utilities | Updated to use existing cache utilities from `src/lib/cache.ts` | E09-002 |
| 9 | LOW | Missing test for `FcmTokenManager` with null region | Added test case for `updateSubscription(true, null)` as no-op | E09-001 |
| 10 | LOW | Pipeline `weekly_report.py` should reuse existing `D1Client` pattern | Updated to reuse existing `D1Client` class from `src/clients/d1_client.py` | E09-004 |
| 11 | LOW | Coroutine scope should match existing `SafeAnotApp` pattern | Specified using existing `applicationScope` / `ProcessLifecycleOwner.get().lifecycleScope` pattern | E09-001 |
| 12 | LOW | Missing rate limiting on `/api/alerts/notify` | Added rate limiting (10 req/min/IP) via KV-based counter with 60s TTL | E09-002 |

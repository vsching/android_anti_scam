# APK Guard — Technical Feasibility Assessment

> **Date:** 2026-03-16
> **Purpose:** Evaluate whether each brainstormed feature is technically possible on a normal (non-rooted, non-MDM) Android consumer device.

---

## Summary

| # | Feature | Feasibility | Key Blocker |
|---|---------|-------------|-------------|
| 1 | Family Guardian Mode | PARTIALLY FEASIBLE | Sideload permission detection needs `PACKAGE_USAGE_STATS` (manual user grant) |
| 2 | Scam News Feed | FULLY FEASIBLE | None |
| 3 | "Is This a Scam?" Checker | FULLY FEASIBLE | None |
| 4 | Community Scam Reporting | PARTIALLY FEASIBLE | No public SemakMule/ScamShield APIs |
| 5 | Emergency Lockdown | PARTIALLY FEASIBLE | Cannot toggle airplane mode programmatically |
| 6 | App Permission Watchdog | FULLY FEASIBLE | `QUERY_ALL_PACKAGES` needs Play Store approval |
| 7 | Safe Banking Mode | PARTIALLY FEASIBLE | Screen capture detection Android 14+ only; remote access detection is heuristic |
| 8 | Scam Call/SMS Warning | PARTIALLY FEASIBLE | SMS reading restricted to default handler; CallScreeningService is fine |
| 9 | Senior Mode UI | FULLY FEASIBLE | Malay TTS quality may vary by device |
| 10 | Offline Scam Pattern DB | FULLY FEASIBLE | None |
| 11 | Trusted App Directory | FULLY FEASIBLE | `QUERY_ALL_PACKAGES` needs Play Store approval |
| 12 | Detect New App Installs | FULLY FEASIBLE | `QUERY_ALL_PACKAGES` for comprehensive coverage |

---

## Detailed Analysis

### 1. Family Guardian Mode

**Goal:** Child links to parent's phone and gets push notifications when risky changes happen.

#### Can we detect when "Install unknown apps" is re-enabled for another app?

| Approach | API | Works? |
|----------|-----|--------|
| Read per-app sideload permission | `AppOpsManager.checkOpNoThrow(OP_REQUEST_INSTALL_PACKAGES, uid, packageName)` | Yes, but requires `PACKAGE_USAGE_STATS` permission |
| Legacy global setting | `Settings.Secure.INSTALL_NON_MARKET_APPS` | No — always returns 1 on Android 8+ (per-app model replaced it) |
| Check own app only | `PackageManager.canRequestPackageInstalls()` | Only checks the calling app, not others |

**Verdict:** We CAN detect other apps' sideload permission via `AppOpsManager`, but the user must manually grant `PACKAGE_USAGE_STATS` in Settings > Special App Access > Usage Access. This is a one-time setup that we can guide during onboarding.

#### Can we detect new app installs from outside Play Store?

| API | Details |
|-----|---------|
| `ACTION_PACKAGE_ADDED` broadcast | Fires when any new app is installed. Exempted from Android 8+ background broadcast restrictions — works even from manifest-declared receivers. |
| `PackageManager.getInstallSourceInfo(packageName)` (API 30+) | Returns installer: `com.android.vending` = Play Store, `null` or `com.google.android.packageinstaller` = sideloaded |
| Package visibility | Requires `QUERY_ALL_PACKAGES` on Android 11+ for comprehensive coverage |

**Verdict:** FULLY FEASIBLE. We can detect every new install and check its source.

#### Can we send push notifications to another device?

| API | Details |
|-----|---------|
| Firebase Cloud Messaging (FCM) | Standard, free, reliable. Parent's app sends heartbeat to backend; backend pushes alert to guardian's device. |

**Verdict:** FULLY FEASIBLE. Standard FCM pattern.

#### Overall Feasibility: PARTIALLY FEASIBLE
- Sideload permission monitoring: requires `PACKAGE_USAGE_STATS` (manual user grant in Settings)
- New install detection: fully feasible via `ACTION_PACKAGE_ADDED`
- Cross-device alerts: fully feasible via FCM
- **Workaround for permission grant:** Guide user through granting `PACKAGE_USAGE_STATS` during onboarding with deep-link to Settings

---

### 2. Scam News Feed

| Aspect | Details |
|--------|---------|
| Fetch JSON feed | Standard HTTP (`OkHttp`/`Retrofit`), `INTERNET` permission only |
| Display content | `RecyclerView` or Compose `LazyColumn` |
| Push notifications | FCM for "Scam of the Week" alerts |
| Play Store policy | Educational/safety content is explicitly permitted |
| Offline cache | Room DB or file cache for offline reading |

**Feasibility: FULLY FEASIBLE** — No technical barriers.

---

### 3. "Is This a Scam?" Checker

#### Receive shared content from other apps

| API | Details |
|-----|---------|
| `ACTION_SEND` intent filter | Register in manifest with `text/plain` and `image/*` MIME types. App appears in share sheet. |
| Get shared text | `intent.getStringExtra(Intent.EXTRA_TEXT)` |
| Get shared image | `intent.getParcelableExtra(Intent.EXTRA_STREAM)` |
| Permissions needed | None for receiving share intents |

#### OCR on screenshots

| Library | Size | Offline? | Languages | Notes |
|---------|------|----------|-----------|-------|
| **Google ML Kit Text Recognition v2** | ~5MB bundled model | Yes | Latin, Chinese, Japanese, Korean, Devanagari | Recommended — good accuracy, small size |
| Tesseract4Android | ~15MB per language model | Yes | 100+ languages | Open source, larger but more flexible |

- Permission needed: `READ_MEDIA_IMAGES` (Android 13+) or `READ_EXTERNAL_STORAGE` (older) to access screenshots from gallery
- Alternatively: receive image via share intent (no storage permission needed)

**Feasibility: FULLY FEASIBLE** — Share intents and on-device OCR are well-supported.

---

### 4. Community Scam Reporting

| Aspect | Feasibility |
|--------|-------------|
| User submits reports in-app | FULLY FEASIBLE — standard form submission to backend |
| Play Store UGC policy | Allowed — must implement moderation, reporting, user blocking, and ToS |
| SemakMule API integration | NOT FEASIBLE directly — no public API. Can deep-link to web portal as workaround. Formal partnership with PDRM needed for API access. [SemakMule may expand with National Fraud Portal integration](https://www.thestar.com.my/news/nation/2025/02/08/semakmule-may-expand-with-national-fraud-portal-integration) |
| ScamShield API integration | NOT FEASIBLE directly — no public API. Can direct users to official ScamShield app |
| Build our own database | FULLY FEASIBLE — Firebase/PostgreSQL backend with moderation |

**Feasibility: PARTIALLY FEASIBLE** — Building our own community reporting is straightforward. Government API integration requires formal partnerships.

---

### 5. Emergency Lockdown / Incident Response

| Action | API | Feasible? |
|--------|-----|-----------|
| **Enable airplane mode** | `Settings.Global.AIRPLANE_MODE_ON` | **NO** — write-protected since Android 4.2. Only system/firmware-signed apps can toggle. |
| **Guide to airplane mode** | `Settings.ACTION_AIRPLANE_MODE_SETTINGS` intent | **YES** — opens the settings page, user toggles manually |
| **List recently installed apps** | `PackageManager.getInstalledApplications()` + filter by `PackageInfo.firstInstallTime` | **YES** — needs `QUERY_ALL_PACKAGES` or `PACKAGE_USAGE_STATS` |
| **Detect Accessibility services** | `Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)` | **YES** — no special permission needed |
| **Detect Device Admin apps** | `DevicePolicyManager.getActiveAdmins()` | **YES** — no special permission needed |
| **Detect Notification Listeners** | `Settings.Secure.getString(contentResolver, "enabled_notification_listeners")` | **YES** — no special permission needed |
| **Prompt uninstall of suspicious app** | `Intent(ACTION_UNINSTALL_PACKAGE)` with `REQUEST_DELETE_PACKAGES` permission | **YES** — shows system confirmation dialog, user must confirm |
| **Direct links to bank fraud hotlines** | `Intent(ACTION_DIAL)` with `tel:` URI | **YES** — no permission needed for dial (not call) |
| **Link to police report** | `Intent(ACTION_VIEW)` with URL | **YES** — opens browser |

**Feasibility: PARTIALLY FEASIBLE** — Everything works except auto-toggling airplane mode. We guide the user to toggle it manually instead. Detection of dangerous permissions (Accessibility, Device Admin, Notification Listener) requires ZERO special permissions.

---

### 6. App Permission Watchdog

| Capability | API | Permission Required |
|------------|-----|---------------------|
| Read other apps' permissions | `PackageManager.getPackageInfo(pkg, GET_PERMISSIONS)` → `requestedPermissions` array | `QUERY_ALL_PACKAGES` (Android 11+) or targeted `<queries>` |
| Monitor Accessibility changes | `ContentObserver` on `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` URI | None |
| Monitor Notification Listener changes | `ContentObserver` on notification listeners setting URI | None |
| Check app usage | `UsageStatsManager.queryUsageStats()` | `PACKAGE_USAGE_STATS` (manual grant) |

**Feasibility: FULLY FEASIBLE** — The key APIs exist. `QUERY_ALL_PACKAGES` needs Play Store approval (strong justification for a security app). Accessibility/notification monitoring requires no permissions at all via `ContentObserver`.

---

### 7. Safe Banking Mode

| Detection | API | Feasible? | Notes |
|-----------|-----|-----------|-------|
| **Screen overlay active** | `MotionEvent.FLAG_WINDOW_IS_OBSCURED` (API 9+), `FLAG_WINDOW_IS_PARTIALLY_OBSCURED` (API 29+) | **YES** | Detect touch events obscured by overlays |
| **Dismiss overlays** | `Window.setHideOverlayWindows(true)` (API 31+) | **YES** | Hides all overlay windows while your activity is visible |
| **Screen capture/recording** | `Activity.registerScreenCaptureCallback()` (API 34 / Android 14+) | **PARTIAL** | Only works on Android 14+. Pre-14: can set `FLAG_SECURE` to prevent capture but cannot detect it. |
| **Remote access tools running** | Check installed packages for AnyDesk (`com.anydesk.anydeskandroid`), TeamViewer (`com.teamviewer.teamviewer.market.mobile`), etc. + check `ENABLED_ACCESSIBILITY_SERVICES` | **HEURISTIC** | Can detect if installed and if accessibility service is active, but cannot definitively confirm "remote session in progress" |
| **User on a phone call** | `TelephonyCallback.CallStateListener` (API 31+) or `TelephonyManager.getCallState()` (deprecated) | **YES** | Requires `READ_PHONE_STATE` runtime permission |

**Feasibility: PARTIALLY FEASIBLE** — Overlay and call detection work well. Screen capture detection is Android 14+ only. Remote access detection is heuristic (check packages + accessibility), not deterministic. Best suited as a B2B SDK where the banking app itself performs these checks.

---

### 8. Scam Call/SMS Warning

#### Call screening

| API | Details |
|-----|---------|
| `CallScreeningService` (API 24+) | See incoming call details, respond with allow/reject/silence. Does NOT need `READ_CALL_LOG`. |
| `RoleManager.ROLE_CALL_SCREENING` (API 29+) | User sets your app as default call screening app |
| Play Store policy | Caller ID and spam detection is explicitly permitted |

**Verdict:** FULLY FEASIBLE for call screening.

#### SMS reading

| Approach | Feasible? | Notes |
|----------|-----------|-------|
| `RECEIVE_SMS` permission | **RESTRICTED** | Play Store requires app to be default SMS handler. Must apply for policy exception. |
| `NotificationListenerService` | **YES** | Reads SMS notification content without SMS permission. User must grant notification access in Settings. Less reliable (only catches notifications, not all SMS). |
| Share intent from SMS app | **YES** | User manually shares suspicious SMS to our app for checking |

**Verdict:** PARTIALLY FEASIBLE. `CallScreeningService` is clean. SMS reading is restricted — best approach is share intent + notification listener as fallback.

---

### 9. Senior Mode UI

| Feature | API | Feasible? |
|---------|-----|-----------|
| Text-to-Speech | `android.speech.tts.TextToSpeech` | **YES** — built into every Android device. Supports Malay (`ms`), Chinese (`zh`), Tamil (`ta`), English (`en`). No permissions needed. |
| Large text / high contrast | Standard Compose theming | **YES** |
| Auto-detect language | `Locale.getDefault()` | **YES** |
| Offline TTS | Most languages have downloadable offline voice data | **YES** — quality varies by language/device |

**Feasibility: FULLY FEASIBLE** — TTS is built-in. Malay voice quality may be limited on some devices but is generally available.

---

### 10. Offline Scam Pattern Database

| Aspect | Details |
|--------|---------|
| Ship bundled database | `Room.databaseBuilder().createFromAsset("scam_patterns.db")` |
| APK size impact | A database of 100K+ scam patterns (phone numbers, URLs, keywords) would be well under 10MB |
| APK size limit | 150MB for AAB (base). Plus up to 2GB via Play Asset Delivery. |
| Delta updates | Sync new patterns when connected via standard API call |
| Permissions | None (internal storage) |

**Feasibility: FULLY FEASIBLE** — Standard Room pattern.

---

### 11. Trusted App Directory

| Capability | API | Notes |
|------------|-----|-------|
| Check if app was installed from Play Store | `PackageManager.getInstallSourceInfo(pkg)` (API 30+) | Returns `com.android.vending` for Play Store installs |
| Legacy API | `PackageManager.getInstallerPackageName(pkg)` (deprecated but works) | Pre-API 30 |
| Detect all installed apps | Requires `QUERY_ALL_PACKAGES` (Android 11+) | Play Store approval needed |
| Maintain allowlist | Local Room DB or bundled JSON of verified packages | Standard |

**Feasibility: FULLY FEASIBLE** — API is straightforward. `QUERY_ALL_PACKAGES` approval needed for comprehensive scanning.

---

### 12. Detect New App Installs

| Aspect | Details |
|--------|---------|
| Broadcast | `ACTION_PACKAGE_ADDED` — fires when any new app is installed |
| Background delivery | **Exempted** from Android 8+ implicit broadcast restrictions — delivered to manifest-declared receivers |
| New vs update | Check `EXTRA_REPLACING` extra to distinguish |
| Android 14+ | Must specify `RECEIVER_EXPORTED` flag for context-registered receivers. Manifest-declared still works. |
| Package visibility | Requires `QUERY_ALL_PACKAGES` on Android 11+ for ALL installs. Without it, only receives broadcasts for visible packages. |
| Check install source | `PackageManager.getInstallSourceInfo()` on the new package |

**Feasibility: FULLY FEASIBLE** — `ACTION_PACKAGE_ADDED` is explicitly exempted from background restrictions. Works from Android 8 through 15.

---

## Permissions Master List

All permissions the app would need across all features:

| Permission | Type | Required For | User Action |
|------------|------|--------------|-------------|
| `INTERNET` | Normal | Scam feed, FCM, community reporting | Auto-granted |
| `POST_NOTIFICATIONS` (API 33+) | Runtime | Push notifications, reminders | Runtime dialog |
| `QUERY_ALL_PACKAGES` | Install-time | Permission watchdog, install detection, trusted directory | Play Store approval |
| `PACKAGE_USAGE_STATS` | Special | Sideload permission monitoring, recently installed apps | User enables in Settings |
| `REQUEST_DELETE_PACKAGES` | Normal | Emergency uninstall prompts | Auto-granted (system dialog per uninstall) |
| `READ_PHONE_STATE` | Runtime | Call state detection (Safe Banking Mode) | Runtime dialog |
| `READ_MEDIA_IMAGES` (API 33+) | Runtime | OCR on screenshots from gallery | Runtime dialog |
| `RECEIVE_BOOT_COMPLETED` | Normal | Restart WorkManager after reboot | Auto-granted |
| Notification Listener | Special | SMS scam detection workaround | User enables in Settings |
| `CallScreeningService` role | Role | Scam call screening | User sets via RoleManager |

### v1 Permissions (minimal)
- `INTERNET`
- `POST_NOTIFICATIONS`
- `RECEIVE_BOOT_COMPLETED`

### v2 Permissions (Family Guardian + more)
- Add: `PACKAGE_USAGE_STATS`, `QUERY_ALL_PACKAGES`

### v3 Permissions (full suite)
- Add: `READ_PHONE_STATE`, `READ_MEDIA_IMAGES`, Notification Listener, CallScreeningService

**Strategy:** Start with ZERO dangerous permissions in v1. Add permissions incrementally as features justify them. Always explain why each permission is needed in plain language.

---

## Play Store Approval Risks

| Permission/Feature | Risk Level | Mitigation |
|--------------------|------------|------------|
| `QUERY_ALL_PACKAGES` | Medium | Strong justification: security audit app. Document in Data Safety section. Provide screenshots showing security-focused UI. |
| `PACKAGE_USAGE_STATS` | Low | User grants manually. Play Store does not restrict this. |
| `CallScreeningService` | Low | Explicitly permitted use case for caller ID / spam detection. |
| Notification Listener | Medium | Must declare AccessibilityService policy compliance. Justify as scam SMS detection. |
| UGC (community reports) | Low | Implement moderation, reporting, blocking per Play Store UGC policy. |

---

## Sources

- [Android Package Visibility](https://developer.android.com/training/package-visibility)
- [ACTION_MANAGE_UNKNOWN_APP_SOURCES](https://developer.android.com/reference/android/provider/Settings#ACTION_MANAGE_UNKNOWN_APP_SOURCES)
- [Android Broadcasts (exemption list)](https://developer.android.com/develop/background-work/background-tasks/broadcasts#changes-system-broadcasts)
- [CallScreeningService](https://developer.android.com/reference/android/telecom/CallScreeningService)
- [Screen Calls](https://developer.android.com/develop/connectivity/telecom/dialer-app/screen-calls)
- [SMS/Call Log Permission Policy](https://support.google.com/googleplay/android-developer/answer/10208820)
- [QUERY_ALL_PACKAGES Policy](https://support.google.com/googleplay/android-developer/answer/10158779)
- [ML Kit Text Recognition v2](https://developers.google.com/ml-kit/vision/text-recognition/v2/android)
- [UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager)
- [TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech)
- [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging)
- [Play Store UGC Policy](https://support.google.com/googleplay/android-developer/answer/9876937)
- [Android 14 ScreenCaptureCallback](https://developer.android.com/about/versions/14/behavior-changes-14)
- [Play Asset Delivery](https://developer.android.com/guide/playcore/asset-delivery)
- [SemakMule Fraud Portal Expansion](https://www.thestar.com.my/news/nation/2025/02/08/semakmule-may-expand-with-national-fraud-portal-integration)

# APK Guard - Technical & Requirements Documentation

> **Version:** 1.0 (Draft)
> **Last Updated:** 2026-03-16
> **Status:** Pre-Development

---

## Table of Contents

1. [Product Overview](#1-product-overview)
2. [Functional Requirements](#2-functional-requirements)
3. [Technical Architecture](#3-technical-architecture)
4. [Non-Functional Requirements](#4-non-functional-requirements)
5. [UI/UX Requirements](#5-uiux-requirements)
6. [Data Model](#6-data-model)
7. [Constraints & Limitations](#7-constraints--limitations)
8. [Distribution](#8-distribution)
9. [Appendix](#9-appendix)

---

## 1. Product Overview

| Field | Detail |
|---|---|
| **App Name** | APK Guard |
| **Tagline** | "Detect, warn, and reduce APK scam risk before users get compromised" |
| **Platform** | Android |
| **Minimum SDK** | API 26 (Android 8.0 Oreo) |
| **Target SDK** | API 34+ (Android 14+) |
| **License** | Proprietary |

### 1.1 Problem Statement

Scam operations increasingly distribute malicious APK files through messaging apps (WhatsApp, Telegram), browsers (Chrome, Edge, Firefox), and file managers. Victims -- often elderly or less tech-savvy users -- unknowingly grant "Install unknown apps" permission to these sources, allowing sideloaded malware to steal banking credentials, personal data, and device control.

There is no built-in Android feature that gives users a single, clear view of which apps on their device are allowed to sideload APKs, nor guided steps to revoke that permission.

### 1.2 Solution

APK Guard provides a security-checklist companion that:

1. **Detects** which high-risk sideload-capable apps are installed on the device.
2. **Warns** users with a clear dashboard showing which apps need attention.
3. **Guides** users step-by-step to disable "Install unknown apps" for each source.
4. **Reminds** users to re-audit periodically via background checks.

### 1.3 Target Users

| Segment | Description |
|---|---|
| **General consumers** | Everyday Android users who may not understand sideloading risks |
| **Elderly / vulnerable users** | Primary scam targets who need simple, clear guidance |
| **Families** | Parents/children who set up devices for elderly relatives |
| **SME staff** | Small-business employees using personal or lightly-managed devices |

### 1.4 Success Metrics

- Number of audit items marked "Secured" per user session.
- Percentage of users who complete the full remediation flow.
- Weekly active users returning via reminder notifications.
- App store rating >= 4.5 stars.

---

## 2. Functional Requirements

### 2.1 v1 MVP Features

#### FR-01: Security Audit Dashboard

| Field | Detail |
|---|---|
| **Priority** | P0 (Must Have) |
| **Description** | A home screen that shows a checklist of potentially risky apps with their current status. |

**Acceptance Criteria:**
- Display all queried apps grouped by category (Messaging, Browser, File Manager).
- Each item shows: app icon, app name, risk description, status badge, action button.
- Status badges: **Secured** (green), **Needs Review** (red), **Not Installed** (gray).
- Pull-to-refresh triggers a re-scan of all items.
- Dashboard loads within 1 second on mid-range devices.

#### FR-02: Install Source Detection

| Field | Detail |
|---|---|
| **Priority** | P0 (Must Have) |
| **Description** | Detect whether known high-risk apps are installed on the device using `<queries>` manifest declarations. |

**Acceptance Criteria:**
- Detect the presence of all apps listed in [Section 3.5 (Package Query List)](#35-package-query-list).
- Use `PackageManager.getPackageInfo()` or `resolveActivity()` with appropriate exception handling.
- Handle Android 11+ package visibility restrictions via `<queries>` in `AndroidManifest.xml`.
- Correctly differentiate between "not installed" and "installed but not queryable."
- Detection completes in under 500ms for all packages.

#### FR-03: Guided Remediation Flow

| Field | Detail |
|---|---|
| **Priority** | P0 (Must Have) |
| **Description** | For each detected app, provide a step-by-step flow to disable its "Install unknown apps" permission. |

**Acceptance Criteria:**
- Tapping "Fix" on an audit item opens an instruction screen specific to that app.
- Instruction screen shows:
  - App name and icon.
  - Why this permission is risky (1-2 sentences).
  - Numbered step-by-step instructions with screenshots/illustrations.
  - "Open Settings" button that deep-links to that app's install-sources settings page.
  - "I've Done It" button to mark the item as secured.
  - "Skip" button to defer remediation.
- Deep-link uses `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` with the target app's package URI:

```kotlin
val intent = Intent(
    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
    Uri.parse("package:com.whatsapp")
)
startActivity(intent)
```

- If the settings page cannot be resolved (e.g., older Android versions), fall back to `Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS` and show manual instructions.
- After the user returns from settings, prompt for confirmation ("Did you disable it?").

#### FR-04: Play Protect Status Check

| Field | Detail |
|---|---|
| **Priority** | P1 (Should Have) |
| **Description** | Remind users to enable Google Play Protect and its "Improve harmful app detection" setting. |

**Acceptance Criteria:**
- Dashboard includes a Play Protect card showing current advisory status.
- "Check Play Protect" button opens Play Protect settings via:

```kotlin
val intent = Intent("com.google.android.gms.security.VERIFY_APPS_SETTINGS")
startActivity(intent)
```

- Provide fallback instructions if Google Play Services is unavailable.
- Display tips about what Play Protect does and why "improved detection" matters.

#### FR-05: Security Score

| Field | Detail |
|---|---|
| **Priority** | P1 (Should Have) |
| **Description** | A local score reflecting how many risky install sources have been addressed. |

**Acceptance Criteria:**
- Score formula: `(securedItems / totalDetectedItems) * 100`.
- Only installed apps count toward the total. "Not Installed" apps are excluded.
- Score displayed as a circular progress indicator with percentage on the dashboard.
- Color coding: 0-49% red, 50-79% amber, 80-100% green.
- Score persists locally across sessions.
- Recalculates on every audit refresh.

#### FR-06: Periodic Reminders

| Field | Detail |
|---|---|
| **Priority** | P1 (Should Have) |
| **Description** | Weekly background re-check via WorkManager to remind users if their security posture has changed. |

**Acceptance Criteria:**
- WorkManager `PeriodicWorkRequest` runs every 7 days (configurable in settings: 3/7/14/30 days).
- Worker re-scans all queried packages and compares against last audit state.
- If any new risky app is detected or status has regressed, post a notification.
- Notification taps open the dashboard.
- Respects battery optimization and Doze mode (WorkManager handles this).
- User can enable/disable reminders in settings.

#### FR-07: Onboarding

| Field | Detail |
|---|---|
| **Priority** | P1 (Should Have) |
| **Description** | First-launch experience explaining the app's purpose and anti-scam education. |

**Acceptance Criteria:**
- 3-4 onboarding screens shown only on first launch.
- Content covers:
  1. What APK scams are and how they work.
  2. How scam APKs reach your device (WhatsApp, Telegram, Chrome, etc.).
  3. What APK Guard does to protect you.
  4. Quick-start: "Let's audit your device now."
- "Skip" and "Next" navigation.
- Onboarding completion state persisted in SharedPreferences.
- Re-accessible from Settings > "View Onboarding."

---

### 2.2 v2 Features (Post-MVP)

#### FR-08: High-Risk Permissions Review

| Field | Detail |
|---|---|
| **Priority** | P2 (v2) |
| **Description** | Audit dangerous permissions granted to apps: Accessibility Service, Notification Listener, Draw Over Other Apps (SYSTEM_ALERT_WINDOW). |

**Details:**
- List apps that hold these permissions.
- Flag apps that are not from the Play Store.
- Guide users to revoke suspicious grants.
- Deep-link to the relevant settings pages:
  - `Settings.ACTION_ACCESSIBILITY_SETTINGS`
  - `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`
  - `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`

#### FR-09: Post-Install Incident Response

| Field | Detail |
|---|---|
| **Priority** | P2 (v2) |
| **Description** | If a user suspects they installed a scam app, guide them through containment steps. |

**Workflow:**
1. Enable airplane mode.
2. Identify and uninstall the suspicious app.
3. Revoke accessibility/admin permissions if the app resists uninstall.
4. Change passwords for banking and email.
5. Contact bank to freeze accounts.
6. File a police report (with local reporting links).

#### FR-10: Suspicious Link Scanner

| Field | Detail |
|---|---|
| **Priority** | P3 (v2) |
| **Description** | Accept shared URLs via Android share intents and check against known scam patterns. |

**Details:**
- Register as a share target for `text/plain` intents.
- Check URL against a local pattern database (regex for common scam domains, URL shorteners, APK download patterns).
- Display risk verdict: Safe / Suspicious / Dangerous.
- No network requests in v1 of this feature; pattern matching is local only.

#### FR-11: App Reputation Checker

| Field | Detail |
|---|---|
| **Priority** | P3 (v2) |
| **Description** | Users paste an APK download link or package name and the app checks it against an allowlist. |

**Details:**
- Maintain a local allowlist of known-good package names (top 500 Play Store apps).
- Flag anything not on the allowlist as "Unverified."
- Future: integrate with a cloud reputation API.

---

## 3. Technical Architecture

### 3.1 Tech Stack

| Component | Technology |
|---|---|
| **Language** | Kotlin 1.9+ |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Local Database** | Room (SQLite) |
| **Background Work** | WorkManager |
| **Dependency Injection** | Hilt (Dagger) |
| **Navigation** | Compose Navigation |
| **Build System** | Gradle (Kotlin DSL) |
| **Min SDK** | 26 (Android 8.0) |
| **Target SDK** | 34+ (Android 14+) |
| **Compile SDK** | 34+ |

### 3.2 App Architecture

The app follows **MVVM + Clean Architecture** with feature-based module organization.

```
app/
├── build.gradle.kts
├── src/main/
│   ├── AndroidManifest.xml
│   └── java/com/apkguard/
│       ├── ApkGuardApp.kt                  # Application class (Hilt entry point)
│       ├── MainActivity.kt                 # Single-activity host
│       │
│       ├── core/                           # Shared utilities
│       │   ├── di/                         # Hilt modules
│       │   ├── data/                       # Room database, DAOs, entities
│       │   ├── domain/                     # Shared domain models
│       │   ├── ui/                         # Shared composables, theme
│       │   └── util/                       # Extensions, constants
│       │
│       ├── feature/
│       │   ├── audit/                      # Security audit & dashboard
│       │   │   ├── data/                   # Repository implementations
│       │   │   ├── domain/                 # Use cases, repository interfaces
│       │   │   └── ui/                     # ViewModels, screens, composables
│       │   │
│       │   ├── remediation/                # Guided fix flow
│       │   │   ├── data/
│       │   │   ├── domain/
│       │   │   └── ui/
│       │   │
│       │   ├── score/                      # Security score calculation
│       │   │   ├── data/
│       │   │   ├── domain/
│       │   │   └── ui/
│       │   │
│       │   ├── onboarding/                 # First-launch onboarding
│       │   │   └── ui/
│       │   │
│       │   └── settings/                   # App settings
│       │       └── ui/
│       │
│       └── worker/                         # WorkManager workers
│           └── AuditReminderWorker.kt
```

### 3.3 Layer Responsibilities

| Layer | Responsibility | Example |
|---|---|---|
| **UI (Presentation)** | Compose screens, ViewModels, UI state | `AuditDashboardScreen.kt`, `AuditViewModel.kt` |
| **Domain** | Use cases, repository interfaces, domain models | `RunAuditUseCase.kt`, `AuditRepository.kt` |
| **Data** | Repository implementations, Room DAOs, PackageManager queries | `AuditRepositoryImpl.kt`, `AuditDao.kt` |

### 3.4 Key Android APIs

#### 3.4.1 Package Detection

```kotlin
// Check if a package is installed
fun isPackageInstalled(packageName: String, pm: PackageManager): Boolean {
    return try {
        pm.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
```

#### 3.4.2 Deep-Link to Install Source Settings

```kotlin
// Open the "Install unknown apps" settings for a specific app
fun openInstallSourceSettings(context: Context, packageName: String) {
    try {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:$packageName")
        )
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Fallback for devices that don't support the deep-link
        val fallback = Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)
        context.startActivity(fallback)
    }
}
```

**Note:** `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` requires API 26+, which aligns with our minimum SDK.

#### 3.4.3 WorkManager Periodic Audit

```kotlin
// Schedule periodic audit reminder
fun scheduleAuditReminder(context: Context, intervalDays: Long = 7) {
    val workRequest = PeriodicWorkRequestBuilder<AuditReminderWorker>(
        intervalDays, TimeUnit.DAYS
    )
        .setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
        )
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "audit_reminder",
        ExistingPeriodicWorkPolicy.UPDATE,
        workRequest
    )
}
```

#### 3.4.4 Play Protect Settings Intent

```kotlin
fun openPlayProtectSettings(context: Context) {
    try {
        val intent = Intent("com.google.android.gms.security.VERIFY_APPS_SETTINGS")
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Fallback: open Google Play Store
        val fallback = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=com.google.android.gms")
        }
        context.startActivity(fallback)
    }
}
```

### 3.5 Package Query List

The following packages must be declared in `AndroidManifest.xml` for Android 11+ (API 30+) package visibility:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <queries>
        <!-- Messaging Apps -->
        <package android:name="com.whatsapp" />
        <package android:name="com.whatsapp.w4b" />           <!-- WhatsApp Business -->
        <package android:name="org.telegram.messenger" />

        <!-- Browsers — Global & Regional (SE Asia focus) -->
        <package android:name="com.android.chrome" />          <!-- Chrome (~71% market share) -->
        <package android:name="com.sec.android.app.sbrowser" /> <!-- Samsung Internet (pre-installed on Samsung) -->
        <package android:name="com.microsoft.emmx" />           <!-- Microsoft Edge -->
        <package android:name="org.mozilla.firefox" />          <!-- Firefox -->
        <package android:name="com.opera.browser" />            <!-- Opera -->
        <package android:name="com.opera.mini.native" />        <!-- Opera Mini (popular in emerging markets) -->
        <package android:name="com.brave.browser" />            <!-- Brave -->
        <package android:name="com.UCMobile.intl" />            <!-- UC Browser (popular in India/Indonesia) -->
        <package android:name="com.duckduckgo.mobile.android" /> <!-- DuckDuckGo -->
        <package android:name="com.vivaldi.browser" />          <!-- Vivaldi -->
        <package android:name="com.mi.globalbrowser" />         <!-- Mi Browser (pre-installed on Xiaomi) -->
        <package android:name="com.vivo.browser" />             <!-- Vivo Browser (pre-installed on Vivo) -->
        <package android:name="com.heytap.browser" />           <!-- OPPO Browser (pre-installed on OPPO) -->
        <package android:name="com.huawei.browser" />           <!-- Huawei Browser (pre-installed on Huawei) -->

        <!-- File Managers -->
        <package android:name="com.google.android.apps.nbu.files" /> <!-- Files by Google -->
        <package android:name="com.mi.android.globalFileexplorer" /> <!-- Xiaomi File Manager -->
    </queries>

    <!-- ... -->
</manifest>
```

**Important:** The `QUERY_ALL_PACKAGES` permission must **NOT** be used. Google Play policy restricts this permission to specific app categories (device management, antivirus, financial apps with regulatory requirements). APK Guard does not qualify and would be rejected during review.

### 3.6 Dependency Versions (Indicative)

| Dependency | Version |
|---|---|
| Kotlin | 1.9.x |
| Compose BOM | 2024.x |
| Material 3 | 1.2.x |
| Room | 2.6.x |
| WorkManager | 2.9.x |
| Hilt | 2.50+ |
| Compose Navigation | 2.7.x |
| Core KTX | 1.12.x |

---

## 4. Non-Functional Requirements

### 4.1 Permissions

| Requirement | Detail |
|---|---|
| **Dangerous permissions** | ZERO required for v1 |
| **Normal permissions** | `POST_NOTIFICATIONS` (API 33+, runtime request), `RECEIVE_BOOT_COMPLETED` (restart WorkManager after reboot) |
| **Prohibited permissions** | `QUERY_ALL_PACKAGES`, `REQUEST_INSTALL_PACKAGES` |

The app explicitly avoids requesting any dangerous permissions to maintain user trust and simplify the Play Store review process.

### 4.2 Privacy

| Requirement | Detail |
|---|---|
| **Data collection** | No personal data collected in v1 |
| **Network access** | No internet permission required in v1 |
| **Data storage** | All audit state stored locally on-device via Room |
| **Analytics** | None in v1; if added later, must be opt-in and privacy-respecting |
| **Data sharing** | No data leaves the device |

### 4.3 Performance

| Metric | Target |
|---|---|
| **Cold start** | < 2 seconds on mid-range devices |
| **Audit scan** | < 500ms for all package queries |
| **Dashboard render** | < 1 second |
| **APK size** | < 10 MB |
| **Memory usage** | < 50 MB resident |
| **Battery impact** | Negligible (WorkManager respects Doze/standby) |

### 4.4 Accessibility

| Requirement | Detail |
|---|---|
| **Screen reader** | Full TalkBack support; all interactive elements have content descriptions |
| **Text scaling** | Support system font scaling up to 200% |
| **Touch targets** | Minimum 48dp x 48dp for all interactive elements |
| **Color contrast** | Meet WCAG 2.1 AA contrast ratios (4.5:1 for text, 3:1 for large text) |
| **Motion** | Respect `prefers-reduced-motion` system setting |

### 4.5 Localization

| Phase | Languages |
|---|---|
| **v1.0** | English (en) |
| **v1.1** | Malay (ms), Chinese Simplified (zh-CN) |
| **v1.2** | Thai (th) |

**Implementation notes:**
- All user-facing strings in `res/values/strings.xml` with language-specific overrides.
- RTL layout support not required for v1 target languages but should not be blocked.
- Date/time formatting via `java.time` with locale awareness.

### 4.6 Security

| Requirement | Detail |
|---|---|
| **Code obfuscation** | R8/ProGuard enabled for release builds |
| **Signing** | APK signed with a dedicated release keystore (not debug) |
| **Backup** | `android:allowBackup="false"` to prevent audit state extraction |
| **Exported components** | No exported activities, services, or receivers beyond the launcher activity |

---

## 5. UI/UX Requirements

### 5.1 Design System

- **Theme:** Material 3 / Material You with dynamic color support (Android 12+).
- **Fallback:** Static Material 3 color scheme on Android 8-11.
- **Typography:** Default Material 3 type scale.
- **Dark mode:** Supported, follows system setting.

### 5.2 Screen Map

```
Onboarding (first launch only)
  ├── Page 1: What are APK scams?
  ├── Page 2: How scam APKs reach you
  ├── Page 3: What APK Guard does
  └── Page 4: Let's audit your device

Main App
  ├── Home / Dashboard
  │   ├── Security Score (circular progress)
  │   ├── Play Protect status card
  │   └── Quick summary of audit status
  │
  ├── Audit Checklist
  │   ├── Messaging Apps section
  │   │   ├── WhatsApp card
  │   │   ├── WhatsApp Business card
  │   │   └── Telegram card
  │   ├── Browsers section
  │   │   ├── Chrome card
  │   │   ├── Samsung Internet card
  │   │   ├── Microsoft Edge card
  │   │   └── Firefox card
  │   └── File Managers section
  │       ├── Files by Google card
  │       └── Xiaomi File Manager card
  │
  ├── Remediation Flow (per app)
  │   ├── Instruction screen
  │   ├── → Deep-link to Settings
  │   └── Confirmation screen (I've Done It / Skip)
  │
  └── Settings
      ├── Reminder frequency
      ├── Notifications toggle
      ├── View onboarding
      ├── About / Version
      └── Privacy policy
```

### 5.3 Audit Card Design

Each audit item is rendered as a Material 3 card with the following layout:

```
┌─────────────────────────────────────────────┐
│  [App Icon]  App Name              [Badge]  │
│              Risk description text           │
│                                    [Action]  │
└─────────────────────────────────────────────┘
```

| Element | Detail |
|---|---|
| **App Icon** | Loaded from device if installed; fallback drawable if not |
| **App Name** | e.g., "WhatsApp", "Chrome" |
| **Risk Description** | e.g., "Can install APK files sent via chat" |
| **Status Badge** | Chip with color and label |
| **Action Button** | "Fix" (if Needs Review), "Re-check" (if Secured), hidden (if Not Installed) |

### 5.4 Status Badge Definitions

| Status | Color | Label | Meaning |
|---|---|---|---|
| **Secured** | Green (`#4CAF50`) | "Secured" | User confirmed they disabled install permission |
| **Needs Review** | Red (`#F44336`) | "Needs Review" | App is installed and user has not confirmed remediation |
| **Not Installed** | Gray (`#9E9E9E`) | "Not Installed" | App is not present on the device |
| **Skipped** | Amber (`#FF9800`) | "Skipped" | User chose to skip remediation for now |

### 5.5 Security Score Display

- Circular progress indicator (arc/donut style) centered on the dashboard.
- Percentage value displayed in the center in large bold text.
- Color transitions: red (0-49%) -> amber (50-79%) -> green (80-100%).
- Subtitle text: "X of Y sources secured."

### 5.6 Fix Flow Screens

**Instruction Screen:**
```
┌─────────────────────────────────────────────┐
│  [Back Arrow]     Fix: WhatsApp             │
│                                             │
│  [App Icon - Large]                         │
│                                             │
│  WhatsApp can install APK files sent via    │
│  chat. Scammers use this to send malware    │
│  disguised as useful apps.                  │
│                                             │
│  Steps:                                     │
│  1. Tap "Open Settings" below              │
│  2. Find "Install unknown apps"            │
│  3. Toggle it OFF                          │
│  4. Come back and tap "I've Done It"       │
│                                             │
│  ┌─────────────────────────────────────┐    │
│  │        Open Settings                │    │
│  └─────────────────────────────────────┘    │
│                                             │
│  [I've Done It]          [Skip for Now]     │
└─────────────────────────────────────────────┘
```

---

## 6. Data Model

### 6.1 Room Database Schema

#### Table: `audit_items`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `INTEGER` | PRIMARY KEY, AUTOINCREMENT | Unique identifier |
| `package_name` | `TEXT` | NOT NULL, UNIQUE | Android package name |
| `app_name` | `TEXT` | NOT NULL | Display name |
| `category` | `TEXT` | NOT NULL | One of: MESSAGING, BROWSER, FILE_MANAGER |
| `status` | `TEXT` | NOT NULL, DEFAULT 'NEEDS_REVIEW' | SECURED, NEEDS_REVIEW, NOT_INSTALLED, SKIPPED |
| `risk_description` | `TEXT` | NOT NULL | User-facing risk explanation |
| `last_checked` | `INTEGER` | NOT NULL | Unix timestamp (millis) of last audit |

#### Table: `security_scores`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `INTEGER` | PRIMARY KEY, DEFAULT 1 | Singleton row |
| `total_items` | `INTEGER` | NOT NULL | Number of installed auditable apps |
| `secured_items` | `INTEGER` | NOT NULL | Number marked as secured |
| `score_percentage` | `INTEGER` | NOT NULL | Calculated score (0-100) |
| `last_audit_date` | `INTEGER` | NOT NULL | Unix timestamp of last full audit |

#### Table: `reminder_config`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `INTEGER` | PRIMARY KEY, DEFAULT 1 | Singleton row |
| `enabled` | `INTEGER` | NOT NULL, DEFAULT 1 | Boolean (0/1) |
| `interval_days` | `INTEGER` | NOT NULL, DEFAULT 7 | Reminder interval |
| `last_reminder_date` | `INTEGER` | | Unix timestamp of last reminder sent |

### 6.2 Room Entity Definitions

```kotlin
@Entity(tableName = "audit_items")
data class AuditItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "app_name") val appName: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "status") val status: String = "NEEDS_REVIEW",
    @ColumnInfo(name = "risk_description") val riskDescription: String,
    @ColumnInfo(name = "last_checked") val lastChecked: Long
)

@Entity(tableName = "security_scores")
data class SecurityScoreEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "total_items") val totalItems: Int,
    @ColumnInfo(name = "secured_items") val securedItems: Int,
    @ColumnInfo(name = "score_percentage") val scorePercentage: Int,
    @ColumnInfo(name = "last_audit_date") val lastAuditDate: Long
)

@Entity(tableName = "reminder_config")
data class ReminderConfigEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "enabled") val enabled: Boolean = true,
    @ColumnInfo(name = "interval_days") val intervalDays: Int = 7,
    @ColumnInfo(name = "last_reminder_date") val lastReminderDate: Long? = null
)
```

### 6.3 Domain Models

```kotlin
enum class AuditStatus {
    SECURED, NEEDS_REVIEW, NOT_INSTALLED, SKIPPED
}

enum class AppCategory {
    MESSAGING, BROWSER, FILE_MANAGER
}

data class AuditItem(
    val id: Int,
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val status: AuditStatus,
    val riskDescription: String,
    val lastChecked: Instant
)

data class SecurityScore(
    val totalItems: Int,
    val securedItems: Int,
    val scorePercentage: Int,
    val lastAuditDate: Instant
)

data class ReminderConfig(
    val enabled: Boolean,
    val intervalDays: Int,
    val lastReminderDate: Instant?
)
```

### 6.4 Pre-Seeded Audit Data

The database is pre-seeded with the following audit items on first launch:

| Package Name | App Name | Category | Risk Description |
|---|---|---|---|
| `com.whatsapp` | WhatsApp | MESSAGING | Can install APK files sent via chat |
| `com.whatsapp.w4b` | WhatsApp Business | MESSAGING | Can install APK files sent via chat |
| `org.telegram.messenger` | Telegram | MESSAGING | Can install APK files sent via chat or channels |
| `com.android.chrome` | Chrome | BROWSER | Can install APK files downloaded from websites |
| `com.sec.android.app.sbrowser` | Samsung Internet | BROWSER | Can install APK files downloaded from websites |
| `com.microsoft.emmx` | Microsoft Edge | BROWSER | Can install APK files downloaded from websites |
| `org.mozilla.firefox` | Firefox | BROWSER | Can install APK files downloaded from websites |
| `com.google.android.apps.nbu.files` | Files by Google | FILE_MANAGER | Can install APK files from device storage |
| `com.mi.android.globalFileexplorer` | Xiaomi File Manager | FILE_MANAGER | Can install APK files from device storage |

---

## 7. Constraints & Limitations

### 7.1 Technical Constraints

| Constraint | Explanation | Impact |
|---|---|---|
| **Cannot programmatically toggle install permission** | Android does not expose an API to enable/disable "Install unknown apps" for other apps. Only the user can do this manually through Settings. | The app must use a user-confirmed checklist approach rather than automatic remediation. |
| **`canRequestPackageInstalls()` scope** | `PackageManager.canRequestPackageInstalls()` only returns the status for the **calling** package, not for arbitrary packages. | We cannot read whether WhatsApp/Chrome/etc. currently have install permission. We must rely on user self-reporting. |
| **No public API for other apps' install permission state** | There is no Android API to query whether a third-party app has been granted `REQUEST_INSTALL_PACKAGES`. | Reinforces the checklist/user-confirmation model as the only viable approach. |
| **`QUERY_ALL_PACKAGES` prohibited** | Google Play policy restricts this permission. Using it would result in app rejection. | Must declare specific `<queries>` entries for each package to detect. New packages require an app update. |
| **`REQUEST_INSTALL_PACKAGES` prohibited** | APK Guard must not request this permission as it would contradict the app's purpose. | The app itself cannot install APKs, which is the correct behavior. |
| **WorkManager minimum interval** | `PeriodicWorkRequest` has a minimum interval of 15 minutes. | Not a practical issue since our minimum reminder interval is 3 days. |

### 7.2 UX Constraints

| Constraint | Explanation |
|---|---|
| **User honesty dependency** | The "I've Done It" confirmation relies on user honesty. There is no way to verify the action was actually taken. |
| **Settings UI varies by OEM** | The "Install unknown apps" settings page may look different on Samsung, Xiaomi, OnePlus, etc. Instructions must be generic enough to work across OEMs. |
| **No auto-detection of regression** | If a user re-enables install permission after marking it "Secured," the app cannot detect this automatically. Periodic re-audits mitigate but don't eliminate this gap. |

### 7.3 Scope Exclusions (v1)

The following are explicitly **out of scope** for v1:

- Real-time APK install interception or blocking.
- Network-based threat intelligence lookups.
- Device administrator or MDM functionality.
- Root detection or SafetyNet/Play Integrity checks.
- Scanning APK files for malware signatures.
- Integration with any external API or cloud service.

---

## 8. Distribution

### 8.1 Distribution Channel

| Requirement | Detail |
|---|---|
| **Primary channel** | Google Play Store |
| **Direct APK distribution** | Explicitly prohibited -- the app must practice what it preaches |
| **Alternative stores** | Not planned for v1 |

### 8.2 Play Store Listing

| Field | Content |
|---|---|
| **Category** | Tools |
| **Content rating** | Everyone |
| **Price** | Free |
| **In-app purchases** | None in v1; potential premium features in future |
| **Ads** | None in v1 |

### 8.3 Play Store Compliance

| Requirement | Detail |
|---|---|
| **Data Safety section** | Declare: no data collected, no data shared, no data transferred |
| **Permissions declaration** | Justify `POST_NOTIFICATIONS` and `RECEIVE_BOOT_COMPLETED` |
| **`<queries>` justification** | May need to explain package queries during review; document that specific packages are queried for security audit purposes |
| **Target API** | Must meet Google Play's current target API requirement (API 34+) |

---

## 9. Appendix

### 9.1 Glossary

| Term | Definition |
|---|---|
| **APK** | Android Package Kit -- the file format used to distribute and install apps on Android |
| **Sideloading** | Installing an app from a source other than the Google Play Store |
| **Install unknown apps** | An Android permission that allows a specific app to install APK files |
| **Play Protect** | Google's built-in malware scanning service for Android |
| **Package visibility** | Android 11+ restriction requiring apps to declare which other packages they need to query |

### 9.2 Reference Links

| Resource | URL |
|---|---|
| Android `<queries>` documentation | https://developer.android.com/training/package-visibility |
| `ACTION_MANAGE_UNKNOWN_APP_SOURCES` | https://developer.android.com/reference/android/provider/Settings#ACTION_MANAGE_UNKNOWN_APP_SOURCES |
| WorkManager documentation | https://developer.android.com/topic/libraries/architecture/workmanager |
| Material 3 for Compose | https://developer.android.com/jetpack/compose/designsystems/material3 |
| Play Store target API requirements | https://developer.android.com/google/play/requirements/target-sdk |

### 9.3 Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Google Play rejects app due to `<queries>` usage | Low | High | Document legitimate security purpose; use specific queries, not `QUERY_ALL_PACKAGES` |
| Users mark items "Secured" without actually fixing | Medium | Medium | Periodic reminders to re-audit; educational content explaining importance |
| OEM-specific settings pages break deep-links | Medium | Low | Provide fallback to general app settings; include manual instructions |
| New sideload-capable apps not in query list | Medium | Low | Regular app updates to add new packages; v2 could add user-reported apps |
| Users confuse APK Guard with an antivirus app | Medium | Low | Clear onboarding and Play Store description managing expectations |

---

*End of document.*

# E02: Link Checker Feature

> **Phase:** 1 (MVP)
> **Priority:** P0 — Core user-facing feature
> **Depends On:** E01 (Backend API + Scam Database)
> **Estimated Effort:** 1-2 weeks

---

## Overview

Implement the link checker feature in the Android app. Users can paste a URL or share a link from another app to check if it's a scam. The checker uses a 3-layer architecture: on-device SQLite database (95%+ of checks), Bloom filter for fast negatives, and API fallback for unknowns. Includes daily database sync from the backend, share intent handling, and a verdict result screen with sharing capability.

## Technical Specs

- Architecture: [docs/INFRASTRUCTURE_ARCHITECTURE.md](../INFRASTRUCTURE_ARCHITECTURE.md) (Caching Strategy section)
- Technical Requirements: [docs/TECHNICAL_REQUIREMENTS.md](../TECHNICAL_REQUIREMENTS.md)
- Backend API: E01 endpoints (`/api/check`, `/api/data/*`)

## Tech Stack

- Kotlin + Jetpack Compose + Material 3
- Room (local scam domain database)
- WorkManager (daily database sync)
- Retrofit/OkHttp (API calls to backend)
- Hilt (dependency injection)
- Bloom filter (on-device, downloaded from backend)

---

## Issues

### E02-001: Local Scam Domain Database + Room Setup

Set up the Room database for storing scam domains locally, including the domain lookup DAO and Bloom filter storage.

**Acceptance Criteria:**
- Room database with `domains` table matching backend schema (domain PK, verdict, reason, source, confidence)
- `metadata` table tracking last sync version, timestamp, and domain count
- DAO with `findByDomain(domain)` query for instant lookups
- Bloom filter loaded into memory from local file storage
- Bloom filter `mightContain(domain)` check with MurmurHash3
- Database pre-populated with bundled snapshot from APK assets (or empty on first install)

**Test Cases:**
- Domain lookup returns correct verdict for known domain
- Domain lookup returns null for unknown domain
- Bloom filter correctly identifies all inserted domains (zero false negatives)
- Bloom filter has <1% false positive rate
- Metadata table tracks sync version correctly

---

### E02-002: Daily Database Sync (WorkManager)

Implement background sync that downloads the latest scam database delta from the backend daily.

**Acceptance Criteria:**
- WorkManager periodic task runs daily (on WiFi preferred, battery-not-low)
- Sync flow: GET `/api/data/latest` → compare version → GET `/api/data/delta` → merge into Room → GET `/api/data/bloom` → update Bloom filter
- Full database download on first install or if delta too old (>7 days)
- Sync status tracked in metadata table
- Notification shown on sync failure (optional, non-blocking)
- Respects battery optimization and Doze mode

**Test Cases:**
- Sync downloads delta when new version available
- Sync skips when already up to date
- Sync falls back to full download when delta too old
- Sync updates metadata after successful completion
- Sync retries on network failure (WorkManager retry policy)

---

### E02-003: Link Check Flow (3-Layer Architecture)

Implement the core check logic: local DB → Bloom filter → API fallback.

**Acceptance Criteria:**
- Check flow: Room query → Bloom filter → POST `/api/check`
- Local DB hit returns verdict instantly (<10ms)
- Bloom filter negative returns "Not in our database — proceed with caution"
- Bloom filter positive (possible match) triggers API call
- API fallback for domains not in local DB or Bloom filter
- Results cached locally in Room for recent checks
- Domain normalization: strip protocol, path, query, www, lowercase
- Handle Punycode/IDN domains

**Test Cases:**
- Known scam domain returns "dangerous" from local DB
- Known safe domain returns "safe" from local DB
- Unknown domain not in Bloom filter returns "not known scam"
- Unknown domain in Bloom filter triggers API call
- API returns verdict for unknown domain
- Domain extraction from full URL works correctly
- Results cached for subsequent lookups

---

### E02-004: Link Checker UI (Paste & Check Screen)

Build the link checker user interface with input field, verdict display, and sharing.

**Acceptance Criteria:**
- Input field accepting URL or domain text
- "Check" button triggers the check flow
- Loading state while checking
- Verdict result screen showing:
  - Domain name
  - Verdict badge (Safe/Suspicious/Dangerous/Unknown) with color coding
  - Reason text explaining the verdict
  - Confidence indicator
  - "Share Result" button generating a shareable verdict card
  - "Check Another" button to reset
- Error states: no internet (for API fallback), invalid input
- Share intent receiver: app accepts shared text from other apps (WhatsApp, Chrome, etc.)
- Material 3 design with dark mode support

**Test Cases:**
- Input field accepts and validates URLs
- Check button triggers check flow and shows loading
- Verdict screen displays correct verdict with styling
- Share button generates shareable content
- Share intent from other apps triggers check
- Invalid input shows error message
- Offline mode works for local DB checks

---

### E02-005: Shareable Verdict Card + Viral Loop

Generate shareable verdict cards for social sharing (WhatsApp, TikTok, etc.).

**Acceptance Criteria:**
- Generate verdict image card (bitmap) with:
  - Domain name
  - Verdict (Safe/Suspicious/Dangerous) with icon and color
  - "Checked by Safe Anot?" branding
  - QR code or link to download the app
- Share via Android share sheet (WhatsApp, Telegram, social media)
- POST `/api/score/share` to record shared verdicts (for viral metrics)
- Deep link: `safeanot.com/result?domain=xxx` opens web verdict page (E06)
- Card design follows brand guidelines

**Test Cases:**
- Verdict card generated with correct content
- Share intent launches with image and text
- Share API call records the share event
- Deep link URL included in share text
- Card renders correctly for all verdict types

---

## Implementation Order

1. **E02-001** — Local database + Bloom filter setup (foundation for local checks)
2. **E02-002** — Daily sync (populates the local database)
3. **E02-003** — Check flow logic (3-layer architecture)
4. **E02-004** — UI (paste & check screen + share intent)
5. **E02-005** — Shareable verdict card (viral growth mechanism)

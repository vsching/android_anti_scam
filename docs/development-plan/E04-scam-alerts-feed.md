# E04: Scam Alerts Feed

> **Phase:** 1 (MVP)
> **Priority:** P1 — Awareness and engagement feature
> **Depends On:** E01 (Backend API — /api/alerts endpoint)
> **Estimated Effort:** 1 week

---

## Overview

Display trending scam alerts from the backend as a feed in the app. Users can browse current scam campaigns in their region (Malaysia/Singapore), share alerts with contacts, and stay informed about active threats. The backend `/api/alerts` endpoint (built in E01) provides the data.

## Technical Specs

- Architecture: [docs/INFRASTRUCTURE_ARCHITECTURE.md](../INFRASTRUCTURE_ARCHITECTURE.md) (Endpoints table)
- Backend: `/api/alerts` endpoint with region filtering (E01-004, already built)
- Alert schema: `{ id, title, description, scam_type, severity, region, report_count, created_at }`

## Tech Stack

- Kotlin + Jetpack Compose + Material 3
- Retrofit (API calls — already set up in E02)
- Room (offline cache for alerts)
- Hilt (dependency injection)

---

## Issues

### E04-001: Alerts API Integration + Local Cache

Connect to the `/api/alerts` endpoint and cache results locally for offline access.

**Acceptance Criteria:**
- Retrofit endpoint: `GET /api/alerts?region={MY|SG}`
- Alert data class matching backend schema
- Room entity + DAO for offline caching
- Repository fetches from API, falls back to cached data when offline
- Auto-detect user region from device locale (MY/SG, default to both)

**Test Cases:**
- API response parsed correctly
- Cached alerts returned when offline
- Region detection from locale works
- Cache updated on successful API fetch

---

### E04-002: Alerts Feed Screen

Display scam alerts in a scrollable feed with Material 3 cards.

**Acceptance Criteria:**
- LazyColumn feed of alert cards sorted by recency
- Each card shows: severity badge (High/Medium/Low with color), title, scam type chip, description preview (2 lines), report count, relative timestamp
- Pull-to-refresh triggers API reload
- Loading state with shimmer/skeleton placeholders
- Empty state when no alerts available
- Region filter chips at top (All / Malaysia / Singapore)

**Test Cases:**
- Feed renders alert cards correctly
- Severity badges show correct colors (red/amber/green)
- Pull-to-refresh triggers reload
- Region filter updates feed content
- Empty state shown when no alerts

---

### E04-003: Alert Detail + Sharing

Full alert detail view with sharing capability.

**Acceptance Criteria:**
- Tapping an alert card opens full detail screen
- Detail shows: full title, complete description, scam type, severity, region, report count, date, "How to Stay Safe" tips
- "Share Alert" button generates shareable text with alert summary
- Share via Android share sheet (WhatsApp, Telegram, etc.)
- Deep link: `safeanot.com/alert/{id}` (for future web support)

**Test Cases:**
- Detail screen shows all alert fields
- Share button launches share sheet with formatted text
- Back navigation returns to feed

---

## Implementation Order

1. **E04-001** — API integration + cache (data layer)
2. **E04-002** — Feed screen (display layer)
3. **E04-003** — Detail + sharing (interaction layer)

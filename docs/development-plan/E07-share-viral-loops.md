# E07: Share & Viral Loops

> **Phase:** 1 (MVP)
> **Priority:** P1 — Growth engine for user acquisition
> **Depends On:** E02 (Link Checker), E03 (Phone Shield)
> **Estimated Effort:** 1-2 weeks

---

## Overview

Build the sharing and viral loop infrastructure that turns every scam check and security audit into a shareable moment. This epic implements shareable security score cards, WhatsApp-optimized warning message templates, share event analytics tracking via the backend API, a "Warn My Contacts" quick-share flow from verdict results, and a unified share utility that all features can use. The goal is to maximize the app's K-factor by making sharing frictionless, contextual, and visually compelling.

## Technical Specs

- Growth Strategy: [docs/GROWTH_AND_DATA_STRATEGY.md](../GROWTH_AND_DATA_STRATEGY.md) (Part 1: Viral Growth Mechanisms)
- Viral Case Studies: [docs/VIRAL_APPS_CASE_STUDIES.md](../VIRAL_APPS_CASE_STUDIES.md)
- Technical Feasibility: [docs/TECHNICAL_FEASIBILITY.md](../TECHNICAL_FEASIBILITY.md) (Share intents)
- Backend API: E01 endpoints (`/api/score/share`)

## Tech Stack

- Kotlin + Jetpack Compose + Material 3
- Android Canvas/Paint (bitmap card generation)
- FileProvider (image sharing)
- Android Share Intent (ACTION_SEND)
- Retrofit/OkHttp (share event tracking API)
- Hilt (dependency injection)

---

## Issues

### E07-001: Unified Share Infrastructure

Extract and centralize the share intent creation, bitmap-to-file caching, and FileProvider URI generation into a reusable share utility. Currently, share logic is duplicated/scattered across CheckViewModel, ProfileScreen ShareHelper, and AlertShareFormatter.

**Acceptance Criteria:**
- Single `ShareIntentFactory` utility that handles text-only shares, image+text shares, and WhatsApp-targeted shares
- Centralized bitmap-to-file cache management with automatic cleanup of old cached images (>24h)
- FileProvider URI generation for all shareable images
- All existing share callers (CheckViewModel, ProfileScreen, AlertDetailScreen) migrated to use the unified utility
- try/catch ActivityNotFoundException wrapping on all share launches
- Supports both generic share sheet and WhatsApp-specific intent targeting

**Test Cases:**
- Text-only share intent has correct action, type, and extras
- Image+text share intent includes EXTRA_STREAM and EXTRA_TEXT
- WhatsApp-targeted intent sets package to "com.whatsapp"
- Cache cleanup removes files older than 24 hours
- FileProvider URI generated correctly from cached bitmap

---

### E07-002: Security Score Shareable Card

Generate a visually compelling, branded security score card (1080x1920 vertical for WhatsApp Status, 1080x1080 square for general sharing) that users can share to show their phone protection status.

**Acceptance Criteria:**
- Bitmap card generator for security score with: score percentage, color-coded ring (red/amber/green), number of items secured vs total, "Safe Anot?" branding, download CTA
- 9:16 vertical variant optimized for WhatsApp Status / Instagram Stories
- 1:1 square variant for general social sharing
- "Share My Score" button on Shield screen generates card and opens share sheet
- Card includes app download deep link text
- Card renders correctly for all score ranges (0%, 50%, 100%)

**Test Cases:**
- Card generates correct bitmap dimensions (1080x1920, 1080x1080)
- Score ring color matches threshold (0-49 red, 50-79 amber, 80-100 green)
- Card includes branding text and download URL
- Share button triggers share intent with image and text
- Card renders correctly at boundary values (0%, 49%, 50%, 79%, 80%, 100%)

---

### E07-003: WhatsApp Warning Message Templates

Create pre-filled, contextual warning message templates that users can one-tap send to WhatsApp contacts or groups after receiving a DANGEROUS or SUSPICIOUS verdict. This is the "Trusted Family Reply Assistant" viral loop.

**Acceptance Criteria:**
- After a DANGEROUS verdict, show a "Warn My Contacts" button below the verdict card
- Tapping opens a template picker with 3 pre-written messages in different tones:
  - Polite: "Hi, I checked this link and it appears to be dangerous. Please don't click it."
  - Urgent: "WARNING: This link is a known scam! Do not click or enter any information."
  - Elder-friendly: "This link is not safe. Please delete it. If you clicked it, contact your bank immediately."
- Each template includes the domain name, verdict, and Safe Anot? app download link
- Templates available in English and Bahasa Malaysia
- One-tap share opens WhatsApp share sheet (falls back to generic share if WhatsApp not installed)
- Message includes the checked domain for context

**Test Cases:**
- Template picker shows 3 options after DANGEROUS verdict
- Template picker not shown for SAFE verdict
- Templates include domain name and verdict
- Templates include app download link
- WhatsApp intent created with correct package when installed
- Fallback to generic share sheet when WhatsApp not installed
- Templates render correctly in both English and Bahasa Malaysia

---

### E07-004: Share Event Analytics (Backend + Client)

Track share events to measure viral coefficient (K-factor). Record what was shared (verdict, score, alert), where it was shared (WhatsApp, generic), and correlate with new installs via referral attribution.

**Acceptance Criteria:**
- New `POST /api/score/share` backend endpoint accepting: shareType (verdict/score/alert), contentId (domain/alertId), platform (whatsapp/generic), timestamp
- Client-side share tracking: fire API call after successful share intent launch
- ShareEvent domain model and repository for queuing/sending events
- Offline queue: store share events locally if offline, sync when connected
- Backend stores events in D1 for analytics queries
- Rate limiting: max 100 share events per device per day

**Test Cases:**
- Share event API call fires after successful share
- Share events queued locally when offline
- Queued events synced when connectivity restored
- Rate limiting prevents excessive event submissions
- API endpoint validates required fields
- Backend stores events in D1 correctly

---

### E07-005: "Forwarded by Someone You Love" Rescue Card

After a DANGEROUS verdict, generate a special share card designed for emotional impact. The card reframes the share from "I almost got scammed" to "I'm protecting my family." This is optimized for WhatsApp group sharing where other recipients likely received the same scam link.

**Acceptance Criteria:**
- After DANGEROUS verdict, show "Protect Your Family" share option alongside existing "Share Result"
- Generate rescue card bitmap (1080x1920 vertical) with:
  - Headline: "This scam was sent to someone you love"
  - Domain name with DANGEROUS badge
  - Reason text from verdict
  - "Check your links before you click" CTA
  - Safe Anot? branding and download link
- Card design uses warm/protective tone (not alarming)
- Share intent includes pre-filled text: "I found a dangerous link in our group. Please don't click [domain]. Check your links with Safe Anot?"
- Track share event with shareType "rescue_card"

**Test Cases:**
- Rescue card option only appears for DANGEROUS verdicts
- Card generates with correct dimensions and content
- Card includes domain name and verdict reason
- Share text includes domain and app download link
- Share event tracked with correct shareType
- Card renders correctly for long domain names (truncation)

---

## Implementation Order

1. **E07-001** -- Unified share infrastructure (foundation for all other issues)
2. **E07-002** -- Security score shareable card (depends on share infra, builds on Shield feature from E03)
3. **E07-003** -- WhatsApp warning templates (depends on share infra, builds on Check feature from E02)
4. **E07-004** -- Share event analytics (depends on share infra, adds tracking to all share surfaces)
5. **E07-005** -- Rescue card (depends on share infra + analytics, most impactful viral loop)

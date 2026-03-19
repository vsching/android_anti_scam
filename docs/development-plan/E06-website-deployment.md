# E06: Website Deployment

> **Phase:** 1 — MVP
> **Depends On:** E01 (Backend API)
> **Status:** Not Started
> **Issues:** 4

---

## Overview

Deploy the existing static website (index.html, check.html, result.html, challenge.html, join.html) to Cloudflare Pages with production configuration, backend API wiring, Android deep link verification, and security hardening.

The HTML pages already exist in `website/`. This epic is about deployment, wiring, and hardening -- not UI creation.

---

## E06-001: Cloudflare Pages Deployment Config

**Description:** Configure Cloudflare Pages to serve the static website from the `website/` directory with proper redirects and routing.

**Tasks:**
- Create `website/_redirects` for clean URL routing (e.g., `/check` -> `/check.html`)
- Create `website/_headers` for security headers on all pages
- Update `website/` internal links from `.html` extensions to clean URLs
- Add `website/404.html` custom error page

**Acceptance Criteria:**
- [ ] Website deploys to Cloudflare Pages from `website/` directory
- [ ] Clean URLs work: `/check`, `/result`, `/challenge`, `/join`
- [ ] Security headers applied to all pages
- [ ] Custom 404 page displayed for unknown routes
- [ ] All internal navigation works correctly with clean URLs

**Test Cases:**
- Verify `_redirects` file contains correct rewrite rules
- Verify `_headers` file applies to all routes
- Verify all internal links use clean URLs (no `.html` suffix)
- Verify 404 page renders for unknown paths

---

## E06-002: Wire check.html and result.html to Backend API

**Description:** Replace the hardcoded SCAM_DB in check.html with a real `fetch()` call to the backend API (`POST /api/check`). Wire result.html to also call the API for its domain param.

**Tasks:**
- Update `check.html` JavaScript to call `POST /api/check` with `{ "url": "..." }`
- Display loading spinner during API call (already exists in CSS)
- Handle API response and render verdict, reason, confidence
- Handle API errors gracefully (show "could not check" message)
- Update `result.html` to fetch verdict from API on page load using `?domain=` param
- Keep example chips functional (fill input and trigger API call)

**Acceptance Criteria:**
- [ ] check.html sends real API requests to backend
- [ ] Verdict display shows API response (domain, verdict, reason)
- [ ] Loading spinner shows during API call
- [ ] Error state handled (network error, API error)
- [ ] result.html fetches live verdict from API for `?domain=xxx`
- [ ] Share URLs still work: `safeanot.com/result?domain=xxx`

**Test Cases:**
- check.html: enter known scam domain -> shows DANGEROUS verdict from API
- check.html: enter known safe domain -> shows SAFE verdict from API
- check.html: disconnect network -> shows error message
- result.html: load with `?domain=maybank-secure-update.xyz` -> shows API verdict
- result.html: load with no domain param -> shows fallback message

---

## E06-003: Digital Asset Links for Android App Verification

**Description:** Serve `/.well-known/assetlinks.json` to enable Android App Links verification for deep links (result page, alert pages).

**Tasks:**
- Create `website/.well-known/assetlinks.json` with app's signing key fingerprint
- Ensure `_redirects` does not interfere with `.well-known` path
- Document the SHA-256 fingerprint update process for release signing key

**Acceptance Criteria:**
- [ ] `safeanot.com/.well-known/assetlinks.json` returns valid Digital Asset Links JSON
- [ ] JSON contains correct package name and SHA-256 fingerprint placeholder
- [ ] Deep links `safeanot.com/result?domain=xxx` verified by Android
- [ ] File served with correct `Content-Type: application/json`

**Test Cases:**
- GET `/.well-known/assetlinks.json` returns 200 with valid JSON
- JSON validates against Google's Digital Asset Links spec
- Android `adb shell am start` with verified link opens app (manual test)

---

## E06-004: Security Headers, CSP, and Legal Pages

**Description:** Add comprehensive security headers via `_headers`, create privacy policy and terms of service pages, and configure Content Security Policy appropriate for the static site.

**Tasks:**
- Configure CSP in `_headers` allowing fonts.googleapis.com, fonts.gstatic.com, and inline styles/scripts
- Add HSTS, Permissions-Policy, and other hardening headers
- Create `website/privacy.html` privacy policy page
- Create `website/terms.html` terms of service page
- Update footer links on all pages to point to `/privacy` and `/terms`

**Acceptance Criteria:**
- [ ] All pages have proper CSP header
- [ ] HSTS enabled with appropriate max-age
- [ ] Permissions-Policy restricts unnecessary APIs
- [ ] Privacy policy page accessible at `/privacy`
- [ ] Terms of service page accessible at `/terms`
- [ ] All footer "Privacy" and "Terms" links point to correct pages

**Test Cases:**
- Response headers include CSP, HSTS, X-Content-Type-Options, X-Frame-Options
- CSP allows Google Fonts, Material Symbols, inline styles/scripts
- CSP blocks loading resources from unauthorized origins
- Privacy and terms pages render correctly
- All 5 existing pages link to privacy and terms correctly

# E06 Implementation Plan: Website Deployment

> Generated from: docs/development-plan/E06-website-deployment.md
> Technical specs referenced: docs/INFRASTRUCTURE_ARCHITECTURE.md
> Date: 2026-03-19

## Pre-Implementation Checklist

- [x] Dependencies complete: E01 (Backend API) -- complete
- [x] Technical specs reviewed: INFRASTRUCTURE_ARCHITECTURE.md
- [x] Plan reviewed by Codex (round 1 -- 8 findings fixed)
- [ ] Plan approved by user

---

## Issue E06-001: Cloudflare Pages Deployment Config

### Context

The `website/` directory contains 5 HTML pages (index.html, check.html, result.html, challenge.html, join.html) plus a `variants/` directory. Cloudflare Pages deploys static files and supports `_redirects` and `_headers` configuration files. Internal links currently use `.html` extensions (e.g., `href="index.html"`, `href="check.html"`). These need clean URL rewrites and link updates.

### Tasks

1. **Create Cloudflare Pages redirects file**
   - File: `website/_redirects`
   - Action: Create
   - Details: Add rewrite rules for clean URLs. Cloudflare Pages `_redirects` uses the format `source destination [status]`. Use 200 status for rewrites (not redirects). Rules:
     ```
     /check    /check.html    200
     /result   /result.html   200
     /challenge /challenge.html 200
     /join     /join.html     200
     /privacy  /privacy.html  200
     /terms    /terms.html    200
     ```
     Do NOT add a rewrite for `/.well-known/*` -- Cloudflare Pages serves static files at their exact path by default, so `/.well-known/assetlinks.json` will serve without any rewrite rule.

2. **Create custom 404 error page**
   - File: `website/404.html`
   - Action: Create
   - Details: Create a simple 404 page matching the site's design (indigo gradient header, Quicksand font, shield icon). Include a link back to the homepage. Cloudflare Pages automatically serves `404.html` for unmatched routes.

3. **Update internal links to use clean URLs**
   - File: `website/index.html`
   - Action: Modify
   - Details: No internal links to other pages in index.html (footer links point to `#` for Privacy/Terms). No changes needed beyond footer links which will be updated in E06-004.

4. **Update internal links in check.html**
   - File: `website/check.html`
   - Action: Modify
   - Details: Change `href="index.html"` to `href="/"` in nav-back and nav-logo links. Change `href="challenge.html"` to `href="/challenge"` in footer. Update `window.location.origin + '/check.html'` references in share text to use `/check`.

5. **Update internal links in result.html**
   - File: `website/result.html`
   - Action: Modify
   - Details: Change `href="index.html"` to `href="/"` in nav-back and nav-logo. Change `href="check.html"` to `href="/check"` in footer. Change `href="challenge.html"` to `href="/challenge"` in footer. Update `goCheck()` function to redirect to `/check?url=...` instead of `check.html?url=...`. Update the `window.location.href` redirect to `check.html?url=...` in the inline checker to `/check?url=...`.

6. **Update internal links in challenge.html**
   - File: `website/challenge.html`
   - Action: Modify
   - Details: Change `href="index.html"` to `href="/"` in nav-back and nav-logo. Change `href="check.html"` to `href="/check"` in footer.

7. **Update internal links in join.html**
   - File: `website/join.html`
   - Action: Modify
   - Details: Change `href="index.html"` to `href="/"` in all instances. Change `href="check.html"` to `href="/check"` in footer. Change `href="challenge.html"` to `href="/challenge"` in footer.

### Tests
- `website/tests/test_redirects.sh` -- Bash script that validates `_redirects` file format: each line has 3 fields, source starts with `/`, status is `200`. Verify all 6 clean URL routes are present.
- `website/tests/test_deploy_smoke.sh` -- CI-compatible smoke test script that takes a base URL argument (e.g., a Pages preview URL or `http://localhost:8788`) and uses `curl` to verify: (1) `_headers` security headers are present in responses, (2) `_redirects` clean URLs return 200, (3) `/.well-known/assetlinks.json` returns valid JSON. Exits non-zero on any failure. Can be run in CI after `npx wrangler pages dev` or against a deployed preview URL.
- Manual test: after deployment, verify `/check` serves check.html content, `/result?domain=test` serves result.html, etc.

### Acceptance Criteria
- [x] Website deploys to Cloudflare Pages from `website/` directory
- [x] Clean URLs work: `/check`, `/result`, `/challenge`, `/join`
- [x] Security headers applied to all pages
- [x] Custom 404 page displayed for unknown routes
- [x] All internal navigation works correctly with clean URLs

---

## Issue E06-002: Wire check.html and result.html to Backend API

### Context

Currently `check.html` uses a hardcoded `SCAM_DB` JavaScript object for link checking. The real backend API is at `POST /api/check` (Cloudflare Workers, defined in `backend/workers/src/routes/check.ts`). The API accepts `{ "url": "..." }` or `{ "domain": "..." }` and returns `{ domain, verdict, reason, confidence, details }`. `result.html` also uses a hardcoded `REASONS` object. Both need to call the live API.

The Pages site (`safeanot.com`) and Workers API (`api.safeanot.com` or `*.workers.dev`) are different origins. The API base URL must be set to the actual Workers URL -- never empty string. Use a `const API_BASE` at the top of the script, set to `https://api.safeanot.com` (or the workers.dev URL during development). A custom domain for the Workers API should be configured in Cloudflare DNS (CNAME `api.safeanot.com` -> the workers.dev hostname).

### Tasks

1. **Replace hardcoded SCAM_DB in check.html with API fetch**
   - File: `website/check.html`
   - Action: Modify
   - Details: Remove the `SCAM_DB` constant object (lines 601-656). Replace `checkURL()` function to:
     - Extract domain from input using existing `extractDomain()` function (keep it)
     - Show loading spinner (existing CSS class `loading` + `loading-spinner`)
     - `fetch(API_BASE + '/api/check', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({ url: input }) })`
     - On success: parse JSON, call `showResult(response.domain, response.verdict, response.reason, null)`. Map `confidence` < 0.5 with verdict `suspicious` to show "suspicious" styling
     - On error (network/HTTP): show inline error card with message "Could not check this link. Please try again." with retry button
     - Keep `fillAndCheck()` working for example chips
     - Add `const API_BASE = 'https://api.safeanot.com'` at top of script. Pages and Workers are different origins, so this must be the full Workers API URL. During development, use the workers.dev URL instead.
     - **XSS prevention**: When rendering API response data (domain, reason) into the DOM, use `textContent` instead of `innerHTML`. Allowlist valid verdict values (`safe`, `dangerous`, `suspicious`, `unknown`) -- reject any verdict not in the allowlist.

2. **Add API error handling UI to check.html**
   - File: `website/check.html`
   - Action: Modify
   - Details: Add an `showError(message)` function that renders an error card in `#result` div. Style: white card with red-left border, error icon, message text, and "Try Again" button that re-calls `checkURL()`. Use existing CSS variables (`--red`, `--card`, `--radius`). Add inline CSS for `.result-card.error` style.

3. **Replace hardcoded REASONS in result.html with API fetch**
   - File: `website/result.html`
   - Action: Modify
   - Details: Remove the `REASONS` constant object (lines 431-442). Replace the inline script at bottom to:
     - On page load, read `domain` from URL params (keep existing code)
     - If no domain param: show a fallback message "No link specified" with link to `/check`
     - If domain present: show loading state in verdict display, then `fetch(API_BASE + '/api/check', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({ domain }) })`
     - On success: populate `#vLabel`, `#vDomain`, `#vReason`, `#vWarning` from API response. **XSS prevention**: Use `textContent` (not `innerHTML`) for all API-supplied values. Allowlist verdict values (`safe`, `dangerous`, `suspicious`, `unknown`).
     - On error: show "Could not load verdict" with link to check the URL manually at `/check`
     - Keep the `verdict` URL param as a hint for OG meta tags (existing pre-render script stays), but API response overrides the display
     - Add `const API_BASE = 'https://api.safeanot.com'` at top (must match check.html; never empty string)

4. **Add CORS consideration for API calls**
   - File: `backend/workers/src/middleware/cors.ts`
   - Action: Modify (if needed)
   - Details: Update the CORS middleware to allow requests from `safeanot.com`, `localhost`, **and** `*.safeanot-app.pages.dev` (Cloudflare Pages preview deployments). The existing CORS middleware only allows `safeanot.com` and `localhost`. Add a regex or pattern match for `*.safeanot-app.pages.dev` origins so that PR preview deployments can call the API. The `Access-Control-Allow-Origin` header must be set dynamically to the requesting origin (not `*`) when credentials are involved.

### Tests
- `website/tests/test_api_wiring.html` -- Manual test page that calls the API and logs response to console. Can be used during development to verify API connectivity.
- Manual test: on deployed site, enter `maybank-secure-update.xyz` in check page -> verify DANGEROUS verdict appears from API
- Manual test: load `/result?domain=shopee.com.my` -> verify SAFE verdict from API
- Manual test: with API unreachable -> verify error message appears with retry button

### Acceptance Criteria
- [x] check.html sends real API requests to backend
- [x] Verdict display shows API response (domain, verdict, reason)
- [x] Loading spinner shows during API call
- [x] Error state handled (network error, API error)
- [x] result.html fetches live verdict from API for `?domain=xxx`
- [x] Share URLs still work: `safeanot.com/result?domain=xxx`

---

## Issue E06-003: Digital Asset Links for Android App Verification

### Context

Android App Links require a `/.well-known/assetlinks.json` file served from the website domain to verify that the app is authorized to handle deep links. The E02 plan references this: "Add verified domain via Digital Asset Links (assetlinks.json in E06)". The app's package name is `com.safeanot.app`. The SHA-256 fingerprint of the signing key needs to be configured (placeholder for now, updated when release key is generated).

### Tasks

1. **Create .well-known directory and assetlinks.json**
   - File: `website/.well-known/assetlinks.json`
   - Action: Create
   - Details: Create the Digital Asset Links JSON file:
     ```json
     [{
       "relation": ["delegate_permission/common.handle_all_urls"],
       "target": {
         "namespace": "android_app",
         "package_name": "com.safeanot.app",
         "sha256_cert_fingerprints": [
           "PLACEHOLDER:UPDATE:WITH:RELEASE:SIGNING:KEY:SHA256:FINGERPRINT"
         ]
       }
     }]
     ```
     Add a comment in the epic/plan noting how to get the fingerprint:
     `keytool -list -v -keystore release.keystore | grep SHA256`
     For debug builds, also include the debug keystore fingerprint during development.

2. **Verify _redirects does not interfere with .well-known path**
   - File: `website/_redirects`
   - Action: Verify (no change needed)
   - Details: Cloudflare Pages serves static files before processing `_redirects`. Since `website/.well-known/assetlinks.json` is a physical file, it will be served directly. No rewrite rule needed. Verify this works after deployment.

3. **Add documentation for fingerprint update process**
   - File: `website/.well-known/README.md`
   - Action: Create
   - Details: Document the process to update the SHA-256 fingerprint:
     - How to extract fingerprint from debug keystore
     - How to extract fingerprint from release keystore / Play App Signing
     - How to test with `adb shell am start -a android.intent.action.VIEW -d "https://safeanot.com/result?domain=test"`
     - Link to Google's Digital Asset Links testing tool: `https://developers.google.com/digital-asset-links/tools/generator`

### Tests
- `website/tests/test_assetlinks.sh` -- Validate JSON syntax of assetlinks.json using `python3 -m json.tool`. Verify required fields are present (relation, target, namespace, package_name, sha256_cert_fingerprints).
- Manual test: after deployment, `curl https://safeanot.com/.well-known/assetlinks.json` returns valid JSON with Content-Type `application/json`.
- Manual test: Google Digital Asset Links API validator passes for the domain.

### Acceptance Criteria
- [x] `safeanot.com/.well-known/assetlinks.json` returns valid Digital Asset Links JSON
- [x] JSON contains correct package name and SHA-256 fingerprint placeholder
- [x] Deep links `safeanot.com/result?domain=xxx` verified by Android
- [x] File served with correct `Content-Type: application/json`

---

## Issue E06-004: Security Headers, CSP, and Legal Pages

### Context

The backend API already has security headers in `backend/workers/src/middleware/security-headers.ts` (X-Content-Type-Options, X-Frame-Options, X-XSS-Protection, Referrer-Policy, CSP). The website needs its own headers configured via Cloudflare Pages `_headers` file. The CSP must be more permissive than the API's `default-src 'none'` because the website loads Google Fonts, Material Symbols, and uses inline styles/scripts.

All 5 existing pages have footer links to `#` for Privacy and Terms. These need real pages.

### Tasks

1. **Create Cloudflare Pages headers file**
   - File: `website/_headers`
   - Action: Create
   - Details: Apply headers to all routes:
     ```
     /*
       X-Content-Type-Options: nosniff
       X-Frame-Options: DENY
       X-XSS-Protection: 1; mode=block
       Referrer-Policy: strict-origin-when-cross-origin
       Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()
       Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
       Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src https://fonts.gstatic.com; img-src 'self' data:; connect-src 'self' https://api.safeanot.com https://*.workers.dev; object-src 'none'; base-uri 'none'; frame-ancestors 'none'
     ```
     Notes on CSP:
     - `'unsafe-inline'` for script-src is needed because all pages use inline `<script>` tags and `onclick` handlers. Refactoring to external scripts is out of scope for this epic.
     - `'unsafe-inline'` for style-src is needed because all pages use inline `<style>` tags.
     - `connect-src` includes the Workers API domain for fetch calls from check.html and result.html.
     - `frame-ancestors 'none'` replaces X-Frame-Options for modern browsers.

2. **Create privacy policy page**
   - File: `website/privacy.html`
   - Action: Create
   - Details: Create a privacy policy page using the same design system (indigo gradient header, Quicksand font, nav with shield logo and back arrow). **DRY note**: Reuse the same CSS/styling pattern (inline `<style>` block) as existing pages (e.g., check.html) for visual consistency. Extracting shared CSS into a common stylesheet is deferred to a future epic. Content should cover:
     - What data is collected (anonymous link checks, no personal data stored)
     - Device-first architecture (95%+ checks happen locally)
     - No tracking, no ads, no third-party analytics
     - Scam reports: what is collected (domain, source app, region), anonymized after 90 days
     - Guardian feature: FCM tokens stored only for pairing, deleted when pairing removed
     - Third-party services: Cloudflare (hosting), Firebase (push notifications)
     - Contact information
     - Date: effective date placeholder

3. **Create terms of service page**
   - File: `website/terms.html`
   - Action: Create
   - Details: Create a terms page using the same design system. **DRY note**: Same as privacy.html -- reuse the existing CSS/styling pattern from other pages; shared CSS extraction is deferred. Content should cover:
     - Service description (scam protection advisory tool)
     - Disclaimer: verdicts are advisory, not guaranteed
     - Acceptable use (no abuse of API, no automated scraping)
     - Intellectual property
     - Limitation of liability
     - Changes to terms
     - Contact information

4. **Update footer links in index.html**
   - File: `website/index.html`
   - Action: Modify
   - Details: Change `<a href="#">Privacy</a>` to `<a href="/privacy">Privacy</a>`. Change `<a href="#">Terms</a>` to `<a href="/terms">Terms</a>`.

5. **Update footer links in check.html**
   - File: `website/check.html`
   - Action: Modify
   - Details: Change `<a href="#">Privacy</a>` to `<a href="/privacy">Privacy</a>`. Change `<a href="#">Terms</a>` to `<a href="/terms">Terms</a>`.

6. **Update footer links in result.html**
   - File: `website/result.html`
   - Action: Modify
   - Details: Change `<a href="#">Privacy</a>` to `<a href="/privacy">Privacy</a>`. Change `<a href="#">Terms</a>` to `<a href="/terms">Terms</a>`.

7. **Update footer links in challenge.html**
   - File: `website/challenge.html`
   - Action: Modify
   - Details: Change `<a href="#">Privacy</a>` to `<a href="/privacy">Privacy</a>`. Change `<a href="#">Terms</a>` to `<a href="/terms">Terms</a>`.

8. **Update footer links in join.html**
   - File: `website/join.html`
   - Action: Modify
   - Details: Change `<a href="#">Privacy</a>` to `<a href="/privacy">Privacy</a>`. Change `<a href="#">Terms</a>` to `<a href="/terms">Terms</a>`.

### Tests
- `website/tests/test_headers.sh` -- Parse `_headers` file and verify all required headers are present: CSP, HSTS, X-Content-Type-Options, X-Frame-Options, Referrer-Policy, Permissions-Policy.
- `website/tests/test_csp.sh` -- Verify CSP allows: fonts.googleapis.com (style-src), fonts.gstatic.com (font-src), inline scripts, inline styles, self, API connect-src. Verify CSP blocks: arbitrary external scripts, iframes.
- Manual test: load any page in browser, check response headers in DevTools Network tab.
- Manual test: verify Google Fonts and Material Symbols load correctly with CSP active.
- Manual test: verify `/privacy` and `/terms` pages render and are accessible from all page footers.

### Acceptance Criteria
- [x] All pages have proper CSP header
- [x] HSTS enabled with appropriate max-age
- [x] Permissions-Policy restricts unnecessary APIs
- [x] Privacy policy page accessible at `/privacy`
- [x] Terms of service page accessible at `/terms`
- [x] All footer "Privacy" and "Terms" links point to correct pages

---

## Implementation Order

Recommended sequence (respects internal dependencies):

1. **E06-001** -- Deployment config must come first. `_redirects` and `404.html` establish the URL structure that all other issues depend on. Clean URL links must be updated before API wiring references them.
2. **E06-004** -- Security headers and legal pages next. The `_headers` file must include `connect-src` for the API domain, which E06-002 depends on. Privacy/terms pages are independent and can ship early.
3. **E06-003** -- Digital Asset Links is independent of other issues and can be done in parallel with E06-004. The `.well-known` directory just needs to exist in the deployed site.
4. **E06-002** -- API wiring last. Depends on clean URLs from E06-001 (share links use `/check` and `/result`), CSP connect-src from E06-004, and CORS configuration. This is the most complex issue.

---

## Files Summary

| File | Action | Issues |
|------|--------|--------|
| `website/_redirects` | Create | E06-001 |
| `website/_headers` | Create | E06-004 |
| `website/404.html` | Create | E06-001 |
| `website/index.html` | Modify | E06-001, E06-004 |
| `website/check.html` | Modify | E06-001, E06-002, E06-004 |
| `website/result.html` | Modify | E06-001, E06-002, E06-004 |
| `website/challenge.html` | Modify | E06-001, E06-004 |
| `website/join.html` | Modify | E06-001, E06-004 |
| `website/.well-known/assetlinks.json` | Create | E06-003 |
| `website/.well-known/README.md` | Create | E06-003 |
| `website/privacy.html` | Create | E06-004 |
| `website/terms.html` | Create | E06-004 |
| `backend/workers/src/middleware/cors.ts` | Verify/Modify | E06-002 |
| `website/tests/test_redirects.sh` | Create | E06-001 |
| `website/tests/test_assetlinks.sh` | Create | E06-003 |
| `website/tests/test_headers.sh` | Create | E06-004 |
| `website/tests/test_csp.sh` | Create | E06-004 |
| `website/tests/test_deploy_smoke.sh` | Create | E06-001 |
| `website/tests/test_api_wiring.html` | Create | E06-002 |

---

## Codex Review Trace

| # | Severity | Finding | Fix Applied | Issue |
|---|----------|---------|-------------|-------|
| 1 | CRITICAL | `API_BASE = ''` won't work -- Pages and Workers are different origins | Set `API_BASE` to `https://api.safeanot.com`; added Workers custom domain note | E06-002 |
| 2 | WARNING | CORS doesn't support `*.pages.dev` previews | Added `*.safeanot-app.pages.dev` to CORS task description | E06-002 |
| 3 | WARNING | XSS risk -- `innerHTML` with API data | Added explicit `textContent` requirement and verdict allowlist to both check.html and result.html tasks | E06-002 |
| 4 | WARNING | Missing `Content-Type` header in result.html fetch | Added `headers: {'Content-Type': 'application/json'}` to result.html fetch task | E06-002 |
| 5 | WARNING | CSP missing `object-src` and `base-uri` | Added `object-src 'none'; base-uri 'none'` to CSP in `_headers` | E06-004 |
| 6 | INFO | Task references wrong file context (result.html redirect in check.html task) | Moved redirect reference from check.html task to result.html task in E06-001 | E06-001 |
| 7 | WARNING | Tests are mostly manual | Added `test_deploy_smoke.sh` CI script task using curl against preview/deployed URL | E06-001 |
| 8 | INFO | DRY risk with new standalone pages | Added DRY notes to privacy.html and terms.html tasks; shared CSS extraction deferred | E06-004 |

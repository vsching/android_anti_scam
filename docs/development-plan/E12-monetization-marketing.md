# E12: Monetization & Marketing Strategy

> **Phase:** 3 — Growth
> **Depends On:** E06 (Website), E11 (Pipeline)
> **Status:** Planning
> **Priority:** Low — document now, implement later

---

## Overview

Safe Anot? is free for end users. This epic documents monetization and marketing opportunities that can be implemented in later stages without compromising the core mission of scam protection.

---

## Revenue Streams

### Tier 1: Low Effort (implement anytime)

#### 1. Google AdSense on Web Pages
- **Where:** Banner ad below verdict card on `/check` and `/result` pages
- **Revenue model:** CPM (cost per 1000 impressions) + CPC (cost per click)
- **Expected:** RM 50–500/month depending on traffic
- **Implementation:** Add AdSense script tag + ad unit div to check.html and result.html
- **Risk:** Low — non-intrusive placement, doesn't affect UX

#### 2. "Check with Safe Anot?" Embed Widget
- **What:** Embeddable iframe/script that other websites can add (like a "Verified by Norton" badge but for link checking)
- **Revenue model:** Free tier = backlink to safeanot.com (SEO value), premium = white-label (no branding)
- **Implementation:** Create `/embed.js` script + `/widget` endpoint
- **Marketing value:** Every embed is a free advertisement

#### 3. Affiliate Links on Safe Verdicts
- **What:** After showing "SAFE" for a bank domain, show contextual CTA like "Open Maybank savings account →"
- **Revenue model:** Affiliate commission per signup (RM 10–50 per lead)
- **Partners:** Malaysian banks, insurance, e-wallets (Boost, BigPay, GrabPay)
- **Implementation:** Map allowlist categories to affiliate links, show on result page
- **Risk:** Must be clearly labeled "Sponsored" — trust is everything for a security app

### Tier 2: Medium Effort (post-MVP)

#### 4. Premium API Tier
- **Free tier:** 100 checks/day, web only
- **Pro tier (RM 99/month):** Unlimited API calls, bulk check endpoint, webhook alerts for new threats, custom allowlist
- **Enterprise tier (RM 999/month):** SLA, dedicated support, white-label, threat intelligence feed
- **Target customers:** Fintechs, banks, telcos, e-commerce platforms, cybersecurity teams
- **Implementation:** Add API key management, usage tracking, Stripe billing
- **Revenue potential:** 10 enterprise customers = RM 10K/month

#### 5. Sponsored Safety Tips / Security Awareness
- **What:** Cybersecurity companies pay to show educational tips on the result/alerts pages
- **Revenue model:** CPM or flat monthly sponsorship
- **Partners:** VPN providers (NordVPN, Surfshark), antivirus (Kaspersky), password managers (1Password)
- **Implementation:** Add "Safety tip" component below verdict, served from D1 with sponsor rotation
- **Risk:** Must be genuinely useful tips, not ads disguised as advice

#### 6. Corporate Training / Phishing Simulation
- **What:** Offer companies a phishing simulation service — send fake scam links to employees, track who clicks
- **Revenue model:** Per-employee per-year (RM 5–20/employee)
- **Target:** Malaysian corporates, banks, government agencies
- **Implementation:** New `/enterprise` section, simulation dashboard, reporting

### Tier 3: Higher Effort (long-term)

#### 7. B2B White-Label SDK
- **What:** Banks/telcos embed Safe Anot? link checking inside their own apps
- **Revenue model:** SaaS licensing (RM 5K–50K/month per client)
- **Target:** Maybank, CIMB, Maxis, Celcom, Grab, Shopee
- **Pitch:** "Protect your customers from scam links inside your app"
- **Implementation:** Android SDK, API integration guide, SLA dashboard

#### 8. Threat Intelligence Reports
- **What:** Sell anonymized scam trend reports (trending domains, attack patterns, geographic targeting)
- **Revenue model:** Subscription (RM 2K–10K/month) or per-report
- **Target:** BNM, MCMC, CyberSecurity Malaysia, PDRM, banks' fraud teams
- **Implementation:** Weekly automated report from pipeline data, PDF generation

#### 9. Government / NGO Grants
- **What:** Apply for digital safety grants from Malaysian/Singapore government
- **Target:** MDEC, MCMC, Cybersecurity Agency of Singapore, IMDA
- **Revenue model:** Grant funding (one-time or annual)
- **Implementation:** Proposal writing, impact metrics dashboard

---

## Marketing Channels

### Organic (Free)

| Channel | Strategy | Expected Impact |
|---------|----------|-----------------|
| **WhatsApp sharing** | "Share with family" button on every verdict | Viral — each scam check shared to 5+ family members |
| **SEO** | Rank for "is this link safe malaysia", "check scam link", "scam website checker" | 1000+ organic visits/month |
| **Social media** | Post weekly "Scam of the Week" on Facebook/TikTok/Instagram | Brand awareness, 25-40 age group |
| **Reddit / Lowyat** | Share on r/malaysia, Lowyat.net forum | Tech-savvy early adopters |
| **Word of mouth** | Parents tell other parents at school | Trust-based growth |

### Paid (Later Stage)

| Channel | Strategy | Budget |
|---------|----------|--------|
| **Facebook/Instagram ads** | Target 25-40 year olds in MY/SG with "Protect your parents" messaging | RM 500–2000/month |
| **Google Ads** | Bid on "scam checker", "is this link safe" keywords | RM 300–1000/month |
| **TikTok ads** | Short video showing scam → check → safe flow | RM 500/month |

---

## Marketing Touchpoints on Website

These can be added to the existing website without major changes:

### On `/check` page (before check)
- **Trust signals:** "500K+ scam domains checked" counter (live from /api/data/latest)
- **Social proof:** "Join 12,000 families protecting their parents"

### On `/result` page (after check)
- **Share CTA:** "Share this result with your family on WhatsApp" (already exists)
- **Upsell CTA:** "Download the app for real-time protection" → Play Store
- **Cross-sell:** If verdict is SAFE → "Want to check more? Try our browser extension" (future)
- **Affiliate:** If verdict is SAFE for a bank → "Open [bank] account →" (affiliate)
- **Newsletter:** "Get weekly scam alerts — enter your email" (future, when we have email infra)

### On `/` homepage
- **Live stats widget:** Pull from /api/data/latest and /api/health to show real-time domain count
- **Testimonials section:** User quotes about how Safe Anot? helped them
- **Press/media logos:** If featured by The Star, Malay Mail, CNA, etc.

---

## Implementation Order (when ready)

1. **AdSense** — immediate, zero-code revenue
2. **Live stats widget** — pull from existing API, adds credibility
3. **WhatsApp share optimization** — improve OG tags, share text for viral growth
4. **Premium API planning** — design tiers, billing integration
5. **Affiliate partnerships** — approach banks/e-wallets
6. **Enterprise outreach** — pitch B2B to banks/telcos

---

## Notes

- **Trust is the #1 asset** — never compromise user trust for revenue. All ads/sponsors must be clearly labeled.
- **Privacy-first** — never sell user data. Monetize the service, not the users.
- **Free tier must always exist** — the mission is scam protection for everyone.
- Revenue enables sustainability, not the other way around.

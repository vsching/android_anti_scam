# Safe Anot? — Infrastructure Architecture

> **Date:** 2026-03-16
> **Status:** Pre-Development
> **Core Principle:** Scale to zero when idle, scale to millions when viral. Never go down during a spike.

---

## Architecture Overview

```
                         ┌──────────────────────────┐
                         │   Cloudflare Pages        │  ← Website (FREE)
                         │   Static HTML/CSS/JS      │     Global CDN, infinite scale
                         └──────────────────────────┘

      App / Website
           │
           ▼
┌──────────────────────────┐     ┌───────────────────┐
│   Cloudflare Workers      │────▶│  Cloudflare KV     │  ← Allowlist + new discoveries
│   (Edge API — 300+ cities)│     │  (Key-Value store)  │     NOT bulk domain store
│                           │     └───────────────────┘     Sub-ms lookup
│   /api/check   — lookup   │
│   /api/report  — submit   │     ┌───────────────────┐
│   /api/alerts  — feed     │────▶│  Cloudflare D1     │  ← SQLite at edge
│   /api/guardian — pairing  │     │  (Edge database)    │     Reports, alerts, pairings
│   /api/score   — share    │     └───────────────────┘
└──────────────────────────┘

           │                      ┌───────────────────┐
           └─────────────────────▶│  Firebase FCM       │  ← Push notifications (FREE)
                                  └───────────────────┘

┌──────────────────────────┐
│   DigitalOcean Droplet    │  ← Data pipeline (NOT user-facing)
│   ($6/mo, 1 small VPS)   │     Scraping, processing, feed updates
│                           │
│   Scrapling cron jobs:    │     ┌───────────────────┐
│   - SemakMule scraper     │────▶│  Cloudflare R2     │  Push bulk DB artifacts
│   - Scam news scraper     │     │  (Object storage)   │  (SQLite, Bloom, delta)
│   - Open-source feeds     │     └───────────────────┘
│   - Domain monitor        │     + KV: allowlist + new discoveries only
└──────────────────────────┘
```

## Design Principles

1. **Users never hit DigitalOcean directly.** All user-facing traffic goes through Cloudflare edge. If the droplet goes down, the app still works (cached data on device + R2).
2. **Device-first.** 95%+ of link checks happen locally on the device (SQLite + Bloom filter). The API handles only the remaining ~5% unknowns.
3. **Scale to zero.** Free tier covers 100K requests/day. No cost when idle.
4. **Scale to millions.** Cloudflare Workers auto-scale across 300+ cities. No configuration change needed.
5. **Separate data plane from serving plane.** DigitalOcean processes data in the background. Cloudflare serves it to users. They're independent.

---

## Component Details

### 1. Cloudflare Workers (Edge API)

**What:** Serverless JavaScript/TypeScript functions running at Cloudflare's edge (300+ data centers globally).

**Why:** Sub-50ms response times globally. Auto-scales from 0 to billions of requests. No server to manage.

**Endpoints:**

| Endpoint | Method | Purpose | Data Source | Cache Strategy |
|----------|--------|---------|-------------|----------------|
| `/api/check` | POST | Check if a URL/domain is safe | KV lookup → heuristic fallback (D1 NOT used) | Cache API → KV → heuristic |
| `/api/check/batch` | POST | Check multiple URLs at once (burst mode) | KV batch lookup → heuristic fallback | Same as above |
| `/api/report` | POST | Submit a scam report from user | Write to D1 | No cache needed |
| `/api/alerts` | GET | Get trending scam alerts | D1 query, cached in KV | KV cache, 15-min TTL |
| `/api/alerts/latest` | GET | Get latest alert (for push notification content) | KV | 5-min TTL |
| `/api/guardian/pair` | POST | Create guardian pairing | D1 | No cache |
| `/api/guardian/heartbeat` | POST | Parent's app sends security status | D1 + trigger FCM if needed | No cache |
| `/api/guardian/status` | GET | Guardian checks parent's status | D1, cached in KV | KV cache, 5-min TTL |
| `/api/score/share` | POST | Generate shareable score card data | D1 | KV cache by score ID |
| `/api/challenge` | GET | Get quiz questions | KV (static data) | Long TTL |

**Request flow for link checker (hot path):**

```
User pastes URL → App checks local DB first (95% resolved here, no API call)
  → Not found locally → App sends POST /api/check { url: "maybank-secure-update.xyz" }
    → Cloudflare Worker (nearest edge city to user)
      → Check Workers Cache API (per-city cache)
        → HIT: return cached verdict. Cost: $0.
      → Cache miss → KV.get("verdict:maybank-secure-update.xyz")
        → HIT: return verdict, cache in Cache API. Cost: 1 KV read.
        → MISS: run heuristic engine (typosquatting, TLD check, pattern matching)
          → Return heuristic verdict, cache in Cache API + KV with 1-hour TTL
          → Queue domain for async pipeline review
    → D1 is NOT queried in this path.
  → Total response time: < 20ms (Cache API hit), < 50ms (KV hit), < 100ms (heuristic)
```

**Pricing:**

| Tier | Requests/day | Monthly Cost |
|------|-------------|--------------|
| Free | 100,000 | $0 |
| Paid ($5/mo base) | 10,000,000 | $5 |
| High traffic | 50,000,000 | ~$45 |
| Viral spike | 100,000,000+ | ~$100-200 |

### 2. Cloudflare KV (Allowlist + New Discoveries Only)

**What:** Global key-value store replicated to all Cloudflare edge locations. Optimized for read-heavy workloads.

**Why:** KV stores ONLY the allowlist of safe domains and newly discovered scam domains (from user reports/heuristics). The bulk 500K+ domain database lives in R2 and is downloaded to devices — NOT stored in KV.

**Important:** KV is NOT the bulk domain store. The bulk scam DB lives in R2 (downloaded to device SQLite). KV handles only:
- ~500 allowlisted safe domains (banks, gov, e-commerce)
- ~1K-10K newly discovered scam domains (from reports + heuristic confirmations)
- Cached alert metadata (15-min TTL)
- Cached R2 manifest metadata (1-hour TTL)

**Data structure:**

```
Key: "safe:shopee.com.my"
Value: {
  "verdict": "safe",
  "reason": "Official Shopee Malaysia domain",
  "source": "allowlist",
  "verified": true
}

Key: "discovery:new-scam-site.xyz"
Value: {
  "verdict": "dangerous",
  "reason": "Confirmed scam via user reports",
  "reportCount": 48,
  "discoveredAt": "2026-03-15"
}
```

**Size estimate:**
- ~500 allowlisted safe domains × ~100 bytes = ~50KB
- ~5K-10K discovered scam domains × ~200 bytes = ~2MB
- Total: well under 1MB for most of KV's life

**Pricing:**

| Tier | Reads/day | Writes/day | Monthly Cost |
|------|-----------|------------|--------------|
| Free | 100,000 | 1,000 | $0 |
| Paid | Up to 500K (only unknowns that miss local + cache) | ~100-500 (new discoveries) | $0-5/mo |

### 3. Cloudflare D1 (Edge Database)

**What:** SQLite database running at Cloudflare's edge. SQL-compatible, serverless.

**Why:** For data that needs querying (user reports, alert feeds, guardian pairings) but doesn't need the full scale of PostgreSQL.

**Tables:**

```sql
-- User-submitted scam reports
CREATE TABLE reports (
  id TEXT PRIMARY KEY,
  domain TEXT,
  phone_number TEXT,
  message_text TEXT,
  source_app TEXT,       -- whatsapp, telegram, sms, browser
  reporter_region TEXT,  -- MY, SG
  reporter_state TEXT,   -- Selangor, KL, Johor, etc.
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  verified BOOLEAN DEFAULT FALSE,
  report_count INTEGER DEFAULT 1
);

-- Scam alerts (curated by pipeline)
CREATE TABLE alerts (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  description TEXT,
  scam_type TEXT,        -- phishing, investment, parcel, tax, love
  severity TEXT,         -- high, medium, low
  region TEXT,           -- MY, SG, both
  state TEXT,            -- specific state/area
  report_count INTEGER DEFAULT 0,
  source_url TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Family Guardian pairings
CREATE TABLE guardian_pairs (
  id TEXT PRIMARY KEY,
  pair_code TEXT UNIQUE NOT NULL,
  parent_device_token TEXT,  -- FCM token
  guardian_device_token TEXT, -- FCM token
  parent_score INTEGER DEFAULT 0,
  parent_last_heartbeat DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  active BOOLEAN DEFAULT TRUE
);

-- Shared score cards (for viral sharing)
CREATE TABLE shared_scores (
  id TEXT PRIMARY KEY,
  score_percent INTEGER,
  secured_count INTEGER,
  total_count INTEGER,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Suspicious domains queued for pipeline review (async discovery)
CREATE TABLE pending_discoveries (
  id TEXT PRIMARY KEY,
  domain TEXT NOT NULL,
  verdict TEXT,            -- heuristic verdict (suspicious/unknown)
  reason TEXT,
  source TEXT,             -- 'heuristic', 'user_report'
  check_count INTEGER DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  processed BOOLEAN DEFAULT FALSE,
  processed_at DATETIME
);
CREATE INDEX idx_pending_unprocessed ON pending_discoveries(processed, created_at);
```

**Pricing:**

| Tier | Reads/day | Writes/day | Storage | Monthly Cost |
|------|-----------|------------|---------|--------------|
| Free | 5,000,000 | 100,000 | 5GB | $0 |
| Paid | Unlimited | Unlimited | 5GB+ | $5/mo |

### 4. Cloudflare Pages (Website Hosting)

**What:** Static site hosting with global CDN.

**Why:** Free, instant deployment from Git, unlimited bandwidth, global CDN.

**Hosts:**
- `safeanot.com` — landing page
- `safeanot.com/check` — link checker (web version)
- `safeanot.com/challenge` — spot the fake quiz
- `safeanot.com/result?domain=xxx` — shared verdict page
- `safeanot.com/join?ref=xxx` — referral landing

**Pricing:** $0 (free tier: unlimited sites, unlimited bandwidth, unlimited requests)

### 5. Firebase (Push Notifications)

**What:** Firebase Cloud Messaging (FCM) for push notifications to Android.

**Why:** Free, reliable, built for this exact use case.

**Used for:**
- Guardian alerts (parent's security changed → notify guardian)
- Weekly audit reminders
- "Scam of the Week" push notifications
- Score change alerts

**Pricing:** $0 (FCM is free for unlimited messages)

### 6. DigitalOcean Droplet (Data Pipeline)

**What:** 1 small VPS ($6/mo) running scheduled Python scripts.

**Why:** Scraping, data processing, and feed merging don't need edge computing. A cheap always-on server running cron jobs is the right tool.

**NOT user-facing.** If this server goes down, the app and API continue working with cached data.

**Cron jobs:**

| Schedule | Job | What It Does |
|----------|-----|-------------|
| Daily 3am MYT | `fetch_scam_feeds.py` | Download latest from Phishing.Database, Scam-Blocklist, Phishing Army, OpenPhish |
| Daily 3:30am MYT | `scrape_semakmule.py` | Scrapling: scrape SemakMule for new flagged numbers |
| Daily 4am MYT | `scrape_scam_news.py` | Scrapling: scrape MY/SG news sites for scam articles |
| Daily 4:30am MYT | `process_and_merge.py` | Deduplicate, merge all sources, generate verdicts |
| Daily 5am MYT | `push_to_cloudflare.py` | Bulk upload processed data to Cloudflare KV via API |
| Daily 5:30am MYT | `update_alerts.py` | Insert new scam alerts into D1 |
| Hourly | `verify_active_scams.py` | Check if reported scam URLs are still active |
| Weekly Mon 9am | `generate_weekly_report.py` | Aggregate stats for "Scam of the Week" push notification |

**Tech stack on droplet:**
- Python 3.12
- Scrapling (web scraping)
- httpx (async HTTP client for Cloudflare API)
- Schedule / cron for job scheduling
- SQLite (local staging before pushing to Cloudflare)

**Pricing:** $6/mo (1GB RAM, 1 vCPU — more than enough for daily scripts)

---

## Caching Strategy (Revised — 3-Layer Device-First)

> **Core principle:** Move 95%+ of domain checks to the device. The API handles only what the device can't do alone. This reduces cost by 95% at viral scale.

### Why Device-First?

The scam domain database changes once per day. Paying for a KV read on every single check is wasteful when the same data can live on the user's device.

**Without device caching:** 50M checks/day = ~1.5B KV reads/month = $750+/mo
**With device caching:** 50M checks/day × 5% API = ~2.5M API calls/day = ~75M/month = ~$39/mo

### Data Storage Roles (Corrected)

| Data | Storage | Why |
|------|---------|-----|
| Bulk scam DB (500K+ domains) | **R2** (versioned files, free egress) + **device SQLite** | Apps download directly from R2. Free. |
| Hot lookups for unknown domains | **KV** (small number of new discoveries) | Only domains NOT in the bulk DB |
| Reports, alerts, guardian pairings | **D1** (relational data) | Low-volume writes, read-replicated |
| Heuristic results (ephemeral) | **Workers Cache API** (per edge city) | Free, auto-evicts |

**Important:** D1 is NOT in the domain check hot path. KV is NOT the bulk domain store. R2 hosts the bulk database, devices download it.

### The 3 Layers

#### Layer 1: On-Device (FREE, instant, handles 95%+ of checks)

```
App opens (once daily, on WiFi):
  → GET safeanot-data.r2.dev/domains/latest-delta.json (~50-200KB)
  → Merge delta into local Room/SQLite database
  → App now has 500K+ domains locally
  → Also downloads bloom filter (~100KB) for fast negative lookups

User checks a link:
  → Room query: SELECT verdict FROM domains WHERE domain = ?
  → Found? Return instantly. DONE. Cost: $0.
  → Not found? Check bloom filter...
    → Bloom says "definitely not in scam DB"? Return NOT_KNOWN_SCAM (not "SAFE" — absence of evidence is not evidence of safety). Show user: "Not in our database. Proceed with caution." Cost: $0.
    → Bloom says "maybe in scam DB"? Proceed to Layer 2 for authoritative check.
```

**Components:**
- Bundled SQLite in APK (snapshot at build time, ~10-15MB — needs benchmarking)
- Daily delta download from R2 (only new/changed domains since last sync)
- Bloom filter for fast negative lookups (~100KB for 500K domains, <1% false positive)
- Local result cache (recent checks cached in memory/Room)

**R2 cost:**
- Storage: free (first 10GB)
- Egress: **$0** (R2 has zero egress fees — Cloudflare's key advantage)
- 100K users downloading 200KB daily = ~20GB/month egress = still $0

#### Layer 2: Cloudflare Workers Cache API (FREE, handles viral spikes)

```
Domain not found locally → App calls API:
  POST /api/check { domain: "new-scam-site.xyz" }

Worker receives request:
  → Check Workers Cache API (per edge city cache)
    → HIT? Return cached verdict. No KV read. Cost: $0.
    → MISS? Proceed to Layer 3.
  → After Layer 3 returns, cache result in Cache API (TTL: 1 hour)
```

**Why this matters for viral spikes:**
When a fake Maybank link goes viral, 500K people in KL check the same domain.
- First request: KV read (cost: $0.0000005)
- Next 499,999 requests from KL edge: Cache API hit (cost: $0)
- Same domain from Singapore edge: 1 more KV read, then cached
- Total KV reads for 500K checks of the same domain: ~300 (one per edge city)

**Workers Cache API pricing:** Free (included in Workers, no extra charge).

#### Layer 3: KV Lookup + Heuristic Engine (rare, for true unknowns only)

```
Cache miss → Worker does:
  → KV.get("verdict:" + domain)
    → HIT? Return verdict, cache in Layer 2.
    → MISS? Run heuristic engine:
      → Typosquatting check (Levenshtein distance from bank domains)
      → Suspicious TLD check (.xyz, .top, .buzz, .click, .loan)
      → Bank-name pattern matching
      → URL shortener detection
    → Return heuristic verdict, cache in Layer 2 (TTL: 1 hour)
    → If suspicious: queue for async review by pipeline
```

**KV contains only:**
- Newly discovered scam domains (from user reports, not yet in bulk DB)
- Allowlisted safe domains (bank/gov official domains)
- ~5K-10K entries total, NOT the full 500K database

**KV cost with this strategy:**
- Reads: ~50K-500K/month (only true unknowns that miss local + cache)
- Writes: ~1K-5K/month (new discoveries from reports/heuristics)
- Cost: well within free tier for most traffic levels

### Miss Coalescing (Prevents Thundering Herd)

When a new unknown domain goes viral (not in local DB, not in KV):
- First request triggers heuristic evaluation
- Worker uses Cloudflare Cache API to store a "pending" result
- Subsequent requests for the same domain within the next second get the cached pending/result
- Prevents thousands of duplicate heuristic evaluations for the same domain

### KV Update Strategy (Delta, Not Full Rewrite)

**Problem (from Codex review):** Full daily rewrite of 500K keys = 15M writes/month = expensive.

**Solution:** KV only stores new discoveries. The bulk database lives in R2.

| Action | Frequency | KV Writes |
|--------|-----------|-----------|
| New scam domain confirmed from report | Per event | 1 write |
| Heuristic result cached | Per new unknown domain | 1 write |
| Pipeline pushes newly discovered domains | Daily | ~100-500 writes |
| **Total writes/month** | | **~5K-15K** (well within free tier) |

### KV Consistency Note

KV is eventually consistent. Updates can take 60+ seconds to propagate globally. For our use case this is acceptable because:
- The bulk database is on-device (not affected by KV propagation)
- KV only stores newly discovered domains (real-time isn't critical for discoveries)
- The Cache API layer adds its own TTL on top
- Worst case: a newly reported domain appears "unknown" for 60 seconds before showing as "dangerous" — acceptable for a non-blocking advisory tool

For critical urgent blocks (e.g., mass-reported active phishing campaign), we can:
- Push a high-priority alert via FCM directly to devices
- Force an immediate delta sync from R2
- Both bypass KV entirely

### Bundled Database Size (Needs Benchmarking)

| Content | Estimated Size |
|---------|---------------|
| 500K domains (avg 25 chars each) | ~12.5MB raw |
| With verdict, source, reason fields | ~25-30MB raw |
| SQLite with indexes | ~30-40MB |
| Compressed (gzip) | ~8-12MB |
| Delta file (daily changes, ~1% churn) | ~100-300KB |

**Action item:** Benchmark actual size before shipping. If >15MB compressed, consider:
- Ship top 100K most common scam domains in APK (~3MB)
- Download full database on first launch from R2
- Daily deltas after that

### Cache Hit Rate Estimates (Revised)

| Check Type | Layer Hit | % of All Checks | API Cost |
|------------|-----------|-----------------|----------|
| Known scam domain (in local DB) | Layer 1 (device) | ~70% | $0 |
| Known safe domain (in local allowlist) | Layer 1 (device) | ~15% | $0 |
| Bloom filter negative (not in scam DB — returns NOT_KNOWN_SCAM, not "safe") | Layer 1 (device) | ~10% | $0 |
| Hot unknown domain (viral, cached at edge) | Layer 2 (Cache API) | ~4% | $0 |
| True unknown (KV lookup + heuristic) | Layer 3 (KV) | ~1% | ~$0.0000005/read |

**Result: 95%+ of checks are free. Only ~1% hit KV.**

---

## Cost Summary (Corrected)

**Pricing basis (current Cloudflare pricing):**
- Workers Paid: $5/mo base, includes 10M requests/month, then $0.30/million overage
- KV: includes 10M reads/month, $0.50/million overage; 1M writes/month, $5/million overage
- D1: includes 25B rows read/month, 50M rows written/month on paid plan
- R2: 10GB storage free, egress always free
- Pages: free
- FCM: free

**API call estimate with 3-layer caching:**
- 95% of domain checks handled on-device (never hit API)
- Remaining 5% hit API: 4% served from Cache API (free), 1% hit KV
- Other API calls: alerts feed, reports, guardian (low volume)

### Idle (pre-launch, testing)

| Component | Monthly Cost |
|-----------|-------------|
| Cloudflare Workers | $0 (free tier) |
| Cloudflare KV | $0 (free tier) |
| Cloudflare D1 | $0 (free tier) |
| Cloudflare R2 | $0 (free tier) |
| Cloudflare Pages | $0 |
| Firebase FCM | $0 |
| DigitalOcean droplet | $6 |
| Domain (safeanot.com) | ~$1 |
| **Total** | **~$7/mo** |

### Moderate traffic (10K DAU, 50K checks/day)

- Device handles ~47.5K checks locally
- ~2.5K API calls/day = ~75K/month
- Well within free tier for everything

| Component | Monthly Cost |
|-----------|-------------|
| All Cloudflare services | $0 (free tier) |
| DigitalOcean droplet | $6 |
| **Total** | **~$7/mo** |

### High traffic (100K DAU, 2M checks/day)

- Device handles ~1.9M checks locally
- ~100K API calls/day = ~3M/month
- KV reads: ~20K/day = ~600K/month (within free tier)

| Component | Monthly Cost |
|-----------|-------------|
| Cloudflare Workers | $5 (base paid plan) |
| Cloudflare KV | $0 (within included reads) |
| Cloudflare D1 | $0 (within included reads/writes) |
| Cloudflare R2 | $0 (free egress) |
| Cloudflare Pages | $0 |
| Firebase FCM | $0 |
| DigitalOcean droplet | $6 |
| **Total** | **~$12/mo** |

### Viral spike (1M+ DAU, 50M checks/day)

- Device handles ~47.5M checks locally
- ~2.5M API calls/day = ~75M/month
- KV reads: ~500K/day = ~15M/month
- Cache API absorbs viral hot domains

| Component | Monthly Cost |
|-----------|-------------|
| Cloudflare Workers | $5 + (65M × $0.30/M) = ~$25 |
| Cloudflare KV | (5M overage reads × $0.50/M) = ~$3 |
| Cloudflare D1 | ~$5 (reports + guardian writes) |
| Cloudflare R2 | $0 |
| Cloudflare Pages | $0 |
| Firebase FCM | $0 |
| DigitalOcean droplet | $6 |
| **Total** | **~$39/mo** |

### Extreme viral (10M+ DAU, 200M checks/day)

- Device handles ~190M checks locally
- ~10M API calls/day = ~300M/month
- KV reads: ~2M/day = ~60M/month
- Cache API critical for absorbing repeated hot domains

| Component | Monthly Cost |
|-----------|-------------|
| Cloudflare Workers | $5 + (290M × $0.30/M) = ~$92 |
| Cloudflare KV | (50M overage reads × $0.50/M) = ~$25 |
| Cloudflare D1 | ~$10 |
| Cloudflare R2 | $0 |
| Cloudflare Pages | $0 |
| Firebase FCM | $0 |
| DigitalOcean droplet | $12 |
| **Total** | **~$144/mo** |

### Cost Comparison: With vs Without Device-First Caching

| Traffic | Without Caching (all API) | With 3-Layer Caching | Savings |
|---------|--------------------------|---------------------|---------|
| 50K checks/day | ~$7/mo | ~$7/mo | Same (free tier) |
| 2M checks/day | ~$60-80/mo | ~$12/mo | **85% less** |
| 50M checks/day | ~$500-800/mo | ~$39/mo | **95% less** |
| 200M checks/day | ~$2000+/mo | ~$144/mo | **93% less** |

---

## Security Considerations (Corrected)

### PII and Data Governance

**The following data IS personal data and must be treated accordingly:**

| Data | Where Stored | Classification | Retention |
|------|-------------|----------------|-----------|
| Phone numbers (in scam reports) | D1 | PII | 90 days, then hash/anonymize |
| Message text (in scam reports) | D1 | PII | 90 days, then delete |
| FCM device tokens (guardian) | D1 | PII | Until pairing deleted |
| Checked URLs (if logged) | Not stored by default | Potentially sensitive | Do not log checked URLs |
| IP addresses | Cloudflare logs | PII | Cloudflare default retention (72h) |

**Policies required:**
- Data retention: auto-delete reports older than 90 days, anonymize for aggregate stats
- Data minimization: only collect what's needed, don't log checked URLs
- Right to deletion:
  - Guardian pairings: user can delete via pair-code + device token (proof of ownership)
  - Scam reports: anonymous reports cannot be attributed to a user, so no deletion is needed (no user identity attached). Reports are auto-deleted after 90 days.
  - If we add user accounts in the future, implement full deletion API
- Encryption at rest: D1 encrypts at rest by default; ensure FCM tokens are not exposed in logs

### Abuse Prevention

| Attack | Mitigation |
|--------|------------|
| API spam (millions of fake checks) | Cloudflare rate limiting: 100 req/min per IP for `/api/check`, 10 req/min for `/api/report` |
| Report flooding (fake scam reports) | Rate limit + require minimum app usage before reporting |
| Guardian pair-code brute force | 6-digit code + 5 attempt limit + 15-min lockout + code expires in 10 minutes |
| DDoS on API | Cloudflare DDoS protection (free, automatic) |
| Web endpoint abuse | Cloudflare Turnstile (free CAPTCHA) on web link checker |
| Malicious data in reports | Input validation + sanitization in Worker. No raw HTML stored. |

### Authentication & Authorization

| Endpoint | Auth Required | Method |
|----------|--------------|--------|
| `/api/check` | None (public, rate-limited) | Rate limit by IP |
| `/api/report` | App attestation token (optional v1, required v2) | Play Integrity or simple API key |
| `/api/guardian/*` | Pair-code + device token | Token-based, no user accounts |
| `/api/alerts` | None (public, cacheable) | Rate limit by IP |
| Pipeline → KV/R2 publish | API token (least-privilege) | Cloudflare API token with write-only scope |

### Operational Security

- Secret rotation: Cloudflare Worker secrets + Firebase API keys rotated quarterly
- Audit logging: Log all pipeline publishes, guardian pairings, and admin actions
- Incident response: If scam database is compromised, rollback to previous R2 version
- Scraping legal: Review TOS for all scraped sources. SemakMule is a public government portal.

---

## Observability

| Metric | Source | Alert Threshold |
|--------|--------|-----------------|
| API p50/p95/p99 latency | Cloudflare Analytics | p99 > 500ms |
| KV hit ratio | Worker logging | < 90% (indicates caching problem) |
| Device-to-API ratio | Worker logging | > 10% API (indicates local DB issue) |
| Unknown domain rate | Worker logging | > 5% (indicates DB is stale) |
| Pipeline publish lag | GitHub Actions / DO monitoring | > 6 hours since last publish |
| D1 write latency | Cloudflare D1 analytics | p99 > 200ms |
| FCM failure rate | Firebase console | > 1% failure |
| R2 download errors | Cloudflare R2 analytics | > 0.1% error rate |
| Report submission rate | D1 query | Spike detection (possible spam) |

---

## Deployment Flow

```
Developer pushes to GitHub
  │
  ├── Cloudflare Pages (auto-deploy website from /website directory)
  │
  ├── Wrangler CLI (deploy Workers from /backend/workers)
  │
  └── GitHub Actions (deploy pipeline scripts to DO droplet via SSH)

Data pipeline (daily):
  │
  ├── GitHub Actions cron → triggers DO droplet scripts
  │
  ├── DO droplet: fetch feeds, scrape, process, merge
  │
  ├── Upload versioned scam DB to R2 (full + delta)
  │
  ├── Push new discoveries to KV (delta only, ~100-500 keys)
  │
  └── Update alerts in D1
```

**CI/CD:** GitHub Actions for all deployments and pipeline triggers.

**Pipeline resilience:**
- Each dataset version stored in R2 with timestamp prefix
- If pipeline fails, previous version remains active
- Idempotent publish: re-running the pipeline produces the same result
- GitHub Actions provides logging, retry, and alerting

---

## Migration Path

| When | Migrate To | Trigger |
|------|-----------|---------|
| Guardian writes become chatty | Cloudflare Durable Objects or Supabase | > 100K guardian heartbeats/day |
| Need complex domain queries | Add Supabase PostgreSQL | When heuristic engine needs SQL-level analysis |
| Need real-time features | Cloudflare Durable Objects or Supabase Realtime | Real-time guardian status |
| Need ML/AI processing | Cloudflare AI or dedicated GPU instance | AI-based scam detection |
| Need multi-region write DB | PlanetScale or CockroachDB | Global write requirements |

**Note:** This architecture handles millions of users before any migration is needed, but the specific threshold depends on usage patterns (checks per user per day, guardian adoption rate, report volume). Monitor observability metrics to anticipate migration needs.

# E01B: Pipeline Deployment + Initial Data Seed

> **Phase:** 1 (MVP — required for app to function)
> **Priority:** P0 — App ships with empty database without this
> **Depends On:** E01 (Backend API + Scam Database)
> **Estimated Effort:** 2-3 days

---

## Overview

Deploy the Python pipeline to actually fetch scam domains from open-source feeds, generate artifacts (SQLite, Bloom filter, delta JSON, manifest), and upload to Cloudflare R2. Set up daily cron via GitHub Actions (no DigitalOcean needed for MVP). Without this, the link checker has no data.

## What Exists

- Pipeline code: `backend/pipeline/seed_database.py` + all source modules (tested, 123 tests passing)
- Workers API: deployed via GitHub Actions on push to main
- Cloudflare bindings: KV, R2, D1 declared in `wrangler.toml` (placeholder IDs)

## What's Missing

- Real Cloudflare resource IDs (KV namespace, R2 bucket, D1 database)
- Pipeline has never run against real feeds
- R2 bucket is empty — no scam database artifacts
- No scheduled pipeline runs (daily cron)
- No monitoring for pipeline health

---

## Issues

### E01B-001: Cloudflare Resource Provisioning

Create the actual Cloudflare resources and update configuration.

**Acceptance Criteria:**
- KV namespace `safeanot-verdicts` created via Wrangler CLI
- R2 bucket `safeanot-data` created via Wrangler CLI
- D1 database `safeanot-db` created via Wrangler CLI with migrations applied
- `wrangler.toml` updated with real resource IDs
- `.dev.vars` populated with real credentials for local dev
- Rate limiting rulesets provisioned via `/infra/rate-limiting.sh`
- Worker deployed and responding at production URL

**Manual Steps:**
```bash
# Create resources
wrangler kv:namespace create VERDICTS
wrangler r2 bucket create safeanot-data
wrangler d1 create safeanot-db
wrangler d1 migrations apply safeanot-db

# Update wrangler.toml with returned IDs
# Set GitHub secrets: CLOUDFLARE_API_TOKEN, CLOUDFLARE_ACCOUNT_ID

# Deploy
wrangler deploy

# Provision rate limiting
CF_ZONE_ID=xxx CF_API_TOKEN=xxx bash infra/rate-limiting.sh
```

---

### E01B-002: Initial Pipeline Run + Data Seed

Run the pipeline for the first time to populate R2 with scam domain artifacts.

**Acceptance Criteria:**
- Pipeline fetches from 3+ sources (Phishing.Database, Scam-Blocklist, Phishing Army)
- Generates all artifacts: `domains-full-YYYY-MM-DD.json`, `.sqlite`, `bloom-YYYY-MM-DD.bin`, `manifest-YYYY-MM-DD.json`
- Uploads all artifacts to R2 bucket `safeanot-data`
- Updates `latest.json` in R2
- Seeds KV with allowlisted safe domains (Malaysian banks, SG banks, gov, e-commerce)
- R2 contains 100K+ scam domains after initial seed
- `/api/data/latest` returns valid metadata
- `/api/data/full` streams the SQLite file
- `/api/data/bloom` streams the Bloom filter
- `/api/check` returns verdicts for known scam domains

**Manual Steps:**
```bash
cd backend/pipeline
pip install -e ".[dev]"

# Set env vars
export CLOUDFLARE_API_TOKEN=xxx
export CLOUDFLARE_ACCOUNT_ID=xxx
export R2_BUCKET_NAME=safeanot-data
export KV_NAMESPACE_ID=xxx

# Run pipeline
python seed_database.py

# Verify
curl https://safeanot-api.workers.dev/api/data/latest
curl -X POST https://safeanot-api.workers.dev/api/check \
  -H "Content-Type: application/json" \
  -d '{"domain": "maybank-secure-update.xyz"}'
```

---

### E01B-003: Daily Pipeline Cron (GitHub Actions)

Set up automated daily pipeline runs via GitHub Actions (no DigitalOcean for MVP).

**Acceptance Criteria:**
- GitHub Actions workflow runs daily at 3am MYT (7pm UTC previous day)
- Workflow: checkout → install Python deps → run pipeline → verify artifacts
- Pipeline credentials stored as GitHub secrets
- Workflow logs pipeline output (domain count, artifact sizes, sources fetched)
- Manual trigger available via `workflow_dispatch`
- Failure notification via GitHub Actions (email to repo owner)
- Pipeline is idempotent — running twice on same day is safe

**File:** `.github/workflows/pipeline-daily.yml`

---

### E01B-004: Smoke Tests + Health Monitoring

Verify the full stack works end-to-end after deployment.

**Acceptance Criteria:**
- Smoke test script that verifies:
  - `GET /` returns 200 with version info
  - `GET /api/data/latest` returns valid metadata with domain_count > 0
  - `GET /api/alerts` returns 5 seeded alerts
  - `POST /api/check {"domain":"shopee.com.my"}` returns safe verdict
  - `POST /api/check {"domain":"maybank-secure.xyz"}` returns suspicious/dangerous
  - `GET /api/data/bloom` returns binary data with SAFB header
- GitHub Actions step runs smoke tests after each deploy
- README updated with production URL and smoke test instructions

**File:** `scripts/smoke-test.sh`

---

## Implementation Order

1. **E01B-001** — Provision Cloudflare resources (manual, ~30 min)
2. **E01B-002** — Run initial pipeline seed (manual, ~15 min)
3. **E01B-003** — Daily cron via GitHub Actions (automated)
4. **E01B-004** — Smoke tests + monitoring

## Notes

- This is mostly ops/deployment work, not code
- E01B-003 replaces the DigitalOcean droplet for MVP — GitHub Actions is free and simpler
- Full scraping infra (SemakMule, news sites) is deferred to E12 (Phase 3)
- The 3 open-source feeds provide 100K+ domains — sufficient for MVP

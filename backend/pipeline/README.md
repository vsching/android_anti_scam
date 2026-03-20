# Safe Anot? Data Pipeline

Fetches, normalises, and publishes scam domain data to Cloudflare R2/KV/D1.

## Prerequisites

- Python 3.12+

## Setup

```bash
pip install -r requirements.txt
cp .env.example .env   # fill in Cloudflare credentials
```

## Usage

```bash
python seed_database.py
```

### Weekly Report (FCM Notification Trigger)

Selects the highest-severity alert from the last 7 days and triggers an FCM
push notification via POST /api/alerts/notify.

```bash
# Dry run — print selected alert without sending notification
python -m src.weekly_report --dry-run

# Live run — send FCM notification for top alert
python -m src.weekly_report
```

**Cron schedule:** `0 1 * * 1` (every Monday at 01:00 UTC / 09:00 MYT)

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `CLOUDFLARE_API_TOKEN` | Yes | Cloudflare API token with D1/R2/KV access |
| `CLOUDFLARE_ACCOUNT_ID` | Yes | Cloudflare account ID |
| `CLOUDFLARE_D1_DATABASE_ID` | Yes | D1 database ID |
| `CLOUDFLARE_KV_NAMESPACE_ID` | No | KV namespace ID (for seed/domain pipeline) |
| `CLOUDFLARE_R2_BUCKET` | No | R2 bucket name (for seed/domain pipeline) |
| `PIPELINE_SECRET` | Yes | Shared secret for X-Pipeline-Key header auth |
| `API_BASE_URL` | Yes | Base URL of the workers API (e.g. `https://safeanot.com`) |

## Tests

```bash
pytest
```

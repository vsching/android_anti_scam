# Safe Anot?

Scam protection app for Malaysia and Singapore — link checker, phone shield, family guardian mode.

## Operations

### Smoke Tests

Run HTTP smoke tests against the production API:

```bash
chmod +x scripts/smoke-test.sh
bash scripts/smoke-test.sh
```

Override the target URL:

```bash
API_BASE_URL=https://your-staging-url.example.com bash scripts/smoke-test.sh
```

### Health Check

Check pipeline data freshness:

```
GET https://safeanot-api.management-481.workers.dev/api/health
```

Returns `status: "ok"` when data is less than 48 hours old, `"stale"` otherwise.

### Admin CLI

```bash
bash scripts/admin.sh
```

### Pipeline Manual Trigger

Trigger the daily pipeline manually from GitHub Actions:

1. Go to **Actions** > **Daily Pipeline**
2. Click **Run workflow**

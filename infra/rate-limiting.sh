#!/usr/bin/env bash
# Provision Cloudflare Rate Limiting Rulesets for the Safe Anot? API.
#
# Rules:
#   - /api/check: 100 requests/minute per IP
#   - /api/report: 10 requests/minute per IP
#
# Environment variables:
#   CF_ZONE_ID    — Cloudflare Zone ID (required)
#   CF_API_TOKEN  — Cloudflare API Token with Zone.Firewall permissions (required)
#
# Usage:
#   ./rate-limiting.sh             # Apply rate limiting rules
#   ./rate-limiting.sh --dry-run   # Preview API calls without applying
#
# This script is idempotent — it checks for existing rulesets before creating.

set -euo pipefail

DRY_RUN=false
if [[ "${1:-}" == "--dry-run" ]]; then
  DRY_RUN=true
  echo "[DRY RUN] No changes will be applied."
fi

# Validate required environment variables
if [[ -z "${CF_ZONE_ID:-}" ]]; then
  echo "ERROR: CF_ZONE_ID environment variable is required." >&2
  exit 1
fi

if [[ -z "${CF_API_TOKEN:-}" ]]; then
  echo "ERROR: CF_API_TOKEN environment variable is required." >&2
  exit 1
fi

API_BASE="https://api.cloudflare.com/client/v4/zones/${CF_ZONE_ID}"
AUTH_HEADER="Authorization: Bearer ${CF_API_TOKEN}"
CONTENT_TYPE="Content-Type: application/json"

RULESET_NAME="SafeAnot Rate Limiting"

# -------------------------------------------------------------------
# Helper: Make an API call or print it in dry-run mode
# -------------------------------------------------------------------
api_call() {
  local method="$1"
  local url="$2"
  local data="${3:-}"

  if [[ "$DRY_RUN" == "true" ]]; then
    echo "[DRY RUN] ${method} ${url}"
    if [[ -n "$data" ]]; then
      printf "  Body: %.500s" "$data"
      echo ""
    fi
    return 0
  fi

  if [[ -n "$data" ]]; then
    curl -s -X "${method}" "${url}" \
      -H "${AUTH_HEADER}" \
      -H "${CONTENT_TYPE}" \
      -d "${data}"
  else
    curl -s -X "${method}" "${url}" \
      -H "${AUTH_HEADER}" \
      -H "${CONTENT_TYPE}"
  fi
}

# -------------------------------------------------------------------
# Step 1: Check for existing rate-limiting ruleset
# -------------------------------------------------------------------
echo "Checking for existing rate-limiting rulesets..."

EXISTING_RULESETS=$(api_call GET "${API_BASE}/rulesets")

if [[ "$DRY_RUN" == "true" ]]; then
  echo "[DRY RUN] Would check for existing ruleset named '${RULESET_NAME}'"
  EXISTING_ID=""
else
  EXISTING_ID=$(echo "$EXISTING_RULESETS" | \
    python3 -c "
import sys, json
data = json.load(sys.stdin)
for rs in data.get('result', []):
    if rs.get('name') == '${RULESET_NAME}':
        print(rs['id'])
        break
" 2>/dev/null || echo "")
fi

# -------------------------------------------------------------------
# Step 2: Define the rate-limiting rules
# -------------------------------------------------------------------
RULES_PAYLOAD=$(cat <<'RULES_EOF'
{
  "name": "SafeAnot Rate Limiting",
  "description": "Rate limiting for Safe Anot? API endpoints",
  "kind": "zone",
  "phase": "http_ratelimit",
  "rules": [
    {
      "action": "block",
      "expression": "(http.request.uri.path eq \"/api/check\" and http.request.method eq \"POST\")",
      "description": "/api/check — 100 req/min per IP",
      "ratelimit": {
        "characteristics": ["ip.src"],
        "period": 60,
        "requests_per_period": 100,
        "mitigation_timeout": 60
      }
    },
    {
      "action": "block",
      "expression": "(http.request.uri.path eq \"/api/report\" and http.request.method eq \"POST\")",
      "description": "/api/report — 10 req/min per IP",
      "ratelimit": {
        "characteristics": ["ip.src"],
        "period": 60,
        "requests_per_period": 10,
        "mitigation_timeout": 60
      }
    }
  ]
}
RULES_EOF
)

# -------------------------------------------------------------------
# Step 3: Create or update the ruleset
# -------------------------------------------------------------------
if [[ -n "$EXISTING_ID" ]]; then
  echo "Found existing ruleset: ${EXISTING_ID}"
  echo "Updating ruleset..."
  RESULT=$(api_call PUT "${API_BASE}/rulesets/${EXISTING_ID}" "$RULES_PAYLOAD")
else
  echo "No existing ruleset found. Creating new ruleset..."
  RESULT=$(api_call POST "${API_BASE}/rulesets" "$RULES_PAYLOAD")
fi

if [[ "$DRY_RUN" == "true" ]]; then
  echo ""
  echo "[DRY RUN] Complete. Run without --dry-run to apply changes."
  exit 0
fi

# -------------------------------------------------------------------
# Step 4: Verify result
# -------------------------------------------------------------------
SUCCESS=$(echo "$RESULT" | python3 -c "
import sys, json
data = json.load(sys.stdin)
print('true' if data.get('success') else 'false')
" 2>/dev/null || echo "false")

if [[ "$SUCCESS" == "true" ]]; then
  RULESET_ID=$(echo "$RESULT" | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(data['result']['id'])
" 2>/dev/null || echo "unknown")
  echo "Rate limiting rules applied successfully."
  echo "Ruleset ID: ${RULESET_ID}"
  echo ""
  echo "Rules configured:"
  echo "  - POST /api/check: 100 req/min per IP"
  echo "  - POST /api/report: 10 req/min per IP"
else
  echo "ERROR: Failed to apply rate limiting rules." >&2
  echo "Response: ${RESULT}" >&2
  exit 1
fi

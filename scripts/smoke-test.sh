#!/usr/bin/env bash
# smoke-test.sh — quick HTTP smoke tests against the Safe Anot? API.
# Usage: API_BASE_URL=https://... bash scripts/smoke-test.sh
# Exits non-zero on any failure.

set -euo pipefail

API_BASE_URL="${API_BASE_URL:-https://safeanot-api.management-481.workers.dev}"
PASS=0
FAIL=0

check() {
  local label="$1"
  local result="$2"  # 0 = pass, non-zero = fail
  if [ "$result" -eq 0 ]; then
    echo "  PASS: $label"
    PASS=$((PASS + 1))
  else
    echo "  FAIL: $label"
    FAIL=$((FAIL + 1))
  fi
}

echo "Smoke tests against: $API_BASE_URL"
echo "=========================================="

# 1. GET / — assert 200 and JSON contains "status"
echo ""
echo "[1/6] GET /"
RESP=$(curl -s -w "\n%{http_code}" "$API_BASE_URL/")
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
if [ "$HTTP_CODE" = "200" ] && echo "$BODY" | grep -q '"status"'; then
  check "GET / returns 200 with status field" 0
else
  check "GET / returns 200 with status field (got $HTTP_CODE)" 1
fi

# 2. GET /api/data/latest — assert 200 and domain_count > 0
echo ""
echo "[2/6] GET /api/data/latest"
RESP=$(curl -s -w "\n%{http_code}" "$API_BASE_URL/api/data/latest")
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
if [ "$HTTP_CODE" = "200" ]; then
  DOMAIN_COUNT=$(echo "$BODY" | grep -o '"domain_count":[0-9]*' | grep -o '[0-9]*' || echo "0")
  if [ "$DOMAIN_COUNT" -gt 0 ] 2>/dev/null; then
    check "GET /api/data/latest returns 200 with domain_count > 0 ($DOMAIN_COUNT)" 0
  else
    check "GET /api/data/latest domain_count > 0 (got $DOMAIN_COUNT)" 1
  fi
else
  check "GET /api/data/latest returns 200 (got $HTTP_CODE)" 1
fi

# 3. GET /api/health — assert 200 and response contains "status"
echo ""
echo "[3/6] GET /api/health"
RESP=$(curl -s -w "\n%{http_code}" "$API_BASE_URL/api/health")
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
if [ "$HTTP_CODE" = "200" ] && echo "$BODY" | grep -q '"status"'; then
  check "GET /api/health returns 200 with status field" 0
else
  check "GET /api/health returns 200 with status field (got $HTTP_CODE)" 1
fi

# 4. GET /api/alerts — assert 200 and JSON array
echo ""
echo "[4/6] GET /api/alerts"
RESP=$(curl -s -w "\n%{http_code}" "$API_BASE_URL/api/alerts")
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
if [ "$HTTP_CODE" = "200" ] && echo "$BODY" | grep -q '^\['; then
  check "GET /api/alerts returns 200 with JSON array" 0
else
  check "GET /api/alerts returns 200 with JSON array (got $HTTP_CODE)" 1
fi

# 5. POST /api/check — safe domain
echo ""
echo "[5/6] POST /api/check (safe domain)"
RESP=$(curl -s -w "\n%{http_code}" -X POST \
  -H "Content-Type: application/json" \
  -d '{"url":"maybank2u.com.my"}' \
  "$API_BASE_URL/api/check")
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
VERDICT=$(echo "$BODY" | grep -o '"verdict":"[^"]*"' | grep -o ':"[^"]*"' | tr -d ':"')
if [ "$HTTP_CODE" = "200" ] && [ "$VERDICT" = "safe" ]; then
  check "POST /api/check maybank2u.com.my verdict=safe" 0
else
  check "POST /api/check maybank2u.com.my verdict=safe (got $HTTP_CODE, verdict=$VERDICT)" 1
fi

# 6. POST /api/check — suspicious domain
echo ""
echo "[6/6] POST /api/check (suspicious domain)"
RESP=$(curl -s -w "\n%{http_code}" -X POST \
  -H "Content-Type: application/json" \
  -d '{"url":"maybank-secure-update.xyz"}' \
  "$API_BASE_URL/api/check")
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
VERDICT=$(echo "$BODY" | grep -o '"verdict":"[^"]*"' | grep -o ':"[^"]*"' | tr -d ':"')
if [ "$HTTP_CODE" = "200" ] && [ "$VERDICT" != "safe" ]; then
  check "POST /api/check maybank-secure-update.xyz verdict!=safe (got $VERDICT)" 0
else
  check "POST /api/check maybank-secure-update.xyz verdict!=safe (got $HTTP_CODE, verdict=$VERDICT)" 1
fi

# Summary
echo ""
echo "=========================================="
echo "Results: $PASS passed, $FAIL failed (total $((PASS + FAIL)))"

if [ "$FAIL" -gt 0 ]; then
  echo "SMOKE TESTS FAILED"
  exit 1
else
  echo "ALL SMOKE TESTS PASSED"
  exit 0
fi

#!/usr/bin/env bash
# test_deploy_smoke.sh — Smoke test for Cloudflare Pages deployment
# Usage: ./test_deploy_smoke.sh <base_url>
# Example: ./test_deploy_smoke.sh https://safeanot.com
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: $0 <base_url>"
  echo "Example: $0 https://safeanot.com"
  exit 1
fi

BASE_URL="${1%/}"
ERRORS=0
PASS=0

check_url() {
  local path="$1"
  local url="${BASE_URL}${path}"
  local status
  status=$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "$url" 2>/dev/null || echo "000")

  if [ "$status" = "200" ]; then
    echo "  PASS: $path -> $status"
    PASS=$((PASS + 1))
  else
    echo "  FAIL: $path -> $status (expected 200)"
    ERRORS=$((ERRORS + 1))
  fi
}

echo "Smoke testing deployment at: $BASE_URL"
echo ""

# Test clean URLs (core routes)
echo "=== Clean URL Routes ==="
check_url "/"
check_url "/check"
check_url "/result"
check_url "/challenge"
check_url "/join"
check_url "/privacy"
check_url "/terms"
echo ""

# Test 404 page
echo "=== 404 Page ==="
NOT_FOUND_STATUS=$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "${BASE_URL}/nonexistent-page-xyz" 2>/dev/null || echo "000")
if [ "$NOT_FOUND_STATUS" = "404" ]; then
  echo "  PASS: /nonexistent-page-xyz -> 404"
  PASS=$((PASS + 1))
else
  echo "  FAIL: /nonexistent-page-xyz -> $NOT_FOUND_STATUS (expected 404)"
  ERRORS=$((ERRORS + 1))
fi
echo ""

# Placeholder: Security headers (E06-003)
echo "=== Security Headers (placeholder — requires E06-003) ==="
echo "  SKIP: X-Content-Type-Options header check"
echo "  SKIP: X-Frame-Options header check"
echo "  SKIP: Content-Security-Policy header check"
echo ""

# Placeholder: assetlinks.json (E06-004)
echo "=== Asset Links (placeholder — requires E06-004) ==="
echo "  SKIP: /.well-known/assetlinks.json check"
echo ""

echo "=== Summary ==="
echo "Passed: $PASS  Failed: $ERRORS"
if [ "$ERRORS" -eq 0 ]; then
  echo "All checks passed."
  exit 0
else
  echo "$ERRORS check(s) failed."
  exit 1
fi

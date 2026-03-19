#!/bin/bash
# test_headers.sh — Validates _headers file has all required security headers
# Usage: ./test_headers.sh

set -euo pipefail

HEADERS_FILE="$(dirname "$0")/../_headers"
PASS=0
FAIL=0

check_header() {
  local header="$1"
  if grep -q "$header" "$HEADERS_FILE" 2>/dev/null; then
    echo "  PASS: $header"
    PASS=$((PASS + 1))
  else
    echo "  FAIL: $header NOT FOUND"
    FAIL=$((FAIL + 1))
  fi
}

echo "=== Security Headers Validation ==="
echo ""

# Check that the file exists
if [ ! -f "$HEADERS_FILE" ]; then
  echo "FAIL: _headers file not found at $HEADERS_FILE"
  exit 1
fi

echo "Checking required headers in _headers..."
echo ""

# Check route
check_header "/\*"

# Required security headers
check_header "X-Content-Type-Options: nosniff"
check_header "X-Frame-Options: DENY"
check_header "X-XSS-Protection: 1; mode=block"
check_header "Referrer-Policy: strict-origin-when-cross-origin"
check_header "Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()"
check_header "Strict-Transport-Security: max-age=31536000; includeSubDomains; preload"

# CSP directives
check_header "Content-Security-Policy:"
check_header "default-src 'self'"
check_header "script-src 'self' 'unsafe-inline'"
check_header "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com"
check_header "font-src https://fonts.gstatic.com"
check_header "img-src 'self' data:"
check_header "connect-src 'self' https://api.safeanot.com https://\*\.workers\.dev"
check_header "object-src 'none'"
check_header "base-uri 'none'"
check_header "frame-ancestors 'none'"

echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="

if [ "$FAIL" -gt 0 ]; then
  echo "FAILED"
  exit 1
else
  echo "ALL CHECKS PASSED"
  exit 0
fi

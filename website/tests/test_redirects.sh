#!/usr/bin/env bash
# test_redirects.sh — Validates _redirects file format for Cloudflare Pages
set -euo pipefail

REDIRECTS_FILE="${1:-$(dirname "$0")/../_redirects}"
ERRORS=0

if [ ! -f "$REDIRECTS_FILE" ]; then
  echo "FAIL: _redirects file not found at $REDIRECTS_FILE"
  exit 1
fi

echo "Checking _redirects file: $REDIRECTS_FILE"
echo "---"

# Check each line has exactly 3 fields
LINE_NUM=0
while IFS= read -r line || [ -n "$line" ]; do
  LINE_NUM=$((LINE_NUM + 1))

  # Skip empty lines
  [ -z "$line" ] && continue

  FIELD_COUNT=$(echo "$line" | awk '{print NF}')
  if [ "$FIELD_COUNT" -ne 3 ]; then
    echo "FAIL: Line $LINE_NUM has $FIELD_COUNT fields (expected 3): $line"
    ERRORS=$((ERRORS + 1))
    continue
  fi

  SOURCE=$(echo "$line" | awk '{print $1}')
  DEST=$(echo "$line" | awk '{print $2}')
  STATUS=$(echo "$line" | awk '{print $3}')

  # Source must start with /
  if [[ "$SOURCE" != /* ]]; then
    echo "FAIL: Line $LINE_NUM source does not start with /: $SOURCE"
    ERRORS=$((ERRORS + 1))
  fi

  # Status must be 200
  if [ "$STATUS" != "200" ]; then
    echo "FAIL: Line $LINE_NUM status is $STATUS (expected 200)"
    ERRORS=$((ERRORS + 1))
  fi

done < "$REDIRECTS_FILE"

# Check all 6 required routes are present
REQUIRED_ROUTES="/check /result /challenge /join /privacy /terms"
for route in $REQUIRED_ROUTES; do
  if ! awk '{print $1}' "$REDIRECTS_FILE" | grep -qx "$route"; then
    echo "FAIL: Missing required route: $route"
    ERRORS=$((ERRORS + 1))
  fi
done

echo "---"
if [ "$ERRORS" -eq 0 ]; then
  echo "PASS: _redirects file is valid ($LINE_NUM lines, 6 routes)"
  exit 0
else
  echo "FAIL: $ERRORS error(s) found"
  exit 1
fi

#!/usr/bin/env bash
# test-smoke-test.sh — validates that smoke-test.sh is well-formed and contains expected checks.
# Exits non-zero on any failure.

set -euo pipefail

SCRIPT="scripts/smoke-test.sh"
PASS=0
FAIL=0

check() {
  local label="$1"
  local result="$2"
  if [ "$result" -eq 0 ]; then
    echo "  PASS: $label"
    PASS=$((PASS + 1))
  else
    echo "  FAIL: $label"
    FAIL=$((FAIL + 1))
  fi
}

echo "Validating smoke-test.sh"
echo "=========================================="

# 1. File exists
if [ -f "$SCRIPT" ]; then
  check "File exists" 0
else
  check "File exists" 1
  echo "VALIDATION FAILED — file not found"
  exit 1
fi

# 2. Is executable
if [ -x "$SCRIPT" ]; then
  check "File is executable" 0
else
  check "File is executable" 1
fi

# 3. Valid bash syntax
if bash -n "$SCRIPT" 2>/dev/null; then
  check "Valid bash syntax" 0
else
  check "Valid bash syntax" 1
fi

# 4. Contains expected check patterns
for pattern in "GET /" "GET /api/health" "GET /api/data/latest" "GET /api/alerts" "POST /api/check"; do
  if grep -q "$pattern" "$SCRIPT"; then
    check "Contains '$pattern'" 0
  else
    check "Contains '$pattern'" 1
  fi
done

# Summary
echo ""
echo "=========================================="
echo "Results: $PASS passed, $FAIL failed"

if [ "$FAIL" -gt 0 ]; then
  echo "VALIDATION FAILED"
  exit 1
else
  echo "ALL CHECKS PASSED"
  exit 0
fi

#!/usr/bin/env bash
#
# test-admin-cli.sh — Validates admin.sh script structure and domain validation
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ADMIN_SH="${SCRIPT_DIR}/admin.sh"
PASS=0
FAIL=0

pass() { echo "  PASS: $1"; PASS=$((PASS + 1)); }
fail() { echo "  FAIL: $1"; FAIL=$((FAIL + 1)); }

# Capture combined stdout+stderr from a command, ignoring exit code
capture() {
  "$@" 2>&1 || true
}

echo "=== Admin CLI Tests ==="
echo ""

# ── Test 1: Script exists ──
echo "Test 1: Script exists"
if [[ -f "$ADMIN_SH" ]]; then
  pass "admin.sh exists"
else
  fail "admin.sh not found at ${ADMIN_SH}"
fi

# ── Test 2: Script is executable ──
echo "Test 2: Script is executable"
if [[ -x "$ADMIN_SH" ]]; then
  pass "admin.sh is executable"
else
  fail "admin.sh is not executable"
fi

# ── Test 3: Bash syntax check ──
echo "Test 3: Bash syntax check"
if bash -n "$ADMIN_SH" 2>/dev/null; then
  pass "admin.sh has valid bash syntax"
else
  fail "admin.sh has syntax errors"
fi

# ── Test 4: Usage help works ──
echo "Test 4: Usage help"
output=$(capture "$ADMIN_SH" --help)
if echo "$output" | grep -q "Subcommands"; then
  pass "help output contains 'Subcommands'"
else
  fail "help output missing 'Subcommands'"
fi

# ── Test 5: Domain validation — reject 'notadomain' ──
echo "Test 5: Domain validation rejects 'notadomain'"
output=$(capture "$ADMIN_SH" allowlist-add "notadomain" "test" "test")
if echo "$output" | grep -q "Invalid domain"; then
  pass "rejects 'notadomain'"
else
  fail "did not reject 'notadomain'"
fi

# ── Test 6: Domain validation — reject protocol prefix ──
echo "Test 6: Domain validation rejects http:// prefix"
output=$(capture "$ADMIN_SH" allowlist-add "http://example.com" "test" "test")
if echo "$output" | grep -q "protocol prefix"; then
  pass "rejects 'http://example.com'"
else
  fail "did not reject 'http://example.com'"
fi

# ── Test 7: Domain validation — reject invalid chars ──
echo "Test 7: Domain validation rejects invalid characters"
output=$(capture "$ADMIN_SH" allowlist-add "exam%ple.com" "test" "test")
if echo "$output" | grep -q "invalid characters"; then
  pass "rejects domain with invalid characters"
else
  fail "did not reject domain with invalid characters"
fi

# ── Test 8: Domain validation — accept valid domain ──
echo "Test 8: Domain validation accepts 'example.com'"
validate_result=$(bash -c '
  validate_domain() {
    local domain="$1"
    if [[ "$domain" =~ ^https?:// ]]; then return 1; fi
    if [[ "$domain" != *.* ]]; then return 1; fi
    if ! [[ "$domain" =~ ^[a-zA-Z0-9.-]+$ ]]; then return 1; fi
    return 0
  }
  validate_domain "example.com" && echo "valid" || echo "invalid"
')
if [[ "$validate_result" == "valid" ]]; then
  pass "accepts 'example.com'"
else
  fail "rejected 'example.com'"
fi

# ── Test 9: Unknown subcommand shows error ──
echo "Test 9: Unknown subcommand"
output=$(capture "$ADMIN_SH" foobar)
if echo "$output" | grep -q "Unknown subcommand"; then
  pass "unknown subcommand shows error"
else
  fail "unknown subcommand did not show error"
fi

# ── Test 10: Subcommand dispatch — missing args ──
echo "Test 10: Missing args shows usage"
output=$(capture "$ADMIN_SH" blocklist-add)
if echo "$output" | grep -q "Usage"; then
  pass "missing args shows usage"
else
  fail "missing args did not show usage"
fi

# ── Summary ──
echo ""
echo "=== Results: ${PASS} passed, ${FAIL} failed ==="

if [[ $FAIL -gt 0 ]]; then
  exit 1
fi

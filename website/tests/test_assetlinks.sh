#!/usr/bin/env bash
# test_assetlinks.sh — Validates assetlinks.json syntax and required fields
set -euo pipefail

ASSETLINKS_FILE="${1:-$(dirname "$0")/../.well-known/assetlinks.json}"
ERRORS=0

if [ ! -f "$ASSETLINKS_FILE" ]; then
  echo "FAIL: assetlinks.json not found at $ASSETLINKS_FILE"
  exit 1
fi

echo "Checking assetlinks.json: $ASSETLINKS_FILE"
echo "---"

# 1. Validate JSON syntax
if command -v python3 &>/dev/null; then
  if ! python3 -m json.tool "$ASSETLINKS_FILE" >/dev/null 2>&1; then
    echo "FAIL: Invalid JSON syntax"
    ERRORS=$((ERRORS + 1))
  else
    echo "OK: JSON syntax is valid"
  fi
elif command -v jq &>/dev/null; then
  if ! jq empty "$ASSETLINKS_FILE" 2>/dev/null; then
    echo "FAIL: Invalid JSON syntax"
    ERRORS=$((ERRORS + 1))
  else
    echo "OK: JSON syntax is valid"
  fi
else
  echo "WARN: Neither python3 nor jq found — skipping JSON syntax check"
fi

# 2. Check that file is a JSON array
IS_ARRAY=$(python3 -c "
import json, sys
with open('$ASSETLINKS_FILE') as f:
    data = json.load(f)
print('yes' if isinstance(data, list) else 'no')
" 2>/dev/null || echo "error")

if [ "$IS_ARRAY" = "yes" ]; then
  echo "OK: Top-level structure is a JSON array"
elif [ "$IS_ARRAY" = "no" ]; then
  echo "FAIL: Top-level structure must be a JSON array"
  ERRORS=$((ERRORS + 1))
else
  echo "WARN: Could not verify top-level structure"
fi

# 3. Check required fields using python3
FIELD_CHECK=$(python3 -c "
import json, sys

with open('$ASSETLINKS_FILE') as f:
    data = json.load(f)

errors = []
if not isinstance(data, list) or len(data) == 0:
    errors.append('File must contain at least one entry')
    print('\n'.join(errors))
    sys.exit(0)

entry = data[0]

# Check relation
rel = entry.get('relation', [])
if 'delegate_permission/common.handle_all_urls' not in rel:
    errors.append('Missing relation: delegate_permission/common.handle_all_urls')

# Check target
target = entry.get('target', {})
if target.get('namespace') != 'android_app':
    errors.append('Missing or wrong target.namespace (expected android_app)')
if target.get('package_name') != 'com.safeanot.app':
    errors.append('Missing or wrong target.package_name (expected com.safeanot.app)')

fingerprints = target.get('sha256_cert_fingerprints', [])
if not fingerprints:
    errors.append('Missing target.sha256_cert_fingerprints')
else:
    for fp in fingerprints:
        # Must be 32 colon-separated hex pairs
        import re
        if not re.match(r'^([0-9A-F]{2}:){31}[0-9A-F]{2}$', fp):
            errors.append(f'Invalid fingerprint format: {fp}')

print('\n'.join(errors) if errors else '')
" 2>/dev/null || echo "__error__")

if [ "$FIELD_CHECK" = "__error__" ]; then
  echo "WARN: Could not verify required fields (python3 error)"
elif [ -z "$FIELD_CHECK" ]; then
  echo "OK: All required fields present and valid"
  echo "OK: relation = delegate_permission/common.handle_all_urls"
  echo "OK: namespace = android_app"
  echo "OK: package_name = com.safeanot.app"
  echo "OK: sha256_cert_fingerprints format valid"
else
  while IFS= read -r err; do
    echo "FAIL: $err"
    ERRORS=$((ERRORS + 1))
  done <<< "$FIELD_CHECK"
fi

# 4. Check no debug fingerprints (simple heuristic: file should have few entries)
ENTRY_COUNT=$(python3 -c "
import json
with open('$ASSETLINKS_FILE') as f:
    data = json.load(f)
print(len(data))
" 2>/dev/null || echo "0")

if [ "$ENTRY_COUNT" -gt 3 ]; then
  echo "WARN: $ENTRY_COUNT entries found — verify no debug fingerprints are included"
fi

echo "---"
if [ "$ERRORS" -eq 0 ]; then
  echo "PASS: assetlinks.json is valid"
  exit 0
else
  echo "FAIL: $ERRORS error(s) found"
  exit 1
fi

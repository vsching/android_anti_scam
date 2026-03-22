#!/usr/bin/env bash
#
# admin.sh — Admin CLI for Safe Anot? Allowlist & Blocklist Management
#
# Usage:
#   ./scripts/admin.sh <subcommand> [args...]
#
# Subcommands:
#   allowlist-add <domain> <entity> <category>   Add domain to allowlist
#   allowlist-remove <domain>                     Remove domain from allowlist
#   blocklist-add <domain> <reason>               Add domain to blocklist
#   blocklist-remove <domain>                     Remove domain from blocklist
#   list --allowlist                              List allowlisted domains
#   list --blocklist                              List blocklisted domains
#   discoveries-list                              List pending discoveries (last 50)
#   discoveries-clear                             Clear discoveries older than 30 days
#
# Environment variables:
#   KV_NAMESPACE_ID — KV namespace ID (default: production namespace)
#   ADMIN_SECRET   — Required for cache purge after writes (optional, warns if unset)
#   API_BASE       — Base URL for cache purge endpoint (default: https://api.safeanot.com)
#
# Notes:
#   - wrangler kv key list handles pagination automatically.
#   - For very large lists, use --limit flag to cap output.
#   - ADMIN_SECRET is passed via curl -H and may be visible in `ps` output.
#     This is acceptable for a developer-only local tool. Do not use in CI/CD
#     pipelines or shared environments where process listings are exposed.
#

set -euo pipefail

WORKERS_DIR="backend/workers"
# Production KV namespace ID. Override via KV_NAMESPACE_ID env var for staging.
KV_NAMESPACE_ID="${KV_NAMESPACE_ID:-af9c8116ef6d45aca90dbbd4970acf1d}"
API_BASE="${API_BASE:-https://api.safeanot.com}"

# ──────────────────────────────────────────────
# Helpers
# ──────────────────────────────────────────────

usage() {
  cat <<'USAGE'
Usage: admin.sh <subcommand> [args...]

Subcommands:
  allowlist-add <domain> <entity> <category>   Add domain to allowlist
  allowlist-remove <domain>                     Remove domain from allowlist
  blocklist-add <domain> <reason>               Add domain to blocklist
  blocklist-remove <domain>                     Remove domain from blocklist
  list --allowlist                              List allowlisted domains
  list --blocklist                              List blocklisted domains
  discoveries-list                              List pending discoveries (last 50)
  discoveries-clear                             Clear discoveries older than 30 days

Environment:
  ADMIN_SECRET     Secret key for cache purge (optional, warns if unset)
  API_BASE         API base URL (default: https://api.safeanot.com)
  KV_NAMESPACE_ID  KV namespace ID (default: production namespace)
USAGE
  exit 1
}

validate_domain() {
  local domain="$1"

  # Reject protocol prefixes
  if [[ "$domain" =~ ^https?:// ]]; then
    echo "Error: Domain must not include protocol prefix (http:// or https://)" >&2
    return 1
  fi

  # Must contain at least one dot
  if [[ "$domain" != *.* ]]; then
    echo "Error: Invalid domain format — must contain at least one dot (e.g. example.com)" >&2
    return 1
  fi

  # Only valid hostname characters: a-z, 0-9, hyphen, dot
  if ! [[ "$domain" =~ ^[a-zA-Z0-9.-]+$ ]]; then
    echo "Error: Domain contains invalid characters — only a-z, 0-9, hyphens, and dots allowed" >&2
    return 1
  fi

  return 0
}

purge_cache() {
  local domain="$1"

  if [[ -z "${ADMIN_SECRET:-}" ]]; then
    echo "Warning: ADMIN_SECRET not set — cache purge skipped. The cached entry for '${domain}' may be stale." >&2
    return 0
  fi

  echo "Purging cache for '${domain}'..."
  local http_code
  http_code=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
    -H "X-Admin-Key: ${ADMIN_SECRET}" \
    "${API_BASE}/api/admin/cache/${domain}")

  if [[ "$http_code" == "200" || "$http_code" == "204" ]]; then
    echo "Cache purged successfully."
  else
    echo "Warning: Cache purge returned HTTP ${http_code} — entry may still be cached." >&2
  fi
}

# ──────────────────────────────────────────────
# Subcommands
# ──────────────────────────────────────────────

cmd_allowlist_add() {
  if [[ $# -lt 3 ]]; then
    echo "Usage: admin.sh allowlist-add <domain> <entity> <category>" >&2
    exit 1
  fi

  local domain="$1" entity="$2" category="$3"
  validate_domain "$domain" || exit 1

  local json
  if command -v jq &>/dev/null; then
    json=$(jq -n --arg domain "$domain" --arg category "$category" --arg entity "$entity" \
      --arg added_at "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
      '{domain:$domain,verdict:"safe",category:$category,entity:$entity,reason:"Manually allowlisted",added_at:$added_at}')
  else
    # Fallback: basic escaping of " and \ for JSON safety
    local esc_domain esc_entity esc_category
    esc_domain="${domain//\\/\\\\}"; esc_domain="${esc_domain//\"/\\\"}"
    esc_entity="${entity//\\/\\\\}"; esc_entity="${esc_entity//\"/\\\"}"
    esc_category="${category//\\/\\\\}"; esc_category="${esc_category//\"/\\\"}"
    json=$(printf '{"domain":"%s","verdict":"safe","category":"%s","entity":"%s","reason":"Manually allowlisted","added_at":"%s"}' \
      "$esc_domain" "$esc_category" "$esc_entity" "$(date -u +%Y-%m-%dT%H:%M:%SZ)")
  fi

  echo "Adding '${domain}' to allowlist (entity=${entity}, category=${category})..."
  (cd "${WORKERS_DIR}" && npx wrangler kv key put \
    --namespace-id "${KV_NAMESPACE_ID}" \
    "allowlist:${domain}" "${json}")

  echo "Allowlist entry added."
  purge_cache "$domain"
}

cmd_allowlist_remove() {
  if [[ $# -lt 1 ]]; then
    echo "Usage: admin.sh allowlist-remove <domain>" >&2
    exit 1
  fi

  local domain="$1"
  validate_domain "$domain" || exit 1

  read -r -p "Remove '${domain}' from allowlist? [y/N] " confirm
  if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
    echo "Aborted."
    exit 0
  fi

  echo "Removing '${domain}' from allowlist..."
  (cd "${WORKERS_DIR}" && npx wrangler kv key delete \
    --namespace-id "${KV_NAMESPACE_ID}" \
    "allowlist:${domain}")

  echo "Allowlist entry removed."
  purge_cache "$domain"
}

cmd_blocklist_add() {
  if [[ $# -lt 2 ]]; then
    echo "Usage: admin.sh blocklist-add <domain> <reason>" >&2
    exit 1
  fi

  local domain="$1" reason="$2"
  validate_domain "$domain" || exit 1

  local json
  if command -v jq &>/dev/null; then
    json=$(jq -n --arg domain "$domain" --arg reason "$reason" \
      --arg added_at "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
      '{domain:$domain,verdict:"dangerous",reason:$reason,source:"manual_override",confidence:1.0,added_at:$added_at}')
  else
    # Fallback: basic escaping of " and \ for JSON safety
    local esc_domain esc_reason
    esc_domain="${domain//\\/\\\\}"; esc_domain="${esc_domain//\"/\\\"}"
    esc_reason="${reason//\\/\\\\}"; esc_reason="${esc_reason//\"/\\\"}"
    json=$(printf '{"domain":"%s","verdict":"dangerous","reason":"%s","source":"manual_override","confidence":1.0,"added_at":"%s"}' \
      "$esc_domain" "$esc_reason" "$(date -u +%Y-%m-%dT%H:%M:%SZ)")
  fi

  echo "Adding '${domain}' to blocklist (reason=${reason})..."
  (cd "${WORKERS_DIR}" && npx wrangler kv key put \
    --namespace-id "${KV_NAMESPACE_ID}" \
    "${domain}" "${json}")

  echo "Blocklist entry added."
  purge_cache "$domain"
}

cmd_blocklist_remove() {
  if [[ $# -lt 1 ]]; then
    echo "Usage: admin.sh blocklist-remove <domain>" >&2
    exit 1
  fi

  local domain="$1"
  validate_domain "$domain" || exit 1

  read -r -p "Remove '${domain}' from blocklist? [y/N] " confirm
  if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
    echo "Aborted."
    exit 0
  fi

  echo "Removing '${domain}' from blocklist..."
  (cd "${WORKERS_DIR}" && npx wrangler kv key delete \
    --namespace-id "${KV_NAMESPACE_ID}" \
    "${domain}")

  echo "Blocklist entry removed."
  purge_cache "$domain"
}

cmd_list() {
  if [[ $# -lt 1 ]]; then
    echo "Usage: admin.sh list --allowlist | --blocklist [--limit N]" >&2
    exit 1
  fi

  local mode="" limit=""

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --allowlist) mode="allowlist"; shift ;;
      --blocklist) mode="blocklist"; shift ;;
      --limit)
        shift
        limit="$1"
        shift
        ;;
      *)
        echo "Error: Unknown option '$1'" >&2
        exit 1
        ;;
    esac
  done

  if [[ -z "$mode" ]]; then
    echo "Error: Specify --allowlist or --blocklist" >&2
    exit 1
  fi

  if [[ "$mode" == "allowlist" ]]; then
    echo "Listing allowlisted domains..."
    # wrangler kv key list handles pagination automatically
    local output
    output=$(cd "${WORKERS_DIR}" && npx wrangler kv key list \
      --namespace-id "${KV_NAMESPACE_ID}" \
      --prefix "allowlist:")

    if [[ -n "$limit" ]]; then
      echo "$output" | head -"${limit}"
    else
      echo "$output"
    fi
  elif [[ "$mode" == "blocklist" ]]; then
    echo "Listing blocklisted domains..."
    # List all keys and filter out internal prefixes
    local output
    output=$(cd "${WORKERS_DIR}" && npx wrangler kv key list \
      --namespace-id "${KV_NAMESPACE_ID}")

    # Filter out allowlist:, alerts:, data:, cache: prefixed keys
    # Keep in sync with EXCLUDED_PREFIXES in backend/workers/src/lib/admin-kv.ts
    local filtered
    filtered=$(echo "$output" | grep -v -E '"name"\s*:\s*"(allowlist:|alerts:|data:|cache:)' || true)

    if [[ -n "$limit" ]]; then
      echo "$filtered" | head -"${limit}"
    else
      echo "$filtered"
    fi
  fi
}

cmd_discoveries_list() {
  echo "Listing pending discoveries (last 50)..."
  (cd "${WORKERS_DIR}" && npx wrangler d1 execute safeanot-db --remote \
    --command "SELECT * FROM pending_discoveries ORDER BY last_seen_at DESC LIMIT 50")
}

cmd_discoveries_clear() {
  read -r -p "Clear discoveries older than 30 days? [y/N] " confirm
  if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
    echo "Aborted."
    exit 0
  fi

  echo "Clearing old discoveries..."
  (cd "${WORKERS_DIR}" && npx wrangler d1 execute safeanot-db --remote \
    --command "DELETE FROM pending_discoveries WHERE last_seen_at < datetime('now', '-30 days')")
  echo "Old discoveries cleared."
}

# ──────────────────────────────────────────────
# Main dispatch
# ──────────────────────────────────────────────

if [[ $# -lt 1 ]]; then
  usage
fi

SUBCOMMAND="$1"
shift

case "$SUBCOMMAND" in
  allowlist-add)     cmd_allowlist_add "$@" ;;
  allowlist-remove)  cmd_allowlist_remove "$@" ;;
  blocklist-add)     cmd_blocklist_add "$@" ;;
  blocklist-remove)  cmd_blocklist_remove "$@" ;;
  list)              cmd_list "$@" ;;
  discoveries-list)  cmd_discoveries_list ;;
  discoveries-clear) cmd_discoveries_clear ;;
  help|--help|-h)    usage ;;
  *)
    echo "Error: Unknown subcommand '${SUBCOMMAND}'" >&2
    usage
    ;;
esac

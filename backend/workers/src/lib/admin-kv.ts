// Centralized KV operations for admin allowlist and blocklist management.

/** Write an allowlist entry to KV with `allowlist:<domain>` key. */
export async function writeAllowlistEntry(
  kv: KVNamespace,
  domain: string,
  entity: string,
  category: string,
): Promise<void> {
  const value = JSON.stringify({
    verdict: 'safe',
    reason: `Verified allowlist entry: ${entity}`,
    confidence: 1.0,
    entity,
    category,
    added_at: new Date().toISOString(),
  });
  await kv.put(`allowlist:${domain}`, value);
}

/** Write a blocklist entry to KV with bare domain key. */
export async function writeBlocklistEntry(
  kv: KVNamespace,
  domain: string,
  reason: string,
): Promise<void> {
  const value = JSON.stringify({
    verdict: 'dangerous',
    reason,
    confidence: 1.0,
    added_at: new Date().toISOString(),
  });
  await kv.put(domain, value);
}

/** Delete an allowlist entry from KV. */
export async function deleteAllowlistEntry(
  kv: KVNamespace,
  domain: string,
): Promise<void> {
  await kv.delete(`allowlist:${domain}`);
}

/** Delete a blocklist entry from KV. */
export async function deleteBlocklistEntry(
  kv: KVNamespace,
  domain: string,
): Promise<void> {
  await kv.delete(domain);
}

/** List all allowlist entries from KV, handling pagination. */
export async function listAllowlistEntries(
  kv: KVNamespace,
): Promise<Array<{ domain: string; value: unknown }>> {
  const entries: Array<{ domain: string; value: unknown }> = [];
  let cursor: string | undefined;

  do {
    const list = await kv.list({ prefix: 'allowlist:', cursor });
    for (const key of list.keys) {
      const raw = await kv.get(key.name);
      const domain = key.name.replace(/^allowlist:/, '');
      let value: unknown = raw;
      try {
        value = raw ? JSON.parse(raw) : null;
      } catch {
        // Keep raw string value
      }
      entries.push({ domain, value });
    }
    cursor = list.list_complete ? undefined : list.cursor;
  } while (cursor);

  return entries;
}

/** Prefixes used internally — excluded from blocklist listing. */
const EXCLUDED_PREFIXES = ['allowlist:', 'alerts:', 'data:', 'cache:'];

/** List all blocklist entries from KV, handling pagination. */
export async function listBlocklistEntries(
  kv: KVNamespace,
): Promise<Array<{ domain: string; value: unknown }>> {
  const entries: Array<{ domain: string; value: unknown }> = [];
  let cursor: string | undefined;

  do {
    const list = await kv.list({ cursor });
    for (const key of list.keys) {
      // Skip keys with internal prefixes
      if (EXCLUDED_PREFIXES.some((p) => key.name.startsWith(p))) {
        continue;
      }
      const raw = await kv.get(key.name);
      let value: unknown = raw;
      try {
        value = raw ? JSON.parse(raw) : null;
      } catch {
        // Keep raw string value
      }
      entries.push({ domain: key.name, value });
    }
    cursor = list.list_complete ? undefined : list.cursor;
  } while (cursor);

  return entries;
}

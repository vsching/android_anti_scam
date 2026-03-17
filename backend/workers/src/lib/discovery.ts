// Discovery module: writes suspicious/dangerous domains to D1 pending_discoveries.
// Uses UPSERT to increment check_count on repeated sightings and escalate verdict.

/**
 * Record a suspicious or dangerous domain in the pending_discoveries table.
 * Uses INSERT ... ON CONFLICT to handle duplicates:
 *   - Increments check_count
 *   - Updates last_seen_at
 *   - Escalates verdict if the new verdict is more severe
 */
export async function recordDiscovery(
  db: D1Database,
  domain: string,
  verdict: string,
  reason: string,
): Promise<void> {
  const id = crypto.randomUUID();

  await db
    .prepare(
      `INSERT INTO pending_discoveries (id, domain, verdict, reason, source, check_count)
       VALUES (?, ?, ?, ?, 'heuristic', 1)
       ON CONFLICT(domain) DO UPDATE SET
         check_count = check_count + 1,
         last_seen_at = CURRENT_TIMESTAMP,
         verdict = CASE
           WHEN excluded.verdict > verdict THEN excluded.verdict
           ELSE verdict
         END`,
    )
    .bind(id, domain, verdict, reason)
    .run();
}

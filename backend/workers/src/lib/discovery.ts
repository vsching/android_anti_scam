// Discovery module: writes suspicious/dangerous domains to D1 pending_discoveries.
// Uses UPSERT to increment check_count on repeated sightings and escalate verdict.

// Severity ordering for verdict escalation (higher index = more severe)
const VERDICT_SEVERITY: Record<string, number> = {
  unknown: 0,
  safe: 1,
  suspicious: 2,
  dangerous: 3,
};

/**
 * Record a suspicious or dangerous domain in the pending_discoveries table.
 * Uses INSERT ... ON CONFLICT to handle duplicates:
 *   - Increments check_count
 *   - Updates last_seen_at
 *   - Escalates verdict only if the new verdict is more severe (by severity rank)
 */
export async function recordDiscovery(
  db: D1Database,
  domain: string,
  verdict: string,
  reason: string,
): Promise<void> {
  const id = crypto.randomUUID();
  const severityRank = VERDICT_SEVERITY[verdict] ?? 0;

  // Use a severity rank column approach: store the rank alongside the verdict
  // and only update if the new rank is higher. This avoids lexicographic comparison.
  await db
    .prepare(
      `INSERT INTO pending_discoveries (id, domain, verdict, reason, source, check_count)
       VALUES (?, ?, ?, ?, 'heuristic', 1)
       ON CONFLICT(domain) DO UPDATE SET
         check_count = check_count + 1,
         last_seen_at = CURRENT_TIMESTAMP,
         verdict = CASE WHEN ? > (
           CASE pending_discoveries.verdict
             WHEN 'dangerous' THEN 3
             WHEN 'suspicious' THEN 2
             WHEN 'safe' THEN 1
             ELSE 0
           END
         ) THEN ? ELSE pending_discoveries.verdict END,
         reason = CASE WHEN ? > (
           CASE pending_discoveries.verdict
             WHEN 'dangerous' THEN 3
             WHEN 'suspicious' THEN 2
             WHEN 'safe' THEN 1
             ELSE 0
           END
         ) THEN ? ELSE pending_discoveries.reason END`,
    )
    .bind(id, domain, verdict, reason, severityRank, verdict, severityRank, reason)
    .run();
}

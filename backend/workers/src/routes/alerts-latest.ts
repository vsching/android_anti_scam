// GET /api/alerts/latest — Returns the latest featured alert from D1.
// Prefers the most recent high-severity alert; falls back to any severity.
// Results are cached in KV for 5 minutes.

import { cacheKey, cacheGet, cachePut } from '../lib/cache';
import { jsonResponse } from '../lib/response';

/** Cache TTL for the latest alert: 5 minutes. */
const LATEST_ALERT_CACHE_TTL = 300;

/** Shape of an alert row from D1. */
interface Alert {
  id: string;
  title: string;
  description: string | null;
  scam_type: string | null;
  severity: string | null;
  region: string | null;
  state: string | null;
  report_count: number;
  source_url: string | null;
  created_at: string;
  updated_at: string;
}

/**
 * Handler for GET /api/alerts/latest
 *
 * Returns the single most important recent alert:
 * 1. Highest severity ('high') ordered by recency
 * 2. Falls back to most recent alert of any severity
 */
export async function handleAlertsLatest(
  _request: Request,
  env: Env,
  _params: Record<string, string>,
): Promise<Response> {
  // Check KV cache
  const key = cacheKey('alerts', 'latest');
  const cached = await cacheGet<Alert>(env.VERDICTS, key);

  if (cached !== null) {
    return jsonResponse(cached);
  }

  // Query D1 — prefer high-severity alerts
  let alert: Alert | null = null;

  const highSeverityResult = await env.DB.prepare(
    `SELECT * FROM alerts WHERE severity = 'high' ORDER BY created_at DESC LIMIT 1`,
  ).all<Alert>();

  if (highSeverityResult.results && highSeverityResult.results.length > 0) {
    alert = highSeverityResult.results[0];
  } else {
    // Fallback to most recent alert of any severity
    const anyResult = await env.DB.prepare(
      `SELECT * FROM alerts ORDER BY created_at DESC LIMIT 1`,
    ).all<Alert>();

    if (anyResult.results && anyResult.results.length > 0) {
      alert = anyResult.results[0];
    }
  }

  if (alert === null) {
    return new Response(JSON.stringify({ error: 'No alerts found' }), {
      status: 404,
      headers: { 'Content-Type': 'application/json' },
    });
  }

  // Cache result in KV (5-min TTL)
  await cachePut(env.VERDICTS, key, alert, LATEST_ALERT_CACHE_TTL);

  return jsonResponse(alert);
}

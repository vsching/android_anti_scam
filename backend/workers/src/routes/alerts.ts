// GET /api/alerts — Returns scam alerts from D1, with optional region filtering.
// Results are cached in KV for 15 minutes to reduce D1 reads.

import { cacheKey, cacheGet, cachePut, DEFAULT_CACHE_TTL } from '../lib/cache';

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

const VALID_REGIONS = new Set(['MY', 'SG', 'both']);

/**
 * Handler for GET /api/alerts
 * Query params:
 *   - region (optional): 'MY', 'SG', or 'both'. Invalid values return all alerts.
 */
export async function handleAlerts(
  request: Request,
  env: Env,
  _params: Record<string, string>,
): Promise<Response> {
  const url = new URL(request.url);
  const regionParam = url.searchParams.get('region');

  // Normalise region — invalid values fall through to returning all alerts
  const region = regionParam && VALID_REGIONS.has(regionParam) ? regionParam : null;

  // Check KV cache
  const key = cacheKey('alerts', region ?? undefined);
  const cached = await cacheGet<Alert[]>(env.VERDICTS, key);

  if (cached !== null) {
    return jsonResponse(cached);
  }

  // Query D1
  let query: string;
  let bindValues: string[] = [];

  if (region) {
    query = `SELECT * FROM alerts WHERE region = ? ORDER BY created_at DESC`;
    bindValues = [region];
  } else {
    query = `SELECT * FROM alerts ORDER BY created_at DESC`;
  }

  const stmt = bindValues.length
    ? env.DB.prepare(query).bind(...bindValues)
    : env.DB.prepare(query);

  const result = await stmt.all<Alert>();
  const alerts = result.results ?? [];

  // Cache result in KV
  await cachePut(env.VERDICTS, key, alerts, DEFAULT_CACHE_TTL);

  return jsonResponse(alerts);
}

function jsonResponse(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

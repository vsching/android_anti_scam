// GET /api/health — returns data freshness and pipeline health status.
// Reports "ok" when the latest build is within 48 hours, "stale" otherwise.

import { computeFreshnessHours } from '../lib/r2-manifest';

export async function handleHealth(
  _request: Request,
  env: Env,
): Promise<Response> {
  const obj = await env.DATA_BUCKET.get('latest.json');

  if (obj === null) {
    return new Response(
      JSON.stringify({ status: 'unknown', error: 'No manifest found' }),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    );
  }

  let manifest: { version?: string; build_timestamp?: string };
  try {
    manifest = JSON.parse(await obj.text());
  } catch {
    return new Response(
      JSON.stringify({ status: 'unknown', error: 'Invalid manifest' }),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    );
  }

  const buildTimestamp = manifest.build_timestamp ?? '';
  const freshnessHours = computeFreshnessHours(buildTimestamp);
  const status = freshnessHours <= 48 ? 'ok' : 'stale';

  return new Response(
    JSON.stringify({
      version: manifest.version ?? 'unknown',
      build_timestamp: buildTimestamp,
      freshness_hours: freshnessHours,
      status,
    }),
    { status: 200, headers: { 'Content-Type': 'application/json' } },
  );
}

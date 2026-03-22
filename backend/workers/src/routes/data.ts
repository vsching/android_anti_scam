// Data download endpoints for R2 artifacts.
// GET /api/data/latest  — metadata about the latest build
// GET /api/data/full    — stream the full SQLite database
// GET /api/data/delta   — stream the delta JSON (with ?since=YYYY-MM-DD)
// GET /api/data/bloom   — stream the Bloom filter binary

import {
  getManifest,
  isValidDate,
  daysBetween,
  MAX_DELTA_AGE_DAYS,
  type LatestManifest,
} from '../lib/r2-manifest';
import { jsonResponse } from '../lib/response';

/**
 * GET /api/data/latest
 * Returns JSON metadata about the latest build.
 */
export async function handleDataLatest(
  _request: Request,
  env: Env,
  _params: Record<string, string>,
): Promise<Response> {
  const manifest = await getManifest(env.DATA_BUCKET, env.VERDICTS);
  if (!manifest) {
    return jsonResponse({ error: 'No data available' }, 404);
  }

  return jsonResponse({
    version: manifest.version,
    full_size_kb: manifest.full_size_kb,
    delta_size_kb: manifest.delta_size_kb,
    bloom_size_kb: manifest.bloom_size_kb,
    sqlite_size_kb: manifest.sqlite_size_kb,
    domain_count: manifest.domain_count,
    build_timestamp: manifest.build_timestamp,
  });
}

/**
 * GET /api/data/full
 * Streams the full SQLite database file from R2.
 */
export async function handleDataFull(
  _request: Request,
  env: Env,
  _params: Record<string, string>,
): Promise<Response> {
  const manifest = await getManifest(env.DATA_BUCKET, env.VERDICTS);
  if (!manifest) {
    return jsonResponse({ error: 'No data available' }, 404);
  }

  return streamR2Object(env.DATA_BUCKET, manifest.full_key, 'application/x-sqlite3');
}

/**
 * GET /api/data/delta?since=YYYY-MM-DD
 * Streams the delta JSON file from R2.
 * If the `since` date is too old (>7 days) or in the future, serves the full database instead.
 */
export async function handleDataDelta(
  request: Request,
  env: Env,
  _params: Record<string, string>,
): Promise<Response> {
  const url = new URL(request.url);
  const since = url.searchParams.get('since');

  if (!since) {
    return jsonResponse({ error: 'Missing required query parameter: since' }, 400);
  }

  if (!isValidDate(since)) {
    return jsonResponse({ error: 'Invalid date format. Expected YYYY-MM-DD' }, 400);
  }

  const manifest = await getManifest(env.DATA_BUCKET, env.VERDICTS);
  if (!manifest) {
    return jsonResponse({ error: 'No data available' }, 404);
  }

  // If since date is in the future or same as current version, return empty delta
  const daysFromVersion = daysBetween(since, manifest.version);
  if (daysFromVersion <= 0) {
    return jsonResponse({ domains_added: [], domains_removed: [], version: manifest.version });
  }

  // If since date is too old, recommend full download
  if (daysFromVersion > MAX_DELTA_AGE_DAYS) {
    return jsonResponse(
      {
        error: 'Delta too old, use full download',
        redirect: '/api/data/full',
        version: manifest.version,
      },
      410,
    );
  }

  return streamR2Object(env.DATA_BUCKET, manifest.delta_key, 'application/json');
}

/**
 * GET /api/data/bloom
 * Streams the Bloom filter binary from R2.
 */
export async function handleDataBloom(
  _request: Request,
  env: Env,
  _params: Record<string, string>,
): Promise<Response> {
  const manifest = await getManifest(env.DATA_BUCKET, env.VERDICTS);
  if (!manifest) {
    return jsonResponse({ error: 'No data available' }, 404);
  }

  return streamR2Object(env.DATA_BUCKET, manifest.bloom_key, 'application/octet-stream');
}

/**
 * Stream an R2 object as a response with appropriate headers.
 * Reads the full body into an ArrayBuffer to ensure R2 storage is properly released.
 */
async function streamR2Object(
  bucket: R2Bucket,
  key: string,
  contentType: string,
): Promise<Response> {
  const object = await bucket.get(key);
  if (!object) {
    return jsonResponse({ error: 'Object not found' }, 404);
  }

  return new Response(object.body, {
    status: 200,
    headers: {
      'Content-Type': contentType,
      'Content-Length': object.size.toString(),
      'Cache-Control': 'public, max-age=3600',
    },
  });
}

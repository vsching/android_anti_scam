// POST /api/check — Link checker endpoint.
// Accepts { "url": "..." } or { "domain": "..." }, runs domain through
// Cache API -> KV allowlist -> heuristic engine, and returns a verdict.
//
// LOGGING POLICY: Never log full URLs — only domain hashes and verdicts.

import { extractDomain } from '../lib/domain';
import { runHeuristics, HeuristicResult } from '../lib/heuristics';
import { coalesce } from '../lib/inflight';
import { recordDiscovery } from '../lib/discovery';

/** Maximum request body size in bytes (10 KB). */
const MAX_BODY_SIZE = 10 * 1024;

/** Cache API TTL in seconds (1 hour). */
const CACHE_TTL = 3600;

/** Shape of the check response. */
interface CheckResponse {
  domain: string;
  verdict: string;
  reason: string;
  confidence: number;
  details: Record<string, unknown>;
}

/**
 * Build a Cache API key URL for a given domain.
 * Cache API requires a valid URL as key.
 */
function cacheApiKey(domain: string): string {
  return `https://cache.safeanot.internal/check/${encodeURIComponent(domain)}`;
}

/**
 * Handler for POST /api/check
 */
export async function handleCheck(
  request: Request,
  env: Env,
  _params: Record<string, string>,
): Promise<Response> {
  // 1. Reject oversized bodies
  const contentLength = request.headers.get('Content-Length');
  if (contentLength && parseInt(contentLength, 10) > MAX_BODY_SIZE) {
    return jsonResponse({ error: 'Payload too large' }, 413);
  }

  // Read body and check size
  let bodyText: string;
  try {
    bodyText = await request.text();
  } catch {
    return jsonResponse({ error: 'Failed to read request body' }, 400);
  }

  if (bodyText.length > MAX_BODY_SIZE) {
    return jsonResponse({ error: 'Payload too large' }, 413);
  }

  // 2. Parse and validate JSON
  let body: Record<string, unknown>;
  try {
    body = JSON.parse(bodyText);
  } catch {
    return jsonResponse({ error: 'Invalid JSON' }, 400);
  }

  if (typeof body !== 'object' || body === null || Array.isArray(body)) {
    return jsonResponse({ error: 'Request body must be a JSON object' }, 400);
  }

  // 3. Extract domain
  let domain: string | null = null;

  if (typeof body.url === 'string' && body.url.trim()) {
    domain = extractDomain(body.url);
    if (!domain) {
      return jsonResponse({ error: 'Invalid URL provided' }, 400);
    }
  } else if (typeof body.domain === 'string' && body.domain.trim()) {
    domain = extractDomain(body.domain);
    if (!domain) {
      return jsonResponse({ error: 'Invalid domain provided' }, 400);
    }
  } else {
    return jsonResponse(
      { error: 'Request must include "url" or "domain" field' },
      400,
    );
  }

  // 4. Check Cache API (Workers per-city cache)
  const cache = caches.default;
  const cacheKey = cacheApiKey(domain);
  const cacheRequest = new Request(cacheKey);

  const cachedResponse = await cache.match(cacheRequest);
  if (cachedResponse) {
    return new Response(cachedResponse.body, {
      status: cachedResponse.status,
      headers: { 'Content-Type': 'application/json', 'X-Cache': 'HIT' },
    });
  }

  // 5. Check KV — try allowlist prefix first, then bare domain (for discoveries)
  const kvAllowlist = await env.VERDICTS.get(`allowlist:${domain}`);
  const kvDiscovery = kvAllowlist === null ? await env.VERDICTS.get(domain) : null;
  const kvValue = kvAllowlist ?? kvDiscovery;
  if (kvValue !== null) {
    const parsed = (() => { try { return JSON.parse(kvValue); } catch { return null; } })();
    const kvResult: CheckResponse = {
      domain,
      verdict: parsed?.verdict ?? 'safe',
      reason: parsed?.reason ?? 'Domain found in verified allowlist',
      confidence: parsed?.confidence ?? 1.0,
      details: { check_type: kvAllowlist ? 'allowlist' : 'discovery', source: 'kv' },
    };

    const response = jsonResponse(kvResult, 200);
    // Cache in Cache API
    const cacheResponse = new Response(JSON.stringify(kvResult), {
      status: 200,
      headers: {
        'Content-Type': 'application/json',
        'Cache-Control': `s-maxage=${CACHE_TTL}`,
      },
    });
    await cache.put(cacheRequest, cacheResponse);

    return response;
  }

  // 6. Run heuristic engine with in-flight coalescing
  const heuristicResult = await coalesce(domain, async () => {
    return runHeuristics(domain!);
  });

  // 7. Build response
  const checkResponse: CheckResponse = {
    domain,
    verdict: heuristicResult.verdict,
    reason: heuristicResult.reason,
    confidence: heuristicResult.confidence,
    details: heuristicResult.details,
  };

  // 8. Cache result in Cache API (NOT in KV)
  const responseToCacheBody = JSON.stringify(checkResponse);
  const responseToCache = new Response(responseToCacheBody, {
    status: 200,
    headers: {
      'Content-Type': 'application/json',
      'Cache-Control': `s-maxage=${CACHE_TTL}`,
    },
  });
  await cache.put(cacheRequest, responseToCache);

  // 9. If suspicious or dangerous, write to D1 pending_discoveries
  if (
    heuristicResult.verdict === 'suspicious' ||
    heuristicResult.verdict === 'dangerous'
  ) {
    // Fire and forget — don't block the response
    try {
      await recordDiscovery(env.DB, domain, heuristicResult.verdict, heuristicResult.reason);
    } catch {
      // Silently swallow D1 write errors — verdict is still returned
    }
  }

  // 10. Return verdict
  return new Response(JSON.stringify(checkResponse), {
    status: 200,
    headers: { 'Content-Type': 'application/json', 'X-Cache': 'MISS' },
  });
}

function jsonResponse(data: unknown, status: number): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

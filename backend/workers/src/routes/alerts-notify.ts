// POST /api/alerts/notify — Pipeline-authenticated endpoint to trigger FCM notifications.
// Accepts { alert_id: string }, looks up the alert in D1, and sends FCM topic messages.
// Authenticated via X-Pipeline-Key header with timing-safe comparison.
// Rate limited to 10 requests per minute via KV counter.

import { sendFcmTopicMessage } from '../lib/firebase-messaging';

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

/** Rate limit: max 10 requests per 60-second window. */
const RATE_LIMIT_MAX = 10;
const RATE_LIMIT_WINDOW = 60; // seconds

/**
 * Timing-safe comparison of two strings using crypto.subtle.timingSafeEqual.
 * Returns false if either string is empty.
 */
async function timingSafeEqual(a: string, b: string): Promise<boolean> {
  if (a.length === 0 || b.length === 0) {
    return false;
  }

  const encoder = new TextEncoder();
  const aBytes = encoder.encode(a);
  const bBytes = encoder.encode(b);

  // Lengths must match for timingSafeEqual; if not, compare against self to avoid timing leak
  if (aBytes.byteLength !== bBytes.byteLength) {
    // Compare a against itself to consume constant time, then return false
    // This prevents length-based timing attacks
    crypto.subtle.timingSafeEqual(aBytes, aBytes);
    return false;
  }

  return crypto.subtle.timingSafeEqual(aBytes, bBytes);
}

/**
 * KV-based rate limiter. Uses a counter key with TTL.
 * Returns true if the request is allowed, false if rate limited.
 */
async function checkRateLimit(kv: KVNamespace, clientIp: string): Promise<boolean> {
  const key = `ratelimit:notify:${clientIp}`;
  const current = await kv.get(key, 'text');
  const count = current ? parseInt(current, 10) : 0;

  if (count >= RATE_LIMIT_MAX) {
    return false;
  }

  await kv.put(key, String(count + 1), { expirationTtl: RATE_LIMIT_WINDOW });
  return true;
}

/**
 * Map alert region to FCM topic names.
 */
function regionToTopics(region: string | null): string[] {
  switch (region) {
    case 'MY':
      return ['scam_alerts_MY'];
    case 'SG':
      return ['scam_alerts_SG'];
    case 'both':
      return ['scam_alerts_MY', 'scam_alerts_SG'];
    default:
      // Default to both regions if region is unknown
      return ['scam_alerts_MY', 'scam_alerts_SG'];
  }
}

/**
 * Handler for POST /api/alerts/notify
 */
export async function handleAlertsNotify(
  request: Request,
  env: Env,
  _params: Record<string, string>,
): Promise<Response> {
  // Authenticate via X-Pipeline-Key header
  const pipelineKey = request.headers.get('X-Pipeline-Key');
  if (!pipelineKey) {
    return jsonResponse({ error: 'Missing authentication' }, 401);
  }

  const isValid = await timingSafeEqual(pipelineKey, env.PIPELINE_SECRET);
  if (!isValid) {
    return jsonResponse({ error: 'Invalid authentication' }, 401);
  }

  // Rate limit by client IP
  const clientIp = request.headers.get('CF-Connecting-IP') || 'unknown';
  const allowed = await checkRateLimit(env.VERDICTS, clientIp);
  if (!allowed) {
    return jsonResponse({ error: 'Rate limit exceeded' }, 429);
  }

  // Parse request body
  let body: { alert_id?: string };
  try {
    body = await request.json<{ alert_id?: string }>();
  } catch {
    return jsonResponse({ error: 'Invalid JSON body' }, 400);
  }

  if (!body.alert_id || typeof body.alert_id !== 'string') {
    return jsonResponse({ error: 'Missing or invalid alert_id' }, 400);
  }

  // Look up alert in D1
  const result = await env.DB.prepare(
    `SELECT * FROM alerts WHERE id = ? LIMIT 1`,
  )
    .bind(body.alert_id)
    .all<Alert>();

  const alert = result.results?.[0] ?? null;

  if (!alert) {
    return jsonResponse({ error: 'Alert not found' }, 404);
  }

  // Determine target topics
  const topics = regionToTopics(alert.region);

  // Send FCM notification to each topic
  const deepLink = `https://safeanot.com/alert/${alert.id}`;
  const notificationBody = alert.description
    ? alert.description.substring(0, 200)
    : `New scam alert: ${alert.title}`;

  const results: boolean[] = [];
  for (const topic of topics) {
    const sent = await sendFcmTopicMessage(env, topic, alert.title, notificationBody, {
      alert_id: alert.id,
      deep_link: deepLink,
    });
    results.push(sent);
  }

  return jsonResponse({ sent: true, topics });
}

function jsonResponse(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

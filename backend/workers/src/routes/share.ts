// POST /api/score/share — Share event analytics endpoint.
// Accepts a batch of share events from the Android client.
//
// Abuse controls:
// - Content-Length + post-read byte-length cap (10 KB)
// - Max 50 events per batch
// - Strict enum allowlists for share_type and platform
// - Timestamp validation (not in the future, not older than 30 days)
// - Per-device rate limiting (100/day via device_id)
//
// LOGGING POLICY: Only log anonymised counts, never content IDs or device IDs.

/** Maximum request body size in bytes (10 KB). */
const MAX_BODY_SIZE = 10 * 1024;

/** Maximum events per batch. */
const MAX_EVENTS_PER_BATCH = 50;

/** Maximum events per device per day. */
const DAILY_RATE_LIMIT = 100;

/** Valid share types. */
const VALID_SHARE_TYPES = new Set([
  'VERDICT',
  'SCORE',
  'ALERT',
  'WARNING_TEMPLATE',
  'RESCUE_CARD',
]);

/** Valid platforms. */
const VALID_PLATFORMS = new Set(['WHATSAPP', 'GENERIC']);

/** Maximum age for timestamps (30 days in ms). */
const MAX_TIMESTAMP_AGE_MS = 30 * 24 * 60 * 60 * 1000;

/** Maximum content_id length. */
const MAX_CONTENT_ID_LENGTH = 512;

/** Maximum device_id length. */
const MAX_DEVICE_ID_LENGTH = 128;

/** Regex for allowed content_id characters (domain-safe). */
const CONTENT_ID_PATTERN = /^[a-zA-Z0-9._\-:\/]+$/;

interface ShareEventInput {
  share_type: string;
  content_id: string;
  platform: string;
  timestamp: number;
}

interface ShareBatchInput {
  device_id: string;
  events: ShareEventInput[];
}

function jsonResponse(data: unknown, status: number): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

/**
 * Handler for POST /api/score/share
 */
export async function handleShare(
  request: Request,
  env: Env,
  _params: Record<string, string>,
): Promise<Response> {
  // 1. Reject oversized bodies via Content-Length header
  const contentLength = request.headers.get('Content-Length');
  if (contentLength && parseInt(contentLength, 10) > MAX_BODY_SIZE) {
    return jsonResponse({ error: 'Payload too large' }, 413);
  }

  // 2. Read body and check actual byte length
  let bodyText: string;
  try {
    bodyText = await request.text();
  } catch {
    return jsonResponse({ error: 'Failed to read request body' }, 400);
  }

  if (new TextEncoder().encode(bodyText).length > MAX_BODY_SIZE) {
    return jsonResponse({ error: 'Payload too large' }, 413);
  }

  // 3. Parse JSON
  let body: ShareBatchInput;
  try {
    body = JSON.parse(bodyText);
  } catch {
    return jsonResponse({ error: 'Invalid JSON' }, 400);
  }

  if (typeof body !== 'object' || body === null || Array.isArray(body)) {
    return jsonResponse({ error: 'Request body must be a JSON object' }, 400);
  }

  // 4. Validate device_id
  if (typeof body.device_id !== 'string' || !body.device_id.trim()) {
    return jsonResponse({ error: 'Missing or invalid device_id' }, 400);
  }

  if (body.device_id.length > MAX_DEVICE_ID_LENGTH) {
    return jsonResponse({ error: 'device_id too long' }, 400);
  }

  // 5. Validate events array
  if (!Array.isArray(body.events)) {
    return jsonResponse({ error: 'events must be an array' }, 400);
  }

  if (body.events.length === 0) {
    return jsonResponse({ error: 'events array must not be empty' }, 400);
  }

  if (body.events.length > MAX_EVENTS_PER_BATCH) {
    return jsonResponse(
      { error: `Maximum ${MAX_EVENTS_PER_BATCH} events per batch` },
      400,
    );
  }

  // 6. Validate each event
  const now = Date.now();
  const oldestAllowed = now - MAX_TIMESTAMP_AGE_MS;

  for (let i = 0; i < body.events.length; i++) {
    const event = body.events[i];

    if (typeof event !== 'object' || event === null || Array.isArray(event)) {
      return jsonResponse({ error: `events[${i}] must be an object` }, 400);
    }

    if (!VALID_SHARE_TYPES.has(event.share_type)) {
      return jsonResponse(
        { error: `events[${i}].share_type is invalid` },
        400,
      );
    }

    if (typeof event.content_id !== 'string' || !event.content_id.trim()) {
      return jsonResponse(
        { error: `events[${i}].content_id is required` },
        400,
      );
    }

    if (event.content_id.length > MAX_CONTENT_ID_LENGTH) {
      return jsonResponse(
        { error: `events[${i}].content_id too long` },
        400,
      );
    }

    if (!CONTENT_ID_PATTERN.test(event.content_id)) {
      return jsonResponse(
        { error: `events[${i}].content_id contains invalid characters` },
        400,
      );
    }

    if (!VALID_PLATFORMS.has(event.platform)) {
      return jsonResponse(
        { error: `events[${i}].platform is invalid` },
        400,
      );
    }

    if (typeof event.timestamp !== 'number' || !Number.isFinite(event.timestamp)) {
      return jsonResponse(
        { error: `events[${i}].timestamp must be a finite number` },
        400,
      );
    }

    // Timestamp must not be in the future (with 5 min grace) or too old
    if (event.timestamp > now + 5 * 60 * 1000) {
      return jsonResponse(
        { error: `events[${i}].timestamp is in the future` },
        400,
      );
    }

    if (event.timestamp < oldestAllowed) {
      return jsonResponse(
        { error: `events[${i}].timestamp is too old` },
        400,
      );
    }
  }

  // 7. Per-device rate limiting (100/day)
  const deviceId = body.device_id.trim();
  const oneDayAgoISO = new Date(now - 24 * 60 * 60 * 1000).toISOString();

  let todayCount: number;
  try {
    const result = await env.DB.prepare(
      'SELECT COUNT(*) as cnt FROM share_events WHERE device_id = ? AND created_at >= ?',
    )
      .bind(deviceId, oneDayAgoISO)
      .first<{ cnt: number }>();
    todayCount = result?.cnt ?? 0;
  } catch {
    // If D1 query fails, fail closed to prevent abuse
    return jsonResponse({ error: 'Service temporarily unavailable' }, 503);
  }

  if (todayCount + body.events.length > DAILY_RATE_LIMIT) {
    return jsonResponse(
      { error: 'Daily share event limit exceeded' },
      429,
    );
  }

  // 8. Insert events into D1
  try {
    const stmt = env.DB.prepare(
      `INSERT INTO share_events (device_id, share_type, content_id, platform, client_timestamp)
       VALUES (?, ?, ?, ?, ?)`,
    );

    const batch = body.events.map((event) =>
      stmt.bind(
        deviceId,
        event.share_type,
        event.content_id,
        event.platform,
        new Date(event.timestamp).toISOString(),
      ),
    );

    await env.DB.batch(batch);
  } catch {
    return jsonResponse({ error: 'Failed to store events' }, 500);
  }

  return jsonResponse({ accepted: body.events.length }, 200);
}

// Guardian pairing API routes.
// Handles pairing code generation/claiming, ward/guardian listing, and unpairing.
//
// LOGGING POLICY: Never log device IDs or pairing codes.

import { verifyDeviceHmac } from '../middleware/guardian-auth';
import { sendFcmNotification } from '../lib/fcm';

/** Maximum request body size in bytes (10 KB). */
const MAX_BODY_SIZE = 10 * 1024;

/** Maximum heartbeat history days that can be requested. */
const MAX_HEARTBEAT_DAYS = 30;

/** Default heartbeat history days. */
const DEFAULT_HEARTBEAT_DAYS = 7;

/** Pairing code TTL in seconds (15 minutes). */
const CODE_TTL_SECONDS = 15 * 60;

/** Maximum guardians per ward. */
const MAX_GUARDIANS_PER_WARD = 3;

/** Rate limit: max pairing code generations per device per hour. */
const GENERATE_RATE_LIMIT = 10;

/** Maximum guardian alerts per ward per day (anti-spam). */
const MAX_ALERTS_PER_DAY = 3;

/** Rate limit: max 1 help request per ward per hour. */
const HELP_REQUEST_RATE_LIMIT_SECONDS = 3600;

/** Score drop threshold that triggers an alert. */
const SCORE_DROP_THRESHOLD = 20;

/** Security score RED band threshold. */
const RED_BAND_THRESHOLD = 50;

/** Characters used for pairing codes (no 0/O/1/I/L). */
const CODE_CHARS = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';

/** Length of generated pairing codes. */
const CODE_LENGTH = 6;

function jsonResponse(data: unknown, status: number): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

/**
 * Read and validate the JSON body from the request.
 * Returns [bodyText, parsedBody] on success, or a Response on failure.
 */
async function readBody(
  request: Request,
): Promise<[string, Record<string, unknown>] | Response> {
  const contentLength = request.headers.get('Content-Length');
  if (contentLength && parseInt(contentLength, 10) > MAX_BODY_SIZE) {
    return jsonResponse({ error: 'Payload too large' }, 413);
  }

  let bodyText: string;
  try {
    bodyText = await request.text();
  } catch {
    return jsonResponse({ error: 'Failed to read request body' }, 400);
  }

  if (new TextEncoder().encode(bodyText).length > MAX_BODY_SIZE) {
    return jsonResponse({ error: 'Payload too large' }, 413);
  }

  let body: Record<string, unknown>;
  try {
    body = JSON.parse(bodyText);
  } catch {
    return jsonResponse({ error: 'Invalid JSON' }, 400);
  }

  if (typeof body !== 'object' || body === null || Array.isArray(body)) {
    return jsonResponse({ error: 'Request body must be a JSON object' }, 400);
  }

  return [bodyText, body];
}

/** Generate a random pairing code using the safe character set. */
function generateCode(): string {
  const array = new Uint8Array(CODE_LENGTH);
  crypto.getRandomValues(array);
  let code = '';
  for (let i = 0; i < CODE_LENGTH; i++) {
    code += CODE_CHARS[array[i] % CODE_CHARS.length];
  }
  return code;
}

/**
 * POST /api/guardian/pair/generate
 * Generates a 6-char pairing code for a ward device.
 */
export async function handleGeneratePairingCode(
  request: Request,
  env: Env,
  _params: Record<string, string>,
): Promise<Response> {
  const result = await readBody(request);
  if (result instanceof Response) return result;
  const [bodyText, body] = result;

  // Verify HMAC
  const hmacError = await verifyDeviceHmac(request, bodyText, env);
  if (hmacError) return hmacError;

  // Validate device_id
  const deviceId = body.device_id;
  if (typeof deviceId !== 'string' || !deviceId.trim()) {
    return jsonResponse({ error: 'Missing or invalid device_id' }, 400);
  }

  const now = Math.floor(Date.now() / 1000);

  // Rate limit: max 10 codes per device per hour
  const oneHourAgo = now - 3600;
  const rateResult = await env.DB.prepare(
    'SELECT COUNT(*) as cnt FROM guardian_pairing_codes WHERE ward_device_id = ? AND created_at >= ?',
  )
    .bind(deviceId, oneHourAgo)
    .first<{ cnt: number }>();

  if ((rateResult?.cnt ?? 0) >= GENERATE_RATE_LIMIT) {
    return jsonResponse({ error: 'Rate limit exceeded. Try again later.' }, 429);
  }

  // Generate unique code (retry on collision)
  let code: string;
  let attempts = 0;
  do {
    code = generateCode();
    attempts++;
    if (attempts > 10) {
      return jsonResponse({ error: 'Failed to generate unique code' }, 500);
    }
    const existing = await env.DB.prepare(
      'SELECT code FROM guardian_pairing_codes WHERE code = ?',
    )
      .bind(code)
      .first();
    if (!existing) break;
  } while (true);

  const expiresAt = now + CODE_TTL_SECONDS;

  const wardDisplayName =
    typeof body.label === 'string' ? body.label : '';

  await env.DB.prepare(
    'INSERT INTO guardian_pairing_codes (code, ward_device_id, ward_display_name, created_at, expires_at, claimed) VALUES (?, ?, ?, ?, ?, 0)',
  )
    .bind(code, deviceId, wardDisplayName, now, expiresAt)
    .run();

  return jsonResponse({ code, expires_at: expiresAt }, 200);
}

/**
 * POST /api/guardian/pair/claim
 * Claims a pairing code, creating a guardian-ward relationship.
 */
export async function handleClaimPairingCode(
  request: Request,
  env: Env,
  _params: Record<string, string>,
): Promise<Response> {
  const result = await readBody(request);
  if (result instanceof Response) return result;
  const [bodyText, body] = result;

  // Verify HMAC
  const hmacError = await verifyDeviceHmac(request, bodyText, env);
  if (hmacError) return hmacError;

  const guardianDeviceId = body.device_id;
  if (typeof guardianDeviceId !== 'string' || !guardianDeviceId.trim()) {
    return jsonResponse({ error: 'Missing or invalid device_id' }, 400);
  }

  const code = body.code;
  if (typeof code !== 'string' || !code.trim()) {
    return jsonResponse({ error: 'Missing or invalid code' }, 400);
  }

  const guardianDisplayName =
    typeof body.display_name === 'string' ? body.display_name : '';

  // Look up the pairing code
  const pairingCode = await env.DB.prepare(
    'SELECT * FROM guardian_pairing_codes WHERE code = ?',
  )
    .bind(code.toUpperCase())
    .first<{
      code: string;
      ward_device_id: string;
      ward_display_name: string;
      created_at: number;
      expires_at: number;
      claimed: number;
    }>();

  if (!pairingCode) {
    return jsonResponse({ error: 'Invalid pairing code' }, 404);
  }

  const now = Math.floor(Date.now() / 1000);

  if (pairingCode.claimed || pairingCode.expires_at < now) {
    return jsonResponse({ error: 'Pairing code expired or already used' }, 410);
  }

  // Cannot pair with yourself
  if (pairingCode.ward_device_id === guardianDeviceId) {
    return jsonResponse({ error: 'Cannot pair with yourself' }, 400);
  }

  // Check max guardians per ward
  const guardianCount = await env.DB.prepare(
    'SELECT COUNT(*) as cnt FROM guardian_pairings WHERE ward_device_id = ?',
  )
    .bind(pairingCode.ward_device_id)
    .first<{ cnt: number }>();

  if ((guardianCount?.cnt ?? 0) >= MAX_GUARDIANS_PER_WARD) {
    return jsonResponse(
      { error: `Ward already has maximum ${MAX_GUARDIANS_PER_WARD} guardians` },
      409,
    );
  }

  // Check for existing pairing
  const existingPairing = await env.DB.prepare(
    'SELECT id FROM guardian_pairings WHERE ward_device_id = ? AND guardian_device_id = ?',
  )
    .bind(pairingCode.ward_device_id, guardianDeviceId)
    .first();

  if (existingPairing) {
    // Mark code as claimed even if pairing exists
    await env.DB.prepare(
      'UPDATE guardian_pairing_codes SET claimed = 1 WHERE code = ?',
    )
      .bind(code.toUpperCase())
      .run();
    return jsonResponse({ error: 'Pairing already exists' }, 409);
  }

  // Create pairing and mark code as claimed atomically
  const batchResults = await env.DB.batch([
    env.DB.prepare(
      'UPDATE guardian_pairing_codes SET claimed = 1 WHERE code = ?',
    ).bind(code.toUpperCase()),
    env.DB.prepare(
      `INSERT INTO guardian_pairings (ward_device_id, guardian_device_id, ward_display_name, guardian_display_name, created_at)
       VALUES (?, ?, ?, ?, ?)`,
    ).bind(pairingCode.ward_device_id, guardianDeviceId, pairingCode.ward_display_name || '', guardianDisplayName, now),
  ]);

  // Retrieve the newly created pairing to return a full DTO
  const insertMeta = batchResults[1]?.meta;
  const newPairingId = insertMeta?.last_row_id;

  return jsonResponse({
    id: newPairingId,
    ward_device_id: pairingCode.ward_device_id,
    guardian_device_id: guardianDeviceId,
    ward_display_name: pairingCode.ward_display_name || '',
    guardian_display_name: guardianDisplayName,
    created_at: now,
    status: 'paired',
  }, 200);
}

/**
 * GET /api/guardian/wards?device_id=...
 * Returns ward list with latest heartbeat for a guardian.
 */
export async function handleGetWards(
  request: Request,
  env: Env,
  _params: Record<string, string>,
): Promise<Response> {
  const url = new URL(request.url);
  const deviceId = url.searchParams.get('device_id');

  if (!deviceId) {
    return jsonResponse({ error: 'Missing device_id query parameter' }, 400);
  }

  // Verify HMAC (for GET, use device_id as body)
  const hmacError = await verifyDeviceHmac(request, deviceId, env);
  if (hmacError) return hmacError;

  // Get all wards this device is a guardian of
  const pairings = await env.DB.prepare(
    `SELECT p.id, p.ward_device_id, p.ward_display_name, p.created_at,
            h.security_score, h.secured_items, h.total_items, h.play_protect_enabled, h.timestamp as heartbeat_timestamp
     FROM guardian_pairings p
     LEFT JOIN guardian_heartbeats h ON h.ward_device_id = p.ward_device_id
       AND h.timestamp = (SELECT MAX(h2.timestamp) FROM guardian_heartbeats h2 WHERE h2.ward_device_id = p.ward_device_id)
     WHERE p.guardian_device_id = ?
     ORDER BY p.created_at DESC`,
  )
    .bind(deviceId)
    .all();

  return jsonResponse(pairings.results ?? [], 200);
}

/**
 * GET /api/guardian/guardians?device_id=...
 * Returns guardian list for a ward.
 */
export async function handleGetGuardians(
  request: Request,
  env: Env,
  _params: Record<string, string>,
): Promise<Response> {
  const url = new URL(request.url);
  const deviceId = url.searchParams.get('device_id');

  if (!deviceId) {
    return jsonResponse({ error: 'Missing device_id query parameter' }, 400);
  }

  // Verify HMAC (for GET, use device_id as body)
  const hmacError = await verifyDeviceHmac(request, deviceId, env);
  if (hmacError) return hmacError;

  const pairings = await env.DB.prepare(
    `SELECT id, guardian_device_id, guardian_display_name, created_at
     FROM guardian_pairings
     WHERE ward_device_id = ?
     ORDER BY created_at DESC`,
  )
    .bind(deviceId)
    .all();

  return jsonResponse(pairings.results ?? [], 200);
}

/**
 * DELETE /api/guardian/pair/:pairingId
 * Deletes a pairing. Requester must be either ward or guardian.
 */
export async function handleDeletePairing(
  request: Request,
  env: Env,
  params: Record<string, string>,
): Promise<Response> {
  const result = await readBody(request);
  if (result instanceof Response) return result;
  const [bodyText, body] = result;

  // Verify HMAC
  const hmacError = await verifyDeviceHmac(request, bodyText, env);
  if (hmacError) return hmacError;

  const deviceId = body.device_id;
  if (typeof deviceId !== 'string' || !deviceId.trim()) {
    return jsonResponse({ error: 'Missing or invalid device_id' }, 400);
  }

  const pairingId = parseInt(params.pairingId, 10);
  if (isNaN(pairingId)) {
    return jsonResponse({ error: 'Invalid pairing ID' }, 400);
  }

  // Verify the requester is part of this pairing
  const pairing = await env.DB.prepare(
    'SELECT * FROM guardian_pairings WHERE id = ?',
  )
    .bind(pairingId)
    .first<{
      id: number;
      ward_device_id: string;
      guardian_device_id: string;
    }>();

  if (!pairing) {
    return jsonResponse({ error: 'Pairing not found' }, 404);
  }

  if (pairing.ward_device_id !== deviceId && pairing.guardian_device_id !== deviceId) {
    return jsonResponse({ error: 'Not authorized to delete this pairing' }, 403);
  }

  await env.DB.prepare('DELETE FROM guardian_pairings WHERE id = ?')
    .bind(pairingId)
    .run();

  return jsonResponse({ status: 'deleted' }, 200);
}

/**
 * POST /api/guardian/heartbeat
 * Accepts a security score heartbeat from a ward device.
 * Validates the device has guardian pairings and cleans up old heartbeats.
 */
export async function handlePostHeartbeat(
  request: Request,
  env: Env,
  _params: Record<string, string>,
): Promise<Response> {
  const result = await readBody(request);
  if (result instanceof Response) return result;
  const [bodyText, body] = result;

  // Verify HMAC
  const hmacError = await verifyDeviceHmac(request, bodyText, env);
  if (hmacError) return hmacError;

  // Validate required fields
  const deviceId = body.device_id;
  if (typeof deviceId !== 'string' || !deviceId.trim()) {
    return jsonResponse({ error: 'Missing or invalid device_id' }, 400);
  }

  const securityScore = body.security_score;
  if (typeof securityScore !== 'number' || securityScore < 0 || securityScore > 100) {
    return jsonResponse({ error: 'Missing or invalid security_score (0-100)' }, 400);
  }

  const securedItems = body.secured_items;
  if (typeof securedItems !== 'number' || securedItems < 0) {
    return jsonResponse({ error: 'Missing or invalid secured_items' }, 400);
  }

  const totalItems = body.total_items;
  if (typeof totalItems !== 'number' || totalItems < 0) {
    return jsonResponse({ error: 'Missing or invalid total_items' }, 400);
  }

  const playProtectEnabled = body.play_protect_enabled;
  if (typeof playProtectEnabled !== 'boolean') {
    return jsonResponse({ error: 'Missing or invalid play_protect_enabled' }, 400);
  }

  const timestamp = body.timestamp;
  if (typeof timestamp !== 'number' || timestamp <= 0) {
    return jsonResponse({ error: 'Missing or invalid timestamp' }, 400);
  }

  // Verify device has at least one guardian pairing (device must be a ward)
  const pairingCount = await env.DB.prepare(
    'SELECT COUNT(*) as cnt FROM guardian_pairings WHERE ward_device_id = ?',
  )
    .bind(deviceId)
    .first<{ cnt: number }>();

  if (!pairingCount || pairingCount.cnt === 0) {
    return jsonResponse({ error: 'Device has no guardian pairings' }, 403);
  }

  // Query previous heartbeat before inserting the new one
  const prevHeartbeat = await env.DB.prepare(
    `SELECT security_score, play_protect_enabled
     FROM guardian_heartbeats
     WHERE ward_device_id = ?
     ORDER BY timestamp DESC
     LIMIT 1`,
  )
    .bind(deviceId)
    .first<{ security_score: number; play_protect_enabled: number }>();

  // Insert heartbeat
  await env.DB.prepare(
    `INSERT INTO guardian_heartbeats (ward_device_id, security_score, secured_items, total_items, play_protect_enabled, timestamp)
     VALUES (?, ?, ?, ?, ?, ?)`,
  )
    .bind(deviceId, securityScore, securedItems, totalItems, playProtectEnabled ? 1 : 0, timestamp)
    .run();

  // Cleanup heartbeats older than 30 days
  const cutoff = Math.floor(Date.now() / 1000) - MAX_HEARTBEAT_DAYS * 24 * 60 * 60;
  await env.DB.prepare(
    'DELETE FROM guardian_heartbeats WHERE timestamp < ?',
  )
    .bind(cutoff)
    .run();

  // Check alert conditions (only if there was a previous heartbeat)
  if (prevHeartbeat) {
    const alertReasons: string[] = [];

    const scoreDrop = prevHeartbeat.security_score - securityScore;
    if (scoreDrop >= SCORE_DROP_THRESHOLD) {
      alertReasons.push(`Security score dropped by ${scoreDrop} points (${prevHeartbeat.security_score}% → ${securityScore}%)`);
    }

    if (prevHeartbeat.play_protect_enabled === 1 && !playProtectEnabled) {
      alertReasons.push('Play Protect was disabled');
    }

    if (securityScore < RED_BAND_THRESHOLD && prevHeartbeat.security_score >= RED_BAND_THRESHOLD) {
      alertReasons.push(`Security score entered RED zone (${securityScore}%)`);
    }

    if (alertReasons.length > 0) {
      // Check daily alert limit
      const today = new Date().toISOString().slice(0, 10); // YYYY-MM-DD
      const dailyAlerts = await env.DB.prepare(
        'SELECT alert_count FROM guardian_daily_alerts WHERE ward_device_id = ? AND alert_date = ?',
      )
        .bind(deviceId, today)
        .first<{ alert_count: number }>();

      const currentCount = dailyAlerts?.alert_count ?? 0;

      if (currentCount < MAX_ALERTS_PER_DAY) {
        // Increment daily alert counter
        await env.DB.prepare(
          `INSERT INTO guardian_daily_alerts (ward_device_id, alert_date, alert_count)
           VALUES (?, ?, 1)
           ON CONFLICT(ward_device_id, alert_date)
           DO UPDATE SET alert_count = alert_count + 1`,
        )
          .bind(deviceId, today)
          .run();

        // Get ward display name from any pairing
        const wardInfo = await env.DB.prepare(
          'SELECT ward_display_name FROM guardian_pairings WHERE ward_device_id = ? LIMIT 1',
        )
          .bind(deviceId)
          .first<{ ward_display_name: string }>();

        const wardName = wardInfo?.ward_display_name || 'Your ward';
        const alertReason = alertReasons[0];
        const title = `${wardName} — Security Alert`;
        const body = alertReason;

        // Get all guardians' FCM tokens
        const guardianTokens = await env.DB.prepare(
          `SELECT t.fcm_token
           FROM guardian_pairings p
           JOIN guardian_fcm_tokens t ON t.device_id = p.guardian_device_id
           WHERE p.ward_device_id = ?`,
        )
          .bind(deviceId)
          .all<{ fcm_token: string }>();

        const tokens = guardianTokens.results ?? [];
        const data: Record<string, string> = {
          type: 'guardian_alert',
          ward_device_id: deviceId,
          alert_reason: alertReason,
          security_score: String(securityScore),
        };

        // Send notifications (fire-and-forget, don't block response)
        for (const row of tokens) {
          await sendFcmNotification(env, row.fcm_token, title, body, data);
        }
      }
    }
  }

  return jsonResponse({ status: 'ok' }, 200);
}

/**
 * GET /api/guardian/wards/:deviceId/heartbeats?days=7
 * Returns heartbeat history for a ward device.
 *
 * TODO: In production, HMAC should sign both ward_device_id and guardian_device_id
 * to prevent a guardian from requesting heartbeats for wards they are not paired with
 * by tampering with the query parameter.
 */
export async function handleGetWardHeartbeats(
  request: Request,
  env: Env,
  params: Record<string, string>,
): Promise<Response> {
  const wardDeviceId = params.deviceId;
  if (!wardDeviceId) {
    return jsonResponse({ error: 'Missing deviceId path parameter' }, 400);
  }

  const url = new URL(request.url);

  // Verify HMAC — for GET, we use the wardDeviceId as the body for HMAC
  const hmacError = await verifyDeviceHmac(request, wardDeviceId, env);
  if (hmacError) return hmacError;

  // Authorization: verify the requester is a guardian of this ward
  const guardianDeviceId = url.searchParams.get('guardian_device_id');
  if (!guardianDeviceId) {
    return jsonResponse({ error: 'Missing guardian_device_id query parameter' }, 400);
  }

  const pairingExists = await env.DB.prepare(
    'SELECT id FROM guardian_pairings WHERE ward_device_id = ? AND guardian_device_id = ?',
  )
    .bind(wardDeviceId, guardianDeviceId)
    .first();

  if (!pairingExists) {
    return jsonResponse({ error: 'Not authorized to view this ward\'s heartbeats' }, 403);
  }

  // Parse days parameter
  const daysParam = url.searchParams.get('days');
  let days = DEFAULT_HEARTBEAT_DAYS;
  if (daysParam !== null) {
    days = parseInt(daysParam, 10);
    if (isNaN(days) || days < 1) {
      days = DEFAULT_HEARTBEAT_DAYS;
    } else if (days > MAX_HEARTBEAT_DAYS) {
      days = MAX_HEARTBEAT_DAYS;
    }
  }

  const since = Math.floor(Date.now() / 1000) - days * 24 * 60 * 60;

  const heartbeats = await env.DB.prepare(
    `SELECT security_score, secured_items, total_items, play_protect_enabled, timestamp
     FROM guardian_heartbeats
     WHERE ward_device_id = ? AND timestamp >= ?
     ORDER BY timestamp DESC`,
  )
    .bind(wardDeviceId, since)
    .all();

  return jsonResponse(heartbeats.results ?? [], 200);
}

/**
 * POST /api/guardian/fcm-token
 * Registers or updates an FCM token for a device.
 */
export async function handleRegisterFcmToken(
  request: Request,
  env: Env,
  _params: Record<string, string>,
): Promise<Response> {
  const result = await readBody(request);
  if (result instanceof Response) return result;
  const [bodyText, body] = result;

  // Verify HMAC
  const hmacError = await verifyDeviceHmac(request, bodyText, env);
  if (hmacError) return hmacError;

  const deviceId = body.device_id;
  if (typeof deviceId !== 'string' || !deviceId.trim()) {
    return jsonResponse({ error: 'Missing or invalid device_id' }, 400);
  }

  const fcmToken = body.fcm_token;
  if (typeof fcmToken !== 'string' || !fcmToken.trim()) {
    return jsonResponse({ error: 'Missing or invalid fcm_token' }, 400);
  }

  const now = Math.floor(Date.now() / 1000);

  await env.DB.prepare(
    `INSERT INTO guardian_fcm_tokens (device_id, fcm_token, updated_at)
     VALUES (?, ?, ?)
     ON CONFLICT(device_id)
     DO UPDATE SET fcm_token = excluded.fcm_token, updated_at = excluded.updated_at`,
  )
    .bind(deviceId, fcmToken, now)
    .run();

  return jsonResponse({ status: 'ok' }, 200);
}

/**
 * POST /api/guardian/help-request
 * Ward sends a "Help Me Fix This" request to all their guardians.
 * Rate limited to 1 request per hour per ward device.
 */
export async function handleHelpRequest(
  request: Request,
  env: Env,
  _params: Record<string, string>,
): Promise<Response> {
  const result = await readBody(request);
  if (result instanceof Response) return result;
  const [bodyText, body] = result;

  // Verify HMAC
  const hmacError = await verifyDeviceHmac(request, bodyText, env);
  if (hmacError) return hmacError;

  // Validate required fields
  const deviceId = body.device_id;
  if (typeof deviceId !== 'string' || !deviceId.trim()) {
    return jsonResponse({ error: 'Missing or invalid device_id' }, 400);
  }

  const securityScore = body.security_score;
  if (typeof securityScore !== 'number' || securityScore < 0 || securityScore > 100) {
    return jsonResponse({ error: 'Missing or invalid security_score (0-100)' }, 400);
  }

  const unfixedItems = body.unfixed_items;
  if (!Array.isArray(unfixedItems)) {
    return jsonResponse({ error: 'Missing or invalid unfixed_items (must be array)' }, 400);
  }

  // Validate each unfixed item is a string
  for (const item of unfixedItems) {
    if (typeof item !== 'string') {
      return jsonResponse({ error: 'unfixed_items must contain only strings' }, 400);
    }
  }

  // Verify device has at least one guardian pairing (device must be a ward)
  const pairingCount = await env.DB.prepare(
    'SELECT COUNT(*) as cnt FROM guardian_pairings WHERE ward_device_id = ?',
  )
    .bind(deviceId)
    .first<{ cnt: number }>();

  if (!pairingCount || pairingCount.cnt === 0) {
    return jsonResponse({ error: 'Device has no guardian pairings' }, 403);
  }

  // Rate limit: 1 help request per hour
  const now = Math.floor(Date.now() / 1000);
  const oneHourAgo = now - HELP_REQUEST_RATE_LIMIT_SECONDS;
  const lastRequest = await env.DB.prepare(
    'SELECT created_at FROM guardian_help_requests WHERE ward_device_id = ? AND created_at >= ? ORDER BY created_at DESC LIMIT 1',
  )
    .bind(deviceId, oneHourAgo)
    .first<{ created_at: number }>();

  if (lastRequest) {
    return jsonResponse({ error: 'Rate limit exceeded. Try again later.' }, 429);
  }

  // Record the help request
  await env.DB.prepare(
    'INSERT INTO guardian_help_requests (ward_device_id, security_score, unfixed_items, created_at) VALUES (?, ?, ?, ?)',
  )
    .bind(deviceId, securityScore, JSON.stringify(unfixedItems), now)
    .run();

  // Get ward display name
  const wardInfo = await env.DB.prepare(
    'SELECT ward_display_name FROM guardian_pairings WHERE ward_device_id = ? LIMIT 1',
  )
    .bind(deviceId)
    .first<{ ward_display_name: string }>();

  const wardName = wardInfo?.ward_display_name || 'Your ward';
  const title = `${wardName} — Help Requested`;
  const notifBody = `${wardName} needs help fixing ${unfixedItems.length} security item${unfixedItems.length === 1 ? '' : 's'} (score: ${securityScore}%)`;

  // Get all guardians' FCM tokens
  const guardianTokens = await env.DB.prepare(
    `SELECT t.fcm_token
     FROM guardian_pairings p
     JOIN guardian_fcm_tokens t ON t.device_id = p.guardian_device_id
     WHERE p.ward_device_id = ?`,
  )
    .bind(deviceId)
    .all<{ fcm_token: string }>();

  const tokens = guardianTokens.results ?? [];
  const data: Record<string, string> = {
    type: 'help_request',
    ward_device_id: deviceId,
    security_score: String(securityScore),
    unfixed_items: JSON.stringify(unfixedItems),
  };

  // Send notifications to all guardians
  for (const row of tokens) {
    await sendFcmNotification(env, row.fcm_token, title, notifBody, data);
  }

  return jsonResponse({ status: 'ok' }, 200);
}

// Guardian pairing API routes.
// Handles pairing code generation/claiming, ward/guardian listing, and unpairing.
//
// LOGGING POLICY: Never log device IDs or pairing codes.

import { verifyDeviceHmac } from '../middleware/guardian-auth';

/** Maximum request body size in bytes (10 KB). */
const MAX_BODY_SIZE = 10 * 1024;

/** Pairing code TTL in seconds (15 minutes). */
const CODE_TTL_SECONDS = 15 * 60;

/** Maximum guardians per ward. */
const MAX_GUARDIANS_PER_WARD = 3;

/** Rate limit: max pairing code generations per device per hour. */
const GENERATE_RATE_LIMIT = 10;

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

  await env.DB.prepare(
    'INSERT INTO guardian_pairing_codes (code, ward_device_id, created_at, expires_at, claimed) VALUES (?, ?, ?, ?, 0)',
  )
    .bind(code, deviceId, now, expiresAt)
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
  await env.DB.batch([
    env.DB.prepare(
      'UPDATE guardian_pairing_codes SET claimed = 1 WHERE code = ?',
    ).bind(code.toUpperCase()),
    env.DB.prepare(
      `INSERT INTO guardian_pairings (ward_device_id, guardian_device_id, ward_display_name, guardian_display_name, created_at)
       VALUES (?, ?, '', ?, ?)`,
    ).bind(pairingCode.ward_device_id, guardianDeviceId, guardianDisplayName, now),
  ]);

  return jsonResponse({ status: 'paired', ward_device_id: pairingCode.ward_device_id }, 200);
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

  return jsonResponse({ wards: pairings.results ?? [] }, 200);
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

  return jsonResponse({ guardians: pairings.results ?? [] }, 200);
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

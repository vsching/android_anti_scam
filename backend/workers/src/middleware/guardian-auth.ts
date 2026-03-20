// HMAC authentication middleware for guardian endpoints.
// Validates X-Device-HMAC header using HMAC-SHA256 with constant-time comparison.

/**
 * Verify the HMAC-SHA256 signature of the request body.
 * Returns null if valid, or a 401 Response if invalid.
 */
export async function verifyDeviceHmac(
  request: Request,
  body: string,
  env: Env,
): Promise<Response | null> {
  const hmacHeader = request.headers.get('X-Device-HMAC');

  if (!hmacHeader) {
    return jsonResponse({ error: 'Missing X-Device-HMAC header' }, 401);
  }

  const secret = env.GUARDIAN_HMAC_SECRET;
  if (!secret) {
    return jsonResponse({ error: 'Server misconfiguration' }, 500);
  }

  const encoder = new TextEncoder();

  // Import the secret key
  const key = await crypto.subtle.importKey(
    'raw',
    encoder.encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign'],
  );

  // Compute HMAC of the request body
  const signature = await crypto.subtle.sign('HMAC', key, encoder.encode(body));
  const expectedHex = bufferToHex(signature);

  // Constant-time comparison
  if (!timingSafeEqual(expectedHex, hmacHeader)) {
    return jsonResponse({ error: 'Invalid HMAC signature' }, 401);
  }

  return null;
}

/** Convert an ArrayBuffer to a lowercase hex string. */
function bufferToHex(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let hex = '';
  for (let i = 0; i < bytes.length; i++) {
    hex += bytes[i].toString(16).padStart(2, '0');
  }
  return hex;
}

/** Constant-time string comparison to prevent timing attacks. */
function timingSafeEqual(a: string, b: string): boolean {
  if (a.length !== b.length) {
    return false;
  }

  const encoder = new TextEncoder();
  const bufA = encoder.encode(a);
  const bufB = encoder.encode(b);

  // Use XOR-based constant-time comparison
  let mismatch = 0;
  for (let i = 0; i < bufA.length; i++) {
    mismatch |= bufA[i] ^ bufB[i];
  }

  return mismatch === 0;
}

function jsonResponse(data: unknown, status: number): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

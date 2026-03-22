// Admin authentication middleware.
// Validates X-Admin-Key header against env.ADMIN_SECRET using timing-safe comparison.

import { jsonResponse } from '../lib/response';

/**
 * Validate admin authentication and rate limiting.
 * Returns null if valid, or a 401/429 Response if invalid/rate-limited.
 */
export async function validateAdminAuth(request: Request, env: Env): Promise<Response | null> {
  const adminKey = request.headers.get('X-Admin-Key');

  if (!adminKey || !env.ADMIN_SECRET) {
    return jsonResponse({ error: 'Unauthorized' }, 401);
  }

  if (!timingSafeEqual(adminKey, env.ADMIN_SECRET)) {
    return jsonResponse({ error: 'Unauthorized' }, 401);
  }

  // Rate limit admin requests (binding may not exist or may be a stub in test environments)
  if (env.RATE_LIMITER_ADMIN && typeof env.RATE_LIMITER_ADMIN.limit === 'function') {
    try {
      const clientIp = request.headers.get('CF-Connecting-IP') ?? 'unknown';
      const { success } = await env.RATE_LIMITER_ADMIN.limit({ key: clientIp });
      if (!success) {
        return jsonResponse({ error: 'Rate limit exceeded' }, 429);
      }
    } catch {
      // Rate limiter unavailable — allow request through
    }
  }

  return null;
}

/**
 * Constant-time string comparison to prevent timing attacks.
 * Both inputs are padded to a fixed length (64 bytes) before comparison,
 * so that the early-exit on length mismatch does not leak the secret's length.
 */
function timingSafeEqual(a: string, b: string): boolean {
  const encoder = new TextEncoder();
  const bufA = encoder.encode(a);
  const bufB = encoder.encode(b);

  // Pad both buffers to a fixed size so comparison time is constant
  // regardless of input lengths.
  const PAD_SIZE = 64;
  const paddedA = new Uint8Array(PAD_SIZE);
  const paddedB = new Uint8Array(PAD_SIZE);
  paddedA.set(bufA.subarray(0, PAD_SIZE));
  paddedB.set(bufB.subarray(0, PAD_SIZE));

  let mismatch = 0;
  for (let i = 0; i < PAD_SIZE; i++) {
    mismatch |= paddedA[i] ^ paddedB[i];
  }

  // Also verify original lengths match (constant-time: already compared all PAD_SIZE bytes)
  if (bufA.length !== bufB.length) {
    return false;
  }

  return mismatch === 0;
}

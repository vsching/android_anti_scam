// Admin authentication middleware.
// Validates X-Admin-Key header against env.ADMIN_SECRET using timing-safe comparison.

/**
 * Validate admin authentication.
 * Returns null if valid, or a 401 Response if invalid.
 */
export function validateAdminAuth(request: Request, env: Env): Response | null {
  const adminKey = request.headers.get('X-Admin-Key');

  if (!adminKey || !env.ADMIN_SECRET) {
    return jsonResponse({ error: 'Unauthorized' }, 401);
  }

  if (!timingSafeEqual(adminKey, env.ADMIN_SECRET)) {
    return jsonResponse({ error: 'Unauthorized' }, 401);
  }

  return null;
}

/** Constant-time string comparison to prevent timing attacks. */
function timingSafeEqual(a: string, b: string): boolean {
  if (a.length !== b.length) {
    return false;
  }

  const encoder = new TextEncoder();
  const bufA = encoder.encode(a);
  const bufB = encoder.encode(b);

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

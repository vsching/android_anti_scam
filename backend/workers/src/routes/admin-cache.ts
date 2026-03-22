// Admin cache purge endpoint — delete Cache API entry for a domain.

import { validateAdminAuth } from '../middleware/admin-auth';
import { logAdminAction } from '../lib/admin-audit';

/**
 * DELETE /api/admin/cache/:domain
 */
export async function handleAdminCachePurge(
  request: Request,
  env: Env,
  params: Record<string, string>,
): Promise<Response> {
  const authError = validateAdminAuth(request, env);
  if (authError) return authError;

  const domain = params.domain;
  if (!domain) {
    return jsonResponse({ error: 'Missing domain parameter' }, 400);
  }

  const cacheKey = `https://cache.safeanot.internal/check/${encodeURIComponent(domain)}`;
  const cache = caches.default;
  await cache.delete(new Request(cacheKey));

  logAdminAction(env.DB, request, domain, 'cache_purge', { domain });

  return jsonResponse({ purged: true, domain }, 200);
}

function jsonResponse(data: unknown, status: number): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

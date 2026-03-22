// Admin cache purge endpoint — delete Cache API entry for a domain.

import { validateAdminAuth } from '../middleware/admin-auth';
import { logAdminAction } from '../lib/admin-audit';
import { jsonResponse } from '../lib/response';
import { cacheApiKey } from '../lib/cache';

/**
 * DELETE /api/admin/cache/:domain
 */
export async function handleAdminCachePurge(
  request: Request,
  env: Env,
  params: Record<string, string>,
): Promise<Response> {
  const authError = await validateAdminAuth(request, env);
  if (authError) return authError;

  const domain = params.domain;
  if (!domain) {
    return jsonResponse({ error: 'Missing domain parameter' }, 400);
  }

  const cache = caches.default;
  await cache.delete(new Request(cacheApiKey(domain)));

  logAdminAction(env.DB, request, domain, 'cache_purge', { domain });

  return jsonResponse({ purged: true, domain }, 200);
}

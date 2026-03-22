// Admin pending discoveries endpoints — list and dismiss discovered domains.

import { validateAdminAuth } from '../middleware/admin-auth';
import { logAdminAction } from '../lib/admin-audit';
import { jsonResponse } from '../lib/response';

/**
 * GET /api/admin/discoveries
 */
export async function handleAdminDiscoveriesList(
  request: Request,
  env: Env,
  _params: Record<string, string>,
): Promise<Response> {
  const authError = await validateAdminAuth(request, env);
  if (authError) return authError;

  const result = await env.DB.prepare(
    `SELECT id, domain, verdict, reason, source, check_count,
            last_seen_at, created_at, processed, processed_at
     FROM pending_discoveries
     ORDER BY created_at DESC`,
  ).all();

  return jsonResponse({ entries: result.results }, 200);
}

/**
 * DELETE /api/admin/discoveries/:id
 */
export async function handleAdminDiscoveriesDelete(
  request: Request,
  env: Env,
  params: Record<string, string>,
): Promise<Response> {
  const authError = await validateAdminAuth(request, env);
  if (authError) return authError;

  const id = params.id;
  if (!id) {
    return jsonResponse({ error: 'Missing id parameter' }, 400);
  }

  // Get domain for audit log before deleting
  const existing = await env.DB.prepare(
    'SELECT domain FROM pending_discoveries WHERE id = ?',
  ).bind(id).first<{ domain: string }>();

  if (!existing) {
    return jsonResponse({ error: 'Discovery not found' }, 404);
  }

  await env.DB.prepare('DELETE FROM pending_discoveries WHERE id = ?').bind(id).run();
  logAdminAction(env.DB, request, existing.domain, 'discovery_dismiss', { id });

  return jsonResponse({ ok: true, id }, 200);
}

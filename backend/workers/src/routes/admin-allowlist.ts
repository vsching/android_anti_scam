// Admin allowlist endpoints — add, remove, list verified safe domains.

import { validateAdminAuth } from '../middleware/admin-auth';
import { writeAllowlistEntry, deleteAllowlistEntry, listAllowlistEntries } from '../lib/admin-kv';
import { logAdminAction } from '../lib/admin-audit';
import { jsonResponse } from '../lib/response';

/**
 * POST /api/admin/allowlist
 * Body: { domain, entity, category }
 */
export async function handleAdminAllowlistAdd(
  request: Request,
  env: Env,
  _params: Record<string, string>,
): Promise<Response> {
  const authError = await validateAdminAuth(request, env);
  if (authError) return authError;

  let body: { domain?: string; entity?: string; category?: string };
  try {
    body = await request.json();
  } catch {
    return jsonResponse({ error: 'Invalid JSON' }, 400);
  }

  if (!body.domain || !body.entity || !body.category) {
    return jsonResponse({ error: 'Missing required fields: domain, entity, category' }, 400);
  }

  await writeAllowlistEntry(env.VERDICTS, body.domain, body.entity, body.category);
  logAdminAction(env.DB, request, body.domain, 'allowlist_add', body);

  return jsonResponse({ ok: true, domain: body.domain }, 200);
}

/**
 * DELETE /api/admin/allowlist/:domain
 */
export async function handleAdminAllowlistRemove(
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

  await deleteAllowlistEntry(env.VERDICTS, domain);
  logAdminAction(env.DB, request, domain, 'allowlist_remove', { domain });

  return jsonResponse({ ok: true, domain }, 200);
}

/**
 * GET /api/admin/allowlist
 */
export async function handleAdminAllowlistList(
  request: Request,
  env: Env,
  _params: Record<string, string>,
): Promise<Response> {
  const authError = await validateAdminAuth(request, env);
  if (authError) return authError;

  const entries = await listAllowlistEntries(env.VERDICTS);

  return jsonResponse({ entries }, 200);
}

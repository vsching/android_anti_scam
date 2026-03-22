// Admin blocklist endpoints — add, remove, list blocked domains.

import { validateAdminAuth } from '../middleware/admin-auth';
import { writeBlocklistEntry, deleteBlocklistEntry, listBlocklistEntries } from '../lib/admin-kv';
import { logAdminAction } from '../lib/admin-audit';
import { jsonResponse } from '../lib/response';

/**
 * POST /api/admin/blocklist
 * Body: { domain, reason }
 */
export async function handleAdminBlocklistAdd(
  request: Request,
  env: Env,
  _params: Record<string, string>,
): Promise<Response> {
  const authError = await validateAdminAuth(request, env);
  if (authError) return authError;

  let body: { domain?: string; reason?: string };
  try {
    body = await request.json();
  } catch {
    return jsonResponse({ error: 'Invalid JSON' }, 400);
  }

  if (!body.domain || !body.reason) {
    return jsonResponse({ error: 'Missing required fields: domain, reason' }, 400);
  }

  await writeBlocklistEntry(env.VERDICTS, body.domain, body.reason);
  logAdminAction(env.DB, request, body.domain, 'blocklist_add', body);

  return jsonResponse({ ok: true, domain: body.domain }, 200);
}

/**
 * DELETE /api/admin/blocklist/:domain
 */
export async function handleAdminBlocklistRemove(
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

  await deleteBlocklistEntry(env.VERDICTS, domain);
  logAdminAction(env.DB, request, domain, 'blocklist_remove', { domain });

  return jsonResponse({ ok: true, domain }, 200);
}

/**
 * GET /api/admin/blocklist
 */
export async function handleAdminBlocklistList(
  request: Request,
  env: Env,
  _params: Record<string, string>,
): Promise<Response> {
  const authError = await validateAdminAuth(request, env);
  if (authError) return authError;

  const entries = await listBlocklistEntries(env.VERDICTS);

  return jsonResponse({ entries }, 200);
}

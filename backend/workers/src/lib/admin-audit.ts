// Admin audit logging — writes admin actions to D1 admin_audit_logs table.
// Fire-and-forget: does not block the response.

/**
 * Log an admin action to the audit table.
 * This is fire-and-forget — errors are silently swallowed.
 */
export function logAdminAction(
  db: D1Database,
  request: Request,
  domain: string,
  action: string,
  payload: unknown,
): void {
  // CF-Connecting-IP is trusted only when the worker runs behind Cloudflare proxy.
  // In non-proxied environments this header could be spoofed by the client.
  const adminIp = request.headers.get('CF-Connecting-IP') ?? 'unknown';
  const requestId = request.headers.get('CF-Ray') ?? crypto.randomUUID();
  const payloadJson = payload ? JSON.stringify(payload) : null;

  db.prepare(
    `INSERT INTO admin_audit_logs (domain, action, payload_json, admin_ip, request_id)
     VALUES (?, ?, ?, ?, ?)`,
  )
    .bind(domain, action, payloadJson, adminIp, requestId)
    .run()
    .catch(() => {
      // Silently swallow — audit logging must not block admin operations
    });
}

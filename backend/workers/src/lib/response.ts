// Shared JSON response helper used across all route handlers.

/**
 * Build a JSON Response with the given data and HTTP status code.
 */
export function jsonResponse(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

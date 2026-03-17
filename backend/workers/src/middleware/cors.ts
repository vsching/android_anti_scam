// CORS middleware — allows safeanot.com and localhost origins.
// Handles OPTIONS preflight and adds CORS headers to every response.

const ALLOWED_ORIGINS = [
  'https://safeanot.com',
];

const ALLOWED_METHODS = 'GET, POST, OPTIONS';
const ALLOWED_HEADERS = 'Content-Type, Authorization';
const MAX_AGE = '86400';

/**
 * Check whether the given origin is allowed.
 * Permits https://safeanot.com and any http://localhost port.
 */
function isAllowedOrigin(origin: string | null): boolean {
  if (!origin) return false;
  if (ALLOWED_ORIGINS.includes(origin)) return true;
  if (/^http:\/\/localhost(:\d+)?$/.test(origin)) return true;
  return false;
}

/**
 * Handle an OPTIONS preflight request, returning a 204 with CORS headers.
 */
export function handlePreflight(request: Request): Response | null {
  const origin = request.headers.get('Origin');
  if (request.method !== 'OPTIONS') return null;

  const headers = new Headers();
  if (isAllowedOrigin(origin)) {
    headers.set('Access-Control-Allow-Origin', origin!);
  }
  headers.set('Access-Control-Allow-Methods', ALLOWED_METHODS);
  headers.set('Access-Control-Allow-Headers', ALLOWED_HEADERS);
  headers.set('Access-Control-Max-Age', MAX_AGE);

  return new Response(null, { status: 204, headers });
}

/**
 * Add CORS headers to an existing response.
 * Returns a new Response because Workers headers can be immutable.
 */
export function addCorsHeaders(request: Request, response: Response): Response {
  const origin = request.headers.get('Origin');
  const headers = new Headers(response.headers);

  if (isAllowedOrigin(origin)) {
    headers.set('Access-Control-Allow-Origin', origin!);
  }
  headers.set('Access-Control-Allow-Methods', ALLOWED_METHODS);
  headers.set('Access-Control-Allow-Headers', ALLOWED_HEADERS);

  return new Response(response.body, {
    status: response.status,
    statusText: response.statusText,
    headers,
  });
}

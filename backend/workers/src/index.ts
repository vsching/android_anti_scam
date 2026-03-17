// Main Worker entrypoint for the Safe Anot? API.
// All requests flow through the router with CORS and security-header middleware.
//
// LOGGING POLICY: Never log full URLs checked, phone numbers, or message text.
// Only log domain hashes, verdict results, and anonymised metadata.

import { Router } from './router';
import { handlePreflight, addCorsHeaders } from './middleware/cors';
import { addSecurityHeaders } from './middleware/security-headers';
import { handleRetention } from './lib/retention';

const router = new Router();

// Health / version endpoint
router.get('/', (_request, _env, _params) => {
  return new Response(
    JSON.stringify({
      name: 'Safe Anot? API',
      version: '1.0.0',
      status: 'ok',
    }),
    {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    },
  );
});

export default {
  async scheduled(_event: ScheduledEvent, env: Env, _ctx: ExecutionContext): Promise<void> {
    await handleRetention(env);
  },

  async fetch(request: Request, env: Env): Promise<Response> {
    // Handle CORS preflight
    const preflight = handlePreflight(request);
    if (preflight) {
      return addSecurityHeaders(preflight);
    }

    // Route the request
    const response = await router.match(request, env);

    if (response) {
      return addSecurityHeaders(addCorsHeaders(request, response));
    }

    // 404 for unmatched routes
    const notFound = new Response(
      JSON.stringify({ error: 'Not found' }),
      {
        status: 404,
        headers: { 'Content-Type': 'application/json' },
      },
    );

    return addSecurityHeaders(addCorsHeaders(request, notFound));
  },
};

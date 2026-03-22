// Main Worker entrypoint for the Safe Anot? API.
// All requests flow through the router with CORS and security-header middleware.
//
// LOGGING POLICY: Never log full URLs checked, phone numbers, or message text.
// Only log domain hashes, verdict results, and anonymised metadata.

import { Router } from './router';
import { handlePreflight, addCorsHeaders } from './middleware/cors';
import { addSecurityHeaders } from './middleware/security-headers';
import { handleRetention } from './lib/retention';
import { handleAlerts } from './routes/alerts';
import { handleAlertsLatest } from './routes/alerts-latest';
import { handleAlertsNotify } from './routes/alerts-notify';
import { handleCheck } from './routes/check';
import { handleShare } from './routes/share';
import {
  handleDataLatest,
  handleDataFull,
  handleDataDelta,
  handleDataBloom,
} from './routes/data';
import { handleHealth } from './routes/health';
import {
  handleGeneratePairingCode,
  handleClaimPairingCode,
  handleGetWards,
  handleGetGuardians,
  handleDeletePairing,
  handlePostHeartbeat,
  handleGetWardHeartbeats,
  handleRegisterFcmToken,
  handleHelpRequest,
} from './routes/guardian';
import {
  handleAdminAllowlistAdd,
  handleAdminAllowlistRemove,
  handleAdminAllowlistList,
} from './routes/admin-allowlist';
import {
  handleAdminBlocklistAdd,
  handleAdminBlocklistRemove,
  handleAdminBlocklistList,
} from './routes/admin-blocklist';
import { handleAdminCachePurge } from './routes/admin-cache';
import {
  handleAdminDiscoveriesList,
  handleAdminDiscoveriesDelete,
} from './routes/admin-discoveries';

const router = new Router();

// Scam alerts feed
router.get('/api/alerts', handleAlerts);
router.get('/api/alerts/latest', handleAlertsLatest);
router.post('/api/alerts/notify', handleAlertsNotify);

// Link checker
router.post('/api/check', handleCheck);

// Share event analytics
router.post('/api/score/share', handleShare);

// Guardian pairing
router.post('/api/guardian/pair/generate', handleGeneratePairingCode);
router.post('/api/guardian/pair/claim', handleClaimPairingCode);
router.get('/api/guardian/wards', handleGetWards);
router.get('/api/guardian/guardians', handleGetGuardians);
router.delete('/api/guardian/pair/:pairingId', handleDeletePairing);
router.post('/api/guardian/heartbeat', handlePostHeartbeat);
router.get('/api/guardian/wards/:deviceId/heartbeats', handleGetWardHeartbeats);
router.post('/api/guardian/fcm-token', handleRegisterFcmToken);
router.post('/api/guardian/help-request', handleHelpRequest);

// Pipeline health
router.get('/api/health', handleHealth);

// Data download endpoints (R2 artifacts)
router.get('/api/data/latest', handleDataLatest);
router.get('/api/data/full', handleDataFull);
router.get('/api/data/delta', handleDataDelta);
router.get('/api/data/bloom', handleDataBloom);

// Admin endpoints (auth-protected)
router.post('/api/admin/allowlist', handleAdminAllowlistAdd);
router.delete('/api/admin/allowlist/:domain', handleAdminAllowlistRemove);
router.get('/api/admin/allowlist', handleAdminAllowlistList);
router.post('/api/admin/blocklist', handleAdminBlocklistAdd);
router.delete('/api/admin/blocklist/:domain', handleAdminBlocklistRemove);
router.get('/api/admin/blocklist', handleAdminBlocklistList);
router.delete('/api/admin/cache/:domain', handleAdminCachePurge);
router.get('/api/admin/discoveries', handleAdminDiscoveriesList);
router.delete('/api/admin/discoveries/:id', handleAdminDiscoveriesDelete);

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

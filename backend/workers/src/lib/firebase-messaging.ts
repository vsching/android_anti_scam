// FCM topic-based messaging helper for Cloudflare Workers.
// Sends push notifications to FCM topics (e.g., "scam_alerts_MY").
// Uses the existing fcm.ts OAuth2/JWT infrastructure for authentication.
//
// LOGGING POLICY: Never log FCM tokens or device identifiers.

interface ServiceAccountKey {
  project_id: string;
  client_email: string;
  private_key: string;
}

/**
 * Base64url-encode a string (no padding).
 */
function base64url(input: string): string {
  const encoder = new TextEncoder();
  const bytes = encoder.encode(input);
  let binary = '';
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

/**
 * Base64url-encode raw bytes (no padding).
 */
function base64urlBytes(bytes: Uint8Array): string {
  let binary = '';
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

/**
 * Parse a PEM-encoded RSA private key into a CryptoKey for RS256 signing.
 */
async function importPrivateKey(pem: string): Promise<CryptoKey> {
  const pemContents = pem
    .replace(/-----BEGIN PRIVATE KEY-----/g, '')
    .replace(/-----END PRIVATE KEY-----/g, '')
    .replace(/\s/g, '');

  const binaryString = atob(pemContents);
  const bytes = new Uint8Array(binaryString.length);
  for (let i = 0; i < binaryString.length; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }

  return crypto.subtle.importKey(
    'pkcs8',
    bytes.buffer,
    { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
    false,
    ['sign'],
  );
}

/**
 * Mint a fresh OAuth2 access token using the service account private key.
 * No module-level caching — generates a fresh token per request
 * (acceptable for low-volume weekly endpoint; Workers isolate evictions
 * make module-level caching unreliable).
 */
async function getAccessToken(serviceAccount: ServiceAccountKey): Promise<string> {
  const now = Math.floor(Date.now() / 1000);

  const header = base64url(JSON.stringify({ alg: 'RS256', typ: 'JWT' }));
  const payload = base64url(
    JSON.stringify({
      iss: serviceAccount.client_email,
      sub: serviceAccount.client_email,
      aud: 'https://oauth2.googleapis.com/token',
      iat: now,
      exp: now + 3600,
      scope: 'https://www.googleapis.com/auth/firebase.messaging',
    }),
  );

  const signingInput = `${header}.${payload}`;
  const key = await importPrivateKey(serviceAccount.private_key);
  const signature = await crypto.subtle.sign(
    'RSASSA-PKCS1-v1_5',
    key,
    new TextEncoder().encode(signingInput),
  );
  const jwt = `${signingInput}.${base64urlBytes(new Uint8Array(signature))}`;

  const tokenResponse = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`,
  });

  if (!tokenResponse.ok) {
    const errorText = await tokenResponse.text();
    console.error('FCM OAuth2 token exchange failed:', tokenResponse.status, errorText);
    throw new Error(`OAuth2 token exchange failed: ${tokenResponse.status}`);
  }

  const tokenData = await tokenResponse.json<{ access_token: string; expires_in: number }>();
  return tokenData.access_token;
}

/**
 * Send a push notification to an FCM topic via the HTTP v1 API.
 *
 * @param env - Worker environment bindings
 * @param topic - FCM topic name (e.g., "scam_alerts_MY")
 * @param title - Notification title
 * @param body - Notification body text
 * @param data - Data payload (key-value pairs delivered to the app)
 * @returns true on success, false on failure (logs error but does not throw)
 */
export async function sendFcmTopicMessage(
  env: Env,
  topic: string,
  title: string,
  body: string,
  data: Record<string, string>,
): Promise<boolean> {
  try {
    const serviceAccount: ServiceAccountKey = JSON.parse(env.FIREBASE_SERVICE_ACCOUNT_JSON);
    const accessToken = await getAccessToken(serviceAccount);

    const messagePayload = {
      message: {
        topic,
        notification: { title, body },
        data,
      },
    };

    const response = await fetch(
      `https://fcm.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/messages:send`,
      {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(messagePayload),
      },
    );

    if (response.ok) {
      return true;
    }

    const errorText = await response.text();
    console.error('FCM topic send failed:', response.status, errorText);
    return false;
  } catch (error) {
    console.error('FCM topic send error:', error);
    return false;
  }
}

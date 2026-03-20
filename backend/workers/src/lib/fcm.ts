// FCM HTTP v1 API notification sender for Cloudflare Workers.
// Uses service account JSON + OAuth2 access token with RS256 JWT signing.
//
// LOGGING POLICY: Never log FCM tokens or device identifiers.

/** Cached OAuth2 access token and its expiry time. */
let cachedAccessToken: string | null = null;
let cachedAccessTokenExpiry = 0;

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
 * Mint a short-lived OAuth2 access token using the service account private key.
 * Caches the token until 5 minutes before expiry.
 */
async function getAccessToken(serviceAccount: ServiceAccountKey): Promise<string> {
  const now = Math.floor(Date.now() / 1000);

  // Return cached token if still valid (with 5-minute buffer)
  if (cachedAccessToken && cachedAccessTokenExpiry > now + 300) {
    return cachedAccessToken;
  }

  // Create JWT for token exchange
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

  // Exchange JWT for access token
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
  cachedAccessToken = tokenData.access_token;
  cachedAccessTokenExpiry = now + tokenData.expires_in;

  return cachedAccessToken;
}

/**
 * Send a push notification via the FCM HTTP v1 API.
 *
 * @returns true on success, false on failure (logs error but does not throw).
 */
export async function sendFcmNotification(
  env: Env,
  fcmToken: string,
  title: string,
  body: string,
  data: Record<string, string>,
): Promise<boolean> {
  try {
    const serviceAccount: ServiceAccountKey = JSON.parse(env.FIREBASE_SERVICE_ACCOUNT_JSON);
    const accessToken = await getAccessToken(serviceAccount);

    const messagePayload = {
      message: {
        token: fcmToken,
        notification: { title, body },
        data,
      },
    };

    const response = await fetch(
      `https://fcm.googleapis.com/v1/projects/${serviceAccount.project_id}/messages:send`,
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
    console.error('FCM send failed:', response.status, errorText);
    return false;
  } catch (error) {
    console.error('FCM send error:', error);
    return false;
  }
}

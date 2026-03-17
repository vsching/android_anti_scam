// Integration tests for the Safe Anot? API scaffold.
// Verifies health endpoint, 404 handling, CORS, security headers, and preflight.

import { SELF } from 'cloudflare:test';
import { describe, it, expect } from 'vitest';

describe('Safe Anot? API', () => {
  describe('GET /', () => {
    it('returns 200 with version info', async () => {
      const response = await SELF.fetch('http://localhost/');
      expect(response.status).toBe(200);

      const body = await response.json<{
        name: string;
        version: string;
        status: string;
      }>();
      expect(body.name).toBe('Safe Anot? API');
      expect(body.version).toBe('1.0.0');
      expect(body.status).toBe('ok');
    });

    it('returns JSON content type', async () => {
      const response = await SELF.fetch('http://localhost/');
      expect(response.headers.get('Content-Type')).toBe('application/json');
    });
  });

  describe('Unknown routes', () => {
    it('returns 404 with error body', async () => {
      const response = await SELF.fetch('http://localhost/does-not-exist');
      expect(response.status).toBe(404);

      const body = await response.json<{ error: string }>();
      expect(body.error).toBe('Not found');
    });
  });

  describe('CORS headers', () => {
    it('includes CORS headers for allowed origin', async () => {
      const response = await SELF.fetch('http://localhost/', {
        headers: { Origin: 'https://safeanot.com' },
      });
      expect(response.headers.get('Access-Control-Allow-Origin')).toBe(
        'https://safeanot.com',
      );
      expect(response.headers.get('Access-Control-Allow-Methods')).toContain(
        'GET',
      );
    });

    it('includes CORS headers for localhost origin', async () => {
      const response = await SELF.fetch('http://localhost/', {
        headers: { Origin: 'http://localhost:3000' },
      });
      expect(response.headers.get('Access-Control-Allow-Origin')).toBe(
        'http://localhost:3000',
      );
    });

    it('does not set allow-origin for disallowed origin', async () => {
      const response = await SELF.fetch('http://localhost/', {
        headers: { Origin: 'https://evil.com' },
      });
      expect(response.headers.get('Access-Control-Allow-Origin')).toBeNull();
    });
  });

  describe('Security headers', () => {
    it('includes all security headers', async () => {
      const response = await SELF.fetch('http://localhost/');
      expect(response.headers.get('X-Content-Type-Options')).toBe('nosniff');
      expect(response.headers.get('X-Frame-Options')).toBe('DENY');
      expect(response.headers.get('X-XSS-Protection')).toBe(
        '1; mode=block',
      );
      expect(response.headers.get('Referrer-Policy')).toBe(
        'strict-origin-when-cross-origin',
      );
      expect(response.headers.get('Content-Security-Policy')).toBe(
        "default-src 'none'",
      );
    });
  });

  describe('OPTIONS preflight', () => {
    it('returns 204 for OPTIONS request', async () => {
      const response = await SELF.fetch('http://localhost/', {
        method: 'OPTIONS',
        headers: {
          Origin: 'https://safeanot.com',
          'Access-Control-Request-Method': 'POST',
        },
      });
      expect(response.status).toBe(204);
    });

    it('includes CORS and security headers on preflight', async () => {
      const response = await SELF.fetch('http://localhost/', {
        method: 'OPTIONS',
        headers: {
          Origin: 'https://safeanot.com',
          'Access-Control-Request-Method': 'POST',
        },
      });
      expect(response.headers.get('Access-Control-Allow-Origin')).toBe(
        'https://safeanot.com',
      );
      expect(response.headers.get('Access-Control-Max-Age')).toBe('86400');
      expect(response.headers.get('X-Content-Type-Options')).toBe('nosniff');
    });
  });
});

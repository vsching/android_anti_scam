// Vitest configuration for Cloudflare Workers tests.
// Uses @cloudflare/vitest-pool-workers for local miniflare bindings.

import { defineWorkersConfig } from '@cloudflare/vitest-pool-workers/config';

export default defineWorkersConfig({
  test: {
    globals: true,
    poolOptions: {
      workers: {
        wrangler: {
          configPath: './wrangler.toml',
        },
        miniflare: {
          kvNamespaces: ['VERDICTS'],
          r2Buckets: ['DATA_BUCKET'],
          d1Databases: ['DB'],
          bindings: {
            GUARDIAN_HMAC_SECRET: 'test-hmac-secret',
          },
        },
      },
    },
  },
});

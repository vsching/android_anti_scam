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
            PIPELINE_SECRET: 'test-pipeline-secret',
            FIREBASE_PROJECT_ID: 'test-project-id',
            FIREBASE_SERVICE_ACCOUNT_JSON: '{"project_id":"test","client_email":"test@test.iam.gserviceaccount.com","private_key":"-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7o4qne60TB3pq\\njKMFMKMsJIkLUY/yrPfv7hpIiFJKBp90a6EEotGEfOCmaOVNl0H7aNOb0eMbp/J7\\nWGeYiKUPHHOBYHAGmGaWBx5W6gT+cIYp3MKCLjCm+cJI5B+eVFM5HVnPnQ2bfJx\\nNpLMhI7V6SXSEOV3fpCM3Y+z/Gj5D3mC3Q5FpJEi0tBqzhQBb6Lx+5rKZx6rJKx\\nEhUKPIVXJv1nvGfQPb4q8eKPojCDzHMI+FJNFxi3n5cqk0Z5cGFzwU0PjJO0XwIR\\nt1e8lPe0w9bsNz7MwIE1Q7V8xRD0uNPnhV8+EnnAW8REYjl/sMWBdGBGT2l6nCB\\npEYJ6B3/AgMBAAECggEAIoEl+JJswZHQ7iJE+j4kAfQrHMIb5jV0A9bPxj6qEk/u\\n8JLT8PVcWGRrEHJP+kWB0uxp+gMwkU04IKUS7bOMa8cjK18bjMJ9aLnJIcF5JhbI\\nfqn5o0k8MQzU2qVJfn3jfm6MiacfpCAevPVxNGrzG+n9UMFOv3LOMSpMlDrpMYE5\\n5bvNurlWDFCEbOp8oq3nm/BzfMJxUbj+Y2J8X6pU1MuFp3YW3tWfZ1g7Z+sR4h+p\\nGLvuzTg3cNJb4qS7cFMmW7gflailm4mG6NjTnO/UlxzMfG6O/3jkBbGFz3sCBYlR\\nEYcyM3w2YL5gj3r+fJnQFKCi/y4R2IS7T7xeGmDPaQKBgQDtR0OD6bo0LGkmHPuq\\nfQVMmr5z8UDL4UaFy2cjZcLE7YtKJ5L/mMwrJUa+VgjNvGSEjeM4W1ay0PJt+6Fn\\nXdEVQMlHLam5F3v14Lg2dCdTwadk7HV5jKmLG0FR4N0pGTBDEPNEH8RXJIFBqxL4\\nFOKjdfxQvPR+TQSbvSqBJqzUNQKBgQDLH4MLVJ5JrFGRK4CXhU7S1w7xrdMxRMtK\\njNDfRPIFe8MWiQ6WxaVZMWR5aBp2C/CWwF8KS6xDYwi0KlaBDEYfd5FgQrq+DJhZ\\nEi5olEw5C+5TFC01SYkH6VJrh6jI8DyxbkOE0k+xSlfApPzSR7bKMVBhJ7tjLi03\\n5Ofdz6AzgwKBgGKlWMH9sSj7a6bNnJC9MkN3C2fP3t1dZnhR5o+fHHKySbPKITpJ\\nUaBfz7Aa4nk4v0N3F8k9j8o3/Ks7WJIU6j3ULVra2p9TIJkG8S77U2LA/mxnJP9E\\n3dQV3c0MLdJ+WLDUyNy7rU2+FJ40h8FNRgi8xJ3GWqPL8tJFd+6tOnPxAoGBAMcJ\\nJJIBJl8W4PjL0O/HJGBpOD8qATbSeZ8GXQJUHS75bK3OFsEFTMPkODIE0ViJCmLs\\nJ0ghVP6wrFbN9FWx/252CITMWJ3n5G0Wy0V0e+GWDNFKJIA8G00ZpVV8Zt4/y8L1\\nQWQjI/yij1xDtX23y0J7uJr5/1hwmZ7P6cCFv4H3AoGAZwU8YvV8YhFVUuQMYCba\\n3P6O7LfyN9x+3E7Bnn2BMvGdXe0EpqVl3XKVCXpoJ5T2sMj3Dq5Vtcuzq7qlZLt/\\nyTW6GN2m6V2GJ4K5OoZbr8sF5LpqSRulvnNFTf9sJmvTbUa++G8b3x1CY+CQNDsb\\nlm6DTpLMeNJgQ2ORf2r/F6s=\\n-----END PRIVATE KEY-----\\n"}',
          },
        },
      },
    },
  },
});

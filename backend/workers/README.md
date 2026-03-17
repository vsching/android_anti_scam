# Safe Anot? Workers API

Cloudflare Worker backend for the Safe Anot? scam detection app.

## Prerequisites

- Node 20+

## Setup

```bash
npm install
cp .env.example .dev.vars   # fill in local secrets
```

## Development

```bash
npm run dev          # start local dev server (wrangler dev)
npm test             # run tests
npm run test:coverage  # run tests with coverage
```

## Deploy

```bash
npm run deploy       # deploy to Cloudflare
```

Deployment also runs automatically via GitHub Actions on push to `main`.

---
created: 2026-03-17T13:24:17Z
last_updated: 2026-03-17T13:24:17Z
version: 1.0
author: Claude Code PM System
---

# Project Structure

## Top-Level Layout

```
android_anti_scam/
├── android/              # Kotlin Android app (Compose + MVVM)
│   └── app/
│       └── src/main/java/com/safeanot/app/
│           ├── feature/check/     # Link checker
│           ├── feature/shield/    # Phone shield audit
│           ├── feature/alerts/    # Scam alerts feed
│           ├── data/              # Room DB, repositories
│           ├── domain/            # Use cases, models
│           ├── di/                # Hilt modules
│           └── ui/                # Shared UI components
├── backend/
│   ├── workers/          # Cloudflare Workers (TypeScript)
│   │   ├── src/          # API route handlers
│   │   ├── test/         # Vitest tests
│   │   ├── wrangler.toml # Workers config (KV, D1, R2 bindings)
│   │   └── package.json
│   └── pipeline/         # Python data pipeline
│       ├── src/          # Feed scrapers, processors, uploaders
│       ├── tests/        # pytest tests
│       └── pyproject.toml
├── docs/                 # Specs, plans, research
│   ├── development-plan/ # Epic specs + implementation plans
│   └── *.md              # Architecture, requirements, research
├── infra/                # IaC scripts (Cloudflare, DO)
├── .github/workflows/    # CI/CD (Workers deploy)
└── .claude/context/      # Claude Code context files
```

## Key Config Files

| File | Purpose |
|------|---------|
| `android/app/build.gradle.kts` | Android dependencies, SDK versions |
| `backend/workers/wrangler.toml` | Workers bindings (KV, D1, R2) |
| `backend/workers/package.json` | Workers dependencies |
| `backend/pipeline/pyproject.toml` | Pipeline Python dependencies |
| `.github/workflows/*.yml` | CI/CD pipeline definitions |

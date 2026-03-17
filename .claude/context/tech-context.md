---
created: 2026-03-17T13:24:17Z
last_updated: 2026-03-17T13:24:17Z
version: 1.0
author: Claude Code PM System
---

# Technology Context

## Android App

| Area | Technology |
|------|-----------|
| Language | Kotlin (JVM target 17) |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt (Dagger) + KSP |
| Local DB | Room 2.6.1 |
| Networking | Retrofit 2.9 + OkHttp 4.12 |
| Background | WorkManager 2.9 |
| Navigation | Navigation Compose 2.7 |
| Min SDK | 26 (Android 8.0 Oreo) |
| Target SDK | 34 (Android 14) |
| Compile SDK | 34 |
| Compose BOM | 2024.02.00 |
| Testing | JUnit 4, Espresso, Compose UI Test |

## Backend (Cloudflare Workers)

| Area | Technology |
|------|-----------|
| Runtime | Cloudflare Workers (edge) |
| Language | TypeScript 5.4 (strict) |
| Storage | KV (allowlist + discoveries), D1 (reports, alerts), R2 (bulk artifacts) |
| Testing | Vitest 2.1 + @cloudflare/vitest-pool-workers |
| Deployment | Wrangler 3.91, GitHub Actions |

## Data Pipeline

| Area | Technology |
|------|-----------|
| Language | Python 3.12+ |
| HTTP | httpx 0.27 |
| Hashing | mmh3 4.0 (Bloom filters) |
| CLI | Click 8.1 |
| S3/R2 | boto3 1.34 |
| Testing | pytest 8.0, pytest-asyncio, respx |

## Infrastructure

| Service | Use | Cost |
|---------|-----|------|
| Cloudflare Workers | API (300+ edge cities) | Free tier / $5/mo |
| Cloudflare KV | Allowlist + new discoveries | Included |
| Cloudflare D1 | Reports, alerts, pairings | Included |
| Cloudflare R2 | Bulk DB artifacts (SQLite, Bloom) | Included |
| Cloudflare Pages | Website | Free |
| DigitalOcean Droplet | Pipeline cron jobs | $6/mo |
| Firebase FCM | Push notifications | Free |

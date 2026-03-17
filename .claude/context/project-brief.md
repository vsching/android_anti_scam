---
created: 2026-03-17T13:24:17Z
last_updated: 2026-03-17T13:24:17Z
version: 1.0
author: Claude Code PM System
---

# Project Brief

## Mission

Protect users in Malaysia and Singapore from phishing links and malicious APK sideloading through a free, easy-to-use Android app.

## Growth Model

- **Free**: Core features free forever, no ads
- **Viral**: Shareable verdict cards drive organic growth via WhatsApp/Telegram
- **Community**: User reports feed back into scam database (Phase 3)

## Infrastructure Cost Model

| Scale | Monthly Cost |
|-------|-------------|
| Development | $0 (free tiers) |
| 1K DAU | ~$7/mo |
| 10K DAU | ~$15/mo |
| 100K DAU | ~$39/mo |

Key: Cloudflare Workers + KV + D1 + R2 + Pages. DigitalOcean $6/mo for pipeline.

## Development Phases

| Phase | Focus | Epics |
|-------|-------|-------|
| Phase 1: MVP | Core app + backend + website | E01-E07 |
| Phase 2: Growth | Family guardian, push notifications, gamification | E08-E10 |
| Phase 3: Data Moat | Community reporting, data partnerships, monetization | E11-E13 |

## Key Constraints

- Android only (no iOS planned for MVP)
- Min SDK 26 (Android 8.0) for broad device coverage
- No user accounts for core features (reduce friction)
- All user traffic through Cloudflare edge (never hit origin)

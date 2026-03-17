---
created: 2026-03-17T13:24:17Z
last_updated: 2026-03-17T13:24:17Z
version: 1.0
author: Claude Code PM System
---

# System Patterns

## Android Architecture

- **MVVM + Clean Architecture**: UI (Compose) -> ViewModel -> UseCase -> Repository -> DataSource
- **Feature-based packages**: `feature/check/`, `feature/shield/`, `feature/alerts/`
- **Hilt DI**: `@HiltViewModel`, `@Inject constructor`, module-based provision
- **Room**: `@Entity`, `@Dao`, `@Database` for local scam domain DB + Bloom filter metadata
- **WorkManager**: Background sync of bulk DB updates from R2
- **Navigation Compose**: Single-activity, composable destinations

## Backend Architecture

- **Edge-first**: All user traffic hits Cloudflare Workers (300+ cities), never origin
- **3-layer caching**: Device (SQLite + Bloom) -> Cache API (edge) -> KV (global)
- **Router pattern**: URL pattern matching to handler functions
- **UPSERT for discovery**: Unknown domains written to KV for pipeline review
- **Rate limiting**: Cloudflare Rulesets (not application-level)
- **6 endpoints**: `/api/check`, `/api/report`, `/api/alerts`, `/api/guardian`, `/api/score`, `/api/health`

## Pipeline Architecture

- **Protocol-based DI**: Python `Protocol` classes for testable interfaces
- **Anti-poisoning gates**: Validation checks before publishing to R2/KV
- **SHA-256 verification**: All artifacts signed and verified on download
- **3 feed sources**: SemakMule scraper, scam news scraper, open-source threat feeds
- **Cron-driven**: Scheduled runs on DigitalOcean droplet, pushes to R2

## Data Flow

```
Feeds -> Pipeline (validate + process) -> R2 (bulk artifacts)
                                       -> KV (allowlist + discoveries)
Device <- R2 (sync via WorkManager)
Device -> Workers API (unknown domains only, ~5% of checks)
```

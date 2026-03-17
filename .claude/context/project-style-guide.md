---
created: 2026-03-17T13:24:17Z
last_updated: 2026-03-17T13:24:17Z
version: 1.0
author: Claude Code PM System
---

# Project Style Guide

## Android (Kotlin)

- **UI**: Jetpack Compose, Material 3 components
- **Architecture**: MVVM — `@HiltViewModel` + `StateFlow` + Compose
- **DI**: Hilt `@Inject constructor`, `@Module` + `@Provides` for bindings
- **Database**: Room `@Entity`, `@Dao`, `@Database`; KSP for annotation processing
- **Packages**: Feature-based (`feature/check/`, `feature/shield/`, `feature/alerts/`)
- **Naming**: `PascalCase` classes, `camelCase` functions/properties, `SCREAMING_SNAKE` constants
- **Coroutines**: `suspend` functions in repositories, `viewModelScope` in ViewModels

## Backend (TypeScript)

- **Strict mode**: `strict: true` in tsconfig
- **Testing**: Vitest with `cloudflare:test` imports for Workers-specific testing
- **Router**: URL pattern matching to handler functions
- **Types**: Explicit `Env` interface for Workers bindings (KV, D1, R2)
- **Naming**: `camelCase` functions/variables, `PascalCase` types/interfaces

## Pipeline (Python)

- **Version**: Python 3.12+
- **Testing**: pytest with pytest-asyncio, respx for HTTP mocking
- **Interfaces**: `Protocol` classes (not ABC) for dependency injection
- **Config**: `pyproject.toml` (no setup.py, no requirements.txt)
- **Naming**: `snake_case` functions/variables, `PascalCase` classes

## Git Conventions

- **Commit format**: `E{XX}-{NNN}: description` (issue-level) or `E{XX}: description` (epic-level)
- **Branch naming**: `feature/e{XX}-description` or `fix/e{XX}-description`
- **PR scope**: One epic or sub-task per PR
- **CI**: GitHub Actions for Workers deployment on push to main

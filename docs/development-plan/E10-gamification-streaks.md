# E10: Gamification + Streaks

> **Phase:** 2 (Growth)
> **Priority:** P2 -- Engagement and retention layer
> **Depends On:** E03 (Phone Shield), E07 (Share & Viral Loops)
> **Estimated Effort:** 1-2 weeks

---

## Overview

Gamification adds an engagement layer to Safe Anot? that rewards users for maintaining good security habits. The system tracks daily security streaks (consecutive days with a secure phone), awards achievement badges for milestones and feature usage, and offers a "Spot the Scam" quiz to build scam awareness. This keeps users returning to the app regularly, reinforces positive security behavior, and creates shareable moments that drive organic growth via the existing E07 share infrastructure.

## Technical Specs

- Feature Brainstorm: `docs/BRAINSTORM_FEATURES.md` (Section 11: Gamification & Engagement)
- Prototype: `prototype/index.html` (screen-achievements, screen-quiz)
- Share Infrastructure: E07 (ShareIntentFactory, ShareEventTracking)
- Phone Shield: E03 (security score, audit items)
- Profile: E05 (profile screen, navigation entry point)

## Tech Stack

### Android
- Kotlin + Jetpack Compose + Material 3
- Room (streak data, badge persistence, quiz scores)
- WorkManager (daily streak check)
- Hilt (dependency injection)
- Existing share infrastructure from E07

---

## Issues

### E10-001: Streak Tracking System

Room-based system that tracks consecutive days the user's phone security score has been in the GREEN band (>=80%).

**Acceptance Criteria:**
- `StreakEntity` Room entity storing: current streak count, longest streak count, last check date, streak start date
- `StreakDao` with queries: get current streak, update streak, reset streak
- `StreakRepository` interface + `StreakRepositoryImpl` implementation
- `StreakCheckWorker` (WorkManager) runs daily, reads current security score, increments streak if score >= 80%, resets to 0 if below
- `GetCurrentStreakUseCase` returns reactive Flow of current streak data
- `UpdateStreakUseCase` called by worker and on manual audit completion
- Streak persists across app restarts via Room
- Streak resets to 0 if user misses a day (no check recorded) or score drops below 80%
- Streak data stored as singleton row (id=1) following SecurityScoreEntity pattern

**Test Cases:**
- Streak increments when score >= 80%
- Streak resets when score < 80%
- Streak resets when day is missed (gap in check dates)
- Longest streak updates when current exceeds previous longest
- Worker runs and updates streak correctly
- Streak persists across app restarts

---

### E10-002: Achievement/Badge System

Badge definitions, unlock condition evaluation, and Room persistence for tracking earned badges.

**Acceptance Criteria:**
- `Badge` enum/sealed class defining all badges with: id, title, description, icon resource, unlock condition
- Initial badge set:
  - "Phone Hardened" -- achieve 100% security score
  - "First Scan" -- complete first security audit
  - "Streak Starter" -- maintain 3-day streak
  - "Week Warrior" -- maintain 7-day streak
  - "Month Master" -- maintain 30-day streak
  - "Scam Spotter" -- score 100% on any quiz
  - "Link Checker" -- check first link via E02
  - "Share Guardian" -- share security score via E07
  - "Family Protector" -- set up guardian pairing (if E08 is available)
- `BadgeEntity` Room entity: badge_id (PK), unlocked (Boolean), unlocked_at (Long?)
- `BadgeDao` with queries: get all badges, get unlocked badges, unlock badge
- `BadgeRepository` interface + `BadgeRepositoryImpl`
- `EvaluateBadgesUseCase` checks all badge conditions against current state and unlocks any newly earned badges
- `GetBadgesUseCase` returns Flow<List<BadgeProgress>> with unlock status
- Badge unlock is idempotent -- re-evaluation does not duplicate
- Badge unlock triggers a one-time snackbar/toast notification

**Test Cases:**
- "Phone Hardened" unlocks at 100% score
- "Streak Starter" unlocks at 3-day streak
- Badges persist across app restarts
- Re-evaluation does not duplicate unlocked badges
- Badge conditions evaluated correctly for each badge type
- GetBadgesUseCase returns correct unlock status for all badges

---

### E10-003: Quiz/Challenge Feature

"Spot the Scam" quiz with a question bank, scoring, and badge integration.

**Acceptance Criteria:**
- `QuizQuestion` data class: id, scenario text, options (list of 2-4 choices), correct answer index, explanation text
- `QuizQuestionBank` object with at least 15 hardcoded questions covering: phishing links, fake investment apps, impersonation messages, suspicious APK downloads, social engineering calls
- Questions presented in random order, 5 per quiz session
- `QuizSessionState` tracks: current question index, selected answers, score, isComplete
- `QuizViewModel` manages quiz session state, calculates score, triggers badge evaluation on completion
- Score calculation: correct answers / total questions * 100
- After quiz completion: show score, show correct answers with explanations, "Share My Score" button (uses E07 share infra), unlock "Scam Spotter" badge if score is 100%
- `QuizResultEntity` Room entity storing: session_id, score_percent, completed_at, question_count
- `QuizDao` for persisting quiz results (history)
- `QuizRepository` interface + `QuizRepositoryImpl`

**Test Cases:**
- Quiz presents 5 random questions from the bank
- Score calculates correctly (0%, 20%, 40%, 60%, 80%, 100%)
- "Scam Spotter" badge unlocks only at 100% score
- Quiz results persist to Room
- Questions are shuffled between sessions
- All questions have valid correct answer indices
- Share button generates correct share content

---

### E10-004: UI Screens (Achievements, Quiz, Streak Chip)

Compose UI for the achievements grid, quiz flow, and streak chip on the Shield screen.

**Acceptance Criteria:**
- **AchievementsScreen**: Grid of badge cards showing locked/unlocked state, badge name, icon, description. Unlocked badges show colored icon + unlock date. Locked badges show grayed-out icon + condition hint. Streak banner at top showing current streak count and flame icon. "Take Quiz" CTA button. Navigation via Profile screen entry point.
- **QuizScreen**: Question card with scenario text, multiple-choice options as selectable chips/buttons, progress indicator (e.g., "Question 2 of 5"), next/submit button. Results screen with score, per-question review, share button.
- **Streak chip on Shield screen**: Small chip/banner below the security score ring showing "X day streak" with flame icon. Only visible when streak >= 1.
- **Profile entry point**: "Achievements" row in Profile screen with badge count chip, navigates to AchievementsScreen.
- Navigation routes added to Screen sealed class and SafeAnotNavGraph.
- Bottom bar hidden on Achievements and Quiz screens (following guardian screen pattern).

**Test Cases:**
- AchievementsScreen renders all badges with correct locked/unlocked states
- Streak banner shows correct count and is hidden when streak is 0
- QuizScreen navigates through questions correctly
- QuizScreen shows results after final question
- Streak chip visible on Shield screen when streak >= 1
- Streak chip hidden when streak is 0
- Profile "Achievements" row shows correct badge count
- Navigation to/from Achievements and Quiz screens works

---

## Implementation Order

1. **E10-001** -- Streak tracking system (foundation: Room entity, worker, repository -- needed by badges and UI)
2. **E10-002** -- Achievement/badge system (depends on streak data for streak-based badges)
3. **E10-003** -- Quiz/challenge feature (depends on badge system for "Scam Spotter" unlock)
4. **E10-004** -- UI screens (depends on all data layers being complete)

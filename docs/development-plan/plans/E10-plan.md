# E10 Implementation Plan: Gamification + Streaks

> Generated from: docs/development-plan/E10-gamification-streaks.md
> Technical specs referenced: docs/BRAINSTORM_FEATURES.md (Section 11), prototype/index.html (screen-achievements, screen-quiz)
> Date: 2026-03-20

## Pre-Implementation Checklist

- [ ] Dependencies complete: E03 (Phone Shield) -- DONE, E07 (Share & Viral Loops) -- DONE
- [ ] Technical specs reviewed: BRAINSTORM_FEATURES.md Section 11, E03 epic (security score), E07 epic (share infra)
- [x] Plan reviewed by Codex (10 findings fixed -- see Codex Review Trace)
- [ ] Plan approved by user

---

## Issue E10-001: Streak Tracking System

### Tasks

1. **Create StreakEntity Room entity**
   - File: `android/app/src/main/java/com/safeanot/app/data/local/entity/StreakEntity.kt`
   - Action: Create
   - Details: `@Entity(tableName = "streaks")` with fields: `id: Int = 1` (PK, singleton row following `SecurityScoreEntity` pattern), `currentStreak: Int = 0`, `longestStreak: Int = 0`, `lastCheckDate: Long = 0L` (epoch millis of last daily check), `streakStartDate: Long = 0L` (epoch millis when current streak began). All columns use `@ColumnInfo` with snake_case names.

2. **Create StreakDao**
   - File: `android/app/src/main/java/com/safeanot/app/data/local/StreakDao.kt`
   - Action: Create
   - Details: `@Dao` interface. Methods: `@Query("SELECT * FROM streaks WHERE id = 1") fun observeStreak(): Flow<StreakEntity?>`, `@Query("SELECT * FROM streaks WHERE id = 1") suspend fun getStreakOnce(): StreakEntity?`, `@Upsert suspend fun upsert(entity: StreakEntity)`. Uses `@Upsert` (Room 2.5+) for insert-or-update semantics on the singleton row.

3. **Add StreakEntity, BadgeEntity, QuizResultEntity to SafeAnotDatabase and create ONE atomic migration**
   - File: `android/app/src/main/java/com/safeanot/app/data/local/SafeAnotDatabase.kt`
   - Action: Modify
   - Details: Add `StreakEntity::class`, `BadgeEntity::class`, and `QuizResultEntity::class` to the `@Database entities` array. Bump version from 8 to 9. Add `abstract fun streakDao(): StreakDao`, `abstract fun badgeDao(): BadgeDao`, `abstract fun quizDao(): QuizDao`. Create a single atomic `MIGRATION_8_9` that creates ALL 3 tables in one migration block:
     - `CREATE TABLE IF NOT EXISTS streaks (id INTEGER NOT NULL PRIMARY KEY, current_streak INTEGER NOT NULL DEFAULT 0, longest_streak INTEGER NOT NULL DEFAULT 0, last_check_date INTEGER NOT NULL DEFAULT 0, streak_start_date INTEGER NOT NULL DEFAULT 0)`
     - `CREATE TABLE IF NOT EXISTS badges (badge_id TEXT NOT NULL PRIMARY KEY, unlocked INTEGER NOT NULL DEFAULT 0, unlocked_at INTEGER)`
     - `CREATE TABLE IF NOT EXISTS quiz_results (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, session_id TEXT NOT NULL, score_percent INTEGER NOT NULL, correct_count INTEGER NOT NULL, question_count INTEGER NOT NULL, completed_at INTEGER NOT NULL)`
   - **Note**: This is the ONLY migration for E10. Issues E10-002 and E10-003 reference this same migration -- they do NOT create separate migrations. All 3 tables ship atomically to avoid incremental migration hazards.

4. **Register migration in DatabaseModule**
   - File: `android/app/src/main/java/com/safeanot/app/di/DatabaseModule.kt`
   - Action: Modify
   - Details: Add `SafeAnotDatabase.MIGRATION_8_9` to the `.addMigrations()` chain. Add `@Provides @Singleton fun provideStreakDao(database: SafeAnotDatabase): StreakDao = database.streakDao()`.

5. **Create Streak domain model**
   - File: `android/app/src/main/java/com/safeanot/app/domain/model/Streak.kt`
   - Action: Create
   - Details: `data class Streak(val currentStreak: Int = 0, val longestStreak: Int = 0, val lastCheckDate: Long = 0L, val streakStartDate: Long = 0L)`. Pure domain model with no Room annotations.

6. **Create StreakRepository interface**
   - File: `android/app/src/main/java/com/safeanot/app/domain/repository/StreakRepository.kt`
   - Action: Create
   - Details: Interface with methods: `fun observeStreak(): Flow<Streak>`, `suspend fun getStreak(): Streak`, `suspend fun updateStreak(streak: Streak)`.

7. **Create StreakRepositoryImpl**
   - File: `android/app/src/main/java/com/safeanot/app/data/repository/StreakRepositoryImpl.kt`
   - Action: Create
   - Details: `@Inject constructor(private val streakDao: StreakDao)`. Maps between `StreakEntity` and `Streak` domain model. `observeStreak()` maps Flow, defaulting to `Streak()` when null. `getStreak()` calls `getStreakOnce()` with null fallback. `updateStreak()` maps domain to entity and calls `upsert()`.

8. **Register StreakRepository in RepositoryModule**
   - File: `android/app/src/main/java/com/safeanot/app/di/RepositoryModule.kt`
   - Action: Modify
   - Details: Add `@Binds @Singleton abstract fun bindStreakRepository(impl: StreakRepositoryImpl): StreakRepository`.

9. **Create GetCurrentStreakUseCase**
   - File: `android/app/src/main/java/com/safeanot/app/domain/usecase/GetCurrentStreakUseCase.kt`
   - Action: Create
   - Details: `@Inject constructor(private val streakRepository: StreakRepository)`. `operator fun invoke(): Flow<Streak> = streakRepository.observeStreak()`.

10. **Create UpdateStreakUseCase**
    - File: `android/app/src/main/java/com/safeanot/app/domain/usecase/UpdateStreakUseCase.kt`
    - Action: Create
    - Details: `@Inject constructor(private val streakRepository: StreakRepository, private val evaluateBadgesUseCase: EvaluateBadgesUseCase)`. `suspend operator fun invoke(currentScorePercent: Int): List<BadgeType>` -- returns list of newly unlocked badges (may be empty). Logic: get current streak from repo. **Important**: `lastCheckDate` is stored as epoch millis but comparison must be done in day units to avoid date-unit mismatch. Convert both to days-since-epoch before comparing: `val lastDay = streak.lastCheckDate / (24 * 60 * 60 * 1000L)` and `val today = System.currentTimeMillis() / (24 * 60 * 60 * 1000L)`. **Badge evaluation runs first, unconditionally**: `val newBadges = evaluateBadgesUseCase()` -- this ensures score-based and streak-based badges are evaluated even on same-day repeat calls. Then, if `lastDay == today`, return `newBadges` early (streak already updated today, but badges were still evaluated). If score >= 80 AND (`lastDay == today - 1` OR streak is 0): increment `currentStreak`, update `longestStreak` if exceeded, set `lastCheckDate` to `System.currentTimeMillis()`, set `streakStartDate` if starting fresh. If score >= 80 BUT `today - lastDay > 1`: reset streak to 1 (starting fresh today). If score < 80: reset `currentStreak` to 0, set `lastCheckDate` to `System.currentTimeMillis()`. Save via `streakRepository.updateStreak()`. Return `newBadges`. **Known limitation (MVP)**: clock manipulation by the user (e.g., manually setting the device date forward/backward) is not guarded against; acceptable for MVP.

11. **Create StreakCheckWorker**
    - File: `android/app/src/main/java/com/safeanot/app/worker/StreakCheckWorker.kt`
    - Action: Create
    - Details: `@HiltWorker` class, `@AssistedInject constructor` with `@Assisted appContext: Context`, `@Assisted workerParams: WorkerParameters`, `AuditRepository`, `UpdateStreakUseCase`. In `doWork()`: get current security score from `AuditRepository` (latest `SecurityScoreEntity`), call `val newBadges = updateStreakUseCase(scorePercent)`. If `newBadges` is non-empty, show a system notification for each newly unlocked badge (using `NotificationCompat.Builder` with the app's existing notification channel) -- this is the notification path for badges unlocked via the background worker, where no ViewModel/UI Snackbar is available. Return `Result.success()`. Wrapped in try/catch returning `Result.retry()` on failure. Follows the existing `AuditReminderWorker` pattern.

12. **Create StreakCheckScheduler**
    - File: `android/app/src/main/java/com/safeanot/app/worker/StreakCheckScheduler.kt`
    - Action: Create
    - Details: `@Inject constructor(@ApplicationContext private val context: Context)`. Method `fun schedule()`: enqueues `PeriodicWorkRequestBuilder<StreakCheckWorker>(1, TimeUnit.DAYS)` with `ExistingPeriodicWorkPolicy.KEEP` and unique work name `"streak_check"`. Method `fun cancel()`: cancels by unique work name. Follows `AuditReminderScheduler` pattern.

13. **Schedule streak worker in SafeAnotApp**
    - File: `android/app/src/main/java/com/safeanot/app/SafeAnotApp.kt`
    - Action: Modify
    - Details: Inject `StreakCheckScheduler`. Call `streakCheckScheduler.schedule()` in `onCreate()` alongside existing worker scheduling.

14. **Call UpdateStreakUseCase after manual audit completion**
    - File: `android/app/src/main/java/com/safeanot/app/feature/shield/ShieldViewModel.kt`
    - Action: Modify
    - Details: Inject `UpdateStreakUseCase`. After successful `runAuditUseCase()` in both `runScan()` and `onRefresh()`, call `val newBadges = updateStreakUseCase(securityScore.value.scorePercent)` to update streak immediately when user manually scans. For each badge in `newBadges`, send to `_badgeUnlockEvent` channel (defined in E10-004 task 14) so the UI can show a Snackbar notification.

### Tests

- `android/app/src/test/java/com/safeanot/app/domain/usecase/UpdateStreakUseCaseTest.kt` -- Tests streak increment on score >= 80%, reset on score < 80%, reset on missed day (gap > 1), longest streak tracking, same-day idempotency (streak not updated but badges still evaluated), fresh start when no previous streak exists. Tests that day comparison uses `millis / (24*60*60*1000L)` correctly. Tests that `evaluateBadgesUseCase()` is called even on same-day early return. Tests that newly unlocked badges are returned.
- `android/app/src/test/java/com/safeanot/app/data/repository/StreakRepositoryImplTest.kt` -- Tests entity-to-domain mapping, observe flow emissions, upsert persistence.
- `android/app/src/test/java/com/safeanot/app/worker/StreakCheckWorkerTest.kt` -- Tests worker behavior: calls `updateStreakUseCase` with current security score, returns `Result.success()` on success, returns `Result.retry()` on exception.
- `android/app/src/androidTest/java/com/safeanot/app/data/local/MigrationTest.kt` -- Room migration test (androidTest). Uses `MigrationTestHelper` to verify `MIGRATION_8_9` correctly creates all 3 tables (`streaks`, `badges`, `quiz_results`) with expected schemas. Validates columns, types, and defaults.

### Acceptance Criteria

- `StreakEntity` persists streak data in Room as singleton row
- Daily worker checks security score and increments/resets streak
- Streak increments when score >= 80% on consecutive days
- Streak resets to 0 when score drops below 80%
- Streak resets when a day is missed (no check recorded)
- Longest streak tracked independently
- Streak updates immediately on manual audit completion
- Streak data available as reactive Flow

---

## Issue E10-002: Achievement/Badge System

### Tasks

1. **Create Badge sealed class with all badge definitions**
   - File: `android/app/src/main/java/com/safeanot/app/domain/model/Badge.kt`
   - Action: Create
   - Details: `enum class BadgeType` with entries: `PHONE_HARDENED`, `FIRST_SCAN`, `STREAK_STARTER`, `WEEK_WARRIOR`, `MONTH_MASTER`, `SCAM_SPOTTER`, `LINK_CHECKER`, `SHARE_GUARDIAN`, `FAMILY_PROTECTOR`. Companion object with `fun allBadges(): List<BadgeInfo>` returning a list of `BadgeInfo(type: BadgeType, title: String, description: String, conditionHint: String, icon: String)` where `icon` is a Material Symbol name for the badge (used by `BadgeCard` composable). Examples: `BadgeInfo(PHONE_HARDENED, "Phone Hardened", "Achieved 100% security score", "Get 100% security score", icon = "shield")`, `BadgeInfo(FIRST_SCAN, "First Scan", "Completed your first security scan", "Complete a security scan", icon = "search")`, `BadgeInfo(STREAK_STARTER, ..., icon = "local_fire_department")`, `BadgeInfo(WEEK_WARRIOR, ..., icon = "local_fire_department")`, `BadgeInfo(MONTH_MASTER, ..., icon = "local_fire_department")`, `BadgeInfo(SCAM_SPOTTER, ..., icon = "school")`, `BadgeInfo(LINK_CHECKER, ..., icon = "link")`, `BadgeInfo(SHARE_GUARDIAN, ..., icon = "share")`, `BadgeInfo(FAMILY_PROTECTOR, ..., icon = "people")`.

2. **Create BadgeProgress domain model**
   - File: `android/app/src/main/java/com/safeanot/app/domain/model/BadgeProgress.kt`
   - Action: Create
   - Details: `data class BadgeProgress(val badge: BadgeInfo, val isUnlocked: Boolean, val unlockedAt: Long? = null)`. Used by the UI to render locked/unlocked state.

3. **Create BadgeEntity Room entity**
   - File: `android/app/src/main/java/com/safeanot/app/data/local/entity/BadgeEntity.kt`
   - Action: Create
   - Details: `@Entity(tableName = "badges")` with: `@PrimaryKey val badgeId: String` (maps to `BadgeType.name`), `@ColumnInfo(name = "unlocked") val unlocked: Boolean = false`, `@ColumnInfo(name = "unlocked_at") val unlockedAt: Long? = null`.

4. **Create BadgeDao**
   - File: `android/app/src/main/java/com/safeanot/app/data/local/BadgeDao.kt`
   - Action: Create
   - Details: `@Dao` interface. Methods: `@Query("SELECT * FROM badges") fun observeAll(): Flow<List<BadgeEntity>>`, `@Query("SELECT * FROM badges WHERE badge_id = :badgeId") suspend fun getById(badgeId: String): BadgeEntity?`, `@Upsert suspend fun upsert(entity: BadgeEntity)`, `@Query("SELECT COUNT(*) FROM badges WHERE unlocked = 1") fun observeUnlockedCount(): Flow<Int>`.

5. **Add BadgeEntity to SafeAnotDatabase (already handled in E10-001 task 3)**
   - File: `android/app/src/main/java/com/safeanot/app/data/local/SafeAnotDatabase.kt`
   - Action: No-op (already done in E10-001 task 3)
   - Details: `BadgeEntity::class`, `abstract fun badgeDao(): BadgeDao`, and the `badges` CREATE TABLE statement are all part of the single atomic `MIGRATION_8_9` defined in E10-001 task 3. No additional migration work needed here.

6. **Register BadgeDao in DatabaseModule**
   - File: `android/app/src/main/java/com/safeanot/app/di/DatabaseModule.kt`
   - Action: Modify
   - Details: Add `@Provides @Singleton fun provideBadgeDao(database: SafeAnotDatabase): BadgeDao = database.badgeDao()`.

7. **Create BadgeRepository interface**
   - File: `android/app/src/main/java/com/safeanot/app/domain/repository/BadgeRepository.kt`
   - Action: Create
   - Details: Interface with methods: `fun observeAllBadges(): Flow<List<BadgeProgress>>`, `fun observeUnlockedCount(): Flow<Int>`, `suspend fun unlockBadge(type: BadgeType): Boolean` (returns true if newly unlocked, false if already unlocked).

8. **Create BadgeRepositoryImpl**
   - File: `android/app/src/main/java/com/safeanot/app/data/repository/BadgeRepositoryImpl.kt`
   - Action: Create
   - Details: `@Inject constructor(private val badgeDao: BadgeDao)`. `observeAllBadges()`: combines `badgeDao.observeAll()` with `BadgeType.allBadges()` to produce `List<BadgeProgress>` -- for each `BadgeInfo`, find matching entity (or default to locked). `unlockBadge()`: checks if already unlocked via `getById()`, if not, calls `upsert()` with `unlocked = true, unlockedAt = System.currentTimeMillis()`, returns true. If already unlocked, returns false.

9. **Register BadgeRepository in RepositoryModule**
   - File: `android/app/src/main/java/com/safeanot/app/di/RepositoryModule.kt`
   - Action: Modify
   - Details: Add `@Binds @Singleton abstract fun bindBadgeRepository(impl: BadgeRepositoryImpl): BadgeRepository`.

10. **Create GetBadgesUseCase**
    - File: `android/app/src/main/java/com/safeanot/app/domain/usecase/GetBadgesUseCase.kt`
    - Action: Create
    - Details: `@Inject constructor(private val badgeRepository: BadgeRepository)`. `operator fun invoke(): Flow<List<BadgeProgress>> = badgeRepository.observeAllBadges()`.

10b. **Create UnlockBadgeUseCase**
    - File: `android/app/src/main/java/com/safeanot/app/domain/usecase/UnlockBadgeUseCase.kt`
    - Action: Create
    - Details: `@Inject constructor(private val badgeRepository: BadgeRepository)`. `suspend operator fun invoke(type: BadgeType): Boolean = badgeRepository.unlockBadge(type)`. This is a thin wrapper ensuring ViewModels never inject `BadgeRepository` directly, following the project's clean architecture convention (VMs depend on use cases only).

11. **Create EvaluateBadgesUseCase**
    - File: `android/app/src/main/java/com/safeanot/app/domain/usecase/EvaluateBadgesUseCase.kt`
    - Action: Create
    - Details: `@Inject constructor(private val badgeRepository: BadgeRepository, private val streakRepository: StreakRepository, private val auditRepository: AuditRepository)`. `suspend operator fun invoke(): List<BadgeType>` -- returns list of newly unlocked badges. Evaluation logic:
      - `FIRST_SCAN`: `auditRepository` has any completed audit (security score > 0 or audit count > 0)
      - `PHONE_HARDENED`: current security score == 100
      - `STREAK_STARTER`: current streak >= 3
      - `WEEK_WARRIOR`: current streak >= 7
      - `MONTH_MASTER`: current streak >= 30
      - `LINK_CHECKER`, `SHARE_GUARDIAN`, `FAMILY_PROTECTOR`, `SCAM_SPOTTER`: unlocked by their respective features calling `badgeRepository.unlockBadge()` directly (not evaluated here -- those features trigger unlock themselves)
    - For each condition met, calls `badgeRepository.unlockBadge()` and collects newly unlocked badges.

12. **Badge evaluation is already integrated into UpdateStreakUseCase (see E10-001 task 10)**
    - File: `android/app/src/main/java/com/safeanot/app/domain/usecase/UpdateStreakUseCase.kt`
    - Action: No-op (already done in E10-001 task 10)
    - Details: `EvaluateBadgesUseCase` is injected directly in `UpdateStreakUseCase` (E10-001 task 10) and called **before** the same-day early return, ensuring badges are always evaluated. `UpdateStreakUseCase.invoke()` returns `List<BadgeType>` of newly unlocked badges, which callers (StreakCheckWorker, ShieldViewModel) can use to emit notification events.

13. **Trigger "Link Checker" badge unlock in CheckViewModel**
    - File: `android/app/src/main/java/com/safeanot/app/feature/check/CheckViewModel.kt`
    - Action: Modify
    - Details: Inject `UnlockBadgeUseCase`. After a successful link check (verdict received), call `unlockBadgeUseCase(BadgeType.LINK_CHECKER)` in a viewModelScope launch.

14. **Trigger "Share Guardian" badge unlock in ShieldViewModel**
    - File: `android/app/src/main/java/com/safeanot/app/feature/shield/ShieldViewModel.kt`
    - Action: Modify
    - Details: Inject `UnlockBadgeUseCase`. In `onShareCompleted()`, call `unlockBadgeUseCase(BadgeType.SHARE_GUARDIAN)`.

15. **Trigger "Family Protector" badge unlock on guardian pairing**
    - File: `android/app/src/main/java/com/safeanot/app/feature/guardian/GuardianPairingViewModel.kt` (or `GuardianRepositoryImpl.kt`)
    - Action: Modify
    - Details: Inject `UnlockBadgeUseCase`. After a guardian pairing is successfully created, call `unlockBadgeUseCase(BadgeType.FAMILY_PROTECTOR)` in a `viewModelScope.launch`. This ensures the badge is actually unlocked when its condition is met, rather than relying on the user to navigate to a separate evaluation flow.

### Tests

- `android/app/src/test/java/com/safeanot/app/domain/usecase/EvaluateBadgesUseCaseTest.kt` -- Tests each badge condition: FIRST_SCAN unlocks after audit, PHONE_HARDENED at 100%, streak badges at 3/7/30, idempotent re-evaluation, returns only newly unlocked badges.
- `android/app/src/test/java/com/safeanot/app/data/repository/BadgeRepositoryImplTest.kt` -- Tests observeAllBadges combines definitions with entities, unlockBadge is idempotent, newly unlocked returns true, already unlocked returns false.

### Acceptance Criteria

- 9 badge types defined with title, description, and condition hint
- `BadgeEntity` persists unlock state in Room
- `EvaluateBadgesUseCase` checks score-based and streak-based conditions
- Feature-triggered badges (Link Checker, Share Guardian, Scam Spotter, Family Protector) unlocked by their respective features directly
- Badge unlock is idempotent -- re-evaluation does not duplicate
- Badge data available as reactive Flow with locked/unlocked state
- Unlocked badge count available as Flow for profile badge count chip

---

## Issue E10-003: Quiz/Challenge Feature

### Tasks

1. **Create QuizQuestion data class**
   - File: `android/app/src/main/java/com/safeanot/app/domain/model/QuizQuestion.kt`
   - Action: Create
   - Details: `data class QuizQuestion(val id: String, val scenario: String, val options: List<String>, val correctIndex: Int, val explanation: String)`.

2. **Create QuizQuestionBank**
   - File: `android/app/src/main/java/com/safeanot/app/domain/model/QuizQuestionBank.kt`
   - Action: Create
   - Details: `object QuizQuestionBank` with `val questions: List<QuizQuestion>` containing 15+ hardcoded questions. Categories: phishing links (e.g., "You receive a message: 'Your Maybank account has been locked. Click here to verify.' What should you do?"), fake investment apps (e.g., "A friend invites you to download a trading app from a WhatsApp link. Is this safe?"), impersonation messages, suspicious APK downloads, social engineering calls. Each question has 2-4 options, a correct answer index, and an explanation. Method `fun getRandomQuiz(count: Int = 5): List<QuizQuestion>` returns `count` shuffled questions.

3. **Create QuizResultEntity Room entity**
   - File: `android/app/src/main/java/com/safeanot/app/data/local/entity/QuizResultEntity.kt`
   - Action: Create
   - Details: `@Entity(tableName = "quiz_results")` with: `@PrimaryKey(autoGenerate = true) val id: Long = 0`, `@ColumnInfo(name = "session_id") val sessionId: String` (UUID string, generated via `UUID.randomUUID().toString()` at quiz start -- uniquely identifies each quiz session for analytics and deduplication), `@ColumnInfo(name = "score_percent") val scorePercent: Int`, `@ColumnInfo(name = "correct_count") val correctCount: Int`, `@ColumnInfo(name = "question_count") val questionCount: Int`, `@ColumnInfo(name = "completed_at") val completedAt: Long`.

4. **Create QuizDao**
   - File: `android/app/src/main/java/com/safeanot/app/data/local/QuizDao.kt`
   - Action: Create
   - Details: `@Dao` interface. Methods: `@Insert suspend fun insert(result: QuizResultEntity)`, `@Query("SELECT * FROM quiz_results ORDER BY completed_at DESC") fun observeResults(): Flow<List<QuizResultEntity>>`, `@Query("SELECT MAX(score_percent) FROM quiz_results") suspend fun getBestScore(): Int?`.

5. **Add QuizResultEntity to SafeAnotDatabase (already handled in E10-001 task 3)**
   - File: `android/app/src/main/java/com/safeanot/app/data/local/SafeAnotDatabase.kt`
   - Action: No-op (already done in E10-001 task 3)
   - Details: `QuizResultEntity::class`, `abstract fun quizDao(): QuizDao`, and the `quiz_results` CREATE TABLE statement are all part of the single atomic `MIGRATION_8_9` defined in E10-001 task 3. No additional migration work needed here.

6. **Register QuizDao in DatabaseModule**
   - File: `android/app/src/main/java/com/safeanot/app/di/DatabaseModule.kt`
   - Action: Modify
   - Details: Add `@Provides @Singleton fun provideQuizDao(database: SafeAnotDatabase): QuizDao = database.quizDao()`.

7. **Create QuizRepository interface**
   - File: `android/app/src/main/java/com/safeanot/app/domain/repository/QuizRepository.kt`
   - Action: Create
   - Details: Interface with methods: `suspend fun saveResult(sessionId: String, scorePercent: Int, correctCount: Int, questionCount: Int)`, `fun observeResults(): Flow<List<QuizResult>>`, `suspend fun getBestScore(): Int?`.

8. **Create QuizResult domain model**
   - File: `android/app/src/main/java/com/safeanot/app/domain/model/QuizResult.kt`
   - Action: Create
   - Details: `data class QuizResult(val id: Long, val sessionId: String, val scorePercent: Int, val correctCount: Int, val questionCount: Int, val completedAt: Long)`.

9. **Create QuizRepositoryImpl**
   - File: `android/app/src/main/java/com/safeanot/app/data/repository/QuizRepositoryImpl.kt`
   - Action: Create
   - Details: `@Inject constructor(private val quizDao: QuizDao)`. Maps between entity and domain model. `saveResult()` creates entity with provided `sessionId` and `completedAt = System.currentTimeMillis()` and calls `insert()`.

10. **Register QuizRepository in RepositoryModule**
    - File: `android/app/src/main/java/com/safeanot/app/di/RepositoryModule.kt`
    - Action: Modify
    - Details: Add `@Binds @Singleton abstract fun bindQuizRepository(impl: QuizRepositoryImpl): QuizRepository`.

11. **Create QuizViewModel**
    - File: `android/app/src/main/java/com/safeanot/app/feature/quiz/QuizViewModel.kt`
    - Action: Create
    - Details: `@HiltViewModel @Inject constructor(private val quizRepository: QuizRepository, private val unlockBadgeUseCase: UnlockBadgeUseCase, private val trackShareEventUseCase: TrackShareEventUseCase)`. State: `QuizUiState(questions: List<QuizQuestion> = emptyList(), currentIndex: Int = 0, selectedAnswers: Map<Int, Int> = emptyMap(), isComplete: Boolean = false, scorePercent: Int = 0, correctCount: Int = 0, sessionId: String = "")`. Methods: `startQuiz()` -- generates `sessionId = UUID.randomUUID().toString()`, loads 5 random questions from `QuizQuestionBank.getRandomQuiz()`. `selectAnswer(questionIndex: Int, optionIndex: Int)` -- records answer. `nextQuestion()` -- increments index or completes quiz if last. `completeQuiz()` -- calculates score, saves result via `quizRepository.saveResult(sessionId, scorePercent, correctCount, questionCount)`, if 100% calls `unlockBadgeUseCase(BadgeType.SCAM_SPOTTER)`. No Context dependency -- share event emitted as domain data (following ShieldViewModel pattern with Channel<ShareEvent>).

### Tests

- `android/app/src/test/java/com/safeanot/app/feature/quiz/QuizViewModelTest.kt` -- Tests quiz flow: start loads 5 questions, answer selection, score calculation at 0/20/40/60/80/100%, badge unlock at 100% only, result saved to repository. Tests share event: after quiz completion, `onShare()` emits a `ShareEvent` via channel with expected content string (e.g., "I scored X% on SafeAnot's Spot the Scam quiz!").
- `android/app/src/test/java/com/safeanot/app/domain/model/QuizQuestionBankTest.kt` -- Tests question bank has >= 15 questions, all have valid correctIndex within options range, getRandomQuiz returns requested count, shuffled order.
- `android/app/src/test/java/com/safeanot/app/data/repository/QuizRepositoryImplTest.kt` -- Tests save and observe results, best score query.

### Acceptance Criteria

- 15+ hardcoded quiz questions covering 5 scam categories
- Quiz presents 5 random questions per session
- Score calculation: correctAnswers / totalQuestions * 100
- "Scam Spotter" badge unlocks at 100% score
- Quiz results persist to Room for history
- Questions shuffled between sessions
- Share capability uses existing E07 infrastructure
- ViewModel follows no-Context pattern (ShareEvent via Channel)

---

## Issue E10-004: UI Screens (Achievements, Quiz, Streak Chip)

### Tasks

1. **Add navigation routes to Screen sealed class**
   - File: `android/app/src/main/java/com/safeanot/app/navigation/Screen.kt`
   - Action: Modify
   - Details: Add `data object Achievements : Screen("achievements")` and `data object Quiz : Screen("quiz")`.

2. **Create AchievementsViewModel**
   - File: `android/app/src/main/java/com/safeanot/app/feature/achievements/AchievementsViewModel.kt`
   - Action: Create
   - Details: `@HiltViewModel @Inject constructor(getBadgesUseCase: GetBadgesUseCase, getCurrentStreakUseCase: GetCurrentStreakUseCase)`. State: `AchievementsUiState(badges: List<BadgeProgress> = emptyList(), streak: Streak = Streak())`. Collects both flows in `init` and updates state.

3. **Create BadgeCard composable**
   - File: `android/app/src/main/java/com/safeanot/app/feature/achievements/components/BadgeCard.kt`
   - Action: Create
   - Details: `@Composable fun BadgeCard(badgeProgress: BadgeProgress)`. Material 3 card showing badge icon resolved from `badgeProgress.badge.icon` (Material Symbol name) via a helper `fun badgeIcon(name: String): ImageVector` that maps icon names to `Icons.Default` vectors (e.g., `"shield"` -> `Icons.Default.Shield`, `"search"` -> `Icons.Default.Search`, `"local_fire_department"` -> `Icons.Default.LocalFireDepartment`, `"school"` -> `Icons.Default.School`, `"link"` -> `Icons.Default.Link`, `"share"` -> `Icons.Default.Share`, `"people"` -> `Icons.Default.People`). Unlocked: colored icon + title + unlock date in relative time. Locked: grayed-out icon + title + condition hint text. Uses `DarkCard` background color from existing theme.

4. **Create StreakBanner composable**
   - File: `android/app/src/main/java/com/safeanot/app/feature/achievements/components/StreakBanner.kt`
   - Action: Create
   - Details: `@Composable fun StreakBanner(streak: Streak)`. Full-width card with flame icon, current streak count in large text ("X Day Streak!"), longest streak below in smaller text. Uses `GreenAccent` from theme when streak is active. Hidden when `streak.currentStreak == 0` (caller checks).

5. **Create AchievementsScreen**
   - File: `android/app/src/main/java/com/safeanot/app/feature/achievements/AchievementsScreen.kt`
   - Action: Create
   - Details: `@Composable fun AchievementsScreen(onNavigateBack: () -> Unit, onNavigateToQuiz: () -> Unit, viewModel: AchievementsViewModel = hiltViewModel())`. Top app bar with "Achievements" title and back arrow. Content: `StreakBanner` at top (if streak > 0), "Take Quiz" button (CTA card with brain icon), then `LazyVerticalGrid(columns = GridCells.Fixed(2))` of `BadgeCard` items. Follows scaffold pattern from `GuardianDashboardScreen`.

6. **Create QuizQuestionCard composable**
   - File: `android/app/src/main/java/com/safeanot/app/feature/quiz/components/QuizQuestionCard.kt`
   - Action: Create
   - Details: `@Composable fun QuizQuestionCard(question: QuizQuestion, questionNumber: Int, totalQuestions: Int, selectedAnswer: Int?, onSelectAnswer: (Int) -> Unit)`. Card with progress text ("Question X of Y"), scenario text in body, options as selectable `FilterChip` buttons (following `AlertRegionChips` pattern). Selected option highlighted with `BlueAccent`.

7. **Create QuizResultCard composable**
   - File: `android/app/src/main/java/com/safeanot/app/feature/quiz/components/QuizResultCard.kt`
   - Action: Create
   - Details: `@Composable fun QuizResultCard(scorePercent: Int, correctCount: Int, totalCount: Int, questions: List<QuizQuestion>, selectedAnswers: Map<Int, Int>, onShare: () -> Unit, onRetry: () -> Unit)`. Shows score percentage in large text with color coding (red/amber/green using `ScoreBand.fromPercent()`), per-question review list (correct = green check, wrong = red X with correct answer shown), explanation text for each question, "Share My Score" button, "Try Again" button.

8. **Create QuizScreen**
   - File: `android/app/src/main/java/com/safeanot/app/feature/quiz/QuizScreen.kt`
   - Action: Create
   - Details: `@Composable fun QuizScreen(onNavigateBack: () -> Unit, viewModel: QuizViewModel = hiltViewModel())`. Top app bar with "Spot the Scam" title and back arrow. When `!isComplete`: renders `QuizQuestionCard` for current question + "Next" / "Submit" button. When `isComplete`: renders `QuizResultCard`. Calls `viewModel.startQuiz()` via `LaunchedEffect(Unit)` on first composition. Handles share events from ViewModel channel (following `ShieldScreen` pattern for share intent launching).

9. **Create StreakChip composable for Shield screen**
   - File: `android/app/src/main/java/com/safeanot/app/feature/shield/components/StreakChip.kt`
   - Action: Create
   - Details: `@Composable fun StreakChip(streakDays: Int)`. Small `AssistChip` or `SuggestionChip` with flame icon and text "X day streak". Uses `GreenAccent` container color. Only renders when `streakDays >= 1` (caller checks visibility).

10. **Add streak chip to ShieldScreen**
    - File: `android/app/src/main/java/com/safeanot/app/feature/shield/ShieldScreen.kt`
    - Action: Modify
    - Details: Import and render `StreakChip` below the `SecurityScoreRing`. Add `streak` state from `ShieldViewModel` (see task 11). Conditionally show when `streak.currentStreak >= 1`.

11. **Expose streak data from ShieldViewModel**
    - File: `android/app/src/main/java/com/safeanot/app/feature/shield/ShieldViewModel.kt`
    - Action: Modify
    - Details: Inject `GetCurrentStreakUseCase`. Add `val streak: StateFlow<Streak> = getCurrentStreakUseCase().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Streak())`.

12. **Add "Achievements" entry point to ProfileScreen**
    - File: `android/app/src/main/java/com/safeanot/app/feature/profile/ProfileScreen.kt`
    - Action: Modify
    - Details: Add `onNavigateToAchievements: () -> Unit` parameter. Add a clickable row/card between existing Guardian rows and About section: icon (trophy/star), "Achievements" label, badge count chip showing unlocked count, chevron right icon. Follow the existing guardian navigation row pattern.

13. **Expose badge count in ProfileViewModel**
    - File: `android/app/src/main/java/com/safeanot/app/feature/profile/ProfileViewModel.kt`
    - Action: Modify
    - Details: Inject `GetBadgesUseCase`. Add `val unlockedBadgeCount: StateFlow<Int> = getBadgesUseCase().map { badges -> badges.count { it.isUnlocked } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)`. Note: VMs must not inject repositories directly; use cases enforce this architectural boundary.

14. **Add badge unlock notification event to ViewModels**
    - File: `android/app/src/main/java/com/safeanot/app/feature/shield/ShieldViewModel.kt` (and other VMs that call `unlockBadgeUseCase`)
    - Action: Modify
    - Details: Add a `private val _badgeUnlockEvent = Channel<BadgeType>(Channel.BUFFERED)` and expose `val badgeUnlockEvent = _badgeUnlockEvent.receiveAsFlow()`. After calling `unlockBadgeUseCase(type)`, if it returns `true` (newly unlocked), send the badge type to the channel: `_badgeUnlockEvent.send(type)`. In the corresponding Screen composables, collect this flow via `LaunchedEffect` and show a Snackbar with text like "Badge Unlocked: {badge.title}!" using the scaffold's `SnackbarHostState`. Apply the same pattern in `CheckViewModel`, `QuizViewModel`, and `GuardianPairingViewModel`.

15. **Register routes in SafeAnotNavGraph**
    - File: `android/app/src/main/java/com/safeanot/app/navigation/SafeAnotNavGraph.kt`
    - Action: Modify
    - Details: Import `AchievementsScreen` and `QuizScreen`. Add composable routes: `composable(Screen.Achievements.route) { AchievementsScreen(onNavigateBack = { navController.popBackStack() }, onNavigateToQuiz = { navController.navigate(Screen.Quiz.route) }) }` and `composable(Screen.Quiz.route) { QuizScreen(onNavigateBack = { navController.popBackStack() }) }`. Wire `ProfileScreen` `onNavigateToAchievements` to `navController.navigate(Screen.Achievements.route)`. Update bottom bar hide logic to include `achievements` and `quiz` routes (add to the `startsWith` checks).

### Tests

- `android/app/src/test/java/com/safeanot/app/feature/achievements/AchievementsViewModelTest.kt` -- Tests badges and streak state collected from use cases, initial state shows all badges.
- `android/app/src/test/java/com/safeanot/app/feature/shield/ShieldViewModelTest.kt` -- Update existing test to verify streak StateFlow is exposed and emits default Streak when no data.
- `android/app/src/androidTest/java/com/safeanot/app/navigation/GamificationNavigationTest.kt` -- Navigation tests: Profile -> Achievements route works, Achievements -> Quiz route works, back navigation returns correctly, bottom bar hidden on Achievements and Quiz screens.

### Acceptance Criteria

- AchievementsScreen shows badge grid with locked/unlocked states
- Streak banner visible when streak >= 1, hidden when 0
- "Take Quiz" CTA navigates to QuizScreen
- QuizScreen shows questions with selectable options
- QuizScreen shows results with per-question review after completion
- Streak chip visible on Shield screen when streak >= 1
- Profile screen has "Achievements" row with unlocked badge count
- Navigation to/from Achievements and Quiz screens works correctly
- Bottom bar hidden on Achievements and Quiz screens

---

## Implementation Order

Recommended sequence (respects internal dependencies):

1. **E10-001** -- Streak tracking system. Foundation for everything: Room entity, daily worker, and repository. Required by badge streak conditions and streak chip UI.
2. **E10-002** -- Achievement/badge system. Depends on streak data from E10-001 for streak-based badge conditions. Also hooks into existing CheckViewModel and ShieldViewModel for feature-triggered badges.
3. **E10-003** -- Quiz/challenge feature. Depends on badge repository from E10-002 to unlock "Scam Spotter" badge. Self-contained question bank and quiz flow.
4. **E10-004** -- UI screens. Depends on all data layers (streak, badge, quiz) being complete. Wires everything together with Compose screens and navigation.

## Files Summary

| File | Action | Issues |
|------|--------|--------|
| `android/app/src/main/java/com/safeanot/app/data/local/entity/StreakEntity.kt` | Create | E10-001 |
| `android/app/src/main/java/com/safeanot/app/data/local/StreakDao.kt` | Create | E10-001 |
| `android/app/src/main/java/com/safeanot/app/data/local/SafeAnotDatabase.kt` | Modify | E10-001, E10-002, E10-003 |
| `android/app/src/main/java/com/safeanot/app/di/DatabaseModule.kt` | Modify | E10-001, E10-002, E10-003 |
| `android/app/src/main/java/com/safeanot/app/domain/model/Streak.kt` | Create | E10-001 |
| `android/app/src/main/java/com/safeanot/app/domain/repository/StreakRepository.kt` | Create | E10-001 |
| `android/app/src/main/java/com/safeanot/app/data/repository/StreakRepositoryImpl.kt` | Create | E10-001 |
| `android/app/src/main/java/com/safeanot/app/di/RepositoryModule.kt` | Modify | E10-001, E10-002, E10-003 |
| `android/app/src/main/java/com/safeanot/app/domain/usecase/GetCurrentStreakUseCase.kt` | Create | E10-001 |
| `android/app/src/main/java/com/safeanot/app/domain/usecase/UpdateStreakUseCase.kt` | Create | E10-001, E10-002 |
| `android/app/src/main/java/com/safeanot/app/worker/StreakCheckWorker.kt` | Create | E10-001 |
| `android/app/src/main/java/com/safeanot/app/worker/StreakCheckScheduler.kt` | Create | E10-001 |
| `android/app/src/main/java/com/safeanot/app/SafeAnotApp.kt` | Modify | E10-001 |
| `android/app/src/main/java/com/safeanot/app/feature/shield/ShieldViewModel.kt` | Modify | E10-001, E10-002, E10-004 |
| `android/app/src/main/java/com/safeanot/app/domain/model/Badge.kt` | Create | E10-002 |
| `android/app/src/main/java/com/safeanot/app/domain/model/BadgeProgress.kt` | Create | E10-002 |
| `android/app/src/main/java/com/safeanot/app/data/local/entity/BadgeEntity.kt` | Create | E10-002 |
| `android/app/src/main/java/com/safeanot/app/data/local/BadgeDao.kt` | Create | E10-002 |
| `android/app/src/main/java/com/safeanot/app/domain/repository/BadgeRepository.kt` | Create | E10-002 |
| `android/app/src/main/java/com/safeanot/app/data/repository/BadgeRepositoryImpl.kt` | Create | E10-002 |
| `android/app/src/main/java/com/safeanot/app/domain/usecase/GetBadgesUseCase.kt` | Create | E10-002 |
| `android/app/src/main/java/com/safeanot/app/domain/usecase/UnlockBadgeUseCase.kt` | Create | E10-002 |
| `android/app/src/main/java/com/safeanot/app/domain/usecase/EvaluateBadgesUseCase.kt` | Create | E10-002 |
| `android/app/src/main/java/com/safeanot/app/feature/check/CheckViewModel.kt` | Modify | E10-002 |
| `android/app/src/main/java/com/safeanot/app/feature/guardian/GuardianPairingViewModel.kt` | Modify | E10-002 |
| `android/app/src/main/java/com/safeanot/app/domain/model/QuizQuestion.kt` | Create | E10-003 |
| `android/app/src/main/java/com/safeanot/app/domain/model/QuizQuestionBank.kt` | Create | E10-003 |
| `android/app/src/main/java/com/safeanot/app/domain/model/QuizResult.kt` | Create | E10-003 |
| `android/app/src/main/java/com/safeanot/app/data/local/entity/QuizResultEntity.kt` | Create | E10-003 |
| `android/app/src/main/java/com/safeanot/app/data/local/QuizDao.kt` | Create | E10-003 |
| `android/app/src/main/java/com/safeanot/app/domain/repository/QuizRepository.kt` | Create | E10-003 |
| `android/app/src/main/java/com/safeanot/app/data/repository/QuizRepositoryImpl.kt` | Create | E10-003 |
| `android/app/src/main/java/com/safeanot/app/feature/quiz/QuizViewModel.kt` | Create | E10-003 |
| `android/app/src/main/java/com/safeanot/app/navigation/Screen.kt` | Modify | E10-004 |
| `android/app/src/main/java/com/safeanot/app/feature/achievements/AchievementsViewModel.kt` | Create | E10-004 |
| `android/app/src/main/java/com/safeanot/app/feature/achievements/components/BadgeCard.kt` | Create | E10-004 |
| `android/app/src/main/java/com/safeanot/app/feature/achievements/components/StreakBanner.kt` | Create | E10-004 |
| `android/app/src/main/java/com/safeanot/app/feature/achievements/AchievementsScreen.kt` | Create | E10-004 |
| `android/app/src/main/java/com/safeanot/app/feature/quiz/components/QuizQuestionCard.kt` | Create | E10-004 |
| `android/app/src/main/java/com/safeanot/app/feature/quiz/components/QuizResultCard.kt` | Create | E10-004 |
| `android/app/src/main/java/com/safeanot/app/feature/quiz/QuizScreen.kt` | Create | E10-004 |
| `android/app/src/main/java/com/safeanot/app/feature/shield/components/StreakChip.kt` | Create | E10-004 |
| `android/app/src/main/java/com/safeanot/app/feature/shield/ShieldScreen.kt` | Modify | E10-004 |
| `android/app/src/main/java/com/safeanot/app/feature/profile/ProfileScreen.kt` | Modify | E10-004 |
| `android/app/src/main/java/com/safeanot/app/feature/profile/ProfileViewModel.kt` | Modify | E10-004 |
| `android/app/src/main/java/com/safeanot/app/navigation/SafeAnotNavGraph.kt` | Modify | E10-004 |
| `android/app/src/test/java/com/safeanot/app/domain/usecase/UpdateStreakUseCaseTest.kt` | Create | E10-001 |
| `android/app/src/test/java/com/safeanot/app/data/repository/StreakRepositoryImplTest.kt` | Create | E10-001 |
| `android/app/src/test/java/com/safeanot/app/domain/usecase/EvaluateBadgesUseCaseTest.kt` | Create | E10-002 |
| `android/app/src/test/java/com/safeanot/app/data/repository/BadgeRepositoryImplTest.kt` | Create | E10-002 |
| `android/app/src/test/java/com/safeanot/app/feature/quiz/QuizViewModelTest.kt` | Create | E10-003 |
| `android/app/src/test/java/com/safeanot/app/domain/model/QuizQuestionBankTest.kt` | Create | E10-003 |
| `android/app/src/test/java/com/safeanot/app/data/repository/QuizRepositoryImplTest.kt` | Create | E10-003 |
| `android/app/src/test/java/com/safeanot/app/feature/achievements/AchievementsViewModelTest.kt` | Create | E10-004 |
| `android/app/src/test/java/com/safeanot/app/worker/StreakCheckWorkerTest.kt` | Create | E10-001 |
| `android/app/src/androidTest/java/com/safeanot/app/data/local/MigrationTest.kt` | Create | E10-001 |
| `android/app/src/androidTest/java/com/safeanot/app/navigation/GamificationNavigationTest.kt` | Create | E10-004 |

---

## Codex Review Trace

| # | Severity | Finding | Fix Applied | Issues Affected |
|---|----------|---------|-------------|-----------------|
| 1 | HIGH | Room migration unsafe for incremental shipping -- MIGRATION_8_9 mutated across 3 issues, risking partial migrations on intermediate deploys | Made ONE atomic migration in E10-001 task 3 that creates ALL 3 tables (streaks, badges, quiz_results). E10-002 task 5 and E10-003 task 5 changed to no-op referencing E10-001 task 3. | E10-001, E10-002, E10-003 |
| 2 | HIGH | UpdateStreakUseCase date-unit bug -- lastCheckDate stored as epoch millis but compared as days, causing streak logic to never match "same day" or "yesterday" | Explicitly convert to days in comparison: `val lastDay = lastCheckDate / (24 * 60 * 60 * 1000L)` and `val today = System.currentTimeMillis() / (24 * 60 * 60 * 1000L)`. All comparisons use day units. | E10-001 |
| 3 | HIGH | "Family Protector" badge not unlocked from guardian pairing -- no code path triggers the unlock | Added E10-002 task 15: hook into GuardianPairingViewModel to call `unlockBadgeUseCase(FAMILY_PROTECTOR)` when pairing is created. | E10-002 |
| 4 | MEDIUM | Architecture drift -- BadgeRepository injected directly into VMs, violating clean architecture convention | Added `UnlockBadgeUseCase` (E10-002 task 10b). Updated CheckViewModel, ShieldViewModel, QuizViewModel, and ProfileViewModel to inject use cases instead of repository. | E10-002, E10-003, E10-004 |
| 5 | MEDIUM | Missing tests -- no worker behavior test, no migration test, no quiz share-content test, no navigation tests | Added StreakCheckWorkerTest, MigrationTest (androidTest), quiz share event assertion in QuizViewModelTest, GamificationNavigationTest (androidTest). | E10-001, E10-003, E10-004 |
| 6 | MEDIUM | Badge unlock notification missing -- user has no feedback when a badge is unlocked | Added E10-004 task 14: `_badgeUnlockEvent` Channel in VMs that call `unlockBadgeUseCase`, UI collects flow and shows Snackbar. | E10-004 |
| 7 | LOW | Clock manipulation risk -- user can change device date to game streaks | Accepted for MVP. Added known-limitation comment to UpdateStreakUseCase task description. | E10-001 |
| 8 | HIGH | UpdateStreakUseCase returns early on same-day, skipping badge evaluation -- badges only evaluated when streak changes, not on repeat app opens | Moved badge evaluation (`evaluateBadgesUseCase()`) BEFORE the same-day early return in UpdateStreakUseCase. Changed return type to `List<BadgeType>` so callers can emit unlock events. E10-002 task 12 changed to no-op (injection now in E10-001 task 10). | E10-001, E10-002 |
| 9 | MEDIUM | Badge unlock notification incomplete for background worker -- badges unlocked via StreakCheckWorker have no notification path since no ViewModel/Snackbar is available | Updated StreakCheckWorker (E10-001 task 11) to read `newBadges` return value from `updateStreakUseCase()` and show system notifications via `NotificationCompat.Builder` for each newly unlocked badge. | E10-001 |
| 10 | LOW | Missing badge icon and quiz session_id in data models -- Badge has no icon field for UI rendering; QuizResultEntity has no session identifier for analytics/deduplication | Added `icon: String` (Material Symbol name) to `BadgeInfo` data class (E10-002 task 1). Added `sessionId: String` (UUID) to `QuizResultEntity` (E10-003 task 3), `QuizResult` domain model (E10-003 task 8), `QuizRepository` interface (E10-003 task 7), `QuizRepositoryImpl` (E10-003 task 9), `QuizViewModel` (E10-003 task 11). Updated `MIGRATION_8_9` SQL to include `session_id` column. Updated `BadgeCard` to resolve icon from `BadgeInfo.icon` field. | E10-002, E10-003, E10-004 |

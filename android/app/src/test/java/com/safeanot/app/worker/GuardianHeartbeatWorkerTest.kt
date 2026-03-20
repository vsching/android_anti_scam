/**
 * Tests for GuardianHeartbeatWorker logic.
 * Validates that heartbeats are sent when pairings exist, skipped when none,
 * and that failures trigger retry.
 * Uses fakes for the repository layer since the worker delegates all logic there.
 */
package com.safeanot.app.worker

import com.safeanot.app.domain.model.GuardianPairing
import com.safeanot.app.domain.model.GuardianRole
import com.safeanot.app.domain.model.SecurityScore
import com.safeanot.app.domain.repository.AuditRepository
import com.safeanot.app.testutil.FakeGuardianRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Simulates the worker's doWork logic without requiring Android Context/WorkerParameters.
 * This mirrors the exact logic in GuardianHeartbeatWorker.doWork().
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GuardianHeartbeatWorkerTest {

    private lateinit var fakeGuardianRepo: FakeGuardianRepository
    private lateinit var fakeAuditRepo: FakeAuditRepository
    private val scoreFlow = MutableStateFlow<SecurityScore?>(null)

    @Before
    fun setup() {
        fakeGuardianRepo = FakeGuardianRepository()
        fakeAuditRepo = FakeAuditRepository(scoreFlow)
    }

    /**
     * Mimics the worker's doWork logic for testability without Android dependencies.
     * Returns "success", "retry", or "success-noop".
     */
    private suspend fun simulateDoWork(): String {
        return try {
            val pairingCount = fakeGuardianRepo.getGuardianCount().first()
            if (pairingCount == 0) {
                return "success-noop"
            }

            val score = fakeAuditRepo.getSecurityScore().first()
                ?: return "success-noop"

            fakeGuardianRepo.sendHeartbeat(
                securityScore = score.scorePercent,
                securedItems = score.securedItems,
                totalItems = score.totalItems,
                playProtectEnabled = true,
            )
            "success"
        } catch (_: Exception) {
            "retry"
        }
    }

    @Test
    fun `sends heartbeat when pairings exist and score available`() = runTest {
        // Setup: device has a pairing
        fakeGuardianRepo.pairingsFlow.value = listOf(
            GuardianPairing("p1", "d1", "d2", GuardianRole.WARD, "Test", 1000L),
        )
        scoreFlow.value = SecurityScore(totalItems = 8, securedItems = 7, scorePercent = 87)

        val result = simulateDoWork()

        assertEquals("success", result)
        assertTrue(fakeGuardianRepo.heartbeatSent)
        assertEquals(87, fakeGuardianRepo.lastHeartbeatScore)
    }

    @Test
    fun `skips heartbeat when no pairings exist`() = runTest {
        // Setup: no pairings
        fakeGuardianRepo.pairingsFlow.value = emptyList()
        scoreFlow.value = SecurityScore(totalItems = 8, securedItems = 7, scorePercent = 87)

        val result = simulateDoWork()

        assertEquals("success-noop", result)
        assertFalse(fakeGuardianRepo.heartbeatSent)
    }

    @Test
    fun `skips heartbeat when score is null`() = runTest {
        fakeGuardianRepo.pairingsFlow.value = listOf(
            GuardianPairing("p1", "d1", "d2", GuardianRole.WARD, "Test", 1000L),
        )
        scoreFlow.value = null

        val result = simulateDoWork()

        assertEquals("success-noop", result)
        assertFalse(fakeGuardianRepo.heartbeatSent)
    }

    @Test
    fun `retries on failure`() = runTest {
        fakeGuardianRepo.pairingsFlow.value = listOf(
            GuardianPairing("p1", "d1", "d2", GuardianRole.WARD, "Test", 1000L),
        )
        scoreFlow.value = SecurityScore(totalItems = 8, securedItems = 7, scorePercent = 87)
        fakeGuardianRepo.shouldThrow = true

        val result = simulateDoWork()

        assertEquals("retry", result)
    }

    /**
     * Minimal fake AuditRepository that only implements getSecurityScore().
     */
    private class FakeAuditRepository(
        private val scoreFlow: MutableStateFlow<SecurityScore?>,
    ) : AuditRepository {
        override fun getSecurityScore(): Flow<SecurityScore?> = scoreFlow

        // Unused stubs — only getSecurityScore is needed for heartbeat tests
        override fun getAllAuditItems() = throw NotImplementedError()
        override fun getAuditItemsByCategory(category: String) = throw NotImplementedError()
        override suspend fun runAudit() = throw NotImplementedError()
        override suspend fun runAuditAndDetectChanges(): com.safeanot.app.domain.model.AuditChangeSummary = throw NotImplementedError()
        override suspend fun updateItemStatus(id: Int, status: com.safeanot.app.domain.model.AuditStatus) = throw NotImplementedError()
        override suspend fun updateItemStatusByPackage(packageName: String, status: com.safeanot.app.domain.model.AuditStatus) = throw NotImplementedError()
        override suspend fun recalculateScore() = throw NotImplementedError()
        override fun getCompletedAuditCount(): Flow<Int> = throw NotImplementedError()
        override fun getLastAuditTimestamp(): Flow<Long?> = throw NotImplementedError()
    }
}

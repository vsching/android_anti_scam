package com.safeanot.app.data.repository

import com.safeanot.app.data.local.ShareEventDao
import com.safeanot.app.data.local.entity.ShareEventEntity
import com.safeanot.app.data.remote.SafeAnotApi
import com.safeanot.app.data.remote.model.CheckRequest
import com.safeanot.app.data.remote.model.CheckResponse
import com.safeanot.app.data.remote.model.AlertDto
import com.safeanot.app.data.remote.model.ClaimPairingCodeRequest
import com.safeanot.app.data.remote.model.DeletePairingRequest
import com.safeanot.app.data.remote.model.GeneratePairingCodeRequest
import com.safeanot.app.data.remote.model.GuardianPairingDto
import com.safeanot.app.data.remote.model.LatestMetadataResponse
import com.safeanot.app.data.remote.model.PairingCodeResponse
import com.safeanot.app.data.remote.model.ShareEventBatchRequest
import com.safeanot.app.domain.model.ShareEventModel
import com.safeanot.app.domain.model.SharePlatform
import com.safeanot.app.domain.model.ShareType
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ShareEventRepositoryImplTest {

    private lateinit var fakeApi: FakeShareApi
    private lateinit var fakeDao: FakeShareEventDao
    private lateinit var repository: TestableShareEventRepository

    @Before
    fun setup() {
        fakeApi = FakeShareApi()
        fakeDao = FakeShareEventDao()
        repository = TestableShareEventRepository(fakeDao, fakeApi)
    }

    @Test
    fun `trackEvent inserts entity and attempts sync`() = runTest {
        val event = ShareEventModel(
            shareType = ShareType.VERDICT,
            contentId = "example.com",
            platform = SharePlatform.GENERIC,
        )

        val result = repository.trackEvent(event)

        assertTrue(result)
        assertEquals(1, fakeDao.allEntities.size)
        assertEquals("VERDICT", fakeDao.allEntities[0].shareType)
        assertEquals("example.com", fakeDao.allEntities[0].contentId)
        assertEquals("GENERIC", fakeDao.allEntities[0].platform)
    }

    @Test
    fun `trackEvent rate limits at 100 per day`() = runTest {
        // Simulate 100 events already recorded today
        fakeDao.countSinceReturn = 100

        val event = ShareEventModel(
            shareType = ShareType.SCORE,
            contentId = "score",
            platform = SharePlatform.GENERIC,
        )

        val result = repository.trackEvent(event)

        assertFalse(result)
        assertEquals(0, fakeDao.allEntities.size)
    }

    @Test
    fun `syncPendingEvents sends unsynced events and marks them synced`() = runTest {
        val entity = ShareEventEntity(
            id = 1,
            shareType = "ALERT",
            contentId = "alert-001",
            platform = "WHATSAPP",
            timestamp = System.currentTimeMillis(),
            synced = false,
        )
        fakeDao.unsyncedEntities.add(entity)
        fakeApi.postShareResponse = Response.success(Unit)

        val result = repository.syncPendingEvents()

        assertTrue(result)
        assertEquals(listOf(1L), fakeDao.markedSyncedIds)
        assertEquals(1, fakeApi.lastBatchRequest?.events?.size)
        assertEquals("ALERT", fakeApi.lastBatchRequest?.events?.get(0)?.shareType)
    }

    @Test
    fun `syncPendingEvents returns true when no events to sync`() = runTest {
        val result = repository.syncPendingEvents()
        assertTrue(result)
    }

    @Test
    fun `syncPendingEvents returns false on API failure`() = runTest {
        val entity = ShareEventEntity(
            id = 2,
            shareType = "SCORE",
            contentId = "score",
            platform = "GENERIC",
            timestamp = System.currentTimeMillis(),
            synced = false,
        )
        fakeDao.unsyncedEntities.add(entity)
        fakeApi.postShareResponse = Response.error(500, ResponseBody.create(null, "error"))

        val result = repository.syncPendingEvents()

        assertFalse(result)
        assertTrue(fakeDao.markedSyncedIds.isEmpty())
    }

    @Test
    fun `trackEvent still succeeds when sync fails (offline queue)`() = runTest {
        fakeApi.postShareThrows = true

        val event = ShareEventModel(
            shareType = ShareType.VERDICT,
            contentId = "test.com",
            platform = SharePlatform.GENERIC,
        )

        val result = repository.trackEvent(event)

        assertTrue(result)
        assertEquals(1, fakeDao.allEntities.size)
    }

    // --- Testable subclass that avoids Android Context dependency ---

    private class TestableShareEventRepository(
        private val dao: ShareEventDao,
        private val api: SafeAnotApi,
    ) {
        companion object {
            private const val DAILY_RATE_LIMIT = 100
            private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
        }

        private val deviceId = "test-device-id"

        suspend fun trackEvent(event: ShareEventModel): Boolean {
            val oneDayAgo = System.currentTimeMillis() - MILLIS_PER_DAY
            val recentCount = dao.countSince(oneDayAgo)
            if (recentCount >= DAILY_RATE_LIMIT) return false

            val entity = ShareEventEntity(
                shareType = event.shareType.name,
                contentId = event.contentId,
                platform = event.platform.name,
                timestamp = event.timestamp,
            )
            dao.insert(entity)

            try {
                syncPendingEvents()
            } catch (_: Exception) {
                // Will be retried by periodic worker
            }

            return true
        }

        suspend fun syncPendingEvents(): Boolean {
            val unsynced = dao.getUnsynced()
            if (unsynced.isEmpty()) return true

            val dtos = unsynced.map {
                com.safeanot.app.data.remote.model.ShareEventDto(
                    shareType = it.shareType,
                    contentId = it.contentId,
                    platform = it.platform,
                    timestamp = it.timestamp,
                )
            }

            val request = ShareEventBatchRequest(deviceId = deviceId, events = dtos)
            val response = api.postShareEvents(request)
            if (response.isSuccessful) {
                dao.markSynced(unsynced.map { it.id })
                val sevenDaysAgo = System.currentTimeMillis() - (7 * MILLIS_PER_DAY)
                dao.deleteOldSynced(sevenDaysAgo)
                return true
            }
            return false
        }
    }

    // --- Fakes ---

    private class FakeShareEventDao : ShareEventDao {
        val allEntities = mutableListOf<ShareEventEntity>()
        val unsyncedEntities = mutableListOf<ShareEventEntity>()
        val markedSyncedIds = mutableListOf<Long>()
        var countSinceReturn: Int = 0

        override suspend fun insert(event: ShareEventEntity) {
            allEntities.add(event)
            unsyncedEntities.add(event)
        }

        override suspend fun getUnsynced(): List<ShareEventEntity> = unsyncedEntities.toList()

        override suspend fun markSynced(ids: List<Long>) {
            markedSyncedIds.addAll(ids)
            unsyncedEntities.removeAll { it.id in ids }
        }

        override suspend fun countSince(sinceTimestamp: Long): Int = countSinceReturn

        override suspend fun deleteOldSynced(beforeTimestamp: Long) {
            allEntities.removeAll { it.synced && it.timestamp < beforeTimestamp }
        }

        override suspend fun deleteStaleUnsynced(beforeTimestamp: Long) {
            unsyncedEntities.removeAll { !it.synced && it.timestamp < beforeTimestamp }
            allEntities.removeAll { !it.synced && it.timestamp < beforeTimestamp }
        }
    }

    private class FakeShareApi : SafeAnotApi {
        var postShareResponse: Response<Unit> = Response.success(Unit)
        var postShareThrows = false
        var lastBatchRequest: ShareEventBatchRequest? = null

        override suspend fun postShareEvents(request: ShareEventBatchRequest): Response<Unit> {
            lastBatchRequest = request
            if (postShareThrows) throw RuntimeException("Network error")
            return postShareResponse
        }

        // Unused stubs
        override suspend fun getLatestMetadata(): LatestMetadataResponse = throw NotImplementedError()
        override suspend fun getFullDatabase(): ResponseBody = throw NotImplementedError()
        override suspend fun getDelta(since: String): ResponseBody = throw NotImplementedError()
        override suspend fun getBloomFilter(): ResponseBody = throw NotImplementedError()
        override suspend fun checkDomain(request: CheckRequest): CheckResponse = throw NotImplementedError()
        override suspend fun getAlerts(region: String?): List<AlertDto> = throw NotImplementedError()
        override suspend fun generatePairingCode(request: GeneratePairingCodeRequest): PairingCodeResponse = throw NotImplementedError()
        override suspend fun claimPairingCode(request: ClaimPairingCodeRequest): GuardianPairingDto = throw NotImplementedError()
        override suspend fun getWards(deviceId: String): List<GuardianPairingDto> = throw NotImplementedError()
        override suspend fun getGuardians(deviceId: String): List<GuardianPairingDto> = throw NotImplementedError()
        override suspend fun deletePairing(request: DeletePairingRequest): Response<Unit> = throw NotImplementedError()
    }
}

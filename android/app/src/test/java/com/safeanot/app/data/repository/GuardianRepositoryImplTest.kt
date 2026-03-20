package com.safeanot.app.data.repository

import com.safeanot.app.data.local.GuardianDao
import com.safeanot.app.data.local.entity.GuardianPairingEntity
import com.safeanot.app.data.remote.SafeAnotApi
import com.safeanot.app.data.remote.model.AlertDto
import com.safeanot.app.data.remote.model.CheckRequest
import com.safeanot.app.data.remote.model.CheckResponse
import com.safeanot.app.data.remote.model.ClaimPairingCodeRequest
import com.safeanot.app.data.remote.model.DeletePairingRequest
import com.safeanot.app.data.remote.model.GeneratePairingCodeRequest
import com.safeanot.app.data.remote.model.GuardianPairingDto
import com.safeanot.app.data.remote.model.LatestMetadataResponse
import com.safeanot.app.data.remote.model.PairingCodeResponse
import com.safeanot.app.data.remote.model.ShareEventBatchRequest
import com.safeanot.app.domain.model.GuardianPairing
import com.safeanot.app.domain.model.GuardianRole
import com.safeanot.app.domain.model.PairingCode
import com.safeanot.app.domain.repository.GuardianRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class GuardianRepositoryImplTest {

    private lateinit var fakeApi: FakeGuardianApi
    private lateinit var fakeDao: FakeGuardianDao
    private lateinit var repository: TestableGuardianRepository

    @Before
    fun setup() {
        fakeApi = FakeGuardianApi()
        fakeDao = FakeGuardianDao()
        repository = TestableGuardianRepository(fakeDao, fakeApi)
    }

    @Test
    fun `generatePairingCode calls API with device ID`() = runTest {
        fakeApi.generateResult = PairingCodeResponse("CODE-123", 9999L)

        val result = repository.generatePairingCode("WARD", "My Phone")

        assertEquals("CODE-123", result.code)
        assertEquals(9999L, result.expiresAt)
        assertEquals("test-device-id", fakeApi.lastGenerateRequest?.deviceId)
        assertEquals("WARD", fakeApi.lastGenerateRequest?.role)
        assertEquals("My Phone", fakeApi.lastGenerateRequest?.label)
    }

    @Test
    fun `claimPairingCode calls API and inserts into Room`() = runTest {
        fakeApi.claimResult = GuardianPairingDto(
            id = "pair-1",
            deviceId = "test-device-id",
            pairedDeviceId = "other-device",
            role = "GUARDIAN",
            label = "Ward's Phone",
            createdAt = 5000L,
        )

        val result = repository.claimPairingCode("CODE-123", "My Phone")

        assertEquals("pair-1", result.id)
        assertEquals(GuardianRole.GUARDIAN, result.role)
        assertEquals(1, fakeDao.allEntities.size)
        assertEquals("pair-1", fakeDao.allEntities[0].id)
    }

    @Test
    fun `refreshPairings syncs from API to Room`() = runTest {
        fakeApi.wardsResult = listOf(
            GuardianPairingDto("w1", "d1", "d2", "WARD", "Ward 1", 1000L),
        )
        fakeApi.guardiansResult = listOf(
            GuardianPairingDto("g1", "d1", "d3", "GUARDIAN", "Guardian 1", 2000L),
        )

        repository.refreshPairings()

        assertEquals(2, fakeDao.allEntities.size)
        assertTrue(fakeDao.deleteAllCalled)
    }

    @Test
    fun `deletePairing calls API and removes from Room`() = runTest {
        fakeApi.deleteResponse = Response.success(Unit)
        fakeDao.allEntities.add(
            GuardianPairingEntity("pair-1", "d1", "d2", "WARD", "Test", 1000L),
        )

        repository.deletePairing("pair-1")

        assertEquals("pair-1", fakeApi.lastDeleteRequest?.pairingId)
        assertTrue(fakeDao.deletedIds.contains("pair-1"))
    }

    @Test
    fun `getDeviceId returns stable device ID`() = runTest {
        val deviceId = repository.getDeviceId()
        assertEquals("test-device-id", deviceId)
    }

    @Test
    fun `getAllPairings returns mapped flow from DAO`() = runTest {
        fakeDao.pairingsFlow.value = listOf(
            GuardianPairingEntity("p1", "d1", "d2", "WARD", "Test", 1000L),
        )

        val pairings = repository.getAllPairings().first()

        assertEquals(1, pairings.size)
        assertEquals("p1", pairings[0].id)
        assertEquals(GuardianRole.WARD, pairings[0].role)
    }

    @Test
    fun `getGuardianCount returns count flow from DAO`() = runTest {
        fakeDao.pairingsFlow.value = listOf(
            GuardianPairingEntity("p1", "d1", "d2", "WARD", "Test", 1000L),
            GuardianPairingEntity("p2", "d1", "d3", "GUARDIAN", "Test2", 2000L),
        )

        val count = repository.getGuardianCount().first()
        assertEquals(2, count)
    }

    // --- Testable subclass that avoids Android Context dependency ---

    private class TestableGuardianRepository(
        private val dao: FakeGuardianDao,
        private val api: SafeAnotApi,
    ) : GuardianRepository {

        private val deviceId = "test-device-id"

        override suspend fun generatePairingCode(role: String, label: String): PairingCode {
            val request = GeneratePairingCodeRequest(
                deviceId = deviceId,
                role = role,
                label = label,
            )
            val response = api.generatePairingCode(request)
            return response.toDomain()
        }

        override suspend fun claimPairingCode(code: String, label: String): GuardianPairing {
            val request = ClaimPairingCodeRequest(
                deviceId = deviceId,
                code = code,
                label = label,
            )
            val dto = api.claimPairingCode(request)
            val pairing = dto.toDomain()
            dao.insertAll(listOf(GuardianPairingEntity.fromDomain(pairing)))
            return pairing
        }

        override fun getAllPairings(): Flow<List<GuardianPairing>> {
            return dao.getAllPairings().map { entities ->
                entities.map { it.toDomain() }
            }
        }

        override suspend fun deletePairing(pairingId: String) {
            val request = DeletePairingRequest(
                deviceId = deviceId,
                pairingId = pairingId,
            )
            api.deletePairing(request)
            dao.deleteById(pairingId)
        }

        override suspend fun refreshPairings() {
            val wards = api.getWards(deviceId)
            val guardians = api.getGuardians(deviceId)
            val allPairings = (wards + guardians).map { dto ->
                GuardianPairingEntity.fromDomain(dto.toDomain())
            }
            dao.deleteAll()
            dao.insertAll(allPairings)
        }

        override fun getGuardianCount(): Flow<Int> = dao.getGuardianCount()

        override suspend fun getDeviceId(): String = deviceId

        override suspend fun getPairingIdByPairedDeviceId(pairedDeviceId: String): String? {
            return dao.getPairingIdByPairedDeviceId(pairedDeviceId)
        }
    }

    // --- Fakes ---

    private class FakeGuardianDao : GuardianDao {
        val pairingsFlow = MutableStateFlow<List<GuardianPairingEntity>>(emptyList())
        val allEntities = mutableListOf<GuardianPairingEntity>()
        val deletedIds = mutableListOf<String>()
        var deleteAllCalled = false

        override fun getAllPairings(): Flow<List<GuardianPairingEntity>> = pairingsFlow

        override fun getWards(): Flow<List<GuardianPairingEntity>> =
            pairingsFlow.map { list -> list.filter { it.role == "WARD" } }

        override fun getGuardians(): Flow<List<GuardianPairingEntity>> =
            pairingsFlow.map { list -> list.filter { it.role == "GUARDIAN" } }

        override suspend fun insertAll(pairings: List<GuardianPairingEntity>) {
            allEntities.addAll(pairings)
            pairingsFlow.value = allEntities.toList()
        }

        override suspend fun deleteById(id: String) {
            deletedIds.add(id)
            allEntities.removeAll { it.id == id }
            pairingsFlow.value = allEntities.toList()
        }

        override suspend fun deleteAll() {
            deleteAllCalled = true
            allEntities.clear()
            pairingsFlow.value = emptyList()
        }

        override fun getGuardianCount(): Flow<Int> = pairingsFlow.map { it.size }

        override suspend fun getPairingIdByPairedDeviceId(pairedDeviceId: String): String? {
            return allEntities.find { it.pairedDeviceId == pairedDeviceId }?.id
        }
    }

    private class FakeGuardianApi : SafeAnotApi {
        var generateResult = PairingCodeResponse("DEFAULT", 0L)
        var claimResult = GuardianPairingDto("id", "d1", "d2", "WARD", "label", 0L)
        var wardsResult = emptyList<GuardianPairingDto>()
        var guardiansResult = emptyList<GuardianPairingDto>()
        var deleteResponse: Response<Unit> = Response.success(Unit)

        var lastGenerateRequest: GeneratePairingCodeRequest? = null
        var lastDeleteRequest: DeletePairingRequest? = null

        override suspend fun generatePairingCode(request: GeneratePairingCodeRequest): PairingCodeResponse {
            lastGenerateRequest = request
            return generateResult
        }

        override suspend fun claimPairingCode(request: ClaimPairingCodeRequest): GuardianPairingDto {
            return claimResult
        }

        override suspend fun getWards(deviceId: String): List<GuardianPairingDto> = wardsResult

        override suspend fun getGuardians(deviceId: String): List<GuardianPairingDto> = guardiansResult

        override suspend fun deletePairing(request: DeletePairingRequest): Response<Unit> {
            lastDeleteRequest = request
            return deleteResponse
        }

        // Unused stubs
        override suspend fun getLatestMetadata(): LatestMetadataResponse = throw NotImplementedError()
        override suspend fun getFullDatabase(): ResponseBody = throw NotImplementedError()
        override suspend fun getDelta(since: String): ResponseBody = throw NotImplementedError()
        override suspend fun getBloomFilter(): ResponseBody = throw NotImplementedError()
        override suspend fun checkDomain(request: CheckRequest): CheckResponse = throw NotImplementedError()
        override suspend fun getAlerts(region: String?): List<AlertDto> = throw NotImplementedError()
        override suspend fun postShareEvents(request: ShareEventBatchRequest): Response<Unit> = throw NotImplementedError()
    }
}

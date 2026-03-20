package com.safeanot.app.feature.guardian

import androidx.lifecycle.SavedStateHandle
import com.safeanot.app.domain.model.HeartbeatEntry
import com.safeanot.app.domain.model.ScoreBand
import com.safeanot.app.domain.model.WardHeartbeatHistory
import com.safeanot.app.testutil.FakeGuardianRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WardDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepo: FakeGuardianRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeGuardianRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(deviceId: String = "ward-device"): WardDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("deviceId" to deviceId))
        return WardDetailViewModel(
            guardianRepository = fakeRepo,
            savedStateHandle = savedStateHandle,
        )
    }

    @Test
    fun `loads ward details by deviceId from SavedStateHandle`() = runTest {
        fakeRepo.wardHeartbeatHistory = WardHeartbeatHistory(
            deviceId = "ward-device",
            displayName = "Child's Phone",
            heartbeats = listOf(
                HeartbeatEntry(
                    securityScore = 85,
                    securedItems = 8,
                    totalItems = 10,
                    playProtectEnabled = true,
                    timestamp = System.currentTimeMillis(),
                ),
            ),
        )

        val vm = createViewModel()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("ward-device", state.deviceId)
        assertEquals("Child's Phone", state.displayName)
        assertEquals(85, state.securityScore)
        assertEquals(ScoreBand.GREEN, state.scoreBand)
        assertEquals(true, state.playProtectEnabled)
    }

    @Test
    fun `exposes heartbeat history`() = runTest {
        val now = System.currentTimeMillis()
        fakeRepo.wardHeartbeatHistory = WardHeartbeatHistory(
            deviceId = "ward-device",
            displayName = "Test",
            heartbeats = listOf(
                HeartbeatEntry(70, 7, 10, true, now - 86400000),
                HeartbeatEntry(80, 8, 10, true, now - 43200000),
                HeartbeatEntry(85, 8, 10, true, now),
            ),
        )

        val vm = createViewModel()

        assertEquals(3, vm.uiState.value.heartbeatHistory.size)
        // Heartbeats should be sorted by timestamp ascending
        assertTrue(vm.uiState.value.heartbeatHistory[0].timestamp < vm.uiState.value.heartbeatHistory[2].timestamp)
    }

    @Test
    fun `unlink calls deletePairing and sets unlinkSuccess`() = runTest {
        fakeRepo.wardHeartbeatHistory = WardHeartbeatHistory(
            deviceId = "ward-device",
            displayName = "Test",
            heartbeats = emptyList(),
        )

        val vm = createViewModel()
        vm.unlink()

        assertTrue(vm.uiState.value.unlinkSuccess)
        assertTrue(fakeRepo.deletedPairingIds.contains("ward-device"))
    }

    @Test
    fun `error state on missing ward or API failure`() = runTest {
        fakeRepo.shouldThrow = true

        val vm = createViewModel()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertEquals("Test error", state.error)
        assertNull(state.securityScore)
    }

    @Test
    fun `unlink error does not set unlinkSuccess`() = runTest {
        fakeRepo.wardHeartbeatHistory = WardHeartbeatHistory(
            deviceId = "ward-device",
            displayName = "Test",
            heartbeats = emptyList(),
        )

        val vm = createViewModel()

        fakeRepo.shouldThrow = true
        vm.unlink()

        assertFalse(vm.uiState.value.unlinkSuccess)
        assertNotNull(vm.uiState.value.error)
    }
}

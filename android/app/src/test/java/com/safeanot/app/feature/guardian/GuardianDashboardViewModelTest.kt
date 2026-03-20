package com.safeanot.app.feature.guardian

import com.safeanot.app.domain.model.GuardianPairing
import com.safeanot.app.domain.model.GuardianRole
import com.safeanot.app.domain.usecase.GetWardsUseCase
import com.safeanot.app.domain.usecase.RefreshWardsUseCase
import com.safeanot.app.testutil.FakeGuardianRepository
import com.safeanot.app.util.DeviceIdProvider
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GuardianDashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepo: FakeGuardianRepository
    private lateinit var fakeDeviceIdProvider: DeviceIdProvider

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeGuardianRepository()
        fakeDeviceIdProvider = com.safeanot.app.testutil.FakeDeviceIdProvider.create()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): GuardianDashboardViewModel {
        return GuardianDashboardViewModel(
            getWardsUseCase = GetWardsUseCase(fakeRepo, fakeDeviceIdProvider),
            refreshWardsUseCase = RefreshWardsUseCase(fakeRepo, fakeDeviceIdProvider),
        )
    }

    @Test
    fun `loads wards on init`() {
        val ward = GuardianPairing(
            id = "pair-1",
            deviceId = "my-device",
            pairedDeviceId = "ward-device",
            role = GuardianRole.GUARDIAN,
            label = "Child's Phone",
            createdAt = 1000L,
        )
        fakeRepo.pairingsFlow.value = listOf(ward)

        val vm = createViewModel()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.wards.size)
        assertEquals("Child's Phone", state.wards[0].displayName)
        assertEquals("ward-device", state.wards[0].deviceId)
    }

    @Test
    fun `refresh updates ward list`() = runTest {
        val vm = createViewModel()

        // Initially empty
        assertTrue(vm.uiState.value.wards.isEmpty())

        // Simulate adding a ward via the flow
        fakeRepo.pairingsFlow.value = listOf(
            GuardianPairing("pair-1", "my-device", "ward-device", GuardianRole.GUARDIAN, "Ward", 1000L),
        )

        vm.refresh()

        assertFalse(vm.uiState.value.isRefreshing)
        assertTrue(fakeRepo.refreshWardsCalled)
    }

    @Test
    fun `empty list when no wards`() {
        val vm = createViewModel()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.wards.isEmpty())
    }

    @Test
    fun `error state on refresh failure`() = runTest {
        fakeRepo.shouldThrow = true
        // Don't throw on getWards (flow construction), only on refresh
        fakeRepo.shouldThrow = false
        val vm = createViewModel()

        fakeRepo.shouldThrow = true
        vm.refresh()

        assertFalse(vm.uiState.value.isRefreshing)
        assertNotNull(vm.uiState.value.error)
        assertEquals("Test error", vm.uiState.value.error)
    }

    @Test
    fun `filters out non-guardian pairings (only shows wards)`() {
        fakeRepo.pairingsFlow.value = listOf(
            GuardianPairing("pair-1", "d1", "d2", GuardianRole.GUARDIAN, "Ward", 1000L),
            GuardianPairing("pair-2", "d1", "d3", GuardianRole.WARD, "My Guardian", 2000L),
        )

        val vm = createViewModel()

        // Only the GUARDIAN role pairing should appear (monitoring d2)
        assertEquals(1, vm.uiState.value.wards.size)
        assertEquals("Ward", vm.uiState.value.wards[0].displayName)
    }

    @Test
    fun `ViewModel constructor has no Context parameter`() {
        val vm = createViewModel()
        assertNotNull(vm)
    }
}

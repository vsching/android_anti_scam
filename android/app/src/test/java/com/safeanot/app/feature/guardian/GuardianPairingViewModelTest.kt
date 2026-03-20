package com.safeanot.app.feature.guardian

import com.safeanot.app.domain.model.BadgeProgress
import com.safeanot.app.domain.model.BadgeType
import com.safeanot.app.domain.model.GuardianPairing
import com.safeanot.app.domain.model.GuardianRole
import com.safeanot.app.domain.model.PairingCode
import com.safeanot.app.domain.repository.BadgeRepository
import com.safeanot.app.domain.usecase.ClaimPairingCodeUseCase
import com.safeanot.app.domain.usecase.DeletePairingUseCase
import com.safeanot.app.domain.usecase.GeneratePairingCodeUseCase
import com.safeanot.app.domain.usecase.GetPairingsUseCase
import com.safeanot.app.domain.usecase.UnlockBadgeUseCase
import com.safeanot.app.testutil.FakeGuardianRepository
import com.safeanot.app.testutil.FakeGuardianHeartbeatScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
class GuardianPairingViewModelTest {

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

    private fun createViewModel(): GuardianPairingViewModel {
        val fakeBadgeRepo = object : BadgeRepository {
            override fun observeAllBadges(): Flow<List<BadgeProgress>> = flowOf(emptyList())
            override fun observeUnlockedCount(): Flow<Int> = flowOf(0)
            override suspend fun unlockBadge(type: BadgeType): Boolean = true
        }
        return GuardianPairingViewModel(
            generatePairingCodeUseCase = GeneratePairingCodeUseCase(fakeRepo),
            claimPairingCodeUseCase = ClaimPairingCodeUseCase(fakeRepo),
            deletePairingUseCase = DeletePairingUseCase(fakeRepo),
            getPairingsUseCase = GetPairingsUseCase(fakeRepo),
            heartbeatScheduler = FakeGuardianHeartbeatScheduler.create(),
            unlockBadgeUseCase = UnlockBadgeUseCase(fakeBadgeRepo),
        )
    }

    @Test
    fun `initial state has empty pairings and no code`() {
        val vm = createViewModel()
        val state = vm.uiState.value

        assertTrue(state.pairings.isEmpty())
        assertNull(state.pairingCode)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(state.claimSuccess)
        assertEquals(0, state.selectedTab)
    }

    @Test
    fun `generateCode updates state with pairing code`() = runTest {
        fakeRepo.generateCodeResult = PairingCode("ABC-123", Long.MAX_VALUE)
        val vm = createViewModel()

        vm.generateCode("WARD", "My Phone")

        val state = vm.uiState.value
        assertNotNull(state.pairingCode)
        assertEquals("ABC-123", state.pairingCode?.code)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `generateCode sets error on failure`() = runTest {
        fakeRepo.shouldThrow = true
        val vm = createViewModel()

        vm.generateCode("WARD", "My Phone")

        val state = vm.uiState.value
        assertNull(state.pairingCode)
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertEquals("Test error", state.error)
    }

    @Test
    fun `claimCode updates state with success`() = runTest {
        fakeRepo.claimCodeResult = GuardianPairing(
            id = "pair-1",
            deviceId = "d1",
            pairedDeviceId = "d2",
            role = GuardianRole.GUARDIAN,
            label = "Ward Device",
            createdAt = 1000L,
        )
        val vm = createViewModel()

        vm.claimCode("ABC-123", "My Phone")

        val state = vm.uiState.value
        assertTrue(state.claimSuccess)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(1, state.pairings.size)
    }

    @Test
    fun `claimCode sets error on failure`() = runTest {
        fakeRepo.shouldThrow = true
        val vm = createViewModel()

        vm.claimCode("BAD-CODE", "My Phone")

        val state = vm.uiState.value
        assertFalse(state.claimSuccess)
        assertFalse(state.isLoading)
        assertEquals("Test error", state.error)
    }

    @Test
    fun `deletePairing removes from pairings list`() = runTest {
        val pairing = GuardianPairing(
            id = "pair-1",
            deviceId = "d1",
            pairedDeviceId = "d2",
            role = GuardianRole.WARD,
            label = "Guardian Device",
            createdAt = 1000L,
        )
        fakeRepo.pairingsFlow.value = listOf(pairing)
        val vm = createViewModel()

        assertEquals(1, vm.uiState.value.pairings.size)

        vm.deletePairing("pair-1")

        assertTrue(vm.uiState.value.pairings.isEmpty())
        assertTrue(fakeRepo.deletedPairingIds.contains("pair-1"))
    }

    @Test
    fun `deletePairing sets error on failure`() = runTest {
        fakeRepo.pairingsFlow.value = listOf(
            GuardianPairing("pair-1", "d1", "d2", GuardianRole.WARD, "Test", 1000L),
        )
        fakeRepo.shouldThrow = true
        val vm = createViewModel()

        vm.deletePairing("pair-1")

        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `selectTab updates selected tab`() {
        val vm = createViewModel()

        vm.selectTab(1)
        assertEquals(1, vm.uiState.value.selectedTab)

        vm.selectTab(0)
        assertEquals(0, vm.uiState.value.selectedTab)
    }

    @Test
    fun `clearError resets error state`() = runTest {
        fakeRepo.shouldThrow = true
        val vm = createViewModel()

        vm.generateCode("WARD", "Test")
        assertNotNull(vm.uiState.value.error)

        vm.clearError()
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `clearClaimSuccess resets claimSuccess state`() = runTest {
        val vm = createViewModel()

        vm.claimCode("ABC", "Test")
        assertTrue(vm.uiState.value.claimSuccess)

        vm.clearClaimSuccess()
        assertFalse(vm.uiState.value.claimSuccess)
    }

    @Test
    fun `ViewModel constructor has no Context parameter`() {
        // This test verifies the architectural constraint that ViewModel has no Context.
        // If GuardianPairingViewModel's constructor required Context, this would fail to compile.
        val vm = createViewModel()
        assertNotNull(vm)
    }
}

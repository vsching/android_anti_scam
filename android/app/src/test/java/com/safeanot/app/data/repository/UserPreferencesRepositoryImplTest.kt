package com.safeanot.app.data.repository

import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.repository.UserPreferencesRepository
import com.safeanot.app.util.RegionResolver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for UserPreferencesRepositoryImpl logic:
 * - Region persistence and loading
 * - Notification preference persistence
 * - Fallback to locale when no region preference is set
 *
 * Uses a test double that replicates the same mapping logic as the real impl
 * to verify the contract without requiring Android Context for DataStore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesRepositoryImplTest {

    private lateinit var repository: TestableUserPreferencesRepository

    @Before
    fun setup() {
        repository = TestableUserPreferencesRepository()
    }

    @Test
    fun `getRegion returns locale-based default when no preference set`() = runTest {
        // No region stored -> falls back to RegionResolver.fromLocale()
        val region = repository.getRegion().first()
        assertEquals(RegionResolver.fromLocale(), region)
    }

    @Test
    fun `getRegion returns MALAYSIA when stored`() = runTest {
        repository.setRegion(AlertRegionFilter.MALAYSIA)
        val region = repository.getRegion().first()
        assertEquals(AlertRegionFilter.MALAYSIA, region)
    }

    @Test
    fun `getRegion returns SINGAPORE when stored`() = runTest {
        repository.setRegion(AlertRegionFilter.SINGAPORE)
        val region = repository.getRegion().first()
        assertEquals(AlertRegionFilter.SINGAPORE, region)
    }

    @Test
    fun `getRegion returns ALL when ALL is stored`() = runTest {
        repository.setRegion(AlertRegionFilter.ALL)
        val region = repository.getRegion().first()
        assertEquals(AlertRegionFilter.ALL, region)
    }

    @Test
    fun `getRegion returns ALL for invalid stored value`() = runTest {
        repository.rawRegion.value = "INVALID_REGION"
        val region = repository.getRegion().first()
        assertEquals(AlertRegionFilter.ALL, region)
    }

    @Test
    fun `setRegion persists the enum name`() = runTest {
        repository.setRegion(AlertRegionFilter.SINGAPORE)
        assertEquals("SINGAPORE", repository.rawRegion.value)
    }

    @Test
    fun `getScamAlertsEnabled returns true by default`() = runTest {
        val enabled = repository.getScamAlertsEnabled().first()
        assertTrue(enabled)
    }

    @Test
    fun `setScamAlertsEnabled persists false`() = runTest {
        repository.setScamAlertsEnabled(false)
        val enabled = repository.getScamAlertsEnabled().first()
        assertFalse(enabled)
    }

    @Test
    fun `setScamAlertsEnabled persists true after being false`() = runTest {
        repository.setScamAlertsEnabled(false)
        repository.setScamAlertsEnabled(true)
        val enabled = repository.getScamAlertsEnabled().first()
        assertTrue(enabled)
    }

    /**
     * Test double that replicates UserPreferencesRepositoryImpl logic
     * (string-to-enum mapping, locale fallback) without requiring Android DataStore.
     */
    private class TestableUserPreferencesRepository : UserPreferencesRepository {
        val rawRegion = MutableStateFlow<String?>(null)
        private val scamAlertsEnabled = MutableStateFlow(true)

        override fun getRegion(): Flow<AlertRegionFilter> = rawRegion.map { stored ->
            if (stored != null) {
                try {
                    AlertRegionFilter.valueOf(stored)
                } catch (_: IllegalArgumentException) {
                    AlertRegionFilter.ALL
                }
            } else {
                RegionResolver.fromLocale()
            }
        }

        override suspend fun setRegion(region: AlertRegionFilter) {
            rawRegion.value = region.name
        }

        override fun getScamAlertsEnabled(): Flow<Boolean> = scamAlertsEnabled

        override suspend fun setScamAlertsEnabled(enabled: Boolean) {
            scamAlertsEnabled.value = enabled
        }
    }
}

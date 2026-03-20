package com.safeanot.app.data.repository

import com.safeanot.app.data.local.BadgeDao
import com.safeanot.app.data.local.entity.BadgeEntity
import com.safeanot.app.domain.model.BadgeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BadgeRepositoryImplTest {

    private lateinit var fakeDao: FakeBadgeDao
    private lateinit var repository: BadgeRepositoryImpl

    @Before
    fun setup() {
        fakeDao = FakeBadgeDao()
        repository = BadgeRepositoryImpl(fakeDao)
    }

    @Test
    fun `observeAllBadges returns all badge types with locked state by default`() = runTest {
        val result = repository.observeAllBadges().first()

        assertEquals(BadgeType.entries.size, result.size)
        assertTrue(result.all { !it.isUnlocked })
    }

    @Test
    fun `observeAllBadges combines entities with badge definitions`() = runTest {
        fakeDao.entitiesFlow.value = listOf(
            BadgeEntity(badgeId = "FIRST_SCAN", unlocked = true, unlockedAt = 12345L),
        )

        val result = repository.observeAllBadges().first()

        val firstScan = result.find { it.badge.type == BadgeType.FIRST_SCAN }!!
        assertTrue(firstScan.isUnlocked)
        assertEquals(12345L, firstScan.unlockedAt)

        val phoneHardened = result.find { it.badge.type == BadgeType.PHONE_HARDENED }!!
        assertFalse(phoneHardened.isUnlocked)
    }

    @Test
    fun `unlockBadge returns true for newly unlocked badge`() = runTest {
        val result = repository.unlockBadge(BadgeType.FIRST_SCAN)

        assertTrue(result)
        val entity = fakeDao.storedEntities["FIRST_SCAN"]!!
        assertTrue(entity.unlocked)
        assertTrue(entity.unlockedAt!! > 0)
    }

    @Test
    fun `unlockBadge returns false when already unlocked`() = runTest {
        fakeDao.storedEntities["FIRST_SCAN"] = BadgeEntity(
            badgeId = "FIRST_SCAN",
            unlocked = true,
            unlockedAt = 12345L,
        )

        val result = repository.unlockBadge(BadgeType.FIRST_SCAN)

        assertFalse(result)
    }

    @Test
    fun `unlockBadge is idempotent`() = runTest {
        repository.unlockBadge(BadgeType.STREAK_STARTER)
        val firstTimestamp = fakeDao.storedEntities["STREAK_STARTER"]!!.unlockedAt

        val secondResult = repository.unlockBadge(BadgeType.STREAK_STARTER)

        assertFalse(secondResult)
        // Original timestamp preserved
        assertEquals(firstTimestamp, fakeDao.storedEntities["STREAK_STARTER"]!!.unlockedAt)
    }

    @Test
    fun `observeUnlockedCount reflects unlocked badges`() = runTest {
        fakeDao.unlockedCountFlow.value = 3

        val result = repository.observeUnlockedCount().first()

        assertEquals(3, result)
    }

    // --- Fake ---

    private class FakeBadgeDao : BadgeDao {
        val entitiesFlow = MutableStateFlow<List<BadgeEntity>>(emptyList())
        val unlockedCountFlow = MutableStateFlow(0)
        val storedEntities = mutableMapOf<String, BadgeEntity>()

        override fun observeAll(): Flow<List<BadgeEntity>> = entitiesFlow

        override suspend fun getById(badgeId: String): BadgeEntity? = storedEntities[badgeId]

        override suspend fun upsert(entity: BadgeEntity) {
            storedEntities[entity.badgeId] = entity
            entitiesFlow.value = storedEntities.values.toList()
            unlockedCountFlow.value = storedEntities.values.count { it.unlocked }
        }

        override fun observeUnlockedCount(): Flow<Int> = unlockedCountFlow
    }
}

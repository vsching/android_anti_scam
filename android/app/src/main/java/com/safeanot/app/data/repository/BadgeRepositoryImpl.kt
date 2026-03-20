/**
 * Concrete implementation of BadgeRepository backed by Room.
 */
package com.safeanot.app.data.repository

import com.safeanot.app.data.local.BadgeDao
import com.safeanot.app.data.local.entity.BadgeEntity
import com.safeanot.app.domain.model.BadgeProgress
import com.safeanot.app.domain.model.BadgeType
import com.safeanot.app.domain.repository.BadgeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BadgeRepositoryImpl @Inject constructor(
    private val badgeDao: BadgeDao,
) : BadgeRepository {

    override fun observeAllBadges(): Flow<List<BadgeProgress>> {
        val allBadges = BadgeType.allBadges()
        return badgeDao.observeAll().map { entities ->
            val entityMap = entities.associateBy { it.badgeId }
            allBadges.map { badgeInfo ->
                val entity = entityMap[badgeInfo.type.name]
                BadgeProgress(
                    badge = badgeInfo,
                    isUnlocked = entity?.unlocked == true,
                    unlockedAt = entity?.unlockedAt,
                )
            }
        }
    }

    override fun observeUnlockedCount(): Flow<Int> {
        return badgeDao.observeUnlockedCount()
    }

    override suspend fun unlockBadge(type: BadgeType): Boolean {
        val existing = badgeDao.getById(type.name)
        if (existing?.unlocked == true) {
            return false
        }
        badgeDao.upsert(
            BadgeEntity(
                badgeId = type.name,
                unlocked = true,
                unlockedAt = System.currentTimeMillis(),
            )
        )
        return true
    }
}

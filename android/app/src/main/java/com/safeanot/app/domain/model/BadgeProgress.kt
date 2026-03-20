/**
 * Badge progress combining badge metadata with unlock state.
 * Used by the UI to render locked/unlocked badge cards.
 */
package com.safeanot.app.domain.model

data class BadgeProgress(
    val badge: BadgeInfo,
    val isUnlocked: Boolean,
    val unlockedAt: Long? = null,
)

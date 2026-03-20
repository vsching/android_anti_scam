/**
 * Badge metadata containing display information for each badge type.
 */
package com.safeanot.app.domain.model

data class BadgeInfo(
    val type: BadgeType,
    val title: String,
    val description: String,
    val conditionHint: String,
    val icon: String,
)

/**
 * Domain model for share event analytics tracking.
 * Named ShareEventModel to avoid conflict with the UI-layer ShareEvent sealed class
 * in feature/check/ShareEvent.kt.
 */
package com.safeanot.app.domain.model

/**
 * Type of content that was shared.
 */
enum class ShareType {
    VERDICT,
    SCORE,
    ALERT,
    WARNING_TEMPLATE,
    RESCUE_CARD,
}

/**
 * Platform the content was shared to.
 */
enum class SharePlatform {
    WHATSAPP,
    GENERIC,
}

/**
 * Analytics event recorded when a user shares content from the app.
 */
data class ShareEventModel(
    val shareType: ShareType,
    val contentId: String,
    val platform: SharePlatform,
    val timestamp: Long = System.currentTimeMillis(),
)

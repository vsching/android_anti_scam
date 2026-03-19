/**
 * Domain model for pre-written warning message templates.
 * Each template has English and Bahasa Malaysia variants with
 * {domain} and {verdict} placeholders replaced at format time.
 */
package com.safeanot.app.domain.model

data class WarningTemplate(
    val id: String,
    val tone: WarningTone,
    val templateEn: String,
    val templateMs: String,
)

enum class WarningTone {
    POLITE,
    URGENT,
    ELDER_FRIENDLY,
}

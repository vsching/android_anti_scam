/**
 * Badge type enumeration for the gamification system.
 * Full badge definitions (BadgeInfo, BadgeProgress) are implemented in E10-002.
 */
package com.safeanot.app.domain.model

enum class BadgeType {
    PHONE_HARDENED,
    FIRST_SCAN,
    STREAK_STARTER,
    WEEK_WARRIOR,
    MONTH_MASTER,
    SCAM_SPOTTER,
    LINK_CHECKER,
    SHARE_GUARDIAN,
    FAMILY_PROTECTOR,
}

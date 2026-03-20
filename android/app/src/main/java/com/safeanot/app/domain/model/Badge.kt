/**
 * Badge type enumeration for the gamification system.
 * Contains all badge definitions with metadata via [allBadges].
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
    ;

    companion object {
        fun allBadges(): List<BadgeInfo> = listOf(
            BadgeInfo(
                type = PHONE_HARDENED,
                title = "Phone Hardened",
                description = "Achieved 100% security score",
                conditionHint = "Get 100% security score",
                icon = "shield",
            ),
            BadgeInfo(
                type = FIRST_SCAN,
                title = "First Scan",
                description = "Completed your first security scan",
                conditionHint = "Complete a security scan",
                icon = "search",
            ),
            BadgeInfo(
                type = STREAK_STARTER,
                title = "Streak Starter",
                description = "Maintained a 3-day security streak",
                conditionHint = "Keep a 3-day streak",
                icon = "local_fire_department",
            ),
            BadgeInfo(
                type = WEEK_WARRIOR,
                title = "Week Warrior",
                description = "Maintained a 7-day security streak",
                conditionHint = "Keep a 7-day streak",
                icon = "local_fire_department",
            ),
            BadgeInfo(
                type = MONTH_MASTER,
                title = "Month Master",
                description = "Maintained a 30-day security streak",
                conditionHint = "Keep a 30-day streak",
                icon = "local_fire_department",
            ),
            BadgeInfo(
                type = SCAM_SPOTTER,
                title = "Scam Spotter",
                description = "Scored 100% on the Spot the Scam quiz",
                conditionHint = "Get a perfect quiz score",
                icon = "school",
            ),
            BadgeInfo(
                type = LINK_CHECKER,
                title = "Link Checker",
                description = "Checked your first suspicious link",
                conditionHint = "Check a link",
                icon = "link",
            ),
            BadgeInfo(
                type = SHARE_GUARDIAN,
                title = "Share Guardian",
                description = "Shared your security results with others",
                conditionHint = "Share your results",
                icon = "share",
            ),
            BadgeInfo(
                type = FAMILY_PROTECTOR,
                title = "Family Protector",
                description = "Paired with a family member as guardian",
                conditionHint = "Set up guardian pairing",
                icon = "people",
            ),
        )
    }
}

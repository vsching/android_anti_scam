/**
 * Sealed class defining all navigation routes in the app.
 */
package com.safeanot.app.navigation

sealed class Screen(val route: String) {
    /** Main shield/audit dashboard screen. */
    data object Shield : Screen("shield")

    /** Link checker screen. */
    data object Check : Screen("check")

    /** Scam alerts feed screen. */
    data object Alerts : Screen("alerts")

    /** User profile and settings screen. */
    data object Profile : Screen("profile")

    /** Guided fix flow for a specific app. */
    data object Fix : Screen("fix/{packageName}/{appName}") {
        fun createRoute(packageName: String, appName: String): String =
            "fix/$packageName/$appName"
    }
}

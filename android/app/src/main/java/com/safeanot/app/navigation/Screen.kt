/**
 * Sealed class defining all navigation routes in the app.
 */
package com.safeanot.app.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    /** Main shield/audit dashboard screen. */
    data object Shield : Screen("shield")

    /** Link checker screen. */
    data object Check : Screen("check")

    /** Scam alerts feed screen. */
    data object Alerts : Screen("alerts")

    /** User profile and settings screen. */
    data object Profile : Screen("profile")

    /** Guided fix flow for a specific app, routed by package name. */
    data object Fix : Screen("fix/{packageName}") {
        fun createRoute(packageName: String): String =
            "fix/$packageName"
    }

    /** Alert detail screen. */
    data object AlertDetail : Screen("alerts/{alertId}") {
        fun createRoute(alertId: String): String =
            "alerts/${Uri.encode(alertId)}"
    }

    /** Guardian pairing screen. */
    data object GuardianPairing : Screen("guardian/pairing")
}

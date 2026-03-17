package com.safeanot.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests that the tracked app list matches the required packages from Section 3.5.
 */
class ConstantsTest {

    private val requiredPackages = listOf(
        // Messaging
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        // Browsers
        "com.android.chrome",
        "com.sec.android.app.sbrowser",
        "com.microsoft.emmx",
        "org.mozilla.firefox",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.brave.browser",
        "com.UCMobile.intl",
        "com.duckduckgo.mobile.android",
        "com.vivaldi.browser",
        "com.mi.globalbrowser",
        "com.vivo.browser",
        "com.heytap.browser",
        "com.huawei.browser",
        // File Managers
        "com.google.android.apps.nbu.files",
        "com.mi.android.globalFileexplorer",
    )

    @Test
    fun `all required packages are in tracked apps list`() {
        val trackedPackages = Constants.TrackedApps.ALL.map { it.packageName }
        for (pkg in requiredPackages) {
            assertTrue(
                "Missing required package: $pkg",
                trackedPackages.contains(pkg),
            )
        }
    }

    @Test
    fun `all tracked apps have non-empty risk description`() {
        for (app in Constants.TrackedApps.ALL) {
            assertTrue(
                "${app.packageName} has empty risk description",
                app.riskDescription.isNotEmpty(),
            )
        }
    }

    @Test
    fun `tracked apps have correct categories`() {
        for (app in Constants.TrackedApps.MESSAGING) {
            assertEquals("MESSAGING", app.category)
        }
        for (app in Constants.TrackedApps.BROWSERS) {
            assertEquals("BROWSERS", app.category)
        }
        for (app in Constants.TrackedApps.FILES) {
            assertEquals("FILES", app.category)
        }
    }

    @Test
    fun `total tracked app count matches expected`() {
        assertEquals(19, Constants.TrackedApps.ALL.size)
    }

    @Test
    fun `reminder interval options are valid`() {
        assertEquals(listOf(3, 7, 14, 30), Constants.REMINDER_INTERVAL_OPTIONS)
    }
}

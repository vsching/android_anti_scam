/**
 * Utility that uses Android PackageManager to check if tracked packages are installed on the device.
 * Handles Android 11+ package visibility restrictions via the <queries> manifest declarations.
 */
package com.safeanot.app.util

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val packageManager: PackageManager
        get() = context.packageManager

    /**
     * Checks whether a given package is installed on the device.
     *
     * @param packageName The Android package name to check (e.g., "com.whatsapp").
     * @return true if the package is installed and queryable, false otherwise.
     */
    fun isInstalled(packageName: String): Boolean {
        return try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Returns a list of package names from the input that are currently installed.
     *
     * @param packageNames List of package names to check.
     * @return Filtered list containing only installed packages.
     */
    fun filterInstalled(packageNames: List<String>): List<String> {
        return packageNames.filter { isInstalled(it) }
    }
}

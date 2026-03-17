/**
 * Utility that uses Android PackageManager to check if tracked packages are installed on the device.
 * Handles Android 11+ package visibility restrictions via the <queries> manifest declarations.
 */
package com.safeanot.app.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents the detection outcome for a single package query.
 */
enum class DetectionState {
    /** Package is installed and queryable. */
    INSTALLED,

    /** Package is not present on the device. */
    NOT_INSTALLED,

    /** Package visibility is restricted; cannot determine install status. */
    NOT_QUERYABLE,
}

/**
 * Structured result from a package detection check.
 */
data class DetectionResult(
    val packageName: String,
    val state: DetectionState,
    val appLabel: String? = null,
    val appIcon: Drawable? = null,
)

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
     * Performs a structured detection check for a package.
     *
     * @param packageName The Android package name to check.
     * @param fallbackLabel A fallback label if the app label cannot be loaded.
     * @return A [DetectionResult] with detection state and optional app metadata.
     */
    fun detect(packageName: String, fallbackLabel: String = packageName): DetectionResult {
        return try {
            @Suppress("DEPRECATION")
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val appInfo = packageInfo.applicationInfo
            val label = appInfo?.let {
                packageManager.getApplicationLabel(it).toString()
            } ?: fallbackLabel
            val icon = appInfo?.let {
                try {
                    packageManager.getApplicationIcon(it)
                } catch (_: Exception) {
                    null
                }
            }
            DetectionResult(
                packageName = packageName,
                state = DetectionState.INSTALLED,
                appLabel = label,
                appIcon = icon,
            )
        } catch (_: PackageManager.NameNotFoundException) {
            DetectionResult(
                packageName = packageName,
                state = DetectionState.NOT_INSTALLED,
                appLabel = fallbackLabel,
            )
        } catch (_: SecurityException) {
            // Android 11+ package visibility restriction
            DetectionResult(
                packageName = packageName,
                state = DetectionState.NOT_QUERYABLE,
                appLabel = fallbackLabel,
            )
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

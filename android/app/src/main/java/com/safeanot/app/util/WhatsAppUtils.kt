/**
 * Utility for checking WhatsApp availability on the device.
 * Should only be called from the UI layer (requires Context).
 */
package com.safeanot.app.util

import android.content.Context
import android.content.pm.PackageManager

object WhatsAppUtils {

    private const val WHATSAPP_PACKAGE = "com.whatsapp"

    /**
     * Checks whether WhatsApp is installed on the device.
     * Requires the `<queries>` declaration in AndroidManifest for API 30+.
     */
    fun isWhatsAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(WHATSAPP_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}

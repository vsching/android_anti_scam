/**
 * Constants and pre-seeded data for all tracked apps organized by category.
 * These are the apps whose "Install unknown apps" permission poses a sideloading risk.
 */
package com.safeanot.app.util

data class TrackedApp(
    val packageName: String,
    val appName: String,
    val shortCode: String,
    val brandColor: String,
    val category: String,
)

object Constants {

    /** Notification channel ID for audit reminders. */
    const val NOTIFICATION_CHANNEL_ID = "audit_reminders"
    const val NOTIFICATION_CHANNEL_NAME = "Audit Reminders"

    /** WorkManager unique work name. */
    const val AUDIT_WORK_NAME = "audit_reminder"

    /** Default audit reminder interval in days. */
    const val DEFAULT_REMINDER_INTERVAL_DAYS = 7L

    object TrackedApps {
        val MESSAGING = listOf(
            TrackedApp("com.whatsapp", "WhatsApp", "WA", "#25D366", "MESSAGING"),
            TrackedApp("com.whatsapp.w4b", "WhatsApp Business", "WB", "#25D366", "MESSAGING"),
            TrackedApp("org.telegram.messenger", "Telegram", "TG", "#0088cc", "MESSAGING"),
        )

        val BROWSERS = listOf(
            TrackedApp("com.android.chrome", "Chrome", "CR", "#4285F4", "BROWSERS"),
            TrackedApp("com.sec.android.app.sbrowser", "Samsung Internet", "SI", "#6E4BBD", "BROWSERS"),
            TrackedApp("com.microsoft.emmx", "Microsoft Edge", "ED", "#0078D7", "BROWSERS"),
            TrackedApp("org.mozilla.firefox", "Firefox", "FF", "#FF7139", "BROWSERS"),
            TrackedApp("com.opera.browser", "Opera", "OP", "#FF1B2D", "BROWSERS"),
            TrackedApp("com.opera.mini.native", "Opera Mini", "OM", "#FF1B2D", "BROWSERS"),
            TrackedApp("com.brave.browser", "Brave", "BR", "#FB542B", "BROWSERS"),
            TrackedApp("com.UCMobile.intl", "UC Browser", "UC", "#F58220", "BROWSERS"),
            TrackedApp("com.duckduckgo.mobile.android", "DuckDuckGo", "DD", "#DE5833", "BROWSERS"),
            TrackedApp("com.vivaldi.browser", "Vivaldi", "VI", "#EF3939", "BROWSERS"),
            TrackedApp("com.mi.globalbrowser", "Mi Browser", "MI", "#FF6900", "BROWSERS"),
            TrackedApp("com.vivo.browser", "Vivo Browser", "VV", "#415FFF", "BROWSERS"),
            TrackedApp("com.heytap.browser", "OPPO Browser", "OB", "#1BA784", "BROWSERS"),
            TrackedApp("com.huawei.browser", "Huawei Browser", "HW", "#CF0A2C", "BROWSERS"),
        )

        val FILES = listOf(
            TrackedApp("com.google.android.apps.nbu.files", "Files by Google", "FG", "#34A853", "FILES"),
            TrackedApp("com.mi.android.globalFileexplorer", "Mi File Manager", "MF", "#FF6900", "FILES"),
        )

        /** All tracked apps across all categories. */
        val ALL: List<TrackedApp> = MESSAGING + BROWSERS + FILES
    }
}

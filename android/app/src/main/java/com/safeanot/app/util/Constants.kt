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
    val riskDescription: String,
)

object Constants {

    /** Notification channel ID for audit reminders. */
    const val NOTIFICATION_CHANNEL_ID = "audit_reminders"
    const val NOTIFICATION_CHANNEL_NAME = "Audit Reminders"

    /** Notification channel for scam alerts (push notifications). */
    const val SCAM_ALERTS_CHANNEL_ID = "scam_alerts"
    const val SCAM_ALERTS_CHANNEL_NAME = "Scam Alerts"

    /** FCM topic prefixes and known region topics. */
    const val FCM_TOPIC_PREFIX = "scam_alerts_"
    const val FCM_TOPIC_SCAM_ALERTS_MY = "scam_alerts_MY"
    const val FCM_TOPIC_SCAM_ALERTS_SG = "scam_alerts_SG"

    /** WorkManager unique work name. */
    const val AUDIT_WORK_NAME = "audit_reminder"

    /** Default audit reminder interval in days. */
    const val DEFAULT_REMINDER_INTERVAL_DAYS = 7L

    /** Allowed reminder interval options in days. */
    val REMINDER_INTERVAL_OPTIONS = listOf(3, 7, 14, 30)

    /** WhatsApp package name used for targeted sharing and availability checks. */
    const val WHATSAPP_PACKAGE = "com.whatsapp"

    /** App download URL for sharing. */
    const val APP_DOWNLOAD_URL = "https://play.google.com/store/apps/details?id=com.safeanot.app"

    /** Legal link URLs. */
    const val PRIVACY_POLICY_URL = "https://safeanot.com/privacy"
    const val TERMS_OF_SERVICE_URL = "https://safeanot.com/terms"

    /** Emergency contact constants - Malaysia. */
    const val NSRC_HOTLINE = "997"
    const val MCMC_URL = "https://aduan.skmm.gov.my/"

    /** Emergency contact constants - Singapore. */
    const val SPF_HOTLINE = "18007226688"
    const val SCAMSHIELD_URL = "https://www.scamshield.gov.sg/"

    object TrackedApps {
        val MESSAGING = listOf(
            TrackedApp(
                "com.whatsapp", "WhatsApp", "WA", "#25D366", "MESSAGING",
                "WhatsApp can install APK files sent in chats. Scammers send malware disguised as useful apps.",
            ),
            TrackedApp(
                "com.whatsapp.w4b", "WhatsApp Business", "WB", "#25D366", "MESSAGING",
                "WhatsApp Business can install APK files sent in chats. Scammers send malware disguised as useful apps.",
            ),
            TrackedApp(
                "org.telegram.messenger", "Telegram", "TG", "#0088cc", "MESSAGING",
                "Telegram can install APK files from channels and chats. Scammers distribute malware through fake groups.",
            ),
        )

        val BROWSERS = listOf(
            TrackedApp(
                "com.android.chrome", "Chrome", "CR", "#4285F4", "BROWSERS",
                "Chrome can install APK files downloaded from websites. Scam sites trick users into downloading malware.",
            ),
            TrackedApp(
                "com.sec.android.app.sbrowser", "Samsung Internet", "SI", "#6E4BBD", "BROWSERS",
                "Samsung Internet can install APK files downloaded from websites. Scam sites trick users into downloading malware.",
            ),
            TrackedApp(
                "com.microsoft.emmx", "Microsoft Edge", "ED", "#0078D7", "BROWSERS",
                "Microsoft Edge can install APK files downloaded from websites. Scam sites trick users into downloading malware.",
            ),
            TrackedApp(
                "org.mozilla.firefox", "Firefox", "FF", "#FF7139", "BROWSERS",
                "Firefox can install APK files downloaded from websites. Scam sites trick users into downloading malware.",
            ),
            TrackedApp(
                "com.opera.browser", "Opera", "OP", "#FF1B2D", "BROWSERS",
                "Opera can install APK files downloaded from websites. Scam sites trick users into downloading malware.",
            ),
            TrackedApp(
                "com.opera.mini.native", "Opera Mini", "OM", "#FF1B2D", "BROWSERS",
                "Opera Mini can install APK files downloaded from websites. Scam sites trick users into downloading malware.",
            ),
            TrackedApp(
                "com.brave.browser", "Brave", "BR", "#FB542B", "BROWSERS",
                "Brave can install APK files downloaded from websites. Scam sites trick users into downloading malware.",
            ),
            TrackedApp(
                "com.UCMobile.intl", "UC Browser", "UC", "#F58220", "BROWSERS",
                "UC Browser can install APK files downloaded from websites. Scam sites trick users into downloading malware.",
            ),
            TrackedApp(
                "com.duckduckgo.mobile.android", "DuckDuckGo", "DD", "#DE5833", "BROWSERS",
                "DuckDuckGo can install APK files downloaded from websites. Scam sites trick users into downloading malware.",
            ),
            TrackedApp(
                "com.vivaldi.browser", "Vivaldi", "VI", "#EF3939", "BROWSERS",
                "Vivaldi can install APK files downloaded from websites. Scam sites trick users into downloading malware.",
            ),
            TrackedApp(
                "com.mi.globalbrowser", "Mi Browser", "MI", "#FF6900", "BROWSERS",
                "Mi Browser can install APK files downloaded from websites. Scam sites trick users into downloading malware.",
            ),
            TrackedApp(
                "com.vivo.browser", "Vivo Browser", "VV", "#415FFF", "BROWSERS",
                "Vivo Browser can install APK files downloaded from websites. Scam sites trick users into downloading malware.",
            ),
            TrackedApp(
                "com.heytap.browser", "OPPO Browser", "OB", "#1BA784", "BROWSERS",
                "OPPO Browser can install APK files downloaded from websites. Scam sites trick users into downloading malware.",
            ),
            TrackedApp(
                "com.huawei.browser", "Huawei Browser", "HW", "#CF0A2C", "BROWSERS",
                "Huawei Browser can install APK files downloaded from websites. Scam sites trick users into downloading malware.",
            ),
        )

        val FILES = listOf(
            TrackedApp(
                "com.google.android.apps.nbu.files", "Files by Google", "FG", "#34A853", "FILES",
                "Files by Google can install APK files from storage. Malware APKs may be hidden among downloaded files.",
            ),
            TrackedApp(
                "com.mi.android.globalFileexplorer", "Mi File Manager", "MF", "#FF6900", "FILES",
                "Mi File Manager can install APK files from storage. Malware APKs may be hidden among downloaded files.",
            ),
        )

        /** All tracked apps across all categories. */
        val ALL: List<TrackedApp> = MESSAGING + BROWSERS + FILES
    }
}

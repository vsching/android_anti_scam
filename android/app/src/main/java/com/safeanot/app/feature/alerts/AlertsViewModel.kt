/**
 * ViewModel for the Scam Alerts feed screen.
 * Uses static data for v1; will integrate with a remote API in v2.
 */
package com.safeanot.app.feature.alerts

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class ScamAlert(
    val id: Int,
    val title: String,
    val summary: String,
    val details: String,
    val severity: String, // "HIGH", "MEDIUM", "LOW"
    val date: String,
)

data class AlertsUiState(
    val alerts: List<ScamAlert> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class AlertsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(
        AlertsUiState(
            alerts = staticAlerts,
        )
    )
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    companion object {
        private val staticAlerts = listOf(
            ScamAlert(
                id = 1,
                title = "Fake Banking App APKs on WhatsApp",
                summary = "Scammers are sending APK files disguised as banking apps via WhatsApp groups.",
                details = "Multiple reports of APK files being shared in WhatsApp groups claiming to be official banking app updates for Maybank, CIMB, and Public Bank. These are malware designed to steal your banking credentials. Never install APK files from WhatsApp — always update apps through the Google Play Store.",
                severity = "HIGH",
                date = "2026-03-15",
            ),
            ScamAlert(
                id = 2,
                title = "Parcel Delivery SMS Scam Links",
                summary = "SMS messages with fake parcel tracking links that download malware.",
                details = "Scam SMS messages claiming to be from J&T Express, Pos Malaysia, or DHL with links to track parcels. The links redirect to APK downloads that install spyware on your device. Do not click links in unsolicited SMS messages. Check parcel tracking directly through the official courier app or website.",
                severity = "HIGH",
                date = "2026-03-12",
            ),
            ScamAlert(
                id = 3,
                title = "Telegram Investment Group Scams",
                summary = "Fake investment groups on Telegram luring victims with promised returns.",
                details = "Scam Telegram groups claiming guaranteed returns on cryptocurrency or stock trading. Victims are asked to download a 'trading platform' APK that is actually malware. Remember: if an investment sounds too good to be true, it probably is. Never download apps from Telegram groups.",
                severity = "MEDIUM",
                date = "2026-03-10",
            ),
            ScamAlert(
                id = 4,
                title = "Browser Pop-up 'Security Update' Scam",
                summary = "Fake security warnings in browsers trick users into installing malware APKs.",
                details = "When browsing certain websites, a pop-up appears claiming your phone has a virus and needs an urgent security update. The 'update' is actually a malicious APK. Your browser will never ask you to install an APK for security. Close the tab immediately if you see such pop-ups.",
                severity = "MEDIUM",
                date = "2026-03-08",
            ),
            ScamAlert(
                id = 5,
                title = "Play Protect: Enable Enhanced Scanning",
                summary = "Google Play Protect can scan sideloaded apps — make sure it's enabled.",
                details = "Google Play Protect includes a feature called 'Improve harmful app detection' that sends unknown apps to Google for analysis. This is especially important if you have ever sideloaded an APK. Go to Play Store > Profile > Play Protect > Settings and enable both scanning options.",
                severity = "LOW",
                date = "2026-03-05",
            ),
        )
    }
}

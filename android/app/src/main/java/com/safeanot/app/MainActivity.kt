/**
 * Single-activity host for the Safe Anot? app.
 * Sets up Jetpack Compose content with the app theme and navigation graph.
 * Handles incoming share intents (ACTION_SEND text/plain) and
 * deep links (ACTION_VIEW safeanot.com/result?domain=...).
 */
package com.safeanot.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.safeanot.app.navigation.SafeAnotNavGraph
import com.safeanot.app.ui.theme.SafeAnotTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** URL extracted from an incoming intent — backed by Compose state for recomposition. */
    var pendingUrl by mutableStateOf<String?>(null)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingUrl = extractUrlFromIntent(intent)

        setContent {
            SafeAnotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SafeAnotNavGraph(pendingUrl = pendingUrl)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingUrl = extractUrlFromIntent(intent)
    }

    private fun extractUrlFromIntent(intent: Intent?): String? {
        if (intent == null) return null

        return when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
                    extractFirstUrl(text) ?: text.trim()
                } else {
                    null
                }
            }

            Intent.ACTION_VIEW -> {
                val data = intent.data ?: return null
                if (data.host == "safeanot.com" && data.path?.startsWith("/result") == true) {
                    data.getQueryParameter("domain")
                } else {
                    data.toString()
                }
            }

            else -> null
        }
    }

    /** Extracts the first URL from text using a simple pattern. */
    private fun extractFirstUrl(text: String): String? {
        val urlPattern = Regex("""https?://\S+""")
        return urlPattern.find(text)?.value
    }
}

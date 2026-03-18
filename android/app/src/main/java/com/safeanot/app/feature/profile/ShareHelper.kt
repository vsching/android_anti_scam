/**
 * Helper object for creating share intents. Kept as a pure object for testability.
 */
package com.safeanot.app.feature.profile

import android.content.Intent

object ShareHelper {

    const val SHARE_TEXT =
        "I use Safe Anot? to protect my phone from scams. " +
            "Check it out: https://play.google.com/store/apps/details?id=com.safeanot.app"

    fun createShareIntent(): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, SHARE_TEXT)
        }
    }
}

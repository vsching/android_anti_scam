/**
 * Helper object for creating share intents. Delegates to ShareIntentFactory.
 */
package com.safeanot.app.feature.profile

import android.content.Intent
import com.safeanot.app.util.ShareIntentFactory

object ShareHelper {

    const val SHARE_TEXT =
        "I use Safe Anot? to protect my phone from scams. " +
            "Check it out: https://play.google.com/store/apps/details?id=com.safeanot.app"

    fun createShareIntent(): Intent {
        return ShareIntentFactory.createTextShare(SHARE_TEXT)
    }
}

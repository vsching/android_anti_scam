/**
 * Pure utility object for creating share intents.
 * No Context dependency — all methods return Intent instances.
 */
package com.safeanot.app.util

import android.content.Intent
import android.net.Uri

object ShareIntentFactory {

    private const val WHATSAPP_PACKAGE = "com.whatsapp"

    /**
     * Creates a text-only share intent with a chooser.
     */
    fun createTextShare(text: String): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        return Intent.createChooser(sendIntent, "Share")
    }

    /**
     * Creates an image share intent with optional accompanying text.
     * Grants read permission on the URI to the receiving app.
     */
    fun createImageShare(imageUri: Uri, text: String? = null): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            if (text != null) {
                putExtra(Intent.EXTRA_TEXT, text)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, "Share")
    }

    /**
     * Creates a text share intent targeted at WhatsApp.
     * If WhatsApp is not installed, the intent will fail at startActivity.
     * Callers should check WhatsAppUtils.isWhatsAppInstalled() first.
     */
    fun createWhatsAppTextShare(text: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage(WHATSAPP_PACKAGE)
        }
    }
}

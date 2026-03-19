/**
 * Sealed class representing share events emitted by CheckViewModel.
 * The UI layer collects these events and handles intent creation + startActivity.
 */
package com.safeanot.app.feature.check

import android.graphics.Bitmap

sealed class ShareEvent {
    /**
     * Share a bitmap image with accompanying text.
     * UI should save the bitmap via ShareImageCache, then create an image share intent.
     */
    data class ImageWithText(val bitmap: Bitmap, val text: String) : ShareEvent()

    /**
     * Share a bitmap image without text.
     * UI should save the bitmap via ShareImageCache, then create an image share intent.
     */
    data class BitmapOnly(val bitmap: Bitmap) : ShareEvent()

    /**
     * Share text only.
     * UI should create a text share intent via ShareIntentFactory.
     */
    data class TextOnly(val text: String) : ShareEvent()
}

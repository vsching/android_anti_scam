/**
 * Sealed class representing share events emitted by ViewModels.
 * The UI layer collects these events and handles intent creation + startActivity.
 * Each event carries metadata (shareType, contentId) for analytics tracking,
 * so the UI doesn't need heuristics to determine what was shared.
 */
package com.safeanot.app.feature.check

import android.graphics.Bitmap
import com.safeanot.app.domain.model.ShareType

sealed class ShareEvent {
    /** The type of share for analytics tracking. */
    abstract val shareType: ShareType
    /** The content identifier for analytics tracking (e.g., domain, score). */
    abstract val contentId: String

    /**
     * Share a bitmap image with accompanying text.
     * UI should save the bitmap via ShareImageCache, then create an image share intent.
     */
    data class ImageWithText(
        val bitmap: Bitmap,
        val text: String,
        override val shareType: ShareType,
        override val contentId: String,
    ) : ShareEvent()

    /**
     * Share a bitmap image without text.
     * UI should save the bitmap via ShareImageCache, then create an image share intent.
     */
    data class BitmapOnly(
        val bitmap: Bitmap,
        override val shareType: ShareType,
        override val contentId: String,
    ) : ShareEvent()

    /**
     * Share text only.
     * UI should create a text share intent via ShareIntentFactory.
     */
    data class TextOnly(
        val text: String,
        override val shareType: ShareType,
        override val contentId: String,
    ) : ShareEvent()
}

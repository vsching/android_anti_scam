/**
 * Manages temporary bitmap files for sharing verdict card images.
 * Writes bitmaps to the cache directory and returns FileProvider URIs.
 */
package com.safeanot.app.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ShareImageCache {

    private const val SHARED_DIR = "shared_verdicts"
    private const val DEFAULT_MAX_AGE_MS = 24 * 60 * 60 * 1000L // 24 hours

    /**
     * Saves a bitmap to the shared_verdicts cache directory and returns a FileProvider URI.
     * Cleans up old files before writing.
     *
     * @param context Application or activity context
     * @param bitmap The bitmap to save as PNG
     * @param prefix Filename prefix (e.g., "verdict")
     * @return FileProvider URI for the saved file
     */
    fun saveBitmap(context: Context, bitmap: Bitmap, prefix: String = "verdict"): Uri {
        cleanupOldFiles(context)

        val sharedDir = File(context.cacheDir, SHARED_DIR)
        sharedDir.mkdirs()

        val file = File(sharedDir, "${prefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    /**
     * Deletes files older than [maxAgeMs] from the shared_verdicts cache directory.
     */
    fun cleanupOldFiles(context: Context, maxAgeMs: Long = DEFAULT_MAX_AGE_MS) {
        val sharedDir = File(context.cacheDir, SHARED_DIR)
        if (!sharedDir.exists()) return

        val cutoff = System.currentTimeMillis() - maxAgeMs
        sharedDir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }
}

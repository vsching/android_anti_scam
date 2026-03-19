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

    private const val MAX_CACHED_FILES = 10

    /**
     * Deletes files older than [maxAgeMs] from the shared_verdicts cache directory,
     * and also caps the total number of cached files to [MAX_CACHED_FILES] (keeps most recent).
     */
    fun cleanupOldFiles(context: Context, maxAgeMs: Long = DEFAULT_MAX_AGE_MS) {
        val sharedDir = File(context.cacheDir, SHARED_DIR)
        if (!sharedDir.exists()) return

        val cutoff = System.currentTimeMillis() - maxAgeMs
        val files = sharedDir.listFiles()?.filter { it.isFile } ?: return

        // Delete files older than the max age
        files.forEach { file ->
            if (file.lastModified() < cutoff) {
                file.delete()
            }
        }

        // Cap total files to MAX_CACHED_FILES, deleting oldest beyond the limit
        val remaining = sharedDir.listFiles()?.filter { it.isFile } ?: return
        if (remaining.size > MAX_CACHED_FILES) {
            remaining
                .sortedByDescending { it.lastModified() }
                .drop(MAX_CACHED_FILES)
                .forEach { it.delete() }
        }
    }
}

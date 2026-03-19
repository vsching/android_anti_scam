package com.safeanot.app.util

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

/**
 * Tests for ShareImageCache. Uses Robolectric for Context and filesystem access.
 * Note: FileProvider.getUriForFile requires full Android setup, so saveBitmap
 * URI generation is tested via androidTest. These tests verify file I/O and cleanup.
 */
@RunWith(RobolectricTestRunner::class)
class ShareImageCacheTest {

    private lateinit var sharedDir: File

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        sharedDir = File(context.cacheDir, "shared_verdicts")
        // Clean up from any prior test runs
        sharedDir.deleteRecursively()
    }

    @Test
    fun `cleanupOldFiles deletes files older than maxAge`() {
        val context = RuntimeEnvironment.getApplication()
        sharedDir.mkdirs()

        // Create an "old" file with a last-modified time well in the past
        val oldFile = File(sharedDir, "old_verdict.png")
        oldFile.createNewFile()
        oldFile.setLastModified(System.currentTimeMillis() - 48 * 60 * 60 * 1000) // 48h ago

        // Create a "new" file
        val newFile = File(sharedDir, "new_verdict.png")
        newFile.createNewFile()
        newFile.setLastModified(System.currentTimeMillis())

        ShareImageCache.cleanupOldFiles(context, maxAgeMs = 24 * 60 * 60 * 1000)

        assertFalse("Old file should be deleted", oldFile.exists())
        assertTrue("New file should be kept", newFile.exists())
    }

    @Test
    fun `cleanupOldFiles handles missing directory gracefully`() {
        val context = RuntimeEnvironment.getApplication()
        // Should not throw even if directory does not exist
        ShareImageCache.cleanupOldFiles(context)
    }

    @Test
    fun `cleanupOldFiles keeps files within maxAge`() {
        val context = RuntimeEnvironment.getApplication()
        sharedDir.mkdirs()

        val recentFile = File(sharedDir, "recent.png")
        recentFile.createNewFile()
        recentFile.setLastModified(System.currentTimeMillis() - 1000) // 1 second ago

        ShareImageCache.cleanupOldFiles(context, maxAgeMs = 24 * 60 * 60 * 1000)

        assertTrue("Recent file should be kept", recentFile.exists())
    }

    @Test
    fun `saveBitmap creates file in shared_verdicts directory`() {
        val context = RuntimeEnvironment.getApplication()
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        // saveBitmap will fail on FileProvider in Robolectric, but we can verify
        // that the file was written by checking the directory after catching the exception
        try {
            ShareImageCache.saveBitmap(context, bitmap, "test")
        } catch (_: Exception) {
            // FileProvider may not be fully configured in Robolectric
        }

        // Verify the directory was created and a file exists
        assertTrue("shared_verdicts directory should exist", sharedDir.exists())
        val files = sharedDir.listFiles()?.filter { it.name.startsWith("test") } ?: emptyList()
        assertEquals("One file should be created with the test prefix", 1, files.size)
        assertTrue("File should have .png extension", files[0].name.endsWith(".png"))
        assertTrue("File should have non-zero size", files[0].length() > 0)
    }
}

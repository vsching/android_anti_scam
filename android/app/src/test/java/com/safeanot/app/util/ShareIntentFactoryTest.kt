package com.safeanot.app.util

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShareIntentFactoryTest {

    @Test
    fun `createTextShare creates chooser with text plain type`() {
        val intent = ShareIntentFactory.createTextShare("Hello world")

        // Chooser wraps the actual send intent
        assertEquals(Intent.ACTION_CHOOSER, intent.action)

        val wrapped = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(wrapped)
        assertEquals(Intent.ACTION_SEND, wrapped!!.action)
        assertEquals("text/plain", wrapped.type)
        assertEquals("Hello world", wrapped.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun `createImageShare creates chooser with image type and URI`() {
        val uri = Uri.parse("content://com.safeanot.app.fileprovider/test.png")
        val intent = ShareIntentFactory.createImageShare(uri, "Check this out")

        assertEquals(Intent.ACTION_CHOOSER, intent.action)

        val wrapped = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(wrapped)
        assertEquals(Intent.ACTION_SEND, wrapped!!.action)
        assertEquals("image/png", wrapped.type)
        assertEquals(uri, wrapped.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        assertEquals("Check this out", wrapped.getStringExtra(Intent.EXTRA_TEXT))
        assertTrue(wrapped.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test
    fun `createImageShare without text omits EXTRA_TEXT`() {
        val uri = Uri.parse("content://com.safeanot.app.fileprovider/test.png")
        val intent = ShareIntentFactory.createImageShare(uri)

        val wrapped = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(wrapped)
        assertEquals(null, wrapped!!.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun `createWhatsAppTextShare targets whatsapp package`() {
        val intent = ShareIntentFactory.createWhatsAppTextShare("Hello WhatsApp")

        // WhatsApp intent is NOT wrapped in a chooser
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals("Hello WhatsApp", intent.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals("com.whatsapp", intent.`package`)
    }
}

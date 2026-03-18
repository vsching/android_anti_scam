package com.safeanot.app.feature.profile

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for ShareHelper share text content.
 * Intent creation tests require Android framework (Robolectric or instrumented)
 * and will be added in E05-002 with the necessary dependencies.
 */
class ShareHelperTest {

    @Test
    fun `share text contains app name`() {
        assertTrue(ShareHelper.SHARE_TEXT.contains("Safe Anot?"))
    }

    @Test
    fun `share text contains play store link`() {
        assertTrue(
            ShareHelper.SHARE_TEXT.contains(
                "https://play.google.com/store/apps/details?id=com.safeanot.app"
            )
        )
    }

    @Test
    fun `share text is not empty`() {
        assertTrue(ShareHelper.SHARE_TEXT.isNotEmpty())
    }

    @Test
    fun `share text mentions scam protection`() {
        assertTrue(ShareHelper.SHARE_TEXT.contains("scam"))
    }
}

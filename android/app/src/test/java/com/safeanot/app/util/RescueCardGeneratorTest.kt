package com.safeanot.app.util

import android.graphics.Bitmap
import com.safeanot.app.domain.model.LinkVerdict
import com.safeanot.app.domain.model.VerdictType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RescueCardGeneratorTest {

    @Test
    fun `generate returns 1080x1920 bitmap`() {
        val verdict = LinkVerdict("evil.com", VerdictType.DANGEROUS, "Phishing site", 0.99f)
        val bitmap = RescueCardGenerator.generate(verdict)
        assertNotNull(bitmap)
        assertEquals(1080, bitmap.width)
        assertEquals(1920, bitmap.height)
    }

    @Test
    fun `generate returns non-null bitmap`() {
        val verdict = LinkVerdict("scam.my", VerdictType.DANGEROUS, "Known scam domain", 0.95f)
        val bitmap = RescueCardGenerator.generate(verdict)
        assertNotNull(bitmap)
    }

    @Test
    fun `generate returns ARGB_8888 config`() {
        val verdict = LinkVerdict("bad-site.com", VerdictType.DANGEROUS, "Malware", 0.9f)
        val bitmap = RescueCardGenerator.generate(verdict)
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    }

    @Test
    fun `long domain is truncated to 40 chars`() {
        val longDomain = "a".repeat(60) + ".com"
        val truncated = RescueCardGenerator.truncateDomain(longDomain)
        assertEquals(40, truncated.length)
        assert(truncated.endsWith("..."))
    }

    @Test
    fun `short domain is not truncated`() {
        val domain = "example.com"
        val result = RescueCardGenerator.truncateDomain(domain)
        assertEquals(domain, result)
    }

    @Test
    fun `domain exactly 40 chars is not truncated`() {
        val domain = "a".repeat(36) + ".com" // 40 chars
        val result = RescueCardGenerator.truncateDomain(domain)
        assertEquals(domain, result)
    }

    @Test
    fun `generate with long domain produces valid bitmap`() {
        val longDomain = "this-is-a-very-long-domain-name-that-exceeds-forty-characters.example.com"
        val verdict = LinkVerdict(longDomain, VerdictType.DANGEROUS, "Phishing", 0.99f)
        val bitmap = RescueCardGenerator.generate(verdict)
        assertNotNull(bitmap)
        assertEquals(1080, bitmap.width)
        assertEquals(1920, bitmap.height)
    }
}

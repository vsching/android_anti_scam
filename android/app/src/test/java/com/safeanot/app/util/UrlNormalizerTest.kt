package com.safeanot.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlNormalizerTest {

    // --- Basic normalization ---

    @Test
    fun `normalize strips https protocol`() {
        assertEquals("example.com", UrlNormalizer.normalize("https://example.com"))
    }

    @Test
    fun `normalize strips http protocol`() {
        assertEquals("example.com", UrlNormalizer.normalize("http://example.com"))
    }

    @Test
    fun `normalize strips path`() {
        assertEquals("example.com", UrlNormalizer.normalize("https://example.com/path/to/page"))
    }

    @Test
    fun `normalize strips query string`() {
        assertEquals("example.com", UrlNormalizer.normalize("https://example.com?foo=bar"))
    }

    @Test
    fun `normalize strips fragment`() {
        assertEquals("example.com", UrlNormalizer.normalize("https://example.com#section"))
    }

    @Test
    fun `normalize strips www prefix`() {
        assertEquals("example.com", UrlNormalizer.normalize("www.example.com"))
    }

    @Test
    fun `normalize strips www and protocol`() {
        assertEquals("example.com", UrlNormalizer.normalize("https://www.example.com/page"))
    }

    @Test
    fun `normalize lowercases domain`() {
        assertEquals("example.com", UrlNormalizer.normalize("EXAMPLE.COM"))
    }

    @Test
    fun `normalize strips port`() {
        assertEquals("example.com", UrlNormalizer.normalize("https://example.com:8080/path"))
    }

    @Test
    fun `normalize strips userinfo`() {
        assertEquals("example.com", UrlNormalizer.normalize("https://user:pass@example.com/path"))
    }

    @Test
    fun `normalize strips trailing dot`() {
        assertEquals("example.com", UrlNormalizer.normalize("example.com."))
    }

    @Test
    fun `normalize bare domain unchanged`() {
        assertEquals("example.com", UrlNormalizer.normalize("example.com"))
    }

    @Test
    fun `normalize subdomain preserved`() {
        assertEquals("sub.example.com", UrlNormalizer.normalize("https://sub.example.com"))
    }

    // --- Invalid inputs ---

    @Test
    fun `normalize returns null for null input`() {
        assertNull(UrlNormalizer.normalize(null))
    }

    @Test
    fun `normalize returns null for empty string`() {
        assertNull(UrlNormalizer.normalize(""))
    }

    @Test
    fun `normalize returns null for blank string`() {
        assertNull(UrlNormalizer.normalize("   "))
    }

    @Test
    fun `normalize returns null for IPv4 address`() {
        assertNull(UrlNormalizer.normalize("192.168.1.1"))
    }

    @Test
    fun `normalize returns null for IPv4 with protocol`() {
        assertNull(UrlNormalizer.normalize("http://192.168.1.1/path"))
    }

    @Test
    fun `normalize returns null for localhost`() {
        assertNull(UrlNormalizer.normalize("localhost"))
    }

    @Test
    fun `normalize returns null for localhost with protocol`() {
        assertNull(UrlNormalizer.normalize("http://localhost:3000"))
    }

    @Test
    fun `normalize returns null for consecutive dots`() {
        assertNull(UrlNormalizer.normalize("example..com"))
    }

    @Test
    fun `normalize returns null for leading hyphen in label`() {
        assertNull(UrlNormalizer.normalize("-example.com"))
    }

    @Test
    fun `normalize returns null for trailing hyphen in label`() {
        assertNull(UrlNormalizer.normalize("example-.com"))
    }

    @Test
    fun `normalize returns null for single label`() {
        assertNull(UrlNormalizer.normalize("localhost"))
    }

    @Test
    fun `normalize returns null for IPv6`() {
        assertNull(UrlNormalizer.normalize("[::1]"))
    }

    @Test
    fun `normalize returns null for label exceeding 63 chars`() {
        val longLabel = "a".repeat(64)
        assertNull(UrlNormalizer.normalize("$longLabel.com"))
    }

    // --- IDN / Punycode ---

    @Test
    fun `normalize handles IDN domain`() {
        val result = UrlNormalizer.normalize("https://xn--nxasmq6b.com")
        assertEquals("xn--nxasmq6b.com", result)
    }

    // --- Two-part TLD extraction ---

    @Test
    fun `extractRegistrableDomain with two-part TLD`() {
        assertEquals("example.com.my", UrlNormalizer.extractRegistrableDomain("example.com.my"))
    }

    @Test
    fun `extractRegistrableDomain with subdomain and two-part TLD`() {
        assertEquals("example.co.uk", UrlNormalizer.extractRegistrableDomain("sub.example.co.uk"))
    }

    @Test
    fun `extractRegistrableDomain with regular TLD`() {
        assertEquals("example.com", UrlNormalizer.extractRegistrableDomain("sub.example.com"))
    }

    @Test
    fun `extractRegistrableDomain with bare domain`() {
        assertEquals("example.com", UrlNormalizer.extractRegistrableDomain("example.com"))
    }

    @Test
    fun `extractRegistrableDomain with com_sg`() {
        assertEquals("example.com.sg", UrlNormalizer.extractRegistrableDomain("www2.example.com.sg"))
    }

    // --- Edge cases ---

    @Test
    fun `normalize handles whitespace around input`() {
        assertEquals("example.com", UrlNormalizer.normalize("  example.com  "))
    }

    @Test
    fun `normalize accepts hyphenated domain`() {
        assertEquals("my-example.com", UrlNormalizer.normalize("my-example.com"))
    }

    @Test
    fun `normalize accepts numeric labels`() {
        assertEquals("123.example.com", UrlNormalizer.normalize("123.example.com"))
    }

    @Test
    fun `normalize handles ftp protocol`() {
        assertEquals("example.com", UrlNormalizer.normalize("ftp://example.com/file"))
    }

    @Test
    fun `normalize with complex URL`() {
        assertEquals(
            "shop.example.com.my",
            UrlNormalizer.normalize("https://www.shop.example.com.my/product?id=123#reviews"),
        )
    }
}

package com.safeanot.app.util

import com.safeanot.app.domain.model.WarningTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WarningTemplateProviderTest {

    @Test
    fun `getTemplates returns 3 templates`() {
        val templates = WarningTemplateProvider.getTemplates()
        assertEquals(3, templates.size)
    }

    @Test
    fun `getTemplates contains all three tones`() {
        val tones = WarningTemplateProvider.getTemplates().map { it.tone }.toSet()
        assertEquals(
            setOf(WarningTone.POLITE, WarningTone.URGENT, WarningTone.ELDER_FRIENDLY),
            tones,
        )
    }

    @Test
    fun `format replaces domain placeholder`() {
        val template = WarningTemplateProvider.getTemplates().first()
        val result = WarningTemplateProvider.format(template, "evil.com", "DANGEROUS")
        assertTrue("Should contain domain", result.contains("evil.com"))
    }

    @Test
    fun `format replaces verdict placeholder`() {
        val template = WarningTemplateProvider.getTemplates().first()
        val result = WarningTemplateProvider.format(template, "evil.com", "DANGEROUS")
        assertTrue("Should contain verdict", result.contains("DANGEROUS"))
    }

    @Test
    fun `format appends download link`() {
        val template = WarningTemplateProvider.getTemplates().first()
        val result = WarningTemplateProvider.format(template, "evil.com", "DANGEROUS")
        assertTrue(
            "Should end with download URL",
            result.endsWith(Constants.APP_DOWNLOAD_URL),
        )
    }

    @Test
    fun `format uses MS text when locale is ms`() {
        val polite = WarningTemplateProvider.getTemplates().first { it.tone == WarningTone.POLITE }
        val result = WarningTemplateProvider.format(polite, "evil.com", "DANGEROUS", "ms")
        assertTrue("Should contain Malay text", result.contains("saya semak pautan"))
    }

    @Test
    fun `format uses EN text for unknown locales`() {
        val polite = WarningTemplateProvider.getTemplates().first { it.tone == WarningTone.POLITE }
        val result = WarningTemplateProvider.format(polite, "evil.com", "DANGEROUS", "fr")
        assertTrue("Should contain English text", result.contains("I checked this link"))
    }

    @Test
    fun `format uses EN text by default`() {
        val polite = WarningTemplateProvider.getTemplates().first { it.tone == WarningTone.POLITE }
        val result = WarningTemplateProvider.format(polite, "evil.com", "DANGEROUS")
        assertTrue("Should contain English text", result.contains("I checked this link"))
    }
}

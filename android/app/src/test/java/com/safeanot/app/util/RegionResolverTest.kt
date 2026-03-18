package com.safeanot.app.util

import com.safeanot.app.domain.model.AlertRegionFilter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class RegionResolverTest {

    @Test
    fun `ms_MY locale maps to MALAYSIA`() {
        val locale = Locale("ms", "MY")
        assertEquals(AlertRegionFilter.MALAYSIA, RegionResolver.fromLocale(locale))
    }

    @Test
    fun `en_SG locale maps to SINGAPORE`() {
        val locale = Locale("en", "SG")
        assertEquals(AlertRegionFilter.SINGAPORE, RegionResolver.fromLocale(locale))
    }

    @Test
    fun `en_US locale maps to ALL`() {
        val locale = Locale("en", "US")
        assertEquals(AlertRegionFilter.ALL, RegionResolver.fromLocale(locale))
    }

    @Test
    fun `unknown locale maps to ALL`() {
        val locale = Locale("de", "DE")
        assertEquals(AlertRegionFilter.ALL, RegionResolver.fromLocale(locale))
    }

    @Test
    fun `lowercase country code still maps correctly`() {
        // Locale constructor normalizes, but ensure our uppercase logic works
        val locale = Locale("en", "my")
        assertEquals(AlertRegionFilter.MALAYSIA, RegionResolver.fromLocale(locale))
    }
}

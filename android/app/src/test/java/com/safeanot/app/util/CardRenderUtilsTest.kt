package com.safeanot.app.util

import android.graphics.Paint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardRenderUtilsTest {

    private val paint = Paint().apply { textSize = 40f }

    @Test
    fun `wrapText returns single line for short text`() {
        val lines = CardRenderUtils.wrapText("Hello", paint, 1000f)
        assertEquals(1, lines.size)
        assertEquals("Hello", lines[0])
    }

    @Test
    fun `wrapText splits long text into multiple lines`() {
        val longText = "This is a very long text that should definitely wrap to multiple lines"
        val lines = CardRenderUtils.wrapText(longText, paint, 200f)
        assertTrue("Expected multiple lines but got ${lines.size}", lines.size > 1)
    }

    @Test
    fun `wrapText returns empty list for empty string`() {
        val lines = CardRenderUtils.wrapText("", paint, 500f)
        assertTrue("Expected empty list for empty input", lines.isEmpty())
    }

    @Test
    fun `wrapText handles single word wider than maxWidth`() {
        val lines = CardRenderUtils.wrapText("Supercalifragilisticexpialidocious", paint, 10f)
        assertEquals(1, lines.size)
    }

    @Test
    fun `wrapText preserves all words`() {
        val input = "one two three"
        val lines = CardRenderUtils.wrapText(input, paint, 1000f)
        assertEquals("one two three", lines.joinToString(" "))
    }
}

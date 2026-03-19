/**
 * Shared rendering utilities for card generators (VerdictCardGenerator, SecurityScoreCardGenerator).
 * Extracted to avoid duplication of text wrapping logic.
 */
package com.safeanot.app.util

import android.graphics.Paint

object CardRenderUtils {

    /**
     * Wraps [text] into lines that fit within [maxWidth] as measured by [paint].
     * Splits on spaces; words wider than [maxWidth] are placed on their own line.
     */
    fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(' ')
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }

        return lines
    }
}

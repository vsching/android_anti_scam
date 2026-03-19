/**
 * Generates shareable security score card bitmaps using Canvas + Paint.
 * No Compose dependency — pure android.graphics rendering.
 * Two formats: square (1080x1080) and vertical (1080x1920).
 */
package com.safeanot.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.safeanot.app.domain.model.ScoreBand
import com.safeanot.app.domain.model.SecurityScore

object SecurityScoreCardGenerator {

    private const val WIDTH = 1080
    private const val SQUARE_HEIGHT = 1080
    private const val VERTICAL_HEIGHT = 1920
    private const val COLOR_BAR_HEIGHT = 120f

    private val COLOR_RED = Color.parseColor("#F44336")
    private val COLOR_AMBER = Color.parseColor("#FF9800")
    private val COLOR_GREEN = Color.parseColor("#4CAF50")

    private fun scoreColor(band: ScoreBand): Int = when (band) {
        ScoreBand.RED -> COLOR_RED
        ScoreBand.AMBER -> COLOR_AMBER
        ScoreBand.GREEN -> COLOR_GREEN
    }

    /**
     * Generates a 1080x1080 square card.
     */
    fun generateSquare(score: SecurityScore): Bitmap {
        return render(score, SQUARE_HEIGHT)
    }

    /**
     * Generates a 1080x1920 vertical card.
     */
    fun generateVertical(score: SecurityScore): Bitmap {
        return render(score, VERTICAL_HEIGHT)
    }

    private fun render(score: SecurityScore, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val color = scoreColor(score.band)

        // White background
        canvas.drawColor(Color.WHITE)

        // Top color bar
        val barPaint = Paint().apply { this.color = color }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), COLOR_BAR_HEIGHT, barPaint)

        // "Safe Anot?" branding on color bar
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Safe Anot?", 40f, COLOR_BAR_HEIGHT - 40f, brandPaint)

        // Center area for score ring
        val centerX = WIDTH / 2f
        val ringCenterY = if (height == VERTICAL_HEIGHT) height * 0.35f else height * 0.42f
        val ringRadius = 180f
        val ringStroke = 24f

        // Ring track (background)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.parseColor("#E0E0E0")
            style = Paint.Style.STROKE
            strokeWidth = ringStroke
            strokeCap = Paint.Cap.ROUND
        }
        val ringRect = RectF(
            centerX - ringRadius,
            ringCenterY - ringRadius,
            centerX + ringRadius,
            ringCenterY + ringRadius,
        )
        canvas.drawArc(ringRect, -90f, 360f, false, trackPaint)

        // Ring progress
        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = ringStroke
            strokeCap = Paint.Cap.ROUND
        }
        val sweepAngle = 360f * score.scorePercent / 100f
        canvas.drawArc(ringRect, -90f, sweepAngle, false, progressPaint)

        // Score percentage in center of ring
        val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = 80f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${score.scorePercent}%", centerX, ringCenterY + 28f, scorePaint)

        // "X of Y items secured" below ring
        val securedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.parseColor("#616161")
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "${score.securedItems} of ${score.totalItems} items secured",
            centerX,
            ringCenterY + ringRadius + 80f,
            securedPaint,
        )

        // Score label
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val bandLabel = when (score.band) {
            ScoreBand.GREEN -> "Your phone is well protected"
            ScoreBand.AMBER -> "Some items need attention"
            ScoreBand.RED -> "Your phone is at risk"
        }
        canvas.drawText(
            bandLabel,
            centerX,
            ringCenterY + ringRadius + 150f,
            labelPaint,
        )

        // Bottom CTA / download link
        val bottomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.parseColor("#9E9E9E")
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Check yours at", 60f, height - 100f, bottomPaint)

        val urlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("https://safeanot.com/download", 60f, height - 55f, urlPaint)

        return bitmap
    }
}

/**
 * Generates a shareable 1080x1080 verdict card as a Bitmap using Canvas + Paint.
 * No Compose dependency — pure android.graphics rendering.
 */
package com.safeanot.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.safeanot.app.domain.model.LinkVerdict
import com.safeanot.app.domain.model.VerdictType

object VerdictCardGenerator {

    private const val SIZE = 1080
    private const val COLOR_BAR_HEIGHT = 120f

    private fun verdictColor(type: VerdictType): Int = when (type) {
        VerdictType.SAFE -> Color.parseColor("#4CAF50")
        VerdictType.DANGEROUS -> Color.parseColor("#F44336")
        VerdictType.SUSPICIOUS -> Color.parseColor("#FF9800")
        VerdictType.UNKNOWN, VerdictType.NOT_IN_DATABASE -> Color.parseColor("#9E9E9E")
    }

    fun generate(verdict: LinkVerdict): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val color = verdictColor(verdict.verdict)

        // White background
        canvas.drawColor(Color.WHITE)

        // Top color bar
        val barPaint = Paint().apply { this.color = color }
        canvas.drawRect(0f, 0f, SIZE.toFloat(), COLOR_BAR_HEIGHT, barPaint)

        // "Safe Anot?" branding on color bar
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Safe Anot?", 40f, COLOR_BAR_HEIGHT - 40f, brandPaint)

        // Verdict label
        val verdictPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = 80f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val verdictLabel = verdict.verdict.name.replace('_', ' ')
        canvas.drawText(verdictLabel, 60f, COLOR_BAR_HEIGHT + 130f, verdictPaint)

        // Domain text
        val domainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.parseColor("#212121")
            textSize = 56f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(verdict.domain, 60f, COLOR_BAR_HEIGHT + 220f, domainPaint)

        // Divider line
        val dividerPaint = Paint().apply {
            this.color = Color.parseColor("#E0E0E0")
            strokeWidth = 2f
        }
        canvas.drawLine(60f, COLOR_BAR_HEIGHT + 260f, SIZE - 60f, COLOR_BAR_HEIGHT + 260f, dividerPaint)

        // Reason text (wrap manually)
        val reasonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.parseColor("#616161")
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val reasonLines = CardRenderUtils.wrapText(verdict.reason, reasonPaint, SIZE - 120f)
        var reasonY = COLOR_BAR_HEIGHT + 330f
        for (line in reasonLines) {
            canvas.drawText(line, 60f, reasonY, reasonPaint)
            reasonY += 52f
        }

        // Confidence bar
        val confLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.parseColor("#757575")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val confY = 700f
        canvas.drawText(
            "Confidence: ${(verdict.confidence * 100).toInt()}%",
            60f,
            confY,
            confLabelPaint,
        )

        // Confidence bar background
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.parseColor("#E0E0E0")
        }
        val trackRect = RectF(60f, confY + 20f, SIZE - 60f, confY + 50f)
        canvas.drawRoundRect(trackRect, 15f, 15f, trackPaint)

        // Confidence bar fill
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
        }
        val fillWidth = 60f + (SIZE - 120f) * verdict.confidence
        val fillRect = RectF(60f, confY + 20f, fillWidth, confY + 50f)
        canvas.drawRoundRect(fillRect, 15f, 15f, fillPaint)

        // Bottom branding / download URL
        val bottomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.parseColor("#9E9E9E")
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Checked by Safe Anot?", 60f, SIZE - 100f, bottomPaint)

        val urlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(
            "https://safeanot.com/result?domain=${verdict.domain}",
            60f,
            SIZE - 55f,
            urlPaint,
        )

        return bitmap
    }

}

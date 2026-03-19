/**
 * Generates a shareable 1080x1920 "Rescue Card" bitmap using Canvas + Paint.
 * Designed for forwarding to loved ones when a DANGEROUS link is detected.
 * No Compose dependency — pure android.graphics rendering.
 */
package com.safeanot.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.safeanot.app.domain.model.LinkVerdict

object RescueCardGenerator {

    private const val WIDTH = 1080
    private const val HEIGHT = 1920
    private const val MAX_DOMAIN_LENGTH = 40

    private val PEACH_TOP = Color.parseColor("#FFB199")
    private val CORAL_BOTTOM = Color.parseColor("#FF6B6B")
    private val BADGE_RED = Color.parseColor("#D32F2F")
    private val TEXT_DARK = Color.parseColor("#3E2723")
    private val TEXT_MEDIUM = Color.parseColor("#5D4037")
    private val DIVIDER_COLOR = Color.parseColor("#FFFFFF")

    fun generate(verdict: LinkVerdict): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Warm peach/coral gradient background
        val gradientPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, HEIGHT.toFloat(),
                PEACH_TOP, CORAL_BOTTOM,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), gradientPaint)

        // Shield icon at top (drawn with paths)
        drawShieldIcon(canvas, WIDTH / 2f, 280f)

        // Headline: "This scam was sent to someone you love"
        val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val headlineLines = CardRenderUtils.wrapText(
            "This scam was sent to someone you love",
            headlinePaint,
            WIDTH - 160f,
        )
        var y = 520f
        for (line in headlineLines) {
            canvas.drawText(line, WIDTH / 2f, y, headlinePaint)
            y += 80f
        }

        // Domain with DANGEROUS badge
        y += 40f
        val domain = truncateDomain(verdict.domain)
        val domainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT_DARK
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(domain, WIDTH / 2f, y, domainPaint)

        // DANGEROUS badge
        y += 50f
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BADGE_RED
        }
        val badgeText = "DANGEROUS"
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val badgeWidth = badgeTextPaint.measureText(badgeText) + 48f
        val badgeRect = RectF(
            WIDTH / 2f - badgeWidth / 2f,
            y - 32f,
            WIDTH / 2f + badgeWidth / 2f,
            y + 16f,
        )
        canvas.drawRoundRect(badgeRect, 20f, 20f, badgePaint)
        canvas.drawText(badgeText, WIDTH / 2f, y + 4f, badgeTextPaint)

        // Reason text
        y += 80f
        val reasonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT_MEDIUM
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val reasonLines = CardRenderUtils.wrapText(
            verdict.reason,
            reasonPaint,
            WIDTH - 160f,
        )
        for (line in reasonLines) {
            canvas.drawText(line, WIDTH / 2f, y, reasonPaint)
            y += 56f
        }

        // Divider line
        y += 30f
        val dividerPaint = Paint().apply {
            color = DIVIDER_COLOR
            alpha = 120
            strokeWidth = 3f
        }
        canvas.drawLine(160f, y, WIDTH - 160f, y, dividerPaint)

        // CTA: "Check your links before you click"
        y += 80f
        val ctaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val ctaLines = CardRenderUtils.wrapText(
            "Check your links before you click",
            ctaPaint,
            WIDTH - 160f,
        )
        for (line in ctaLines) {
            canvas.drawText(line, WIDTH / 2f, y, ctaPaint)
            y += 64f
        }

        // Bottom branding: "Safe Anot?" + download URL
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Safe Anot?", WIDTH / 2f, HEIGHT - 160f, brandPaint)

        val urlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 200
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "https://safeanot.com",
            WIDTH / 2f,
            HEIGHT - 100f,
            urlPaint,
        )

        return bitmap
    }

    private fun drawShieldIcon(canvas: Canvas, cx: Float, cy: Float) {
        val shieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 230
            style = Paint.Style.FILL
        }
        val size = 100f
        val path = Path().apply {
            moveTo(cx, cy - size)
            lineTo(cx + size * 0.85f, cy - size * 0.55f)
            lineTo(cx + size * 0.85f, cy + size * 0.15f)
            quadTo(cx + size * 0.6f, cy + size * 0.9f, cx, cy + size)
            quadTo(cx - size * 0.6f, cy + size * 0.9f, cx - size * 0.85f, cy + size * 0.15f)
            lineTo(cx - size * 0.85f, cy - size * 0.55f)
            close()
        }
        canvas.drawPath(path, shieldPaint)

        // Checkmark inside shield
        val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BADGE_RED
            style = Paint.Style.STROKE
            strokeWidth = 12f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val checkPath = Path().apply {
            moveTo(cx - 35f, cy + 5f)
            lineTo(cx - 8f, cy + 35f)
            lineTo(cx + 40f, cy - 25f)
        }
        canvas.drawPath(checkPath, checkPaint)
    }

    internal fun truncateDomain(domain: String): String {
        if (domain.length <= MAX_DOMAIN_LENGTH) return domain
        return domain.take(MAX_DOMAIN_LENGTH - 3) + "..."
    }
}

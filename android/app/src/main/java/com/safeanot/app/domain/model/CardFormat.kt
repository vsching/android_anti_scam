/**
 * Supported card formats for shareable images.
 */
package com.safeanot.app.domain.model

enum class CardFormat {
    /** 1080x1080 square card for Instagram / feed posts. */
    SQUARE,

    /** 1080x1920 vertical card for Stories / WhatsApp Status. */
    VERTICAL,
}

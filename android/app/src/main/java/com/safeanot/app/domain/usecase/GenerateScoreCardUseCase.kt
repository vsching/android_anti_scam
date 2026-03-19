/**
 * Use case that generates a shareable security score card bitmap.
 * Delegates rendering to SecurityScoreCardGenerator.
 */
package com.safeanot.app.domain.usecase

import android.graphics.Bitmap
import com.safeanot.app.domain.model.CardFormat
import com.safeanot.app.domain.model.SecurityScore
import com.safeanot.app.util.SecurityScoreCardGenerator
import javax.inject.Inject

class GenerateScoreCardUseCase @Inject constructor() {

    /**
     * Generates a bitmap card for the given [score] in the specified [format].
     */
    operator fun invoke(score: SecurityScore, format: CardFormat): Bitmap {
        return when (format) {
            CardFormat.SQUARE -> SecurityScoreCardGenerator.generateSquare(score)
            CardFormat.VERTICAL -> SecurityScoreCardGenerator.generateVertical(score)
        }
    }
}

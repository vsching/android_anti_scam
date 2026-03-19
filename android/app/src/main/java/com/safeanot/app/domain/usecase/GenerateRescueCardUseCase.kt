/**
 * Use case that generates a shareable rescue card bitmap from a link verdict.
 * Delegates to RescueCardGenerator for a 1080x1920 "Forwarded by Someone You Love" card.
 */
package com.safeanot.app.domain.usecase

import android.graphics.Bitmap
import com.safeanot.app.domain.model.LinkVerdict
import com.safeanot.app.util.RescueCardGenerator
import javax.inject.Inject

class GenerateRescueCardUseCase @Inject constructor() {

    /**
     * Generates a rescue card bitmap for the given [verdict].
     */
    operator fun invoke(verdict: LinkVerdict): Bitmap {
        return RescueCardGenerator.generate(verdict)
    }
}

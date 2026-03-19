/**
 * Use case that generates a shareable rescue card bitmap from a link verdict.
 * Placeholder for E07-005 — currently delegates to VerdictCardGenerator.
 */
package com.safeanot.app.domain.usecase

import android.graphics.Bitmap
import com.safeanot.app.domain.model.LinkVerdict
import com.safeanot.app.util.VerdictCardGenerator
import javax.inject.Inject

class GenerateRescueCardUseCase @Inject constructor() {

    /**
     * Generates a rescue card bitmap for the given [verdict].
     * TODO: E07-005 will add a dedicated rescue card design.
     */
    operator fun invoke(verdict: LinkVerdict): Bitmap {
        return VerdictCardGenerator.generate(verdict)
    }
}

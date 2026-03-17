/**
 * Use case for checking whether a link/domain is safe or potentially a scam.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.LinkVerdict
import com.safeanot.app.domain.model.VerdictType
import com.safeanot.app.domain.repository.LinkCheckRepository
import javax.inject.Inject

class CheckLinkUseCase @Inject constructor(
    private val linkCheckRepository: LinkCheckRepository,
) {

    suspend operator fun invoke(input: String): LinkVerdict {
        if (input.isBlank()) {
            return LinkVerdict(
                domain = "",
                verdict = VerdictType.UNKNOWN,
                reason = "Empty input",
                confidence = 0f,
            )
        }

        return linkCheckRepository.checkLink(input)
    }
}

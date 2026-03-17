/**
 * Repository interface for checking link/domain safety.
 */
package com.safeanot.app.domain.repository

import com.safeanot.app.domain.model.LinkVerdict

interface LinkCheckRepository {
    suspend fun checkLink(input: String): LinkVerdict
}

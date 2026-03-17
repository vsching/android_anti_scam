/**
 * Domain model representing the result of a link safety check.
 */
package com.safeanot.app.domain.model

data class LinkVerdict(
    val domain: String,
    val verdict: VerdictType,
    val reason: String,
    val confidence: Float,
)

enum class VerdictType {
    SAFE,
    SUSPICIOUS,
    DANGEROUS,
    UNKNOWN,
    NOT_IN_DATABASE,
}

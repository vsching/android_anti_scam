/**
 * Represents the security audit status of a tracked app.
 * NOT_QUERYABLE detection state is mapped to NEEDS_REVIEW with an explicit explanation
 * that the app's install status could not be verified.
 */
package com.safeanot.app.domain.model

enum class AuditStatus {
    /** App is installed and user has not yet addressed the risk. */
    NEEDS_REVIEW,

    /** User has confirmed they disabled install permission for this app. */
    SECURED,

    /** App is not present on the device. */
    NOT_INSTALLED,

    /** User chose to skip remediation for now. */
    SKIPPED,
}

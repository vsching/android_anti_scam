/**
 * Represents the security audit status of a tracked app.
 */
package com.safeanot.app.domain.model

enum class AuditStatus {
    /** User has confirmed they disabled install permission for this app. */
    NEEDS_REVIEW,

    /** App is installed and user has not yet addressed the risk. */
    SECURED,

    /** App is not present on the device. */
    NOT_INSTALLED,

    /** User chose to skip remediation for now. */
    SKIPPED,
}

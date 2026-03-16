/**
 * Categories for grouping tracked apps in the audit dashboard.
 */
package com.safeanot.app.domain.model

enum class AppCategory(val displayName: String) {
    MESSAGING("Messaging"),
    BROWSERS("Browsers"),
    FILES("File Managers"),
    PROTECTION("Protection"),
}

/**
 * UI filter model for alert region selection.
 */
package com.safeanot.app.domain.model

enum class AlertRegionFilter(val label: String) {
    ALL("All"),
    MALAYSIA("Malaysia"),
    SINGAPORE("Singapore");

    /**
     * Maps the UI filter to the API query parameter value, or null for ALL.
     */
    fun toApiRegion(): String? = when (this) {
        ALL -> null
        MALAYSIA -> "MY"
        SINGAPORE -> "SG"
    }

    /**
     * Maps the UI filter to the DAO region string, or null for ALL.
     */
    fun toDaoRegion(): String? = toApiRegion()
}

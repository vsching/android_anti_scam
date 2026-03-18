/**
 * Shared utility for resolving the default alert region from the device locale.
 * Extracted to avoid duplicating locale-detection logic across repositories.
 */
package com.safeanot.app.util

import com.safeanot.app.domain.model.AlertRegionFilter
import java.util.Locale

object RegionResolver {

    /**
     * Maps the current device locale to an [AlertRegionFilter].
     * Returns MALAYSIA for "MY", SINGAPORE for "SG", or ALL for unknown locales.
     */
    fun fromLocale(locale: Locale = Locale.getDefault()): AlertRegionFilter {
        return when (locale.country.uppercase()) {
            "MY" -> AlertRegionFilter.MALAYSIA
            "SG" -> AlertRegionFilter.SINGAPORE
            else -> AlertRegionFilter.ALL
        }
    }
}

/**
 * Domain model for emergency contacts (scam reporting hotlines and websites).
 * Contacts are region-specific and used in the Profile screen's help section.
 */
package com.safeanot.app.domain.model

import com.safeanot.app.util.Constants

data class EmergencyContact(
    val name: String,
    val description: String,
    val phoneNumber: String?,
    val websiteUrl: String?,
)

object EmergencyContacts {

    private val malaysiaContacts = listOf(
        EmergencyContact(
            name = "MCMC",
            description = "Malaysian Communications and Multimedia Commission - Report scam SMSes, calls, and online fraud.",
            phoneNumber = null,
            websiteUrl = Constants.MCMC_URL,
        ),
        EmergencyContact(
            name = "PDRM Scam Response Centre",
            description = "Royal Malaysia Police commercial crime hotline for reporting scams.",
            phoneNumber = Constants.NSRC_HOTLINE,
            websiteUrl = null,
        ),
        EmergencyContact(
            name = "NSRC 997",
            description = "National Scam Response Centre - Call to report and block scam transactions.",
            phoneNumber = Constants.NSRC_HOTLINE,
            websiteUrl = null,
        ),
    )

    private val singaporeContacts = listOf(
        EmergencyContact(
            name = "ScamShield",
            description = "Singapore government app and website to check and report scams.",
            phoneNumber = null,
            websiteUrl = Constants.SCAMSHIELD_URL,
        ),
        EmergencyContact(
            name = "SPF Anti-Scam Centre",
            description = "Singapore Police Force Anti-Scam Centre for reporting scam incidents.",
            phoneNumber = Constants.SPF_HOTLINE,
            websiteUrl = null,
        ),
        EmergencyContact(
            name = "1800-722-6688",
            description = "Anti-Scam Helpline - Call for assistance with scam-related issues.",
            phoneNumber = Constants.SPF_HOTLINE,
            websiteUrl = null,
        ),
    )

    /**
     * Returns the list of emergency contacts for the given region.
     * Returns an empty list for [AlertRegionFilter.ALL] since no specific region is selected.
     */
    fun forRegion(region: AlertRegionFilter): List<EmergencyContact> = when (region) {
        AlertRegionFilter.MALAYSIA -> malaysiaContacts
        AlertRegionFilter.SINGAPORE -> singaporeContacts
        AlertRegionFilter.ALL -> emptyList()
    }
}

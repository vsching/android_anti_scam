package com.safeanot.app.domain.model

import com.safeanot.app.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyContactTest {

    @Test
    fun `forRegion MALAYSIA returns 3 contacts`() {
        val contacts = EmergencyContacts.forRegion(AlertRegionFilter.MALAYSIA)
        assertEquals(3, contacts.size)
    }

    @Test
    fun `forRegion MALAYSIA contains MCMC with correct URL`() {
        val contacts = EmergencyContacts.forRegion(AlertRegionFilter.MALAYSIA)
        val mcmc = contacts.first { it.name == "MCMC" }
        assertEquals(Constants.MCMC_URL, mcmc.websiteUrl)
    }

    @Test
    fun `forRegion MALAYSIA contains PDRM with correct phone`() {
        val contacts = EmergencyContacts.forRegion(AlertRegionFilter.MALAYSIA)
        val pdrm = contacts.first { it.name == "PDRM Scam Response Centre" }
        assertEquals(Constants.NSRC_HOTLINE, pdrm.phoneNumber)
    }

    @Test
    fun `forRegion MALAYSIA contains NSRC 997 with correct phone`() {
        val contacts = EmergencyContacts.forRegion(AlertRegionFilter.MALAYSIA)
        val nsrc = contacts.first { it.name == "NSRC 997" }
        assertEquals(Constants.NSRC_HOTLINE, nsrc.phoneNumber)
    }

    @Test
    fun `forRegion SINGAPORE returns 3 contacts`() {
        val contacts = EmergencyContacts.forRegion(AlertRegionFilter.SINGAPORE)
        assertEquals(3, contacts.size)
    }

    @Test
    fun `forRegion SINGAPORE contains ScamShield with correct URL`() {
        val contacts = EmergencyContacts.forRegion(AlertRegionFilter.SINGAPORE)
        val scamShield = contacts.first { it.name == "ScamShield" }
        assertEquals(Constants.SCAMSHIELD_URL, scamShield.websiteUrl)
    }

    @Test
    fun `forRegion SINGAPORE contains SPF with correct phone`() {
        val contacts = EmergencyContacts.forRegion(AlertRegionFilter.SINGAPORE)
        val spf = contacts.first { it.name == "SPF Anti-Scam Centre" }
        assertEquals(Constants.SPF_HOTLINE, spf.phoneNumber)
    }

    @Test
    fun `forRegion SINGAPORE contains helpline with correct phone`() {
        val contacts = EmergencyContacts.forRegion(AlertRegionFilter.SINGAPORE)
        val helpline = contacts.first { it.name == "1800-722-6688" }
        assertEquals(Constants.SPF_HOTLINE, helpline.phoneNumber)
    }

    @Test
    fun `forRegion ALL returns empty list`() {
        val contacts = EmergencyContacts.forRegion(AlertRegionFilter.ALL)
        assertTrue(contacts.isEmpty())
    }

    @Test
    fun `all phone numbers are valid dialable formats`() {
        val allContacts = EmergencyContacts.forRegion(AlertRegionFilter.MALAYSIA) +
            EmergencyContacts.forRegion(AlertRegionFilter.SINGAPORE)

        allContacts.filter { it.phoneNumber != null }.forEach { contact ->
            val phone = contact.phoneNumber!!
            assertTrue(
                "Phone number '${phone}' for ${contact.name} should contain only digits",
                phone.all { it.isDigit() },
            )
            assertTrue(
                "Phone number '${phone}' for ${contact.name} should not be empty",
                phone.isNotEmpty(),
            )
        }
    }

    @Test
    fun `all URLs are valid`() {
        val allContacts = EmergencyContacts.forRegion(AlertRegionFilter.MALAYSIA) +
            EmergencyContacts.forRegion(AlertRegionFilter.SINGAPORE)

        allContacts.filter { it.websiteUrl != null }.forEach { contact ->
            val url = contact.websiteUrl!!
            assertTrue(
                "URL '${url}' for ${contact.name} should start with https://",
                url.startsWith("https://"),
            )
        }
    }

    @Test
    fun `every contact has either phone or website`() {
        val allContacts = EmergencyContacts.forRegion(AlertRegionFilter.MALAYSIA) +
            EmergencyContacts.forRegion(AlertRegionFilter.SINGAPORE)

        allContacts.forEach { contact ->
            assertTrue(
                "Contact '${contact.name}' should have phone or website",
                contact.phoneNumber != null || contact.websiteUrl != null,
            )
        }
    }
}

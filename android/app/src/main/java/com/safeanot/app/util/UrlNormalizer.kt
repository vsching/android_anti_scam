/**
 * Utility for normalizing URLs to bare domain form for consistent lookups.
 *
 * Strips protocol, path, query, fragment, and "www." prefix.
 * Handles Punycode/IDN via java.net.IDN.toASCII().
 * Returns null for invalid input (IPs, localhost, malformed domains).
 */
package com.safeanot.app.util

import java.net.IDN

object UrlNormalizer {

    private val TWO_PART_TLDS = setOf(
        "com.my", "com.sg", "co.uk", "org.my", "net.my", "edu.my", "gov.my",
        "com.au", "co.nz", "co.id", "co.th", "com.ph", "com.vn", "com.tw",
        "com.hk", "co.jp", "co.kr",
    )

    private val IP_V4_PATTERN = Regex(
        """^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$"""
    )

    private val VALID_LABEL_PATTERN = Regex(
        """^[a-z0-9]([a-z0-9-]*[a-z0-9])?$"""
    )

    /**
     * Normalizes the given input (URL or domain) to a bare domain string.
     * Returns null if the input is invalid.
     */
    fun normalize(input: String?): String? {
        if (input.isNullOrBlank()) return null

        var domain = input.trim().lowercase()

        // Strip protocol
        val protocolIndex = domain.indexOf("://")
        if (protocolIndex != -1) {
            domain = domain.substring(protocolIndex + 3)
        }

        // Strip path, query, fragment
        domain = domain.split("/", "?", "#").first()

        // Strip userinfo (user:pass@host) BEFORE port stripping
        val atIndex = domain.indexOf("@")
        if (atIndex != -1) {
            domain = domain.substring(atIndex + 1)
        }

        // Strip port (after userinfo so we don't confuse user:pass with host:port)
        val portIndex = domain.lastIndexOf(":")
        if (portIndex != -1) {
            domain = domain.substring(0, portIndex)
        }

        // Strip "www." prefix
        if (domain.startsWith("www.")) {
            domain = domain.removePrefix("www.")
        }

        // Strip trailing dot (FQDN)
        domain = domain.trimEnd('.')

        if (domain.isEmpty()) return null

        // Reject localhost
        if (domain == "localhost") return null

        // Reject IP addresses
        if (IP_V4_PATTERN.matches(domain)) return null
        if (domain.startsWith("[") || domain.contains("::")) return null

        // Reject consecutive dots
        if (domain.contains("..")) return null

        // Handle IDN / Punycode
        domain = try {
            IDN.toASCII(domain, IDN.ALLOW_UNASSIGNED)
        } catch (_: IllegalArgumentException) {
            return null
        }

        // Validate each label
        val labels = domain.split(".")
        if (labels.size < 2) return null

        for (label in labels) {
            if (label.isEmpty()) return null
            if (label.length > 63) return null
            if (!VALID_LABEL_PATTERN.matches(label)) return null
        }

        return domain
    }

    /**
     * Extracts the registrable domain (eTLD+1) from a normalized domain.
     * Handles two-part TLDs like com.my, co.uk, etc.
     */
    fun extractRegistrableDomain(domain: String): String {
        val labels = domain.split(".")
        if (labels.size <= 2) return domain

        val lastTwo = "${labels[labels.size - 2]}.${labels.last()}"
        return if (TWO_PART_TLDS.contains(lastTwo) && labels.size >= 3) {
            "${labels[labels.size - 3]}.$lastTwo"
        } else {
            "$lastTwo"
        }
    }
}

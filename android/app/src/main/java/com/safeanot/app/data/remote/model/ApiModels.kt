/**
 * Data classes for Safe Anot? API request and response models.
 */
package com.safeanot.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class LatestMetadataResponse(
    @SerializedName("version") val version: String,
    @SerializedName("full_key") val fullKey: String,
    @SerializedName("delta_key") val deltaKey: String,
    @SerializedName("bloom_key") val bloomKey: String,
    @SerializedName("full_size_kb") val fullSizeKb: Int,
    @SerializedName("delta_size_kb") val deltaSizeKb: Int,
    @SerializedName("bloom_size_kb") val bloomSizeKb: Int,
    @SerializedName("sqlite_size_kb") val sqliteSizeKb: Int,
    @SerializedName("domain_count") val domainCount: Int,
    @SerializedName("build_timestamp") val buildTimestamp: String,
)

/**
 * Request body for the domain check endpoint.
 * Always send a normalized domain, never a full URL.
 */
data class CheckRequest(
    @SerializedName("domain") val domain: String,
)

data class CheckResponse(
    @SerializedName("domain") val domain: String,
    @SerializedName("verdict") val verdict: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("confidence") val confidence: Float,
    @SerializedName("details") val details: Map<String, String>? = null,
)

data class AlertDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("scam_type") val scamType: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("region") val region: String,
    @SerializedName("report_count") val reportCount: Int,
    @SerializedName("created_at") val createdAt: String,
)

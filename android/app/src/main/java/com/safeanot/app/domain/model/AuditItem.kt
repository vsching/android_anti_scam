/**
 * Domain model representing a single app in the security audit checklist.
 */
package com.safeanot.app.domain.model

import com.safeanot.app.util.DetectionState

data class AuditItem(
    val id: Int = 0,
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val status: AuditStatus,
    val riskDescription: String = "",
    val detectionState: DetectionState = DetectionState.NOT_INSTALLED,
    val lastChecked: Long = System.currentTimeMillis(),
)

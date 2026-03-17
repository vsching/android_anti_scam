/**
 * Formats scam alert data into shareable text with deep link.
 */
package com.safeanot.app.feature.alerts

import com.safeanot.app.domain.model.ScamAlert

object AlertShareFormatter {

    fun format(alert: ScamAlert): String {
        return buildString {
            appendLine("SCAM ALERT: ${alert.title}")
            appendLine()
            appendLine("Severity: ${alert.severity.name}")
            appendLine("Region: ${alert.region.name}")
            appendLine("Reports: ${alert.reportCount}")
            appendLine()
            appendLine(alert.description.take(200))
            if (alert.description.length > 200) append("...")
            appendLine()
            appendLine()
            appendLine("Stay safe! Check this alert on Safe Anot?:")
            append("https://safeanot.com/alert/${android.net.Uri.encode(alert.id)}")
        }
    }
}

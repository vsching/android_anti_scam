/**
 * Bottom sheet dialog for selecting a warning message template.
 * Shows 3 templates (polite, urgent, elder-friendly) as selectable cards
 * with a language toggle between English and Bahasa Malaysia.
 * Only shown for DANGEROUS verdicts.
 */
package com.safeanot.app.feature.check.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safeanot.app.domain.model.WarningTemplate
import com.safeanot.app.domain.model.WarningTone
import com.safeanot.app.ui.theme.DarkCard
import com.safeanot.app.ui.theme.TextPrimary
import com.safeanot.app.ui.theme.TextSecondary
import com.safeanot.app.util.WarningTemplateProvider

private fun toneIcon(tone: WarningTone): ImageVector = when (tone) {
    WarningTone.POLITE -> Icons.Default.Favorite // handshake-like
    WarningTone.URGENT -> Icons.Default.Warning
    WarningTone.ELDER_FRIENDLY -> Icons.Default.Favorite
}

private fun toneLabel(tone: WarningTone): String = when (tone) {
    WarningTone.POLITE -> "Polite"
    WarningTone.URGENT -> "Urgent"
    WarningTone.ELDER_FRIENDLY -> "Elder-Friendly"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarningTemplatePicker(
    domain: String,
    verdict: String,
    onTemplateSelected: (template: WarningTemplate, localeTag: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedLocale by remember { mutableStateOf("en") }
    val templates = remember { WarningTemplateProvider.getTemplates() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkCard,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = "Warn My Contacts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Choose a message tone to share",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Language toggle
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedLocale == "en",
                    onClick = { selectedLocale = "en" },
                    label = { Text("English") },
                )
                FilterChip(
                    selected = selectedLocale == "ms",
                    onClick = { selectedLocale = "ms" },
                    label = { Text("Bahasa Malaysia") },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Template cards
            templates.forEach { template ->
                WarningTemplateCard(
                    template = template,
                    domain = domain,
                    verdict = verdict,
                    locale = selectedLocale,
                    onClick = { onTemplateSelected(template, selectedLocale) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun WarningTemplateCard(
    template: WarningTemplate,
    domain: String,
    verdict: String,
    locale: String,
    onClick: () -> Unit,
) {
    val previewText = WarningTemplateProvider.format(template, domain, verdict, locale)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = toneIcon(template.tone),
                contentDescription = toneLabel(template.tone),
                modifier = Modifier.size(24.dp),
                tint = TextSecondary,
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = toneLabel(template.tone),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Card composable displaying an emergency contact with action buttons for calling and web links.
 */
package com.safeanot.app.feature.profile.components

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.safeanot.app.domain.model.EmergencyContact
import com.safeanot.app.ui.theme.BlueAccent
import com.safeanot.app.ui.theme.DarkCard
import com.safeanot.app.ui.theme.TextSecondary

@Composable
fun EmergencyContactCard(
    contact: EmergencyContact,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BlueAccent,
                )
                Text(
                    text = contact.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }

            if (contact.phoneNumber != null) {
                IconButton(onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = "tel:${contact.phoneNumber}".toUri()
                        }
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        // No dialer available on this device
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call ${contact.name}",
                        tint = BlueAccent,
                    )
                }
            }

            if (contact.websiteUrl != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = contact.websiteUrl.toUri()
                        }
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        // No browser available on this device
                    }
                }) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_compass),
                        contentDescription = "Open ${contact.name} website",
                        tint = BlueAccent,
                    )
                }
            }
        }
    }
}

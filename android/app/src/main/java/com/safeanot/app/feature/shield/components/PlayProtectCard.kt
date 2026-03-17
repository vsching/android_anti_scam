/**
 * Card component advising users to enable Google Play Protect and its enhanced detection setting.
 * Provides a CTA button that deep-links to Play Protect settings with fallback instructions.
 */
package com.safeanot.app.feature.shield.components

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safeanot.app.ui.theme.BlueAccent
import com.safeanot.app.ui.theme.DarkCard
import com.safeanot.app.ui.theme.TextPrimary
import com.safeanot.app.ui.theme.TextSecondary

@Composable
fun PlayProtectCard(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showFallback by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Google Play Protect",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Play Protect scans your device for harmful apps. " +
                    "Enable \"Improve harmful app detection\" for the best protection against sideloaded malware.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    try {
                        val intent = Intent("com.google.android.gms.security.VERIFY_APPS_SETTINGS")
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        showFallback = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = "Check Play Protect",
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (showFallback) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Could not open Play Protect settings. " +
                        "Open Google Play Store > tap your profile icon > Play Protect > Settings > " +
                        "turn on \"Improve harmful app detection\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

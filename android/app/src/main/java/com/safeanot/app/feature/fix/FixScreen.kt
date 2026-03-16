/**
 * Guided Fix flow screen with risk explanation, numbered steps, settings deep-link button,
 * and confirm/skip buttons for user to mark remediation status.
 */
package com.safeanot.app.feature.fix

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safeanot.app.ui.theme.BlueAccent
import com.safeanot.app.ui.theme.DarkBackground
import com.safeanot.app.ui.theme.DarkCard
import com.safeanot.app.ui.theme.GreenAccent
import com.safeanot.app.ui.theme.OrangeAccent
import com.safeanot.app.ui.theme.RedAccent
import com.safeanot.app.ui.theme.TextPrimary
import com.safeanot.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixScreen(
    onNavigateBack: () -> Unit,
    viewModel: FixViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Navigate back when completed or skipped
    LaunchedEffect(uiState.isCompleted, uiState.isSkipped) {
        if (uiState.isCompleted || uiState.isSkipped) {
            onNavigateBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
    ) {
        TopAppBar(
            title = { Text("Fix: ${uiState.appName}") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkBackground,
                titleContentColor = TextPrimary,
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App icon placeholder
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(RedAccent.copy(alpha = 0.15f)),
            ) {
                Text(
                    text = uiState.appName.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = RedAccent,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Risk explanation
            Text(
                text = "${uiState.appName} can install APK files on your device.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Scammers exploit this to send malware disguised as useful apps. " +
                    "Disabling \"Install unknown apps\" for ${uiState.appName} prevents this attack vector.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Steps
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Steps:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                StepItem(number = 1, text = "Tap \"Open Settings\" below")
                StepItem(number = 2, text = "Find \"Install unknown apps\"")
                StepItem(number = 3, text = "Toggle it OFF")
                StepItem(number = 4, text = "Come back and tap \"I've Done It\"")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Open Settings button
            Button(
                onClick = {
                    viewModel.onSettingsOpened()
                    try {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:${uiState.packageName}")
                        )
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        val fallback = Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)
                        context.startActivity(fallback)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = "Open Settings",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm / Skip buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { viewModel.markAsSkipped() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = OrangeAccent,
                    ),
                ) {
                    Text("Skip for Now")
                }

                Button(
                    onClick = { viewModel.markAsSecured() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text(
                        text = "I've Done It",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StepItem(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(BlueAccent.copy(alpha = 0.15f)),
        ) {
            Text(
                text = "$number",
                style = MaterialTheme.typography.labelMedium,
                color = BlueAccent,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
        )
    }
}

/**
 * Link Checker screen with URL input, CHECK button, loading indicator,
 * verdict card display, and error handling with retry.
 */
package com.safeanot.app.feature.check

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safeanot.app.domain.model.VerdictType
import com.safeanot.app.feature.check.components.VerdictCard
import com.safeanot.app.feature.check.components.WarningTemplatePicker
import com.safeanot.app.domain.model.SharePlatform
import com.safeanot.app.domain.model.ShareType
import com.safeanot.app.ui.theme.BlueAccent
import com.safeanot.app.ui.theme.DarkBorder
import com.safeanot.app.ui.theme.DarkCard
import com.safeanot.app.ui.theme.RedAccent
import com.safeanot.app.ui.theme.TextPrimary
import com.safeanot.app.ui.theme.TextSecondary
import com.safeanot.app.util.ShareImageCache
import com.safeanot.app.util.ShareIntentFactory
import com.safeanot.app.util.WhatsAppUtils

@Composable
fun CheckScreen(
    viewModel: CheckViewModel = hiltViewModel(),
) {
    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val checkState by viewModel.checkState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showWarningPicker by remember { mutableStateOf(false) }

    // Collect share events from the ViewModel and handle intent creation in UI
    LaunchedEffect(Unit) {
        viewModel.shareEvents.collect { event ->
            // Use stable domain from result state, not mutable urlInput
            val stableDomain = (checkState as? CheckState.Result)?.verdict?.domain ?: urlInput
            try {
                when (event) {
                    is ShareEvent.ImageWithText -> {
                        val uri = ShareImageCache.saveBitmap(context, event.bitmap)
                        val intent = ShareIntentFactory.createImageShare(uri, event.text)
                        context.startActivity(intent)
                        val isRescueCard = event.text.contains("play.google.com")
                        viewModel.onShareCompleted(
                            if (isRescueCard) ShareType.RESCUE_CARD else ShareType.VERDICT,
                            stableDomain,
                            SharePlatform.GENERIC,
                        )
                    }
                    is ShareEvent.BitmapOnly -> {
                        val uri = ShareImageCache.saveBitmap(context, event.bitmap)
                        val intent = ShareIntentFactory.createImageShare(uri)
                        context.startActivity(intent)
                        viewModel.onShareCompleted(
                            ShareType.VERDICT,
                            stableDomain,
                            SharePlatform.GENERIC,
                        )
                    }
                    is ShareEvent.TextOnly -> {
                        val intent = ShareIntentFactory.createTextShare(event.text)
                        context.startActivity(intent)
                        viewModel.onShareCompleted(
                            ShareType.VERDICT,
                            stableDomain,
                            SharePlatform.GENERIC,
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("CheckScreen", "Failed to share", e)
            }
        }
    }

    // Collect warning share events — plain text for WhatsApp or generic share
    LaunchedEffect(Unit) {
        viewModel.warningShareEvent.collect { text ->
            // Use stable domain from result state, not mutable urlInput
            val warningDomain = (checkState as? CheckState.Result)?.verdict?.domain ?: urlInput
            try {
                val isWhatsApp = WhatsAppUtils.isWhatsAppInstalled(context)
                val intent = if (isWhatsApp) {
                    ShareIntentFactory.createWhatsAppTextShare(text)
                } else {
                    ShareIntentFactory.createTextShare(text)
                }
                context.startActivity(intent)
                viewModel.onShareCompleted(
                    ShareType.WARNING_TEMPLATE,
                    warningDomain,
                    if (isWhatsApp) SharePlatform.WHATSAPP else SharePlatform.GENERIC,
                )
            } catch (e: Exception) {
                Log.e("CheckScreen", "Failed to share warning", e)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Link Checker",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Paste a suspicious link to check if it's safe.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = urlInput,
            onValueChange = { viewModel.onUrlChanged(it) },
            label = { Text("Paste URL here") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BlueAccent,
                unfocusedBorderColor = DarkBorder,
                focusedLabelColor = BlueAccent,
                unfocusedLabelColor = TextSecondary,
                cursorColor = BlueAccent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = DarkCard,
                unfocusedContainerColor = DarkCard,
            ),
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.checkLink() },
            modifier = Modifier.fillMaxWidth(),
            enabled = urlInput.isNotBlank() && checkState !is CheckUiState.Loading,
            colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = "CHECK NOW",
                modifier = Modifier.padding(vertical = 4.dp),
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (val state = checkState) {
            is CheckUiState.Idle -> {
                // Nothing to show
            }

            is CheckUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = BlueAccent)
                }
            }

            is CheckUiState.Result -> {
                Text(
                    text = "Result",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(modifier = Modifier.height(12.dp))

                VerdictCard(
                    verdict = state.verdict,
                    onShareClick = { viewModel.shareResult() },
                    onCheckAnotherClick = { viewModel.clearResults() },
                    onWarnContactsClick = if (state.verdict.verdict == VerdictType.DANGEROUS) {
                        { showWarningPicker = true }
                    } else {
                        null
                    },
                    onProtectFamilyClick = if (state.verdict.verdict == VerdictType.DANGEROUS) {
                        { viewModel.shareRescueCard() }
                    } else {
                        null
                    },
                )

                // Warning template picker bottom sheet
                if (showWarningPicker && state.verdict.verdict == VerdictType.DANGEROUS) {
                    WarningTemplatePicker(
                        domain = state.verdict.domain,
                        verdict = state.verdict.verdict.name,
                        onTemplateSelected = { template, localeTag ->
                            showWarningPicker = false
                            viewModel.shareWarning(template, localeTag)
                        },
                        onDismiss = { showWarningPicker = false },
                    )
                }
            }

            is CheckUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = RedAccent,
                    )

                    OutlinedButton(
                        onClick = { viewModel.checkLink() },
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Retry")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

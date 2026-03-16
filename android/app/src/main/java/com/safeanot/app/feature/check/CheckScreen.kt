/**
 * Link Checker screen with URL input field, CHECK NOW button, and recent verdict cards.
 * Uses local regex pattern matching in v1 (no network requests).
 */
package com.safeanot.app.feature.check

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safeanot.app.feature.check.components.VerdictCard
import com.safeanot.app.ui.theme.BlueAccent
import com.safeanot.app.ui.theme.DarkBorder
import com.safeanot.app.ui.theme.DarkCard
import com.safeanot.app.ui.theme.TextPrimary
import com.safeanot.app.ui.theme.TextSecondary

@Composable
fun CheckScreen(
    viewModel: CheckViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
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
                    value = uiState.urlInput,
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
                    onClick = { viewModel.checkUrl() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.urlInput.isNotBlank() && !uiState.isChecking,
                    colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = if (uiState.isChecking) "Checking..." else "CHECK NOW",
                        modifier = Modifier.padding(vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // Current verdict
        uiState.currentVerdict?.let { verdict ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Result",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            item {
                VerdictCard(
                    result = verdict,
                    onShareClick = { /* TODO: Share intent */ },
                )
            }
        }

        // Recent results
        if (uiState.recentResults.size > 1) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recent Checks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(uiState.recentResults.drop(1)) { result ->
                VerdictCard(
                    result = result,
                    onShareClick = { /* TODO: Share intent */ },
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

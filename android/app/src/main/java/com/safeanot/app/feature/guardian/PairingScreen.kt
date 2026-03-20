/**
 * Guardian Pairing screen with two tabs: Protect (ward) and Monitor (guardian).
 * Allows generating/claiming pairing codes and viewing existing pairings.
 */
package com.safeanot.app.feature.guardian

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safeanot.app.domain.model.GuardianPairing
import com.safeanot.app.domain.model.GuardianRole
import com.safeanot.app.ui.theme.BlueAccent
import com.safeanot.app.ui.theme.DarkCard
import com.safeanot.app.ui.theme.GreenAccent
import com.safeanot.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    onNavigateBack: () -> Unit,
    viewModel: GuardianPairingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guardian Mode") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TabRow(selectedTabIndex = uiState.selectedTab) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("Protect Me") },
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Monitor Others") },
                )
            }

            when (uiState.selectedTab) {
                0 -> ProtectTab(uiState = uiState, viewModel = viewModel)
                1 -> MonitorTab(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun ProtectTab(
    uiState: GuardianUiState,
    viewModel: GuardianPairingViewModel,
) {
    var label by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Let someone protect you",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Generate a code and share it with your guardian. They will be able to monitor your security status.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        item {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Your device name") },
                placeholder = { Text("e.g., Mom's Phone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        item {
            Button(
                onClick = { viewModel.generateCode("WARD", label.ifBlank { "My Phone" }) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp).width(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Generate Pairing Code", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Show generated code
        if (uiState.pairingCode != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GreenAccent.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Your Pairing Code",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.pairingCode.code,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Share this code with your guardian",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        // Error message
        if (uiState.error != null) {
            item {
                Text(
                    text = uiState.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // Show existing guardians (people protecting me)
        val myGuardians = uiState.pairings.filter { it.role == GuardianRole.WARD }
        if (myGuardians.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "My Guardians",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(myGuardians, key = { it.id }) { pairing ->
                PairingCard(pairing = pairing, onDelete = { viewModel.deletePairing(pairing.id) })
            }
        }
    }
}

@Composable
private fun MonitorTab(
    uiState: GuardianUiState,
    viewModel: GuardianPairingViewModel,
) {
    var claimCode by rememberSaveable { mutableStateOf("") }
    var label by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Monitor someone you care about",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enter the pairing code from your ward's device to start monitoring their security status.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        item {
            OutlinedTextField(
                value = claimCode,
                onValueChange = { claimCode = it },
                label = { Text("Pairing code") },
                placeholder = { Text("Enter code from ward's device") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        item {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Your device name") },
                placeholder = { Text("e.g., Dad's Phone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        item {
            Button(
                onClick = {
                    viewModel.claimCode(claimCode, label.ifBlank { "My Phone" })
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && claimCode.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp).width(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Claim Code", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Success message
        if (uiState.claimSuccess) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GreenAccent.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = "Pairing successful! You can now monitor this device.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        // Error message
        if (uiState.error != null) {
            item {
                Text(
                    text = uiState.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // Show existing wards (people I'm monitoring)
        val myWards = uiState.pairings.filter { it.role == GuardianRole.GUARDIAN }
        if (myWards.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "My Wards",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(myWards, key = { it.id }) { pairing ->
                PairingCard(pairing = pairing, onDelete = { viewModel.deletePairing(pairing.id) })
            }
        }
    }
}

@Composable
private fun PairingCard(
    pairing: GuardianPairing,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pairing.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (pairing.role == GuardianRole.GUARDIAN) "Ward" else "Guardian",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            OutlinedButton(
                onClick = onDelete,
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove pairing",
                )
            }
        }
    }
}

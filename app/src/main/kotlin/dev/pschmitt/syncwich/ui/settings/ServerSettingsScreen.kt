package dev.pschmitt.syncwich.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pschmitt.syncwich.data.settings.MealieAuthMethod
import dev.pschmitt.syncwich.data.settings.MealieCredentials

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSettingsScreen(
    onBack: () -> Unit,
    onChangeConnection: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel,
) {
    val credentials by viewModel.credentials.collectAsStateWithLifecycle()
    val isSigningOut by viewModel.isSigningOut.collectAsStateWithLifecycle()
    val credentialsTestState by viewModel.credentialsTestState.collectAsStateWithLifecycle()
    var showSignOutConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Server") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SettingsGroupCard(title = "Connection", icon = Icons.Filled.Dns) {
                    SettingsListItem(
                        headlineContent = { Text("Connection details") },
                        supportingContent = {
                            Text(
                                "Stored securely on this device. Cached recipes remain available offline."
                            )
                        },
                    )
                    ConnectionDetailRow(credentials)
                    SettingsActionRow(
                        title = "Change connection",
                        subtitle = "Change the server URL or replace the saved credentials",
                        onClick = onChangeConnection,
                    )
                    SettingsActionRow(
                        title =
                            if (credentialsTestState is CredentialsTestState.Testing) {
                                "Testing credentials…"
                            } else {
                                "Test credentials"
                            },
                        subtitle = credentialsTestSubtitle(credentialsTestState),
                        onClick = viewModel::testCredentials,
                        enabled = credentialsTestState !is CredentialsTestState.Testing,
                        icon = { Icon(Icons.Filled.PersonSearch, contentDescription = null) },
                    )
                    SettingsActionRow(
                        title = if (isSigningOut) "Signing out…" else "Sign out",
                        subtitle = "Remove this connection and clear its offline cache",
                        onClick = { showSignOutConfirmation = true },
                        enabled = !isSigningOut,
                        icon = {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        },
                    )
                }
            }
        }
    }

    if (showSignOutConfirmation) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmation = false },
            title = { Text("Sign out?") },
            text = { Text("Your saved connection and offline recipe cache will be removed.") },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirmation = false }) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirmation = false
                        viewModel.signOut(onSignedOut)
                    }
                ) {
                    Text("Sign out")
                }
            },
        )
    }
}

internal fun credentialsTestSubtitle(state: CredentialsTestState): String =
    when (state) {
        CredentialsTestState.Idle -> "Verify the saved credentials and show the current user"
        CredentialsTestState.Testing -> "Contacting the Mealie server…"
        is CredentialsTestState.Success -> "Signed in as ${state.userDisplayName}"
        is CredentialsTestState.Error -> state.message
    }

@Composable
private fun ConnectionDetailRow(credentials: MealieCredentials) {
    SettingsListItem(
        headlineContent = { Text("Server URL") },
        supportingContent = { Text(credentials.serverUrl.ifBlank { "Not configured" }) },
    )
    SettingsListItem(
        headlineContent = {
            Text(
                if (credentials.authMethod == MealieAuthMethod.Oidc) "OIDC session" else "API token"
            )
        },
        supportingContent = { Text(maskApiToken(credentials.apiToken)) },
    )
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: @Composable () -> Unit = {},
) {
    SettingsListItem(
        modifier =
            Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick).semantics {
                contentDescription = "$title: $subtitle"
                role = Role.Button
            },
        leadingContent = icon,
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        },
    )
}

internal fun maskApiToken(token: String): String =
    when {
        token.isBlank() -> "Not configured"
        token.length <= 4 -> "••••"
        else -> "••••" + token.takeLast(4)
    }

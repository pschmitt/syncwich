package dev.pschmitt.syncwich.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Edits the connection without changing the first-run onboarding surface. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionSettingsScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel,
) {
    val credentials by viewModel.credentials.collectAsStateWithLifecycle()
    val updateState by viewModel.connectionUpdateState.collectAsStateWithLifecycle()
    var serverUrl by rememberSaveable { mutableStateOf("") }
    var apiToken by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var passwordMode by rememberSaveable { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var tokenVisible by rememberSaveable { mutableStateOf(false) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isValidating = updateState is ConnectionUpdateState.Validating

    LaunchedEffect(credentials.serverUrl) {
        if (serverUrl.isBlank()) serverUrl = credentials.serverUrl
    }
    LaunchedEffect(updateState) {
        if (updateState is ConnectionUpdateState.Saved) onSaved()
    }

    fun submit() {
        keyboardController?.hide()
        if (passwordMode) {
            viewModel.updateConnectionWithPassword(serverUrl, username, password)
        } else {
            viewModel.updateConnection(serverUrl, apiToken)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Change connection") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "The new connection is checked before it replaces the saved one. The current " +
                    "cache remains available if validation fails.",
            )
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("Server URL") },
                placeholder = { Text("https://demo.mealie.io") },
                singleLine = true,
                enabled = !isValidating,
                keyboardOptions =
                    KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Connection method")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = !passwordMode,
                        onClick = { passwordMode = false },
                        label = { Text("API token") },
                        enabled = !isValidating,
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = passwordMode,
                        onClick = { passwordMode = true },
                        label = { Text("Username & password") },
                        enabled = !isValidating,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (passwordMode) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username or email") },
                    singleLine = true,
                    enabled = !isValidating,
                    keyboardOptions =
                        KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = !isValidating,
                    visualTransformation =
                        if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    keyboardOptions =
                        KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector =
                                    if (passwordVisible) Icons.Filled.VisibilityOff
                                    else Icons.Filled.Visibility,
                                contentDescription =
                                    if (passwordVisible) "Hide password" else "Show password",
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Syncwich signs in once and stores only the long-lived API token.")
            } else {
                OutlinedTextField(
                    value = apiToken,
                    onValueChange = { apiToken = it },
                    label = { Text("New API token") },
                    singleLine = true,
                    enabled = !isValidating,
                    visualTransformation =
                        if (tokenVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    keyboardOptions =
                        KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    trailingIcon = {
                        IconButton(onClick = { tokenVisible = !tokenVisible }) {
                            Icon(
                                imageVector =
                                    if (tokenVisible) Icons.Filled.VisibilityOff
                                    else Icons.Filled.Visibility,
                                contentDescription =
                                    if (tokenVisible) "Hide API token" else "Show API token",
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Generate a long-lived token in Mealie under Profile → API Tokens.")
            }
            if (updateState is ConnectionUpdateState.Error) {
                Text((updateState as ConnectionUpdateState.Error).message)
            }
            Button(
                onClick = ::submit,
                enabled = !isValidating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Save connection")
                }
            }
        }
    }
}

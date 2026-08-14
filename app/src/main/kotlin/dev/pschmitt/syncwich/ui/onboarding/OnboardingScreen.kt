package dev.pschmitt.syncwich.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pschmitt.syncwich.R
import dev.pschmitt.syncwich.ui.common.CenteredContent

/**
 * First-run connection setup. Users can paste a long-lived Mealie API token or sign in once with a
 * username and password to mint one; only the resulting token is saved (see [OnboardingViewModel]).
 */
@Composable
fun OnboardingScreen(
    onConnected: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    var serverUrl by rememberSaveable { mutableStateOf("") }
    var apiToken by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var passwordMode by rememberSaveable { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var tokenVisible by rememberSaveable { mutableStateOf(false) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var restorePassword by rememberSaveable { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isValidating = uiState is OnboardingUiState.Validating
    val keyboardController = LocalSoftwareKeyboardController.current
    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(viewModel::restoreBackup)
        }

    val passwordRequiredState = uiState as? OnboardingUiState.PasswordRequired
    if (passwordRequiredState != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                restorePassword = ""
                viewModel.consumeRestoredBackup()
            },
            title = { Text("Password required") },
            text = {
                OutlinedTextField(
                    value = restorePassword,
                    onValueChange = { restorePassword = it },
                    label = { Text("Backup password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.restoreBackup(passwordRequiredState.uri, restorePassword)
                        restorePassword = ""
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        restorePassword = ""
                        viewModel.consumeRestoredBackup()
                    }
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    fun submit() {
        keyboardController?.hide()
        if (passwordMode) {
            viewModel.connectWithPassword(serverUrl, username, password)
        } else {
            viewModel.connect(serverUrl, apiToken)
        }
    }

    LaunchedEffect(uiState) { if (uiState is OnboardingUiState.Success) onConnected() }

    Scaffold(modifier = modifier) { innerPadding ->
        // imePadding() (on top of the scroll) keeps the fields and the Connect button reachable
        // when the keyboard is open, instead of them sitting hidden behind it - the token field's
        // Done action also submits directly (see below), so reaching the button with the keyboard
        // still open is the uncommon path, not the only one.
        CenteredContent(modifier = Modifier.fillMaxSize().padding(innerPadding).imePadding()) {
            Column(
                modifier =
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_monochrome),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp).align(Alignment.CenterHorizontally),
                )
                Text(
                    text = stringResource(R.string.onboarding_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.onboarding_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text(stringResource(R.string.onboarding_server_url_label)) },
                    placeholder = {
                        Text(stringResource(R.string.onboarding_server_url_placeholder))
                    },
                    singleLine = true,
                    enabled = !isValidating,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )

                OnboardingModeControls(
                    passwordMode = passwordMode,
                    onPasswordModeChange = { passwordMode = it },
                )

                if (passwordMode) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.onboarding_username_label)) },
                        singleLine = true,
                        enabled = !isValidating,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.onboarding_password_label)) },
                        singleLine = true,
                        enabled = !isValidating,
                        visualTransformation =
                            if (passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
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
                    Text(
                        text = stringResource(R.string.onboarding_password_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    OutlinedTextField(
                        value = apiToken,
                        onValueChange = { apiToken = it },
                        label = { Text(stringResource(R.string.onboarding_api_token_label)) },
                        singleLine = true,
                        enabled = !isValidating,
                        visualTransformation =
                            if (tokenVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
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
                    Text(
                        text = stringResource(R.string.onboarding_api_token_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (uiState is OnboardingUiState.Error) {
                    Text(
                        text = (uiState as OnboardingUiState.Error).message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
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
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            text = "Connecting…",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    } else {
                        Text(stringResource(R.string.onboarding_connect))
                    }
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = { restoreLauncher.launch(arrayOf("*/*")) },
                    enabled = !isValidating,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Restore, contentDescription = null)
                    Text("Restore from backup", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
fun OnboardingModeControls(
    passwordMode: Boolean,
    onPasswordModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = !passwordMode,
            onClick = { onPasswordModeChange(false) },
            label = { Text(stringResource(R.string.onboarding_mode_token)) },
            modifier = Modifier.weight(1f).fillMaxHeight().testTag("onboarding-mode-token"),
        )
        FilterChip(
            selected = passwordMode,
            onClick = { onPasswordModeChange(true) },
            label = { Text(stringResource(R.string.onboarding_mode_password)) },
            modifier = Modifier.weight(1f).fillMaxHeight().testTag("onboarding-mode-password"),
        )
    }
}

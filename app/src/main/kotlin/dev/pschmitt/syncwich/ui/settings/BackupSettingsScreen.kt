package dev.pschmitt.syncwich.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pschmitt.syncwich.data.settings.BackupFrequency
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupSettingsViewModel = hiltViewModel(),
) {
    val enabled by viewModel.settingsRepository.scheduledBackupEnabled.collectAsStateWithLifecycle(false)
    val frequency by viewModel.settingsRepository.scheduledBackupFrequency.collectAsStateWithLifecycle(BackupFrequency.Weekly)
    val folderUri by viewModel.settingsRepository.scheduledBackupFolderUri.collectAsStateWithLifecycle(null)
    val passwordSet by viewModel.settingsRepository.scheduledBackupPasswordSet.collectAsStateWithLifecycle()
    val lastBackupAt by viewModel.settingsRepository.lastBackupAt.collectAsStateWithLifecycle(null)
    val lastBackupError by viewModel.settingsRepository.lastBackupError.collectAsStateWithLifecycle(null)
    val operation by viewModel.operation.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var exportPassword by rememberSaveable { mutableStateOf("") }
    var scheduledPassword by rememberSaveable { mutableStateOf("") }
    var restorePassword by rememberSaveable { mutableStateOf("") }
    var frequencyExpanded by remember { mutableStateOf(false) }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            uri?.let { viewModel.export(it, exportPassword.takeIf(String::isNotEmpty)) }
        }
    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(viewModel::restore)
        }
    val folderLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                }
                viewModel.setFolderUri(it)
            }
        }

    val passwordRequired = operation as? BackupOperationState.PasswordRequired
    if (passwordRequired != null) {
        AlertDialog(
            onDismissRequest = {
                restorePassword = ""
                viewModel.consumeOperation()
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
                TextButton(
                    onClick = {
                        viewModel.restore(passwordRequired.uri, restorePassword)
                        restorePassword = ""
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        restorePassword = ""
                        viewModel.consumeOperation()
                    }
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Backup") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Backups include your connection, preferences, offline recipe cache, and cached images. " +
                        "Use a password when the file may leave this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it },
                        label = { Text("Backup password (optional)") },
                        supportingText = { Text("Leave empty for an unencrypted backup") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { exportLauncher.launch("Syncwich-backup.syncwich") },
                        enabled = operation !is BackupOperationState.Working,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Backup, contentDescription = null)
                        Text("Export backup", modifier = Modifier.padding(start = 8.dp))
                    }
                    OutlinedButton(
                        onClick = { restoreLauncher.launch(arrayOf("*/*")) },
                        enabled = operation !is BackupOperationState.Working,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Restore, contentDescription = null)
                        Text("Restore backup", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            item {
                SettingsGroupCard(title = "Scheduled backups", icon = Icons.Filled.Schedule) {
                    ListItem(
                        headlineContent = { Text("Create backups automatically") },
                        supportingContent = {
                            Text(
                                if (folderUri.isNullOrBlank()) "Choose a folder first"
                                else "Back up this device on a recurring schedule"
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = enabled,
                                onCheckedChange = viewModel::setEnabled,
                                modifier = Modifier.semantics {
                                    contentDescription = "Enable scheduled backups"
                                },
                            )
                        },
                    )
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        leadingContent = { Icon(Icons.Filled.Folder, contentDescription = null) },
                        headlineContent = { Text("Backup folder") },
                        supportingContent = { Text(folderUri ?: "Not selected") },
                        trailingContent = {
                            TextButton(onClick = { folderLauncher.launch(null) }) { Text("Choose") }
                        },
                    )
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                        headlineContent = { Text("Frequency") },
                        supportingContent = { Text(frequency.label) },
                        trailingContent = {
                            DropdownMenu(
                                expanded = frequencyExpanded,
                                onDismissRequest = { frequencyExpanded = false },
                            ) {
                                BackupFrequency.entries.forEach { choice ->
                                    DropdownMenuItem(
                                        text = { Text(choice.label) },
                                        onClick = {
                                            viewModel.setFrequency(choice)
                                            frequencyExpanded = false
                                        },
                                    )
                                }
                            }
                            TextButton(onClick = { frequencyExpanded = true }) { Text("Change") }
                        },
                    )
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        headlineContent = { Text("Scheduled backup password") },
                        supportingContent = {
                            Text(
                                if (passwordSet) "Encrypted automatically"
                                else "Optional; leave empty for an unencrypted backup"
                            )
                        },
                        trailingContent = {
                            TextButton(onClick = { viewModel.setPassword(scheduledPassword) }) {
                                Text("Save")
                            }
                        },
                    )
                    OutlinedTextField(
                        value = scheduledPassword,
                        onValueChange = { scheduledPassword = it },
                        label = { Text("Scheduled password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                    lastBackupAt?.let { timestamp ->
                        ListItem(
                            headlineContent = { Text("Last backup") },
                            supportingContent = {
                                Text(DateFormat.getDateTimeInstance().format(Date(timestamp)))
                            },
                        )
                    }
                    lastBackupError?.let { error ->
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            if (operation is BackupOperationState.Working) {
                item { Text("Working…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (operation is BackupOperationState.Success || operation is BackupOperationState.Error) {
                item {
                    val message =
                        when (val state = operation) {
                            is BackupOperationState.Success -> state.message
                            is BackupOperationState.Error -> state.message
                            else -> ""
                        }
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    if (operation is BackupOperationState.Error) {
                                        MaterialTheme.colorScheme.errorContainer
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    }
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(message, modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }

}

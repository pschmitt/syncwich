package dev.pschmitt.syncwich.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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

private val syncIntervalPresets = listOf(1, 3, 6, 12, 24)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel,
) {
    val syncOnlyOnWifi by viewModel.syncOnlyOnWifi.collectAsStateWithLifecycle()
    val syncWhileRoaming by viewModel.syncWhileRoaming.collectAsStateWithLifecycle()
    val syncIntervalHours by viewModel.syncIntervalHours.collectAsStateWithLifecycle()
    var intervalMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Sync") },
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SyncToggleRow(
                    title = "Sync only on Wi-Fi",
                    subtitle = "Use an unmetered connection for background sync",
                    checked = syncOnlyOnWifi,
                    onCheckedChange = viewModel::setSyncOnlyOnWifi,
                    icon = Icons.Filled.Wifi,
                )
            }
            item {
                SyncToggleRow(
                    title = "Sync while roaming",
                    subtitle =
                        if (syncOnlyOnWifi) "No effect while Wi-Fi-only sync is enabled"
                        else "Allow background sync over a roaming mobile connection",
                    checked = syncWhileRoaming,
                    enabled = !syncOnlyOnWifi,
                    onCheckedChange = viewModel::setSyncWhileRoaming,
                    icon = Icons.Filled.SignalCellularAlt,
                )
            }
            item {
                ListItem(
                    modifier =
                        Modifier.clickable { intervalMenuExpanded = true }.semantics {
                            contentDescription =
                                "Background sync interval: every $syncIntervalHours hours"
                            role = Role.Button
                        },
                    leadingContent = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                    headlineContent = { Text("Background sync interval") },
                    supportingContent = { Text("Check for changes every $syncIntervalHours hours") },
                    trailingContent = {
                        DropdownMenu(
                            expanded = intervalMenuExpanded,
                            onDismissRequest = { intervalMenuExpanded = false },
                        ) {
                            syncIntervalPresets.forEach { hours ->
                                DropdownMenuItem(
                                    text = { Text("Every $hours hours") },
                                    onClick = {
                                        viewModel.setSyncIntervalHours(hours)
                                        intervalMenuExpanded = false
                                    },
                                )
                            }
                        }
                        Icon(Icons.Filled.ExpandMore, contentDescription = "Choose interval")
                    },
                )
            }
        }
    }
}

@Composable
private fun SyncToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                modifier = Modifier.semantics { contentDescription = title },
            )
        },
    )
}

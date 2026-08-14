package dev.pschmitt.syncwich.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.pschmitt.syncwich.ui.common.PlaceholderScreen

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        icon = Icons.Filled.Settings,
        title = "Settings",
        subtitle = "Server connection, sync status, and app preferences will live here.",
        modifier = modifier,
    )
}

package dev.pschmitt.syncwich.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.graphics.vector.ImageVector

/** The top-level groups shown by the Settings destination. */
enum class SettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
) {
    Server("Server", "Connection details and sign-in", Icons.Filled.Dns),
    Appearance("Appearance", "Theme and bottom navigation", Icons.Filled.Palette),
}

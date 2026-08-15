package dev.pschmitt.syncwich.ui.settings

import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.ui.graphics.vector.ImageVector

/** The top-level groups shown by the Settings destination. */
@Keep
enum class SettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
) {
    Server("Server", "Connection details and sign-in", Icons.Filled.Dns),
    Appearance("Appearance", "Theme, text, and recipe display", Icons.Filled.Palette),
    NavigationBar(
        "Navigation bar",
        "Choose destinations and their order",
        Icons.Filled.ViewCarousel,
    ),
    Sync("Sync", "Background refresh and network use", Icons.Filled.Sync),
    Backup("Backup", "Export, restore, and schedule backups", Icons.Filled.Backup),
    About("About", "Application and build information", Icons.Filled.Info),
}

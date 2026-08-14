package dev.pschmitt.syncwich.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.pschmitt.syncwich.BuildConfig
import dev.pschmitt.syncwich.R

private const val REPOSITORY_URL = "https://github.com/pschmitt/syncwich"
private const val SPONSORS_URL = "https://github.com/sponsors/pschmitt"
private const val PRIVACY_URL = "https://github.com/pschmitt/syncwich/blob/main/PRIVACY.md"
private const val LICENSE_URL = "https://github.com/pschmitt/syncwich/blob/main/LICENSE"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("About") },
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
                SettingsGroupCard(
                    title = "Syncwich",
                    icon = Icons.Filled.Info,
                    headerContent = {
                        Image(
                            painter = painterResource(R.drawable.syncwich_icon),
                            contentDescription = "Syncwich app icon",
                            modifier = Modifier.size(24.dp),
                        )
                    },
                ) {
                    AboutInfoRow(
                        icon = Icons.Filled.Apps,
                        title = "Version",
                        subtitle = "${BuildConfig.VERSION_NAME} · GPL-3.0",
                    )
                    AboutInfoRow(
                        icon = Icons.Filled.Tag,
                        title = "Build",
                        subtitle = BuildConfig.GIT_REVISION,
                    )
                    AboutInfoRow(
                        icon = Icons.Filled.DateRange,
                        title = "Build date",
                        subtitle = BuildConfig.BUILD_DATE,
                    )
                }
            }
            item {
                SettingsGroupCard(title = "Project", icon = Icons.Filled.Code) {
                    ExternalLinkRow(
                        context = context,
                        url = REPOSITORY_URL,
                        title = "GitHub repository",
                        subtitle = "View the source code and report issues",
                    )
                    ExternalLinkRow(
                        context = context,
                        url = SPONSORS_URL,
                        icon = Icons.Filled.Favorite,
                        title = "Sponsor the project",
                        subtitle = "Support development on GitHub Sponsors",
                    )
                    ExternalLinkRow(
                        context = context,
                        url = PRIVACY_URL,
                        title = "Privacy policy",
                        subtitle = "How Syncwich handles data and network access",
                    )
                    ExternalLinkRow(
                        context = context,
                        url = LICENSE_URL,
                        title = "License",
                        subtitle = "Syncwich is free software under GPL-3.0",
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    SettingsListItem(
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
    )
}

@Composable
private fun ExternalLinkRow(
    context: Context,
    url: String,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Public,
) {
    SettingsListItem(
        modifier = Modifier.clickable(role = Role.Button) {
            ContextCompat.startActivity(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)), null)
        }.semantics { role = Role.Button },
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open link")
        },
    )
}

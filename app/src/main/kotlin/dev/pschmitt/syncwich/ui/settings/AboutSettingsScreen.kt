package dev.pschmitt.syncwich.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.pschmitt.syncwich.BuildConfig
import dev.pschmitt.syncwich.R
import dev.pschmitt.syncwich.ui.theme.SyncwichTerracotta40
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private const val REPOSITORY_URL = "https://github.com/pschmitt/syncwich"
private const val SPONSORS_URL = "https://github.com/sponsors/pschmitt"
private const val PRIVACY_URL = "https://github.com/pschmitt/syncwich/blob/main/PRIVACY.md"
private const val LICENSE_URL = "https://github.com/pschmitt/syncwich/blob/main/LICENSE"

private data class Library(val name: String, val license: String, val url: String)

// Keep this list in sync with the libraries used by the app's runtime dependencies.
private val LIBRARIES =
    listOf(
        Library("AndroidX", "Apache License 2.0", "https://github.com/androidx/androidx"),
        Library(
            "Jetpack Compose",
            "Apache License 2.0",
            "https://github.com/androidx/androidx/tree/androidx-main/compose",
        ),
        Library(
            "Material 3",
            "Apache License 2.0",
            "https://github.com/androidx/androidx/tree/androidx-main/compose/material3",
        ),
        Library("Coil", "Apache License 2.0", "https://github.com/coil-kt/coil"),
        Library("Hilt", "Apache License 2.0", "https://github.com/google/dagger"),
        Library("Kotlin", "Apache License 2.0", "https://github.com/JetBrains/kotlin"),
        Library(
            "kotlinx.coroutines",
            "Apache License 2.0",
            "https://github.com/Kotlin/kotlinx.coroutines",
        ),
        Library(
            "kotlinx.datetime",
            "Apache License 2.0",
            "https://github.com/Kotlin/kotlinx-datetime",
        ),
        Library(
            "kotlinx.serialization",
            "Apache License 2.0",
            "https://github.com/Kotlin/kotlinx.serialization",
        ),
        Library(
            "Multiplatform Markdown Renderer",
            "Apache License 2.0",
            "https://github.com/mikepenz/multiplatform-markdown-renderer",
        ),
        Library("OkHttp", "Apache License 2.0", "https://github.com/square/okhttp"),
        Library("Retrofit", "Apache License 2.0", "https://github.com/square/retrofit"),
        Library(
            "Room",
            "Apache License 2.0",
            "https://github.com/androidx/androidx/tree/androidx-main/room",
        ),
        Library(
            "WorkManager",
            "Apache License 2.0",
            "https://github.com/androidx/androidx/tree/androidx-main/work",
        ),
        Library("Timber", "Apache License 2.0", "https://github.com/JakeWharton/timber"),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onBuildTap: () -> Unit = {},
    developerModeToast: Flow<String> = emptyFlow(),
) {
    val context = LocalContext.current
    var progressToast by remember { mutableStateOf<Toast?>(null) }
    DisposableEffect(Unit) { onDispose { progressToast?.cancel() } }
    LaunchedEffect(developerModeToast) {
        developerModeToast.collect { message ->
            progressToast?.cancel()
            progressToast = Toast.makeText(context, message, Toast.LENGTH_SHORT).also { it.show() }
        }
    }
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
            modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("about-settings-list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SettingsGroupCard(
                    title = "Syncwich",
                    icon = Icons.Filled.Info,
                    headerContent = {
                        Box(
                            modifier =
                                Modifier.size(72.dp)
                                    .background(SyncwichTerracotta40, CircleShape)
                                    .padding(8.dp)
                                    .testTag("about-app-icon-background")
                        ) {
                            Image(
                                painter = painterResource(R.drawable.syncwich_icon),
                                contentDescription = "Syncwich app icon",
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    },
                ) {
                    AboutInfoRow(
                        icon = Icons.Filled.Apps,
                        title = "Version",
                        subtitle = "${BuildConfig.VERSION_NAME} · GPL-3.0",
                    )
                    BuildInfoRow(
                        context = context,
                        revision = BuildConfig.GIT_REVISION,
                        onBuildTap = onBuildTap,
                    )
                    AboutInfoRow(
                        icon = Icons.Filled.DateRange,
                        title = "Build date",
                        subtitle = BuildConfig.BUILD_DATE,
                    )
                    AboutInfoRow(
                        icon = Icons.Filled.Apps,
                        title = "Build type",
                        subtitle = aboutBuildTypeLabel(BuildConfig.DEBUG),
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
            item {
                SettingsGroupCard(
                    title = "Libraries",
                    icon = Icons.AutoMirrored.Filled.LibraryBooks,
                ) {
                    LIBRARIES.forEach { library ->
                        ExternalLinkRow(
                            context = context,
                            url = library.url,
                            title = library.name,
                            subtitle = library.license,
                            icon = Icons.AutoMirrored.Filled.LibraryBooks,
                        )
                    }
                }
            }
        }
    }
}

internal fun aboutBuildTypeLabel(isDebug: Boolean): String =
    if (isDebug) "Debug build" else "Release build"

internal fun githubCommitUrl(revision: String): String? {
    val candidate = revision.trim().removeSuffix("-dirty")
    val commit = Regex("(?i)(?:^|-)g?([0-9a-f]{7,40})$").find(candidate)?.groupValues?.get(1)
    return commit?.let { "$REPOSITORY_URL/commit/$it" }
}

@Composable
private fun BuildInfoRow(
    context: Context,
    revision: String,
    onBuildTap: () -> Unit,
) {
    val commitUrl = githubCommitUrl(revision)
    SettingsListItem(
        modifier =
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onBuildTap).semantics {
                contentDescription = "Build $revision"
                role = Role.Button
            },
        leadingContent = { Icon(Icons.Filled.Tag, contentDescription = null) },
        headlineContent = { Text("Build") },
        supportingContent = { Text(revision) },
        trailingContent =
            commitUrl?.let { url ->
                {
                    IconButton(
                        onClick = {
                            ContextCompat.startActivity(
                                context,
                                Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                                null,
                            )
                        },
                        modifier = Modifier.semantics { contentDescription = "Open commit" },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                        )
                    }
                }
            },
    )
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
        modifier =
            Modifier.clickable(role = Role.Button) {
                    ContextCompat.startActivity(
                        context,
                        Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                        null,
                    )
                }
                .semantics { role = Role.Button },
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open link")
        },
    )
}

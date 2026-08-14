package dev.pschmitt.syncwich.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pschmitt.syncwich.data.settings.FONT_SCALE_STEPS
import dev.pschmitt.syncwich.data.settings.MAX_FONT_SCALE
import dev.pschmitt.syncwich.data.settings.MIN_FONT_SCALE
import dev.pschmitt.syncwich.data.settings.NavigationBarItemKeys
import dev.pschmitt.syncwich.data.settings.resolveNavBarOrder
import dev.pschmitt.syncwich.ui.navigation.TopLevelDestination
import dev.pschmitt.syncwich.ui.navigation.NavigationBarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onCategoryClick: (SettingsCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                Text(
                    "Choose a settings category",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(SettingsCategory.entries, key = { it.name }) { category ->
                SettingsCategoryRow(category = category, onClick = onCategoryClick)
            }
        }
    }
}

@Composable
private fun SettingsCategoryRow(
    category: SettingsCategory,
    onClick: (SettingsCategory) -> Unit,
) {
    ListItem(
        modifier =
            Modifier.fillMaxWidth().clickable(role = Role.Button) { onClick(category) }.semantics {
                contentDescription = "${category.title}: ${category.subtitle}"
                role = Role.Button
            },
        leadingContent = { Icon(category.icon, contentDescription = null) },
        headlineContent = { Text(category.title) },
        supportingContent = { Text(category.subtitle) },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        },
    )
}

@Composable
fun SettingsCategoryScreen(
    category: SettingsCategory,
    onBack: () -> Unit,
    onChangeConnection: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    when (category) {
        SettingsCategory.Server ->
            ServerSettingsScreen(
                onBack = onBack,
                onChangeConnection = onChangeConnection,
                onSignedOut = onSignedOut,
                modifier = modifier,
                viewModel = viewModel,
            )
        SettingsCategory.Appearance ->
            AppearanceSettingsScreen(
                onBack = onBack,
                modifier = modifier,
                viewModel = viewModel,
            )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier,
    viewModel: SettingsViewModel,
    navigationBarViewModel: NavigationBarViewModel = hiltViewModel(),
) {
    val persistedOrder by viewModel.navigationBarOrder.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val visibleItems by navigationBarViewModel.visibleItemKeys.collectAsStateWithLifecycle()
    val naturalKeys = TopLevelDestination.entries.map { it.key }
    val orderedKeys = resolveNavBarOrder(naturalKeys, persistedOrder, emptySet())
    val orderedDestinations =
        orderedKeys.mapNotNull { key -> TopLevelDestination.entries.firstOrNull { it.key == key } }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
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
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text("Font size", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Adjust text size throughout the app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${(fontScale * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Slider(
                        value = fontScale,
                        onValueChange = viewModel::saveFontScale,
                        valueRange = MIN_FONT_SCALE..MAX_FONT_SCALE,
                        steps = FONT_SCALE_STEPS,
                        modifier =
                            Modifier.fillMaxWidth().semantics {
                                contentDescription =
                                    "Font size, ${(fontScale * 100).toInt()} percent"
                            },
                    )
                }
            }
            item {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text("Bottom navigation", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Choose which destinations appear and change their order.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(orderedDestinations, key = { it.key }) { destination ->
                val index = orderedDestinations.indexOf(destination)
                val isPinned = destination.key == NavigationBarItemKeys.RECIPES
                ListItem(
                    headlineContent = { Text(destination.label) },
                    supportingContent = {
                        Text(
                            when {
                                isPinned -> "Always visible"
                                destination.key in visibleItems -> "Shown in the bottom bar"
                                else -> "Hidden by default because its cache is empty"
                            }
                        )
                    },
                    trailingContent = {
                        Row {
                            IconButton(
                                onClick = {
                                    viewModel.saveNavigationBarOrder(
                                        orderedKeys.move(index, index - 1)
                                    )
                                },
                                enabled = index > 0,
                                modifier = Modifier.semantics {
                                    contentDescription = "Move ${destination.label} up"
                                },
                            ) {
                                Icon(Icons.Filled.ArrowUpward, contentDescription = null)
                            }
                            IconButton(
                                onClick = {
                                    viewModel.saveNavigationBarOrder(
                                        orderedKeys.move(index, index + 1)
                                    )
                                },
                                enabled = index < orderedDestinations.lastIndex,
                                modifier = Modifier.semantics {
                                    contentDescription = "Move ${destination.label} down"
                                },
                            ) {
                                Icon(Icons.Filled.ArrowDownward, contentDescription = null)
                            }
                            Switch(
                                checked = isPinned || destination.key in visibleItems,
                                onCheckedChange =
                                    if (isPinned) null
                                    else { checked ->
                                        viewModel.setNavigationBarItemHidden(
                                            destination.key,
                                            hidden = !checked,
                                        )
                                    },
                                enabled = !isPinned,
                                modifier = Modifier.semantics {
                                    contentDescription =
                                        if (isPinned) "${destination.label} is always visible"
                                        else "Show ${destination.label} in bottom navigation"
                                },
                            )
                        }
                    },
                )
            }
        }
    }
}

private fun <T> List<T>.move(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices) return this
    return toMutableList().also { items -> items.add(to, items.removeAt(from)) }
}

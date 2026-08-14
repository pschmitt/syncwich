package dev.pschmitt.syncwich.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pschmitt.syncwich.data.settings.FONT_SCALE_STEPS
import dev.pschmitt.syncwich.data.settings.MAX_FONT_SCALE
import dev.pschmitt.syncwich.data.settings.MIN_FONT_SCALE
import dev.pschmitt.syncwich.data.settings.NavigationBarItemKeys
import dev.pschmitt.syncwich.data.settings.ThemeMode
import dev.pschmitt.syncwich.data.settings.resolveNavBarOrder
import dev.pschmitt.syncwich.ui.navigation.NavigationBarViewModel
import dev.pschmitt.syncwich.ui.navigation.TopLevelDestination
import dev.pschmitt.syncwich.ui.common.NavigationTitle

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
                title = { NavigationTitle(TopLevelDestination.SETTINGS.icon, "Settings") },
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    "Tune Syncwich to your kitchen and your workflow.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                SettingsGroupCard(title = "Account", icon = Icons.Filled.Dns) {
                    SettingsCategoryRow(SettingsCategory.Server, onCategoryClick)
                }
            }
            item {
                SettingsGroupCard(title = "Data & sync", icon = Icons.Filled.Sync) {
                    SettingsCategoryRow(SettingsCategory.Sync, onCategoryClick)
                    SettingsCategoryRow(SettingsCategory.Backup, onCategoryClick)
                }
            }
            item {
                SettingsGroupCard(title = "Personalization", icon = Icons.Filled.Palette) {
                    SettingsCategoryRow(SettingsCategory.Appearance, onCategoryClick)
                }
            }
            item {
                SettingsSingleItemCard {
                    SettingsCategoryRow(SettingsCategory.About, onCategoryClick)
                }
            }
        }
    }
}

@Composable
internal fun SettingsListItem(
    modifier: Modifier = Modifier,
    leadingContent: (@Composable (() -> Unit))? = null,
    headlineContent: @Composable () -> Unit,
    supportingContent: (@Composable (() -> Unit))? = null,
    trailingContent: (@Composable (() -> Unit))? = null,
) {
    ListItem(
        modifier = modifier,
        leadingContent = leadingContent,
        headlineContent = headlineContent,
        supportingContent = supportingContent,
        trailingContent = trailingContent,
        colors =
            androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
internal fun SettingsGroupCard(
    title: String,
    icon: ImageVector,
    headerContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            SettingsListItem(
                leadingContent =
                    headerContent
                        ?: {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                headlineContent = {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            content()
        }
    }
}

@Composable
internal fun SettingsSingleItemCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp), content = content)
    }
}

@Composable
private fun SettingsCategoryRow(
    category: SettingsCategory,
    onClick: (SettingsCategory) -> Unit,
) {
    SettingsListItem(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(role = Role.Button) { onClick(category) }
                .semantics {
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
        SettingsCategory.Sync ->
            SyncSettingsScreen(onBack = onBack, modifier = modifier, viewModel = viewModel)
        SettingsCategory.Backup -> BackupSettingsScreen(onBack = onBack, modifier = modifier)
        SettingsCategory.About -> {
            val aboutViewModel: AboutSettingsViewModel = hiltViewModel()
            val developerMode by aboutViewModel.developerMode.collectAsStateWithLifecycle()
            AboutSettingsScreen(
                onBack = onBack,
                modifier = modifier,
                developerMode = developerMode,
                onBuildTap = aboutViewModel::onBuildRowTap,
            )
        }
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
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val ingredientChecklistEnabled by
        viewModel.ingredientChecklistEnabled.collectAsStateWithLifecycle()
    val visibleItems by navigationBarViewModel.visibleItemKeys.collectAsStateWithLifecycle()
    val naturalKeys = TopLevelDestination.entries.map { it.key }
    val orderedKeys = resolveNavBarOrder(naturalKeys, persistedOrder, emptySet())
    val orderedDestinations = orderedKeys.mapNotNull { key ->
        TopLevelDestination.entries.firstOrNull { it.key == key }
    }

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
                SettingsGroupCard(title = "Text", icon = Icons.Filled.FormatSize) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
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
            }
            item {
                SettingsGroupCard(title = "Theme", icon = Icons.Filled.Palette) {
                    ThemeMode.entries.forEach { mode ->
                        SettingsListItem(
                            modifier =
                                Modifier.fillMaxWidth().clickable { viewModel.saveThemeMode(mode) },
                            headlineContent = { Text(mode.label) },
                            supportingContent = {
                                if (mode == ThemeMode.SYSTEM) {
                                    Text("Follow the device appearance setting")
                                }
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = themeMode == mode,
                                    onClick = { viewModel.saveThemeMode(mode) },
                                )
                            },
                        )
                    }
                }
            }
            item {
                SettingsGroupCard(title = "Recipe display", icon = Icons.Filled.Checklist) {
                    SettingsListItem(
                        modifier = Modifier.fillMaxWidth(),
                        headlineContent = { Text("Ingredient checklist") },
                        supportingContent = {
                            Text("Show ingredients as checkable items while cooking")
                        },
                        trailingContent = {
                            Switch(
                                checked = ingredientChecklistEnabled,
                                onCheckedChange = viewModel::setIngredientChecklistEnabled,
                            )
                        },
                    )
                }
            }
            item {
                SettingsGroupCard(title = "Bottom navigation", icon = Icons.Filled.ViewCarousel) {
                    SettingsListItem(
                        supportingContent = {
                            Text(
                                "Choose which destinations appear and change their order.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        headlineContent = { Text("Navigation destinations") },
                    )
                    orderedDestinations.forEachIndexed { index, destination ->
                        NavigationDestinationRow(
                            destination = destination,
                            index = index,
                            orderedDestinations = orderedDestinations,
                            orderedKeys = orderedKeys,
                            visibleItems = visibleItems.toSet(),
                            onMove = viewModel::saveNavigationBarOrder,
                            onSetHidden = viewModel::setNavigationBarItemHidden,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationDestinationRow(
    destination: TopLevelDestination,
    index: Int,
    orderedDestinations: List<TopLevelDestination>,
    orderedKeys: List<String>,
    visibleItems: Set<String>,
    onMove: (List<String>) -> Unit,
    onSetHidden: (String, Boolean) -> Unit,
) {
    val isPinned = destination.key == NavigationBarItemKeys.RECIPES
    SettingsListItem(
        headlineContent = { Text(destination.label) },
        supportingContent = {
            Text(
                when {
                    isPinned -> "Always visible"
                    destination.key in visibleItems -> "Shown in the bottom bar"
                    destination.key == NavigationBarItemKeys.SETTINGS -> "Hidden until you add it"
                    destination.key == NavigationBarItemKeys.FAVORITES -> "Hidden until you add it"
                    else -> "Hidden by default because its cache is empty"
                }
            )
        },
        trailingContent = {
            Row {
                IconButton(
                    onClick = { onMove(orderedKeys.move(index, index - 1)) },
                    enabled = index > 0,
                    modifier =
                        Modifier.semantics { contentDescription = "Move ${destination.label} up" },
                ) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = null)
                }
                IconButton(
                    onClick = { onMove(orderedKeys.move(index, index + 1)) },
                    enabled = index < orderedDestinations.lastIndex,
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Move ${destination.label} down"
                        },
                ) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = null)
                }
                Switch(
                    checked = isPinned || destination.key in visibleItems,
                    onCheckedChange =
                        if (isPinned) null
                        else { checked -> onSetHidden(destination.key, !checked) },
                    enabled = !isPinned,
                    modifier =
                        Modifier.semantics {
                            contentDescription =
                                if (isPinned) "${destination.label} is always visible"
                                else "Show ${destination.label} in bottom navigation"
                        },
                )
            }
        },
    )
}

private fun <T> List<T>.move(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices) return this
    return toMutableList().also { items -> items.add(to, items.removeAt(from)) }
}

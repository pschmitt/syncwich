package dev.pschmitt.syncwich.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pschmitt.syncwich.data.settings.NavigationBarItemKeys
import dev.pschmitt.syncwich.data.settings.resolveNavBarOrder
import dev.pschmitt.syncwich.ui.navigation.TopLevelDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val persistedOrder by viewModel.navigationBarOrder.collectAsStateWithLifecycle()
    val hiddenItems by viewModel.navigationBarHiddenItems.collectAsStateWithLifecycle()
    val naturalKeys = TopLevelDestination.entries.map { it.key }
    val orderedKeys = resolveNavBarOrder(naturalKeys, persistedOrder, emptySet())
    val orderedDestinations = orderedKeys.mapNotNull { key ->
        TopLevelDestination.entries.firstOrNull { it.key == key }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Settings") }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
                        Text(if (isPinned) "Always visible" else "Shown in the bottom bar")
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
                                checked = isPinned || destination.key !in hiddenItems,
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

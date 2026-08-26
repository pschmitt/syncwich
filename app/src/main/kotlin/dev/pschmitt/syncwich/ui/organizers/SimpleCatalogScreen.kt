package dev.pschmitt.syncwich.ui.organizers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.pschmitt.syncwich.ui.common.PlaceholderScreen
import dev.pschmitt.syncwich.ui.common.RefreshErrorBanner
import dev.pschmitt.syncwich.ui.common.RefreshState
import dev.pschmitt.syncwich.ui.common.SearchField

/**
 * Shared list screen for Categories/Tags/Tools - search, pull-to-refresh, delete-with-confirmation,
 * tap-to-edit, a create FAB. See [SimpleCatalogItem]'s kdoc for why these three share UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleCatalogScreen(
    title: String,
    itemNounSingular: String,
    emptyIcon: ImageVector,
    emptySubtitle: String,
    items: List<SimpleCatalogItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    refreshState: RefreshState,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onItemClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    onDelete: (String) -> Unit,
) {
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    val pendingDeleteItem = remember(items, pendingDelete) { items.find { it.id == pendingDelete } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New $itemNounSingular") },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = refreshState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                RefreshErrorBanner(refreshState.errorMessage, onRetry = onRefresh)
                SearchField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = "Search $title",
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
                if (items.isEmpty()) {
                    PlaceholderScreen(
                        icon = emptyIcon,
                        title = "No ${title.lowercase()} yet",
                        subtitle = emptySubtitle,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                        items(items, key = SimpleCatalogItem::id) { item ->
                            ListItem(
                                headlineContent = { Text(item.name) },
                                trailingContent = {
                                    IconButton(onClick = { pendingDelete = item.id }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Delete ${item.name}",
                                        )
                                    }
                                },
                                modifier =
                                    Modifier.fillMaxWidth().clickable { onItemClick(item.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (pendingDeleteItem != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete $itemNounSingular?") },
            text = {
                Text(
                    "\"${pendingDeleteItem.name}\" will be removed from Mealie. This can't be" +
                        " undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(pendingDeleteItem.id)
                        pendingDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

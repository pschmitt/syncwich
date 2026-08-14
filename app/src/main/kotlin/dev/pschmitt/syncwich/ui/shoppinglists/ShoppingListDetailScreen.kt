package dev.pschmitt.syncwich.ui.shoppinglists

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pschmitt.syncwich.data.db.entity.ShoppingListItemEntity
import dev.pschmitt.syncwich.ui.common.PlaceholderScreen
import dev.pschmitt.syncwich.ui.common.RefreshErrorBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShoppingListDetailViewModel = hiltViewModel(),
) {
    val list by viewModel.list.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(list?.name ?: "Shopping list") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = refreshState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            if (list == null) {
                PlaceholderScreen(
                    icon = Icons.Filled.ShoppingCart,
                    title =
                        if (refreshState.isRefreshing) "Loading shopping list"
                        else "Shopping list unavailable offline",
                    subtitle =
                        if (refreshState.errorMessage != null) {
                            "This list is not saved on this device yet. Connect to Mealie and try again."
                        } else {
                            "This list hasn't finished syncing yet."
                        },
                    modifier = Modifier.fillMaxSize(),
                    isLoading = refreshState.isRefreshing,
                    onRetry = viewModel::refresh,
                )
            } else if (items.isEmpty()) {
                PlaceholderScreen(
                    icon = Icons.Filled.ShoppingCart,
                    title = "No items yet",
                    subtitle = "This list is empty, or hasn't finished syncing yet.",
                    modifier = Modifier.fillMaxSize(),
                    onRetry = viewModel::refresh,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        RefreshErrorBanner(
                            errorMessage = refreshState.errorMessage,
                            onRetry = viewModel::refresh,
                        )
                    }
                    items(items, key = { it.id }) { item ->
                        ShoppingListItemRow(item)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/**
 * View-only: shows the checked state Mealie already has for this item but never lets the user
 * toggle it - this app is read-only, see AGENTS.md.
 */
@Composable
private fun ShoppingListItemRow(item: ShoppingListItemEntity) {
    ListItem(
        headlineContent = {
            Text(
                text = item.display,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                color =
                    if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
            )
        },
        supportingContent = item.note?.takeIf { it.isNotBlank() }?.let { note -> { Text(note) } },
        leadingContent = {
            Checkbox(checked = item.checked, onCheckedChange = null, enabled = false)
        },
    )
}

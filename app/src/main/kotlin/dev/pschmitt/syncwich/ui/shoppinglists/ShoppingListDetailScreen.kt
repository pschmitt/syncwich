package dev.pschmitt.syncwich.ui.shoppinglists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
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
    val addItemState by viewModel.addItemState.collectAsStateWithLifecycle()

    if (addItemState.isOpen) {
        AddShoppingItemDialog(
            state = addItemState,
            onTextChange = viewModel::onAddItemTextChange,
            onConfirm = viewModel::confirmAddItem,
            onDismiss = viewModel::dismissAddItem,
        )
    }

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
        floatingActionButton = {
            if (list != null) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::startAddItem,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add item") },
                )
            }
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
                    subtitle = "Tap \"Add item\" to add one, or pull to refresh.",
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
                        ShoppingListItemRow(
                            item = item,
                            onCheckedChange = { checked -> viewModel.setChecked(item.id, checked) },
                            onRemove = { viewModel.removeItem(item.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/**
 * Toggleable (SW-24/SW-33): checking/unchecking calls [onCheckedChange], which writes to Room
 * before any network sync - see `ShoppingListRepository.setItemChecked`'s kdoc - so the row updates
 * immediately even offline. [onRemove] deletes the item outright.
 */
@Composable
private fun ShoppingListItemRow(
    item: ShoppingListItemEntity,
    onCheckedChange: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
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
        leadingContent = { Checkbox(checked = item.checked, onCheckedChange = onCheckedChange) },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove item")
            }
        },
    )
}

@Composable
private fun AddShoppingItemDialog(
    state: AddShoppingItemState,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.display,
                    onValueChange = onTextChange,
                    label = { Text("Item") },
                    singleLine = true,
                    isError = state.errorMessage != null,
                    supportingText = state.errorMessage?.let { { Text(it) } },
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !state.isSaving) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSaving) { Text("Cancel") }
        },
    )
}

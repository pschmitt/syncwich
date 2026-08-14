package dev.pschmitt.syncwich.ui.cookbooks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.pschmitt.syncwich.data.api.recipeImageUrl
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.ui.common.PlaceholderScreen
import dev.pschmitt.syncwich.ui.common.RefreshErrorBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookbookDetailScreen(
    modifier: Modifier = Modifier,
    onRecipeClick: (String, String) -> Unit = { _, _ -> },
    onEditClick: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onDeleted: () -> Unit = {},
    viewModel: CookbookDetailViewModel = hiltViewModel(),
) {
    val cookbook by viewModel.cookbook.collectAsStateWithLifecycle()
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()
    val serverUrl = viewModel.serverUrl
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()
    val deleteState by viewModel.deleteState.collectAsStateWithLifecycle()
    var overflowExpanded by rememberSaveable { mutableStateOf(false) }
    var deleteDialogVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(deleteState) {
        if (deleteState is CookbookDeleteUiState.Deleted) onDeleted()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(cookbook?.name ?: "Cookbook") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    cookbook?.let { currentCookbook ->
                        Box {
                            IconButton(
                                enabled = deleteState !is CookbookDeleteUiState.Deleting,
                                onClick = { overflowExpanded = true },
                            ) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More actions")
                            }
                            CookbookOverflowMenu(
                                expanded = overflowExpanded,
                                onDismiss = { overflowExpanded = false },
                                onEditClick = { onEditClick(currentCookbook.id) },
                                onDeleteClick = { deleteDialogVisible = true },
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        val currentCookbook = cookbook
        PullToRefreshBox(
            isRefreshing = refreshState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            if (currentCookbook == null) {
                PlaceholderScreen(
                    icon = Icons.Filled.Restaurant,
                    title =
                        if (refreshState.isRefreshing) "Loading cookbook"
                        else "Cookbook unavailable offline",
                    subtitle =
                        if (refreshState.errorMessage != null) {
                            "This cookbook is not saved on this device yet. Connect to Mealie and try again."
                        } else {
                            "This cookbook hasn't finished syncing yet."
                        },
                    modifier = Modifier.fillMaxSize(),
                    isLoading = refreshState.isRefreshing,
                    onRetry = viewModel::refresh,
                )
            } else if (recipes.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    RefreshErrorBanner(
                        errorMessage =
                            (deleteState as? CookbookDeleteUiState.Failed)?.message
                                ?: refreshState.errorMessage,
                        onRetry =
                            if (deleteState is CookbookDeleteUiState.Failed) {
                                viewModel::retryDelete
                            } else {
                                viewModel::refresh
                            },
                    )
                    PlaceholderScreen(
                        icon = Icons.Filled.Restaurant,
                        title =
                            if (refreshState.isRefreshing) "Loading recipes" else "No recipes yet",
                        subtitle =
                            currentCookbook.description.takeIf { it.isNotBlank() }
                                ?: "This cookbook has no matching recipes synced yet.",
                        modifier = Modifier.fillMaxSize(),
                        isLoading = refreshState.isRefreshing,
                        onRetry = viewModel::refresh,
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    RefreshErrorBanner(
                        errorMessage =
                            (deleteState as? CookbookDeleteUiState.Failed)?.message
                                ?: refreshState.errorMessage,
                        onRetry =
                            if (deleteState is CookbookDeleteUiState.Failed) {
                                viewModel::retryDelete
                            } else {
                                viewModel::refresh
                            },
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (currentCookbook.description.isNotBlank()) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "About this cookbook",
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Text(
                                            text = currentCookbook.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 6.dp),
                                        )
                                    }
                                }
                            }
                        }
                        items(recipes, key = { it.id }) { recipe ->
                            CookbookRecipeRow(
                                recipe = recipe,
                                serverUrl = serverUrl,
                                onClick = { onRecipeClick(recipe.id, recipe.slug) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (deleteDialogVisible) {
        cookbook?.let { currentCookbook ->
            CookbookDeleteConfirmationDialog(
                cookbookName = currentCookbook.name,
                isDeleting = deleteState is CookbookDeleteUiState.Deleting,
                errorMessage = (deleteState as? CookbookDeleteUiState.Failed)?.message,
                onConfirm = { viewModel.deleteCookbook(currentCookbook.id) },
                onDismiss = { deleteDialogVisible = false },
            )
        }
    }
}

@Composable
internal fun CookbookOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Edit") },
            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
            onClick = {
                onDismiss()
                onEditClick()
            },
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            leadingIcon = {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            onClick = {
                onDismiss()
                onDeleteClick()
            },
        )
    }
}

@Composable
internal fun CookbookDeleteConfirmationDialog(
    cookbookName: String,
    isDeleting: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        icon = {
            Icon(
                Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text("Delete cookbook?") },
        text = {
            Column {
                Text("Delete \"$cookbookName\" from Mealie? This cannot be undone.")
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !isDeleting, onClick = onConfirm) { Text("Delete") }
        },
        dismissButton = {
            TextButton(enabled = !isDeleting, onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun CookbookRecipeRow(recipe: RecipeSummaryEntity, serverUrl: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = recipeImageUrl(serverUrl, recipe.id, recipe.image),
                contentDescription = recipe.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (recipe.description.isNotBlank()) {
                    Text(
                        text = recipe.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

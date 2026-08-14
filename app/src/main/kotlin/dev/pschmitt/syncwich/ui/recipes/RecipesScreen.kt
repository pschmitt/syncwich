package dev.pschmitt.syncwich.ui.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import dev.pschmitt.syncwich.ui.common.SearchField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    onRecipeClick: (RecipeSummaryEntity) -> Unit,
    modifier: Modifier = Modifier,
    onCreateClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewModel: RecipesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Recipes") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New recipe") },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.refreshState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                SearchField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = "Search recipes",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )

                if (uiState.categories.isNotEmpty()) {
                    FilterChipRow(
                        entries = uiState.categories,
                        key = { it.id },
                        label = { it.name },
                        selectedId = uiState.selectedCategoryId,
                        onSelected = viewModel::onCategorySelected,
                    )
                }
                if (uiState.tags.isNotEmpty()) {
                    FilterChipRow(
                        entries = uiState.tags,
                        key = { it.id },
                        label = { it.name },
                        selectedId = uiState.selectedTagId,
                        onSelected = viewModel::onTagSelected,
                    )
                }

                RefreshErrorBanner(
                    errorMessage = uiState.refreshState.errorMessage,
                    onRetry = viewModel::refresh,
                )

                if (uiState.recipes.isEmpty()) {
                    val hasFilter =
                        uiState.searchQuery.isNotBlank() ||
                            uiState.selectedCategoryId != null ||
                            uiState.selectedTagId != null
                    PlaceholderScreen(
                        icon = Icons.Filled.Restaurant,
                        title =
                            when {
                                hasFilter -> "No recipes match"
                                uiState.refreshState.isRefreshing -> "Loading recipes"
                                uiState.refreshState.errorMessage != null -> "No saved recipes yet"
                                else -> "No recipes synced yet"
                            },
                        subtitle =
                            if (hasFilter) "Try a different search or filter."
                            else "Recipes appear here automatically once Syncwich syncs with your Mealie server.",
                        modifier = Modifier.fillMaxSize(),
                        isLoading = uiState.refreshState.isRefreshing,
                        onRetry = if (!hasFilter) viewModel::refresh else null,
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        gridItems(uiState.recipes, key = { it.id }) { recipe ->
                            RecipeCard(
                                recipe = recipe,
                                serverUrl = uiState.serverUrl,
                                onClick = { onRecipeClick(recipe) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> FilterChipRow(
    entries: List<T>,
    key: (T) -> String,
    label: (T) -> String,
    selectedId: String?,
    onSelected: (String) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(entries, key = key) { entry ->
            val id = key(entry)
            FilterChip(
                selected = selectedId == id,
                onClick = { onSelected(id) },
                label = { Text(label(entry)) },
            )
        }
    }
}

@Composable
internal fun RecipeCard(recipe: RecipeSummaryEntity, serverUrl: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column {
            val imageUrl = recipeImageUrl(serverUrl, recipe.id, recipe.image)
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = recipe.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center).size(40.dp),
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (recipe.rating != null || recipe.totalTime != null) {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        recipe.rating?.let { rating ->
                            Row {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = " ${formatRating(rating)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        recipe.totalTime?.let { totalTime ->
                            Row {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = " $totalTime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

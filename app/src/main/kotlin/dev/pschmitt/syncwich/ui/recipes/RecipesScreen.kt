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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.pschmitt.syncwich.data.api.recipeImageUrl
import dev.pschmitt.syncwich.data.db.entity.CategoryEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.data.db.entity.TagEntity
import dev.pschmitt.syncwich.ui.common.PlaceholderScreen
import dev.pschmitt.syncwich.ui.common.RefreshErrorBanner
import dev.pschmitt.syncwich.ui.common.SearchField
import dev.pschmitt.syncwich.ui.common.highlightedSearchText
import dev.pschmitt.syncwich.ui.common.NavigationTitle
import dev.pschmitt.syncwich.ui.navigation.TopLevelDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    onRecipeClick: (RecipeSummaryEntity) -> Unit,
    modifier: Modifier = Modifier,
    onCreateClick: () -> Unit = {},
    onImportUrlClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    initialTagId: String? = null,
    initialCategoryId: String? = null,
    viewModel: RecipesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var filterSheetVisible by rememberSaveable { mutableStateOf(false) }
    var addMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var importDialogVisible by rememberSaveable { mutableStateOf(false) }
    var importUrl by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(initialTagId, initialCategoryId) {
        initialTagId?.let(viewModel::selectTag)
        initialCategoryId?.let(viewModel::selectCategory)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { NavigationTitle(TopLevelDestination.RECIPES.icon, "Recipes") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            Box {
                ExtendedFloatingActionButton(
                    onClick = { addMenuExpanded = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add recipe") },
                )
                DropdownMenu(
                    expanded = addMenuExpanded,
                    onDismissRequest = { addMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("New recipe") },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        onClick = {
                            addMenuExpanded = false
                            onCreateClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Import from URL") },
                        leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                        onClick = {
                            addMenuExpanded = false
                            importDialogVisible = true
                        },
                    )
                }
            }
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

                if (uiState.categories.isNotEmpty() || uiState.tags.isNotEmpty()) {
                    RecipeFilterButton(
                        selectedFilterCount =
                            listOfNotNull(
                                    uiState.selectedCategoryId,
                                    uiState.selectedTagId,
                                )
                                .size,
                        onClick = { filterSheetVisible = true },
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
                            else
                                "Recipes appear here automatically once Syncwich syncs with your Mealie server.",
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
                                searchQuery = uiState.searchQuery,
                                onClick = { onRecipeClick(recipe) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (filterSheetVisible) {
        RecipeFilterSheet(
            categories = uiState.categories,
            tags = uiState.tags,
            selectedCategoryId = uiState.selectedCategoryId,
            selectedTagId = uiState.selectedTagId,
            onCategorySelected = viewModel::onCategorySelected,
            onTagSelected = viewModel::onTagSelected,
            onClearFilters = viewModel::clearFilters,
            onDismiss = { filterSheetVisible = false },
        )
    }

    if (importDialogVisible) {
        AlertDialog(
            onDismissRequest = { importDialogVisible = false },
            title = { Text("Import recipe from URL") },
            text = {
                OutlinedTextField(
                    value = importUrl,
                    onValueChange = { importUrl = it },
                    label = { Text("Recipe URL") },
                    placeholder = { Text("https://example.com/recipe") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = importUrl.trim().startsWith("http://") ||
                        importUrl.trim().startsWith("https://"),
                    onClick = {
                        val url = importUrl.trim()
                        importDialogVisible = false
                        importUrl = ""
                        onImportUrlClick(url)
                    },
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { importDialogVisible = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
internal fun RecipeFilterButton(
    selectedFilterCount: Int,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("recipe-search-filter-button"),
    ) {
        Icon(Icons.Filled.FilterList, contentDescription = null)
        Text(
            text = recipeFilterButtonLabel(selectedFilterCount),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

internal fun recipeFilterButtonLabel(selectedFilterCount: Int): String =
    if (selectedFilterCount == 0) "Filters" else "Filters ($selectedFilterCount)"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecipeFilterSheet(
    categories: List<CategoryEntity>,
    tags: List<TagEntity>,
    selectedCategoryId: String?,
    selectedTagId: String?,
    onCategorySelected: (String) -> Unit,
    onTagSelected: (String) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Filter recipes",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.testTag("recipe-filter-sheet-title"),
            )
            if (categories.isNotEmpty()) {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 24.dp),
                )
                FilterChipRow(
                    entries = categories,
                    key = { it.id },
                    label = { it.name },
                    selectedId = selectedCategoryId,
                    onSelected = onCategorySelected,
                    leadingIcon = { CategoryFilterIcon() },
                    contentPadding = PaddingValues(vertical = 4.dp),
                )
            }
            if (tags.isNotEmpty()) {
                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
                FilterChipRow(
                    entries = tags,
                    key = { it.id },
                    label = { it.name },
                    selectedId = selectedTagId,
                    onSelected = onTagSelected,
                    leadingIcon = { TagFilterIcon() },
                    contentPadding = PaddingValues(vertical = 4.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                if (selectedCategoryId != null || selectedTagId != null) {
                    TextButton(
                        onClick = onClearFilters,
                        modifier = Modifier.testTag("recipe-filter-clear-button"),
                    ) {
                        Text("Clear filters")
                    }
                }
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        }
    }
}

@Composable
private fun TagFilterIcon() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.Label,
        contentDescription = null,
        modifier = Modifier.size(18.dp).testTag("recipe-search-tag-icon"),
    )
}

@Composable
private fun CategoryFilterIcon() {
    Icon(
        imageVector = Icons.Filled.Category,
        contentDescription = null,
        modifier = Modifier.size(18.dp).testTag("recipe-search-category-icon"),
    )
}

@Composable
private fun <T> FilterChipRow(
    entries: List<T>,
    key: (T) -> String,
    label: (T) -> String,
    selectedId: String?,
    onSelected: (String) -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(entries, key = key) { entry ->
            val id = key(entry)
            FilterChip(
                selected = selectedId == id,
                onClick = { onSelected(id) },
                label = { Text(label(entry)) },
                leadingIcon = leadingIcon,
            )
        }
    }
}

@Composable
internal fun RecipeCard(
    recipe: RecipeSummaryEntity,
    serverUrl: String,
    searchQuery: String = "",
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
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
                    text =
                        highlightedSearchText(
                            recipe.name,
                            searchQuery,
                            SpanStyle(
                                background = MaterialTheme.colorScheme.tertiaryContainer,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            ),
                        ),
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

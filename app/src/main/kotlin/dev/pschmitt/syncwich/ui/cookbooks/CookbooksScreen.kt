package dev.pschmitt.syncwich.ui.cookbooks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.pschmitt.syncwich.data.api.recipeImageUrl
import dev.pschmitt.syncwich.data.db.entity.CookbookEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.ui.common.PlaceholderScreen
import dev.pschmitt.syncwich.ui.common.RefreshErrorBanner
import dev.pschmitt.syncwich.ui.common.SearchField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookbooksScreen(
    modifier: Modifier = Modifier,
    onCookbookClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewModel: CookbooksViewModel = hiltViewModel(),
) {
    val cookbooks by viewModel.cookbooks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val recipePreviews by viewModel.recipePreviews.collectAsStateWithLifecycle()
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Cookbooks") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
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
            Column(modifier = Modifier.fillMaxSize()) {
                RefreshErrorBanner(
                    errorMessage = refreshState.errorMessage,
                    onRetry = viewModel::refresh,
                )
                SearchField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = "Search cookbooks",
                    modifier =
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                if (cookbooks.isEmpty()) {
                    PlaceholderScreen(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        title =
                            when {
                                refreshState.isRefreshing -> "Loading cookbooks"
                                searchQuery.isNotBlank() -> "No cookbooks match"
                                else -> "No cookbooks yet"
                            },
                        subtitle =
                            if (searchQuery.isBlank()) {
                                "Cookbooks you curate in Mealie will show up here once synced."
                            } else {
                                "Try a different name or description."
                            },
                        modifier = Modifier.fillMaxSize(),
                        isLoading = refreshState.isRefreshing,
                        onRetry = viewModel::refresh,
                    )
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(cookbookGridColumnCount(maxWidth.value.toInt())),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(COOKBOOK_GRID_PADDING_DP.dp),
                            horizontalArrangement =
                                Arrangement.spacedBy(COOKBOOK_GRID_SPACING_DP.dp),
                            verticalArrangement = Arrangement.spacedBy(COOKBOOK_GRID_SPACING_DP.dp),
                        ) {
                            items(cookbooks, key = { it.id }) { cookbook ->
                                CookbookCard(
                                    cookbook = cookbook,
                                    recipes = recipePreviews[cookbook.id].orEmpty(),
                                    serverUrl = viewModel.serverUrl,
                                    onClick = { onCookbookClick(cookbook.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CookbookCard(
    cookbook: CookbookEntity,
    recipes: List<RecipeSummaryEntity>,
    serverUrl: String,
    onClick: () -> Unit,
) {
    val previewRecipes = filterRecipePreviewsWithImages(recipes, serverUrl)

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        if (previewRecipes.isNotEmpty()) {
            CookbookPreviewCarousel(
                recipes = previewRecipes.take(PREVIEW_RECIPE_LIMIT),
                serverUrl = serverUrl,
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            if (previewRecipes.isEmpty()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = cookbook.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (cookbook.description.isNotBlank()) {
                Text(
                    text = cookbook.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun CookbookPreviewCarousel(recipes: List<RecipeSummaryEntity>, serverUrl: String) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(recipes, key = { it.id }) { recipe ->
            Box(
                modifier =
                    Modifier.size(
                            width = COOKBOOK_PREVIEW_TILE_WIDTH_DP.dp,
                            height = COOKBOOK_PREVIEW_TILE_HEIGHT_DP.dp,
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                recipeImageUrl(serverUrl, recipe.id, recipe.image)?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = recipe.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

fun filterRecipePreviewsWithImages(
    recipes: List<RecipeSummaryEntity>,
    serverUrl: String,
): List<RecipeSummaryEntity> =
    recipes.filter { recipe -> recipeImageUrl(serverUrl, recipe.id, recipe.image) != null }

private const val PREVIEW_RECIPE_LIMIT = 5

internal const val COOKBOOK_GRID_MIN_CARD_WIDTH_DP = 220
internal const val COOKBOOK_GRID_PADDING_DP = 16
internal const val COOKBOOK_GRID_SPACING_DP = 12
internal const val COOKBOOK_PREVIEW_TILE_WIDTH_DP = 144
internal const val COOKBOOK_PREVIEW_TILE_HEIGHT_DP = 108

internal fun cookbookGridColumnCount(availableWidthDp: Int): Int =
    ((availableWidthDp - (2 * COOKBOOK_GRID_PADDING_DP) + COOKBOOK_GRID_SPACING_DP) /
            (COOKBOOK_GRID_MIN_CARD_WIDTH_DP + COOKBOOK_GRID_SPACING_DP))
        .coerceAtLeast(1)

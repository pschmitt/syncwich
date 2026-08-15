package dev.pschmitt.syncwich.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.pschmitt.syncwich.data.api.recipeImageUrl
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.ui.common.NavigationTitle
import dev.pschmitt.syncwich.ui.common.PlaceholderScreen
import dev.pschmitt.syncwich.ui.common.RefreshErrorBanner
import dev.pschmitt.syncwich.ui.navigation.TopLevelDestination
import dev.pschmitt.syncwich.ui.recipes.formatRating

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRecipeClick: (RecipeSummaryEntity) -> Unit,
    onRecipesClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasCachedRecipes =
        uiState.recentlyViewedRecipes.isNotEmpty() ||
            uiState.recentlyAddedRecipes.isNotEmpty() ||
            uiState.recentlyCookedRecipes.isNotEmpty() ||
            uiState.favoriteRecipes.isNotEmpty()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { NavigationTitle(TopLevelDestination.HOME.icon, "Home") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.refreshState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item { HomeSyncStatusCard(status = uiState.syncStatus) }
                item {
                    RefreshErrorBanner(
                        errorMessage = uiState.refreshState.errorMessage,
                        onRetry = viewModel::refresh,
                    )
                }
                if (!hasCachedRecipes && !uiState.refreshState.isRefreshing) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(240.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            PlaceholderScreen(
                                icon = Icons.Filled.Home,
                                title = "Your recipe home",
                                subtitle =
                                    "Syncwich will show recent and favorite recipes here. Pull to refresh or browse the recipe library.",
                                modifier = Modifier.fillMaxSize(),
                                onRetry = viewModel::refresh,
                            )
                        }
                    }
                }
                if (uiState.recentlyViewedRecipes.isNotEmpty()) {
                    item {
                        RecipeSection(
                            title = "Recently viewed",
                            icon = Icons.Filled.History,
                            iconTestTag = "recently-viewed",
                            recipes = uiState.recentlyViewedRecipes,
                            serverUrl = uiState.serverUrl,
                            onRecipeClick = onRecipeClick,
                            onSeeAll = onRecipesClick,
                        )
                    }
                }
                if (uiState.recentlyAddedRecipes.isNotEmpty()) {
                    item {
                        RecipeSection(
                            title = "Recently added",
                            icon = Icons.Filled.NewReleases,
                            iconTestTag = "recently-added",
                            recipes = uiState.recentlyAddedRecipes,
                            serverUrl = uiState.serverUrl,
                            onRecipeClick = onRecipeClick,
                            onSeeAll = onRecipesClick,
                        )
                    }
                }
                if (uiState.recentlyCookedRecipes.isNotEmpty()) {
                    item {
                        RecipeSection(
                            title = "Cooked recently",
                            icon = Icons.Filled.RestaurantMenu,
                            iconTestTag = "cooked-recently",
                            recipes = uiState.recentlyCookedRecipes,
                            serverUrl = uiState.serverUrl,
                            onRecipeClick = onRecipeClick,
                            onSeeAll = onRecipesClick,
                        )
                    }
                }
                if (uiState.favoriteRecipes.isNotEmpty()) {
                    item {
                        FavoriteSection(
                            recipes = uiState.favoriteRecipes,
                            serverUrl = uiState.serverUrl,
                            onRecipeClick = onRecipeClick,
                            onOpenFavorites = onFavoritesClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeSection(
    title: String,
    icon: ImageVector,
    iconTestTag: String,
    recipes: List<RecipeSummaryEntity>,
    serverUrl: String,
    onRecipeClick: (RecipeSummaryEntity) -> Unit,
    onSeeAll: () -> Unit,
) {
    Column {
        HomeSectionHeader(
            title = title,
            icon = icon,
            iconTestTag = iconTestTag,
            actionLabel = "View all recipes",
            onAction = onSeeAll,
        )
        Spacer(Modifier.height(8.dp))
        RecipeRow(
            recipes = recipes,
            serverUrl = serverUrl,
            onRecipeClick = onRecipeClick,
        )
    }
}

@Composable
private fun FavoriteSection(
    recipes: List<RecipeSummaryEntity>,
    serverUrl: String,
    onRecipeClick: (RecipeSummaryEntity) -> Unit,
    onOpenFavorites: () -> Unit,
) {
    Column {
        HomeSectionHeader(
            title = "Favorites",
            icon = Icons.Filled.Favorite,
            iconTestTag = "favorites",
            actionLabel = "View all favorites",
            onAction = onOpenFavorites,
        )
        Spacer(Modifier.height(8.dp))
        RecipeRow(
            recipes = recipes,
            serverUrl = serverUrl,
            onRecipeClick = onRecipeClick,
        )
    }
}

@Composable
internal fun HomeSectionHeader(
    title: String,
    icon: ImageVector,
    iconTestTag: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp).testTag("home-section-icon-$iconTestTag"),
        )
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
private fun RecipeRow(
    recipes: List<RecipeSummaryEntity>,
    serverUrl: String,
    onRecipeClick: (RecipeSummaryEntity) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(end = 8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(recipes, key = { it.id }) { recipe ->
            HomeRecipeCard(
                recipe = recipe,
                serverUrl = serverUrl,
                onClick = { onRecipeClick(recipe) },
            )
        }
    }
}

@Composable
internal fun HomeRecipeCard(
    recipe: RecipeSummaryEntity,
    serverUrl: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier =
            Modifier.width(184.dp).height(HOME_RECIPE_CARD_HEIGHT).testTag("home-recipe-card"),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(124.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                val imageUrl = recipeImageUrl(serverUrl, recipe.id, recipe.image)
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = recipe.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Icons.Filled.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    recipe.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                recipe.rating?.let { rating ->
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            " ${formatRating(rating)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

internal val HOME_RECIPE_CARD_HEIGHT = 244.dp

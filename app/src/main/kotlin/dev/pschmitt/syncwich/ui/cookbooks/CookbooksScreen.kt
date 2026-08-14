package dev.pschmitt.syncwich.ui.cookbooks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            SearchField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                placeholder = "Search cookbooks",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (cookbooks.isEmpty()) {
                PlaceholderScreen(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = if (searchQuery.isBlank()) "No cookbooks yet" else "No cookbooks match",
                    subtitle =
                        if (searchQuery.isBlank()) {
                            "Cookbooks you curate in Mealie will show up here once synced."
                        } else {
                            "Try a different name or description."
                        },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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

@Composable
private fun CookbookCard(
    cookbook: CookbookEntity,
    recipes: List<RecipeSummaryEntity>,
    serverUrl: String,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        if (recipes.isNotEmpty()) {
            CookbookPreviewCarousel(recipes = recipes.take(PREVIEW_RECIPE_LIMIT), serverUrl = serverUrl)
        }
        Column(modifier = Modifier.padding(16.dp)) {
            if (recipes.isEmpty()) {
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
                    Modifier.size(width = 96.dp, height = 76.dp)
                        .clip(RoundedCornerShape(12.dp))
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
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private const val PREVIEW_RECIPE_LIMIT = 5

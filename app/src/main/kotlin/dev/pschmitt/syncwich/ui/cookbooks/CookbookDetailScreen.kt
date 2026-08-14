package dev.pschmitt.syncwich.ui.cookbooks

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Edit
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
    viewModel: CookbookDetailViewModel = hiltViewModel(),
) {
    val cookbook by viewModel.cookbook.collectAsStateWithLifecycle()
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()
    val serverUrl = viewModel.serverUrl
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()

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
                        IconButton(onClick = { onEditClick(currentCookbook.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit cookbook")
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
                        errorMessage = refreshState.errorMessage,
                        onRetry = viewModel::refresh,
                    )
                    PlaceholderScreen(
                        icon = Icons.Filled.Restaurant,
                        title =
                            if (refreshState.isRefreshing) "Loading recipes"
                            else "No recipes yet",
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
                        errorMessage = refreshState.errorMessage,
                        onRetry = viewModel::refresh,
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
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

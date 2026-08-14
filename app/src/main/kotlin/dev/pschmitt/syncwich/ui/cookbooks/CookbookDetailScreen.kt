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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.ui.common.PlaceholderScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookbookDetailScreen(
    modifier: Modifier = Modifier,
    onRecipeClick: (String) -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: CookbookDetailViewModel = hiltViewModel(),
) {
    val cookbook by viewModel.cookbook.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    val serverUrl = viewModel.serverUrl

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
            )
        },
    ) { innerPadding ->
        if (recipes.isEmpty()) {
            PlaceholderScreen(
                icon = Icons.Filled.Restaurant,
                title = "No recipes yet",
                subtitle =
                    cookbook?.description?.takeIf { it.isNotBlank() }
                        ?: "This cookbook has no matching recipes synced yet.",
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(recipes, key = { it.id }) { recipe ->
                CookbookRecipeRow(
                    recipe = recipe,
                    serverUrl = serverUrl,
                    onClick = { onRecipeClick(recipe.id) },
                )
            }
        }
    }
}

@Composable
private fun CookbookRecipeRow(recipe: RecipeSummaryEntity, serverUrl: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = recipe.coverImageUrl(serverUrl),
                contentDescription = null,
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

/**
 * `image` is a cache-busting version string, not a URL - see [RecipeSummaryEntity]'s kdoc for the
 * confirmed live path. Appended as a query param purely so Coil's URL-keyed cache refetches once
 * Mealie's own image version changes; it isn't sent as an API request anywhere.
 */
private fun RecipeSummaryEntity.coverImageUrl(serverUrl: String): String? =
    image?.let { version -> "$serverUrl/api/media/recipes/$id/images/min-original.webp?v=$version" }

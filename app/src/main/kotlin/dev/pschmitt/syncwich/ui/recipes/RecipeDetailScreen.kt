package dev.pschmitt.syncwich.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import dev.pschmitt.syncwich.data.api.dto.RecipeDetailDto
import dev.pschmitt.syncwich.data.api.dto.RecipeIngredientDto
import dev.pschmitt.syncwich.data.api.dto.RecipeInstructionDto
import dev.pschmitt.syncwich.data.api.dto.RecipeNutritionDto
import dev.pschmitt.syncwich.data.api.recipeImageUrl
import dev.pschmitt.syncwich.data.image.isSafeRecipeImageUrl
import dev.pschmitt.syncwich.ui.common.PlaceholderScreen
import dev.pschmitt.syncwich.ui.common.RefreshErrorBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()
    val title = (uiState as? RecipeDetailUiState.Loaded)?.recipe?.name ?: "Recipe"

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            when (val state = uiState) {
                is RecipeDetailUiState.Loading -> {
                    PlaceholderScreen(
                        icon = Icons.Filled.Restaurant,
                        title = "Loading recipe",
                        subtitle = "Checking your saved recipe and refreshing it if possible.",
                        modifier = Modifier.fillMaxSize(),
                        isLoading = true,
                    )
                }
                is RecipeDetailUiState.Unavailable -> {
                    PlaceholderScreen(
                        icon = Icons.Filled.Restaurant,
                        title = "Recipe unavailable offline",
                        subtitle =
                            "This recipe is not saved on this device yet. Connect to Mealie and try again.",
                        modifier = Modifier.fillMaxSize(),
                        onRetry = viewModel::refresh,
                    )
                }
                is RecipeDetailUiState.Loaded -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RefreshErrorBanner(
                            errorMessage = state.refreshError,
                            onRetry = viewModel::refresh,
                        )
                        RecipeDetailContent(
                            recipe = state.recipe,
                            serverUrl = state.serverUrl,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeDetailContent(
    recipe: RecipeDetailDto,
    serverUrl: String,
    modifier: Modifier = Modifier,
) {
    val imageUrl = recipeImageUrl(serverUrl, recipe.id, recipe.image)

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        if (imageUrl != null) {
            item {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = recipe.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = recipe.name, style = MaterialTheme.typography.headlineSmall)

                val times =
                    listOfNotNull(
                        recipe.prepTime?.let { "Prep" to it },
                        recipe.cookTime?.let { "Cook" to it },
                        recipe.performTime?.let { "Active" to it },
                        recipe.totalTime?.let { "Total" to it },
                    )
                if (recipe.rating != null || times.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        recipe.rating?.let { rating ->
                            LabeledRow(
                                icon = Icons.Filled.Star,
                                label = "Rating",
                                value = "${formatRating(rating)} / 5",
                            )
                        }
                        times.forEach { (label, value) ->
                            LabeledRow(icon = Icons.Filled.Schedule, label = label, value = value)
                        }
                    }
                }

                if (!recipe.description.isNullOrBlank()) {
                    Markdown(
                        content = recipe.description,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    )
                }
            }
        }

        if (recipe.recipeIngredient.isNotEmpty()) {
            item { SectionHeader(icon = Icons.Filled.Checklist, title = "Ingredients") }
            items(recipe.recipeIngredient) { ingredient -> IngredientRow(ingredient) }
        }

        if (recipe.recipeInstructions.isNotEmpty()) {
            item { SectionHeader(icon = Icons.AutoMirrored.Filled.ListAlt, title = "Steps") }
            itemsIndexed(recipe.recipeInstructions) { index, instruction ->
                InstructionRow(index + 1, instruction)
            }
        }

        if (recipe.nutrition != null && recipe.nutrition.hasAnyValue()) {
            item { SectionHeader(icon = Icons.Filled.LocalFireDepartment, title = "Nutrition") }
            item { NutritionSection(recipe.nutrition) }
        }

        if (recipe.notes.isNotEmpty()) {
            item { SectionHeader(icon = Icons.AutoMirrored.Filled.StickyNote2, title = "Notes") }
            items(recipe.notes) { note ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    if (!note.title.isNullOrBlank()) {
                        Text(text = note.title, style = MaterialTheme.typography.titleSmall)
                    }
                    if (!note.text.isNullOrBlank()) {
                        Text(text = note.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun LabeledRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun IngredientRow(ingredient: RecipeIngredientDto) {
    val text =
        ingredient.display?.takeIf { it.isNotBlank() }
            ?: ingredient.note?.takeIf { it.isNotBlank() }
    if (text.isNullOrBlank()) return
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(text = "•", modifier = Modifier.padding(end = 8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun InstructionRow(number: Int, instruction: RecipeInstructionDto) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 12.dp),
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!instruction.title.isNullOrBlank()) {
                Text(text = instruction.title, style = MaterialTheme.typography.titleSmall)
            }
            Markdown(
                content = instruction.text,
                imageTransformer = SafeRecipeImageTransformer,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Keeps the Markdown renderer from attempting relative, malformed, or non-HTTP image targets. */
private object SafeRecipeImageTransformer : ImageTransformer {
    @Composable
    override fun transform(link: String): ImageData? =
        if (isSafeRecipeImageUrl(link)) Coil3ImageTransformerImpl.transform(link) else null

    @Composable
    override fun intrinsicSize(painter: Painter): Size =
        Coil3ImageTransformerImpl.intrinsicSize(painter)
}

@Composable
private fun NutritionSection(nutrition: RecipeNutritionDto) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        nutritionRows(nutrition).forEach { (label, value) ->
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Text(text = label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun nutritionRows(nutrition: RecipeNutritionDto): List<Pair<String, String>> =
    listOfNotNull(
        nutrition.calories?.let { "Calories" to it },
        nutrition.proteinContent?.let { "Protein" to it },
        nutrition.carbohydrateContent?.let { "Carbohydrates" to it },
        nutrition.fatContent?.let { "Fat" to it },
        nutrition.saturatedFatContent?.let { "Saturated fat" to it },
        nutrition.unsaturatedFatContent?.let { "Unsaturated fat" to it },
        nutrition.transFatContent?.let { "Trans fat" to it },
        nutrition.sugarContent?.let { "Sugar" to it },
        nutrition.fiberContent?.let { "Fiber" to it },
        nutrition.sodiumContent?.let { "Sodium" to it },
        nutrition.cholesterolContent?.let { "Cholesterol" to it },
    )

private fun RecipeNutritionDto.hasAnyValue(): Boolean = nutritionRows(this).isNotEmpty()

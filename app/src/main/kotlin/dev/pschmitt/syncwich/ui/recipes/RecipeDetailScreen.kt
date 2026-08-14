package dev.pschmitt.syncwich.ui.recipes

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import dev.pschmitt.syncwich.data.api.dto.RecipeDetailDto
import dev.pschmitt.syncwich.data.api.dto.RecipeIngredientDto
import dev.pschmitt.syncwich.data.api.dto.RecipeInstructionDto
import dev.pschmitt.syncwich.data.api.dto.RecipeNutritionDto
import dev.pschmitt.syncwich.data.db.entity.CookbookEntity
import dev.pschmitt.syncwich.data.image.RecipeImageReference
import dev.pschmitt.syncwich.data.image.isSafeRecipeImageUrl
import dev.pschmitt.syncwich.ui.common.PlaceholderScreen
import dev.pschmitt.syncwich.ui.common.RefreshErrorBanner
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    onBack: () -> Unit,
    onOpenTimeline: (String) -> Unit,
    onOpenCookbook: (String) -> Unit = {},
    onOpenTag: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    onEditClick: (recipeId: String, slug: String) -> Unit = { _, _ -> },
    onDeleted: () -> Unit = {},
    viewModel: RecipeDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()
            val deleteState by viewModel.deleteState.collectAsStateWithLifecycle()
    val loadedState = uiState as? RecipeDetailUiState.Loaded
    val title = loadedState?.recipe?.name ?: "Recipe"
    var overflowExpanded by remember { mutableStateOf(false) }
    var deleteDialogVisible by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(deleteState) {
        if (deleteState is RecipeDeleteUiState.Deleted) onDeleted()
    }

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
                actions = {
                    if (loadedState != null) {
                        IconButton(
                            enabled = deleteState !is RecipeDeleteUiState.Deleting,
                            onClick = {
                                onEditClick(loadedState.recipe.id, loadedState.recipe.slug)
                            }
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit recipe")
                        }
                        Box {
                            IconButton(onClick = { overflowExpanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More actions")
                            }
                            RecipeOverflowMenu(
                                expanded = overflowExpanded,
                                actions = loadedState.actions,
                                onDismiss = { overflowExpanded = false },
                                onFavoriteClick = { viewModel.setFavorite(it) },
                                onMadeThisClick = viewModel::recordMadeThis,
                                onOpenTimelineClick = { onOpenTimeline(loadedState.recipe.id) },
                                onShareClick = {
                                    shareRecipe(
                                        context,
                                        loadedState.recipe.name,
                                        recipeWebUrl(
                                            loadedState.serverUrl,
                                            loadedState.recipe.slug,
                                        ),
                                    )
                                },
                                onOpenBrowserClick = {
                                    openRecipeInBrowser(
                                        context,
                                        recipeWebUrl(
                                            loadedState.serverUrl,
                                            loadedState.recipe.slug,
                                        ),
                                    )
                                },
                                onDeleteClick = { deleteDialogVisible = true },
                            )
                        }
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
                            errorMessage =
                                (deleteState as? RecipeDeleteUiState.Failed)?.message
                                    ?: state.refreshError,
                            onRetry =
                                if (deleteState is RecipeDeleteUiState.Failed) {
                                    viewModel::retryDelete
                                } else {
                                    viewModel::refresh
                                },
                        )
                        RecipeDetailContent(
                            recipe = state.recipe,
                            imageIndex = state.imageIndex,
                            actions = state.actions,
                            completedStepIndexes = state.completedStepIndexes,
                            cookbooks = state.cookbooks,
                            ingredientChecklistEnabled = state.ingredientChecklistEnabled,
                            onRatingSelected = viewModel::setRating,
                            onStepCompleted = viewModel::setStepCompleted,
                            onOpenCookbook = onOpenCookbook,
                            onOpenTag = onOpenTag,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    if (deleteDialogVisible && loadedState != null) {
        RecipeDeleteConfirmationDialog(
            recipeName = loadedState.recipe.name,
            isDeleting = deleteState is RecipeDeleteUiState.Deleting,
            errorMessage = (deleteState as? RecipeDeleteUiState.Failed)?.message,
            onConfirm = {
                viewModel.deleteRecipe(loadedState.recipe.id, loadedState.recipe.slug)
            },
            onDismiss = { deleteDialogVisible = false },
        )
    }
}

@Composable
internal fun RecipeOverflowMenu(
    expanded: Boolean,
    actions: RecipeActionUiState,
    onDismiss: () -> Unit,
    onFavoriteClick: (Boolean) -> Unit,
    onMadeThisClick: () -> Unit,
    onOpenTimelineClick: () -> Unit,
    onShareClick: () -> Unit,
    onOpenBrowserClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(if (actions.isFavorite) "Remove favorite" else "Favorite") },
            leadingIcon = {
                Icon(
                    if (actions.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                )
            },
            onClick = {
                onDismiss()
                onFavoriteClick(!actions.isFavorite)
            },
        )
        DropdownMenuItem(
            text = { Text("I made this") },
            leadingIcon = { Icon(Icons.Filled.Checklist, contentDescription = null) },
            onClick = {
                onDismiss()
                onMadeThisClick()
            },
        )
        DropdownMenuItem(
            text = { Text("Show timeline") },
            leadingIcon = { Icon(Icons.Filled.Timeline, contentDescription = null) },
            onClick = {
                onDismiss()
                onOpenTimelineClick()
            },
        )
        DropdownMenuItem(
            text = { Text("Share") },
            leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
            onClick = {
                onDismiss()
                onShareClick()
            },
        )
        DropdownMenuItem(
            text = { Text("Open in browser") },
            leadingIcon = { Icon(Icons.Filled.OpenInBrowser, contentDescription = null) },
            onClick = {
                onDismiss()
                onOpenBrowserClick()
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
private fun RecipeDetailContent(
    recipe: RecipeDetailDto,
    imageIndex: RecipeImageIndex,
    actions: RecipeActionUiState,
    cookbooks: List<CookbookEntity>,
    completedStepIndexes: Set<Int>,
    ingredientChecklistEnabled: Boolean,
    onRatingSelected: (Int) -> Unit,
    onStepCompleted: (Int, Boolean) -> Unit,
    onOpenCookbook: (String) -> Unit,
    onOpenTag: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageUrl = imageIndex.coverUrl
    val viewerImages = remember(recipe.name, imageIndex) { recipeViewerImages(recipe.name, imageIndex) }
    var viewerPage by rememberSaveable { mutableStateOf<Int?>(null) }
    var stepsFullScreen by rememberSaveable { mutableStateOf(false) }

    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        if (imageUrl != null) {
            item {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = recipe.name,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(220.dp)
                            .clickable {
                                viewerPage =
                                    viewerImages.indexOfFirst { it.url == imageUrl }.coerceAtLeast(0)
                            }
                            .semantics { contentDescription = "Open recipe images" },
                )
            }
        }

        if (recipe.tags.isNotEmpty() || cookbooks.isNotEmpty()) {
            item {
                RecipeMetadataCard(
                    tags = recipe.tags,
                    cookbooks = cookbooks,
                    onOpenTag = onOpenTag,
                    onOpenCookbook = onOpenCookbook,
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val times =
                            listOfNotNull(
                                recipe.prepTime?.let { "Prep" to it },
                                recipe.cookTime?.let { "Cook" to it },
                                recipe.performTime?.let { "Active" to it },
                                recipe.totalTime?.let { "Total" to it },
                            )
                        if (times.isNotEmpty()) {
                            Column(modifier = Modifier.weight(1f)) {
                                times.forEach { (label, value) ->
                                    LabeledRow(
                                        icon = Icons.Filled.Schedule,
                                        label = label,
                                        value = value,
                                    )
                                }
                            }
                        }
                        RecipeActionControls(
                            actions = actions,
                            globalRating = recipe.rating,
                            onRatingSelected = onRatingSelected,
                            compact = true,
                            modifier = Modifier.weight(0.82f),
                        )
                    }

                    if (!recipe.description.isNullOrBlank()) {
                        Markdown(
                            content = recipe.description,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        )
                    }
                }
            }
        }

        if (recipe.recipeIngredient.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column {
                        SectionHeader(icon = Icons.Filled.Checklist, title = "Ingredients")
                        recipe.recipeIngredient.forEach { ingredient ->
                            IngredientRow(ingredient, checklistEnabled = ingredientChecklistEnabled)
                        }
                    }
                }
            }
        }

        if (recipe.recipeInstructions.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column {
                        SectionHeader(
                            icon = Icons.AutoMirrored.Filled.ListAlt,
                            title = "Steps",
                            action = {
                                IconButton(onClick = { stepsFullScreen = true }) {
                                    Icon(
                                        Icons.Filled.Fullscreen,
                                        contentDescription = "Open steps full screen",
                                    )
                                }
                            },
                        )
                        recipe.recipeInstructions.forEachIndexed { index, instruction ->
                            InstructionRow(
                                number = index + 1,
                                completed = index in completedStepIndexes,
                                instruction = instruction,
                                imageReferences =
                                    imageIndex.instructionReferences.getOrNull(index).orEmpty(),
                                onCompletedChange = { onStepCompleted(index, it) },
                                onImageClick = {
                                    viewerPage =
                                        viewerImages
                                            .indexOfFirst { image -> image.url == it }
                                            .coerceAtLeast(0)
                                },
                            )
                        }
                    }
                }
            }
        }

        if (recipe.nutrition != null && recipe.nutrition.hasAnyValue()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column {
                        SectionHeader(icon = Icons.Filled.LocalFireDepartment, title = "Nutrition")
                        NutritionSection(recipe.nutrition)
                    }
                }
            }
        }

        if (recipe.notes.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column {
                        SectionHeader(icon = Icons.AutoMirrored.Filled.StickyNote2, title = "Notes")
                        recipe.notes.forEach { note ->
                            Column(
                                modifier =
                                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
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
        }
    }

    viewerPage?.let { page ->
        RecipeImageViewer(
            images = viewerImages,
            initialPage = page,
            onDismiss = { viewerPage = null },
        )
    }

    if (stepsFullScreen) {
        FullScreenStepsDialog(
            recipeName = recipe.name,
            instructions = recipe.recipeInstructions,
            imageReferences = imageIndex.instructionReferences,
            completedStepIndexes = completedStepIndexes,
            onStepCompleted = onStepCompleted,
            onDismiss = { stepsFullScreen = false },
        )
    }
}

@Composable
internal fun RecipeMetadataCard(
    tags: List<dev.pschmitt.syncwich.data.api.dto.OrganizerDto>,
    cookbooks: List<CookbookEntity>,
    onOpenTag: (String) -> Unit,
    onOpenCookbook: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Recipe details", style = MaterialTheme.typography.titleMedium)
            if (tags.isNotEmpty()) {
                Text("Tags", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.forEach { tag ->
                        AssistChip(
                            onClick = { onOpenTag(tag.id) },
                            label = { Text(tag.name) },
                        )
                    }
                }
            }
            if (cookbooks.isNotEmpty()) {
                Text("Cookbooks", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    cookbooks.forEach { cookbook ->
                        AssistChip(
                            onClick = { onOpenCookbook(cookbook.id) },
                            label = { Text(cookbook.name) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun RecipeActionControls(
    actions: RecipeActionUiState,
    globalRating: Double? = null,
    onRatingSelected: (Int) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var ratingDialogVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 4.dp else 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .clickable { ratingDialogVisible = true }
                    .semantics { contentDescription = "Open rating dialog" }
                    .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector =
                    if (globalRating == null) Icons.Filled.StarBorder else Icons.Filled.Star,
                contentDescription = if (globalRating == null) "No ratings yet" else null,
                tint = MaterialTheme.colorScheme.primary,
            )
            globalRating?.let {
                Text(
                    text = "${formatRating(it)} / 5",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        if (ratingDialogVisible) {
            RecipeRatingDialog(
                globalRating = globalRating,
                selectedRating = actions.rating,
                onRatingSelected = {
                    onRatingSelected(it)
                    ratingDialogVisible = false
                },
                onDismiss = { ratingDialogVisible = false },
            )
        }

        if (actions.favoritePending || actions.ratingPending || actions.madeThisPending) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Saved offline; sync pending",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun RecipeRatingDialog(
    globalRating: Double?,
    selectedRating: Int?,
    onRatingSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate this recipe") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (globalRating != null) {
                    Text(
                        text = "Overall rating: ${formatRating(globalRating)} / 5",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.StarBorder,
                        contentDescription = "No overall rating",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Your rating",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { onRatingSelected(star) },
                            modifier = Modifier.size(44.dp),
                        ) {
                            Icon(
                                imageVector =
                                    if (star <= (selectedRating ?: 0)) Icons.Filled.Star
                                    else Icons.Filled.StarBorder,
                                contentDescription = ratingContentDescription(star),
                                tint =
                                    if (star <= (selectedRating ?: 0)) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

internal fun ratingContentDescription(star: Int): String {
    require(star in 1..5) { "Recipe rating must be between 1 and 5" }
    return "Rate $star out of 5 stars"
}

@Composable
internal fun RecipeDeleteConfirmationDialog(
    recipeName: String,
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
        title = { Text("Delete recipe?") },
        text = {
            Column {
                Text("Delete \"$recipeName\" from Mealie? This cannot be undone.")
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

fun recipeImageGalleryUrls(serverUrl: String, recipe: RecipeDetailDto): List<String> =
    recipeImageIndex(serverUrl, recipe).galleryUrls

internal data class RecipeViewerImage(
    val url: String,
    val title: String,
    val sourceLabel: String,
    val altText: String? = null,
)

internal data class ImageDimensions(val width: Int, val height: Int)

internal fun recipeViewerImages(
    recipeName: String,
    imageIndex: RecipeImageIndex,
): List<RecipeViewerImage> {
    val images = linkedMapOf<String, RecipeViewerImage>()
    imageIndex.coverUrl?.let { url ->
        images[url] =
            RecipeViewerImage(
                url = url,
                title = recipeName,
                sourceLabel = "Recipe cover",
            )
    }
    imageIndex.instructionReferences.forEachIndexed { instructionIndex, references ->
        references.forEach { reference ->
            images.putIfAbsent(
                reference.url,
                RecipeViewerImage(
                    url = reference.url,
                    title = recipeName,
                    sourceLabel = "Step ${instructionIndex + 1} image",
                    altText = reference.altText,
                ),
            )
        }
    }
    imageIndex.galleryUrls.forEach { url ->
        images.putIfAbsent(
            url,
            RecipeViewerImage(url = url, title = recipeName, sourceLabel = "Recipe image"),
        )
    }
    return images.values.toList()
}

internal fun stepViewerImages(
    recipeName: String,
    imageReferences: List<List<RecipeImageReference>>,
): List<RecipeViewerImage> =
    imageReferences
        .flatMapIndexed { instructionIndex, references ->
            references.map { reference ->
                RecipeViewerImage(
                    url = reference.url,
                    title = recipeName,
                    sourceLabel = "Step ${instructionIndex + 1} image",
                    altText = reference.altText,
                )
            }
        }
        .distinctBy(RecipeViewerImage::url)

internal fun imageMetadataRows(
    image: RecipeViewerImage,
    dimensions: ImageDimensions?,
): List<Pair<String, String>> =
    buildList {
        add("Source" to image.sourceLabel)
        image.altText?.takeIf(String::isNotBlank)?.let { add("Description" to it) }
        dimensions?.let { add("Dimensions" to "${it.width} × ${it.height} px") }
    }

@Composable
internal fun SectionHeader(
    icon: ImageVector,
    title: String,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp).weight(1f),
        )
        action?.invoke()
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
private fun IngredientRow(ingredient: RecipeIngredientDto, checklistEnabled: Boolean) {
    val text =
        ingredient.display?.takeIf { it.isNotBlank() }
            ?: ingredient.note?.takeIf { it.isNotBlank() }
    if (text.isNullOrBlank()) return
    var checked by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (checklistEnabled) {
            Checkbox(checked = checked, onCheckedChange = { checked = it })
        } else {
            Text(text = "•", modifier = Modifier.padding(horizontal = 4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
internal fun InstructionRow(
    number: Int,
    instruction: RecipeInstructionDto,
    imageReferences: List<RecipeImageReference>,
    completed: Boolean = false,
    onCompletedChange: (Boolean) -> Unit = {},
    onImageClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = completed,
            onCheckedChange = onCompletedChange,
            modifier = Modifier.semantics {
                contentDescription =
                    if (completed) "Mark step $number incomplete" else "Mark step $number complete"
            },
        )
        Text(
            text = "$number.",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 12.dp, end = 12.dp),
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!instruction.title.isNullOrBlank()) {
                Text(
                    text = instruction.title,
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (completed) TextDecoration.LineThrough else null,
                )
            }
            val stepContent = stripRecipeImageSyntax(instruction.text)
            Markdown(
                content = if (completed) "<del>$stepContent</del>" else stepContent,
                imageTransformer = SafeRecipeImageTransformer,
                modifier = Modifier.fillMaxWidth(),
            )
            if (imageReferences.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    imageReferences.forEachIndexed { index, reference ->
                        AsyncImage(
                            model = reference.url,
                            contentDescription =
                                reference.altText ?: "Open step image ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier.width(160.dp)
                                    .height(100.dp)
                                    .clickable { onImageClick(reference.url) }
                                    .semantics {
                                        contentDescription =
                                            reference.altText
                                                ?: "Open step image ${index + 1} of ${imageReferences.size}"
                                    },
                        )
                    }
                }
            }
        }
    }
}

private val MARKDOWN_IMAGE_SYNTAX =
    Regex(
        """!\[[^\]\r\n]*\]\(\s*(?:<[^>\r\n]+>|[^\s)\"']+)(?:\s+(?:\"[^\"\r\n]*\"|'[^'\r\n]*'|\([^\)\r\n]*\)))?\s*\)"""
    )

private fun stripMarkdownImageSyntax(markdown: String): String =
    MARKDOWN_IMAGE_SYNTAX.replace(markdown, "")

private fun stripRecipeImageSyntax(content: String): String =
    HTML_IMAGE_SYNTAX.replace(stripMarkdownImageSyntax(content), "")

private val HTML_IMAGE_SYNTAX = Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenStepsDialog(
    recipeName: String,
    instructions: List<RecipeInstructionDto>,
    imageReferences: List<List<RecipeImageReference>>,
    completedStepIndexes: Set<Int>,
    onStepCompleted: (Int, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val images = remember(recipeName, imageReferences) { stepViewerImages(recipeName, imageReferences) }
    var viewerPage by rememberSaveable { mutableStateOf<Int?>(null) }
    var fontScale by rememberSaveable { mutableFloatStateOf(1f) }
    var firstVisibleItemIndex by rememberSaveable { mutableStateOf(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = firstVisibleItemIndex)
    val baseDensity = LocalDensity.current

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstVisibleItemIndex = it }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Steps · $recipeName",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Close full-screen steps",
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides
                        Density(
                            density = baseDensity.density,
                            fontScale = baseDensity.fontScale * fontScale,
                        )
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        itemsIndexed(instructions) { index, instruction ->
                            InstructionRow(
                                number = index + 1,
                                instruction = instruction,
                                completed = index in completedStepIndexes,
                                onCompletedChange = { onStepCompleted(index, it) },
                                imageReferences = imageReferences.getOrNull(index).orEmpty(),
                                onImageClick = { url ->
                                    val page = images.indexOfFirst { it.url == url }
                                    if (page >= 0) viewerPage = page
                                },
                            )
                        }
                    }
                }

                StepFontSizeControls(
                    fontScale = fontScale,
                    onDecrease = { fontScale = adjustStepFontScale(fontScale, -0.1f) },
                    onIncrease = { fontScale = adjustStepFontScale(fontScale, 0.1f) },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                )
            }
        }
    }

    viewerPage?.let { page ->
        RecipeImageViewer(
            images = images,
            initialPage = page,
            onDismiss = { viewerPage = null },
        )
    }
}

@Composable
internal fun StepFontSizeControls(
    fontScale: Float,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SmallFloatingActionButton(
            onClick = { if (fontScale > 0.8f) onDecrease() },
            modifier = Modifier.semantics { contentDescription = "Decrease step text size" },
        ) {
            Icon(Icons.Filled.TextDecrease, contentDescription = null)
        }
        Text(
            text = "${(fontScale * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer).padding(8.dp),
        )
        SmallFloatingActionButton(
            onClick = { if (fontScale < 1.6f) onIncrease() },
            modifier = Modifier.semantics { contentDescription = "Increase step text size" },
        ) {
            Icon(Icons.Filled.TextIncrease, contentDescription = null)
        }
    }
}

internal fun adjustStepFontScale(current: Float, delta: Float): Float =
    (current + delta).coerceIn(0.8f, 1.6f)

internal fun fullScreenStepImageUrls(
    imageReferences: List<List<RecipeImageReference>>,
): List<String> = imageReferences.flatten().map(RecipeImageReference::url).distinct()

@Composable
internal fun RecipeImageViewer(
    images: List<RecipeViewerImage>,
    initialPage: Int,
    onDismiss: () -> Unit,
) {
    if (images.isEmpty()) return
    val pagerState =
        rememberPagerState(initialPage = initialPage.coerceIn(images.indices)) { images.size }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var isZoomed by remember { mutableStateOf(false) }
    var loadedDimensions by remember { mutableStateOf<Map<String, ImageDimensions>>(emptyMap()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        LaunchedEffect(pagerState.currentPage) { isZoomed = false }
        val currentPage = pagerState.currentPage.coerceIn(0, images.lastIndex)
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(Color.Black)
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) {
                            false
                        } else {
                            val target =
                                when (event.key) {
                                    Key.DirectionLeft -> (currentPage - 1).takeIf { it >= 0 }
                                    Key.DirectionRight ->
                                        (currentPage + 1).takeIf { it < images.size }
                                    else -> null
                                }
                            if (target == null) {
                                false
                            } else {
                                scope.launch { pagerState.animateScrollToPage(target) }
                                true
                            }
                        }
                    }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = !isZoomed,
                    ) { page ->
                        ZoomableRecipeImagePage(
                            image = images[page],
                            page = page,
                            pageCount = images.size,
                            onZoomChanged = { zoomed ->
                                if (page == pagerState.currentPage) isZoomed = zoomed
                            },
                            onDimensionsLoaded = { url, dimensions ->
                                if (dimensions != null) {
                                    loadedDimensions = loadedDimensions + (url to dimensions)
                                }
                            },
                        )
                    }

                    if (images.size > 1) {
                        val previousPage = (currentPage - 1).takeIf { it >= 0 }
                        val nextPage = (currentPage + 1).takeIf { it < images.size }
                        IconButton(
                            onClick = {
                                previousPage?.let { scope.launch { pagerState.animateScrollToPage(it) } }
                            },
                            enabled = previousPage != null,
                            modifier =
                                Modifier.align(Alignment.CenterStart)
                                    .padding(start = 8.dp)
                                    .background(Color.Black.copy(alpha = 0.45f)),
                        ) {
                            Icon(
                                Icons.Filled.ChevronLeft,
                                contentDescription = "Previous image",
                                tint = Color.White.copy(alpha = if (previousPage == null) 0.35f else 1f),
                            )
                        }
                        IconButton(
                            onClick = {
                                nextPage?.let { scope.launch { pagerState.animateScrollToPage(it) } }
                            },
                            enabled = nextPage != null,
                            modifier =
                                Modifier.align(Alignment.CenterEnd)
                                    .padding(end = 8.dp)
                                    .background(Color.Black.copy(alpha = 0.45f)),
                        ) {
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = "Next image",
                                tint = Color.White.copy(alpha = if (nextPage == null) 0.35f else 1f),
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close image viewer",
                            tint = Color.White,
                        )
                    }
                    Text(
                        text = "${currentPage + 1} / ${images.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    )
                }
                ImageMetadataPanel(
                    image = images[currentPage],
                    dimensions = loadedDimensions[images[currentPage].url],
                )
            }
        }
    }
}

@Composable
private fun ZoomableRecipeImagePage(
    image: RecipeViewerImage,
    page: Int,
    pageCount: Int,
    onZoomChanged: (Boolean) -> Unit,
    onDimensionsLoaded: (String, ImageDimensions?) -> Unit,
) {
    var scale by remember(image.url) { mutableFloatStateOf(1f) }
    var offset by remember(image.url) { mutableStateOf(Offset.Zero) }
    val painter = rememberAsyncImagePainter(model = image.url)
    val painterState by painter.state.collectAsState()
    val dimensions =
        (painterState as? AsyncImagePainter.State.Success)?.result?.image?.let {
            ImageDimensions(width = it.width, height = it.height)
        }

    LaunchedEffect(image.url, dimensions) { onDimensionsLoaded(image.url, dimensions) }

    Box(
        modifier =
            Modifier.fillMaxSize()
                .pointerInput(image.url) {
                    detectRecipeImageZoomPan(
                        isZoomed = { scale > MIN_IMAGE_SCALE },
                        onGesture = { pan, zoom ->
                            val newScale =
                                (scale * zoom).coerceIn(MIN_IMAGE_SCALE, MAX_IMAGE_SCALE)
                            val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                            val maxOffsetY = (size.height * (newScale - 1f)) / 2f
                            offset =
                                Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY),
                                )
                            scale = newScale
                            onZoomChanged(newScale > MIN_IMAGE_SCALE)
                        },
                        onDoubleTap = {
                            scale =
                                if (scale > MIN_IMAGE_SCALE) MIN_IMAGE_SCALE
                                else DEFAULT_DOUBLE_TAP_SCALE
                            offset = Offset.Zero
                            onZoomChanged(scale > MIN_IMAGE_SCALE)
                        },
                    )
                }
                .semantics {
                    contentDescription =
                        "${image.title}, ${image.sourceLabel}, image ${page + 1} of $pageCount"
                    stateDescription =
                        if (scale > MIN_IMAGE_SCALE) "Zoomed in; pan with one finger"
                        else "Not zoomed; swipe left or right to change images"
                },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier =
                Modifier.fillMaxSize()
                    .padding(16.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
        )
    }
}

private suspend fun PointerInputScope.detectRecipeImageZoomPan(
    isZoomed: () -> Boolean,
    onGesture: (pan: Offset, zoom: Float) -> Unit,
    onDoubleTap: (Offset) -> Unit,
) {
    var lastTapAt = 0L
    var lastTapPosition = Offset.Unspecified
    awaitEachGesture {
        val firstDown = awaitFirstDown(requireUnconsumed = false)
        var moved = false
        var multiTouch = false
        do {
            val event = awaitPointerEvent()
            val zoomChange = event.calculateZoom()
            val panChange = event.calculatePan()
            multiTouch = multiTouch || event.changes.size > 1
            moved =
                moved ||
                    (event.changes.firstOrNull()?.position?.minus(firstDown.position)?.getDistance()
                        ?: 0f) > viewConfiguration.touchSlop
            if (event.changes.size > 1 || zoomChange != 1f || isZoomed()) {
                onGesture(panChange, zoomChange)
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } while (event.changes.any { it.pressed })

        val now = SystemClock.uptimeMillis()
        val tapPosition = firstDown.position
        if (!moved && !multiTouch) {
            val isSecondTap =
                lastTapAt != 0L &&
                    now - lastTapAt <= viewConfiguration.doubleTapTimeoutMillis &&
                    (tapPosition - lastTapPosition).getDistance() <= viewConfiguration.touchSlop * 2
            if (isSecondTap) {
                onDoubleTap(tapPosition)
                lastTapAt = 0L
            } else {
                lastTapAt = now
                lastTapPosition = tapPosition
            }
        } else {
            lastTapAt = 0L
        }
    }
}

@Composable
private fun ImageMetadataPanel(image: RecipeViewerImage, dimensions: ImageDimensions?) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = image.title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        imageMetadataRows(image, dimensions).forEach { (label, value) ->
            Row(modifier = Modifier.padding(top = 3.dp)) {
                Text(
                    text = "$label: ",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = value,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private const val MIN_IMAGE_SCALE = 1f
private const val DEFAULT_DOUBLE_TAP_SCALE = 2.5f
private const val MAX_IMAGE_SCALE = 5f

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

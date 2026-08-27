package dev.pschmitt.syncwich.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.CategoryEntity
import dev.pschmitt.syncwich.data.db.entity.FoodEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.data.db.entity.TagEntity
import dev.pschmitt.syncwich.data.db.entity.ToolEntity
import dev.pschmitt.syncwich.data.repository.CategoryRepository
import dev.pschmitt.syncwich.data.repository.FoodRepository
import dev.pschmitt.syncwich.data.repository.RecipeRepository
import dev.pschmitt.syncwich.data.repository.TagRepository
import dev.pschmitt.syncwich.data.repository.ToolRepository
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import dev.pschmitt.syncwich.ui.common.RefreshState
import dev.pschmitt.syncwich.ui.common.refreshErrorMessage
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecipesUiState(
    val recipes: List<RecipeSummaryEntity> = emptyList(),
    val favoriteRecipeIds: Set<String> = emptySet(),
    val categories: List<CategoryEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val tools: List<ToolEntity> = emptyList(),
    val foods: List<FoodEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val selectedTagId: String? = null,
    val selectedToolId: String? = null,
    val selectedFoodId: String? = null,
    val serverUrl: String = "",
    val refreshState: RefreshState = RefreshState(),
)

private data class RecipeSelection(
    val searchQuery: String = "",
    val categoryId: String? = null,
    val tagId: String? = null,
    val toolId: String? = null,
    val foodId: String? = null,
)

private data class FilterOptions(
    val categories: List<CategoryEntity>,
    val tags: List<TagEntity>,
    val tools: List<ToolEntity>,
    val foods: List<FoodEntity>,
)

/**
 * Backs [RecipesScreen] - every field in [RecipesUiState] is (transitively) sourced from a Room
 * [kotlinx.coroutines.flow.Flow], so search/filter changes recompute over whatever's cached
 * on-device immediately; [refreshRecipes]/[refreshCategories]/[refreshTags] below are only a
 * best-effort background nudge (see AGENTS.md's offline-first requirement).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecipesViewModel
@Inject
constructor(
    private val recipeRepository: RecipeRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val toolRepository: ToolRepository,
    private val foodRepository: FoodRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val selectedTagId = MutableStateFlow<String?>(null)
    private val refreshState = MutableStateFlow(RefreshState())
    private var refreshJob: Job? = null

    // Category/tag/tool/food chips are mutually exclusive single-select filters (see
    // onCategorySelected/onTagSelected/onToolSelected/onFoodSelected) - the repository only exposes
    // "all"/"by one X" Room queries, and that's the shape a small self-hosted recipe box actually
    // needs. Keeping all five values in one StateFlow means selecting one filter while another is
    // selected causes one Room query switch rather than multiple intermediate emissions.
    private val selection = MutableStateFlow(RecipeSelection())

    private val filteredRecipes =
        selection
            .flatMapLatest { (query, categoryId, tagId, toolId, foodId) ->
                val recipes =
                    when {
                        categoryId != null -> recipeRepository.observeRecipesByCategory(categoryId)
                        tagId != null -> recipeRepository.observeRecipesByTag(tagId)
                        toolId != null -> recipeRepository.observeRecipesByTool(toolId)
                        foodId != null -> recipeRepository.observeRecipesByFood(foodId)
                        else -> recipeRepository.observeRecipes()
                    }
                recipes.map { filterRecipesByQuery(it, query) }
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    private val favoriteRecipeIds =
        recipeRepository.observeFavoriteRecipeIds().map { it.toSet() }.distinctUntilChanged()

    private val recipesAndFavorites =
        combine(filteredRecipes, favoriteRecipeIds) { recipes, favorites -> recipes to favorites }
            .distinctUntilChanged()

    private val filterOptions =
        combine(
                categoryRepository.observeCategories(),
                tagRepository.observeTags(),
                toolRepository.observeTools(),
                foodRepository.observeFoods(),
                ::FilterOptions,
            )
            .distinctUntilChanged()

    val uiState: StateFlow<RecipesUiState> =
        combine(
                recipesAndFavorites,
                filterOptions,
                selection,
                settingsRepository.credentials,
                refreshState,
            ) {
                (recipes, favoriteIds),
                filterOptions,
                (query, categoryId, tagId, toolId, foodId),
                credentials,
                refresh ->
                RecipesUiState(
                    recipes = recipes,
                    favoriteRecipeIds = favoriteIds,
                    categories = filterOptions.categories,
                    tags = filterOptions.tags,
                    tools = filterOptions.tools,
                    foods = filterOptions.foods,
                    searchQuery = query,
                    selectedCategoryId = categoryId,
                    selectedTagId = tagId,
                    selectedToolId = toolId,
                    selectedFoodId = foodId,
                    serverUrl = credentials.serverUrl,
                    refreshState = refresh,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipesUiState())

    init {
        refresh(forceRefresh = false)
    }

    fun refresh() = refresh(forceRefresh = true)

    private fun refresh(forceRefresh: Boolean) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            refreshState.value = RefreshState(isRefreshing = true)
            val results = coroutineScope {
                listOf(
                        async { recipeRepository.refreshRecipes(forceRefresh) },
                        async { categoryRepository.refreshCategories() },
                        async { tagRepository.refreshTags() },
                        async { toolRepository.refreshTools() },
                        async { foodRepository.refreshFoods() },
                    )
                    .awaitAll()
            }
            refreshState.value =
                RefreshState(errorMessage = results.firstNotNullOfOrNull(::refreshErrorMessage))
        }
    }

    fun onSearchQueryChange(query: String) {
        selection.value = selection.value.copy(searchQuery = query)
    }

    fun onCategorySelected(categoryId: String) {
        selection.value =
            selection.value.copy(
                categoryId = if (selection.value.categoryId == categoryId) null else categoryId,
                tagId = null,
                toolId = null,
                foodId = null,
            )
    }

    fun onTagSelected(tagId: String) {
        selection.value =
            selection.value.copy(
                categoryId = null,
                tagId = if (selection.value.tagId == tagId) null else tagId,
                toolId = null,
                foodId = null,
            )
    }

    fun onToolSelected(toolId: String) {
        selection.value =
            selection.value.copy(
                categoryId = null,
                tagId = null,
                toolId = if (selection.value.toolId == toolId) null else toolId,
                foodId = null,
            )
    }

    fun onFoodSelected(foodId: String) {
        selection.value =
            selection.value.copy(
                categoryId = null,
                tagId = null,
                toolId = null,
                foodId = if (selection.value.foodId == foodId) null else foodId,
            )
    }

    fun clearFilters() {
        selection.value =
            selection.value.copy(categoryId = null, tagId = null, toolId = null, foodId = null)
    }

    fun selectTag(tagId: String) {
        selection.value =
            selection.value.copy(categoryId = null, tagId = tagId, toolId = null, foodId = null)
    }

    fun selectCategory(categoryId: String) {
        selection.value =
            selection.value.copy(
                categoryId = categoryId,
                tagId = null,
                toolId = null,
                foodId = null,
            )
    }
}

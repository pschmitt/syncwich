package dev.pschmitt.syncwich.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.CategoryEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.data.db.entity.TagEntity
import dev.pschmitt.syncwich.data.repository.CategoryRepository
import dev.pschmitt.syncwich.data.repository.RecipeRepository
import dev.pschmitt.syncwich.data.repository.TagRepository
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
    val categories: List<CategoryEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val selectedTagId: String? = null,
    val serverUrl: String = "",
    val refreshState: RefreshState = RefreshState(),
)

private data class RecipeSelection(
    val searchQuery: String = "",
    val categoryId: String? = null,
    val tagId: String? = null,
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
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val selectedTagId = MutableStateFlow<String?>(null)
    private val refreshState = MutableStateFlow(RefreshState())
    private var refreshJob: Job? = null

    // Category and tag chips are mutually exclusive single-select filters (see onCategorySelected/
    // onTagSelected) - the repository only exposes "all"/"by one category"/"by one tag" Room
    // queries, and that's the shape a small self-hosted recipe box actually needs.
    // Keeping the three values in one StateFlow means selecting a category while a tag is
    // selected causes one Room query switch rather than two intermediate emissions.
    private val selection = MutableStateFlow(RecipeSelection())

    private val filteredRecipes =
        selection
            .flatMapLatest { (query, categoryId, tagId) ->
                val recipes =
                    when {
                        categoryId != null -> recipeRepository.observeRecipesByCategory(categoryId)
                        tagId != null -> recipeRepository.observeRecipesByTag(tagId)
                        else -> recipeRepository.observeRecipes()
                    }
                recipes.map { filterRecipesByQuery(it, query) }
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    private val categoriesAndTags =
        combine(
                categoryRepository.observeCategories(),
                tagRepository.observeTags(),
                ::Pair,
            )
            .distinctUntilChanged()

    val uiState: StateFlow<RecipesUiState> =
        combine(
                filteredRecipes,
                categoriesAndTags,
                selection,
                settingsRepository.credentials,
                refreshState,
            ) { recipes, categoriesAndTags, (query, categoryId, tagId), credentials, refresh ->
                val (categories, tags) = categoriesAndTags
                RecipesUiState(
                    recipes = recipes,
                    categories = categories,
                    tags = tags,
                    searchQuery = query,
                    selectedCategoryId = categoryId,
                    selectedTagId = tagId,
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
            )
    }

    fun onTagSelected(tagId: String) {
        selection.value =
            selection.value.copy(
                categoryId = null,
                tagId = if (selection.value.tagId == tagId) null else tagId,
            )
    }
}

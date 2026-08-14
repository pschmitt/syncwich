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
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
    categoryRepository: CategoryRepository,
    tagRepository: TagRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val selectedTagId = MutableStateFlow<String?>(null)

    // Category and tag chips are mutually exclusive single-select filters (see onCategorySelected/
    // onTagSelected) - the repository only exposes "all"/"by one category"/"by one tag" Room
    // queries, and that's the shape a small self-hosted recipe box actually needs.
    private val selection = combine(searchQuery, selectedCategoryId, selectedTagId, ::Triple)

    private val filteredRecipes = selection.flatMapLatest { (query, categoryId, tagId) ->
        val recipes =
            when {
                categoryId != null -> recipeRepository.observeRecipesByCategory(categoryId)
                tagId != null -> recipeRepository.observeRecipesByTag(tagId)
                else -> recipeRepository.observeRecipes()
            }
        recipes.map { filterRecipesByQuery(it, query) }
    }

    val uiState: StateFlow<RecipesUiState> =
        combine(
                filteredRecipes,
                combine(
                    categoryRepository.observeCategories(),
                    tagRepository.observeTags(),
                    ::Pair,
                ),
                selection,
                settingsRepository.credentials,
            ) { recipes, categoriesAndTags, (query, categoryId, tagId), credentials ->
                val (categories, tags) = categoriesAndTags
                RecipesUiState(
                    recipes = recipes,
                    categories = categories,
                    tags = tags,
                    searchQuery = query,
                    selectedCategoryId = categoryId,
                    selectedTagId = tagId,
                    serverUrl = credentials.serverUrl,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipesUiState())

    init {
        // Independent launches so one endpoint failing doesn't delay the others - each repository
        // call already swallows its own errors (Result), never touching what's cached on failure.
        viewModelScope.launch { recipeRepository.refreshRecipes() }
        viewModelScope.launch { categoryRepository.refreshCategories() }
        viewModelScope.launch { tagRepository.refreshTags() }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onCategorySelected(categoryId: String) {
        selectedCategoryId.value = if (selectedCategoryId.value == categoryId) null else categoryId
        selectedTagId.value = null
    }

    fun onTagSelected(tagId: String) {
        selectedTagId.value = if (selectedTagId.value == tagId) null else tagId
        selectedCategoryId.value = null
    }
}

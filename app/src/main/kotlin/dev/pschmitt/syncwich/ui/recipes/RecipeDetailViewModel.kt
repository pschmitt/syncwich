package dev.pschmitt.syncwich.ui.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.api.dto.RecipeDetailDto
import dev.pschmitt.syncwich.data.api.recipeImageUrl
import dev.pschmitt.syncwich.data.db.entity.CookbookEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeActionEntity
import dev.pschmitt.syncwich.data.image.RecipeImageReference
import dev.pschmitt.syncwich.data.image.extractRecipeImageReferences
import dev.pschmitt.syncwich.data.repository.CookbookRepository
import dev.pschmitt.syncwich.data.repository.RecipeActionRepository
import dev.pschmitt.syncwich.data.repository.RecipeRepository
import dev.pschmitt.syncwich.data.repository.RecipeStepProgressRepository
import dev.pschmitt.syncwich.data.repository.RecipeTimelineRepository
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import dev.pschmitt.syncwich.ui.common.RefreshState
import dev.pschmitt.syncwich.ui.common.refreshErrorMessage
import dev.pschmitt.syncwich.ui.navigation.Route
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

sealed interface RecipeDetailUiState {
    data object Loading : RecipeDetailUiState

    data class Unavailable(val errorMessage: String? = null) : RecipeDetailUiState

    data class Loaded(
        val recipe: RecipeDetailDto,
        val serverUrl: String,
        val imageIndex: RecipeImageIndex = RecipeImageIndex.EMPTY,
        val actions: RecipeActionUiState = RecipeActionUiState(),
        val cookbooks: List<CookbookEntity> = emptyList(),
        val completedStepIndexes: Set<Int> = emptySet(),
        val ingredientChecklistEnabled: Boolean = false,
        val refreshError: String? = null,
    ) : RecipeDetailUiState
}

sealed interface RecipeDeleteUiState {
    data object Idle : RecipeDeleteUiState

    data object Deleting : RecipeDeleteUiState

    data class Failed(val message: String) : RecipeDeleteUiState

    data object Deleted : RecipeDeleteUiState
}

data class RecipeActionUiState(
    val isFavorite: Boolean = false,
    val rating: Int? = null,
    val favoritePending: Boolean = false,
    val ratingPending: Boolean = false,
    val madeThisPending: Boolean = false,
)

/** Recipe image destinations indexed once on Dispatchers.Default for the detail screen. */
data class RecipeImageIndex(
    val coverUrl: String?,
    val galleryUrls: List<String>,
    val instructionReferences: List<List<RecipeImageReference>>,
) {
    companion object {
        val EMPTY = RecipeImageIndex(null, emptyList(), emptyList())
    }
}

private fun RecipeActionEntity?.toUiState(madeThisPending: Boolean) =
    RecipeActionUiState(
        isFavorite = this?.isFavorite == true,
        rating = this?.rating,
        favoritePending = this?.favoritePending == true,
        ratingPending = this?.ratingPending == true,
        madeThisPending = madeThisPending,
    )

/**
 * Backs [RecipeDetailScreen]. [dev.pschmitt.syncwich.data.db.entity.RecipeDetailEntity.detailJson]
 * is decoded here rather than in the repository - see that entity's kdoc for why the repository
 * hands back raw JSON.
 */
@HiltViewModel
class RecipeDetailViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val recipeActionRepository: RecipeActionRepository,
    private val recipeTimelineRepository: RecipeTimelineRepository,
    private val stepProgressRepository: RecipeStepProgressRepository,
    private val cookbookRepository: CookbookRepository,
    settingsRepository: SettingsRepository,
    private val json: Json,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.RecipeDetail>()
    private val requestedRecipeId = route.recipeId
    private val _refreshState = MutableStateFlow(RefreshState())
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()
    private val _deleteState = MutableStateFlow<RecipeDeleteUiState>(RecipeDeleteUiState.Idle)
    val deleteState: StateFlow<RecipeDeleteUiState> = _deleteState.asStateFlow()
    private var refreshJob: Job? = null
    private var lastDeleteTarget: Pair<String, String>? = null

    private val detailJson: Flow<String?> =
        (if (requestedRecipeId.isBlank()) {
                recipeRepository.observeRecipeDetailBySlug(route.slug)
            } else {
                recipeRepository.observeRecipeDetail(requestedRecipeId)
            })
            .map { it?.detailJson }
            .distinctUntilChanged()

    /** Starts with the screen/view-model, so cached detail decoding never waits for a refresh. */
    private val decodedRecipe: StateFlow<RecipeDetailDto?> =
        detailJson
            .map { rawJson -> rawJson?.let { decodeRecipeDetail(json, it) } }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val effectiveRecipeId: StateFlow<String> =
        decodedRecipe
            .map { it?.id ?: requestedRecipeId }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, requestedRecipeId)

    /** Regex/URI work for Markdown and HTML image extraction stays off the Compose thread. */
    private val recipePresentation =
        combine(decodedRecipe, settingsRepository.credentials) { recipe, credentials ->
                recipe?.let {
                    RecipeDetailPresentation(
                        recipe = it,
                        serverUrl = credentials.serverUrl,
                        imageIndex = recipeImageIndex(credentials.serverUrl, it),
                    )
                }
            }
            .flowOn(Dispatchers.Default)

    private val ingredientChecklistEnabled =
        settingsRepository.ingredientChecklistEnabled.distinctUntilChanged()

    private val completedStepIndexes: Flow<Set<Int>> = effectiveRecipeId.flatMapLatest { recipeId ->
        if (recipeId.isBlank()) flowOf(emptySet())
        else stepProgressRepository.observeCompleted(recipeId)
    }

    private val cookbooks = effectiveRecipeId.flatMapLatest { recipeId ->
        if (recipeId.isBlank()) flowOf(emptyList())
        else cookbookRepository.observeCookbooksForRecipe(recipeId)
    }

    private val actions: Flow<RecipeActionUiState> =
        effectiveRecipeId
            .flatMapLatest { recipeId ->
                combine(
                    recipeActionRepository.observe(recipeId),
                    recipeTimelineRepository.observe(recipeId),
                ) { action, timelineEvents ->
                    action.toUiState(madeThisPending = timelineEvents.any { it.pending })
                }
            }
            .distinctUntilChanged()

    private val presentationSettings = combine(ingredientChecklistEnabled, cookbooks, ::Pair)

    val uiState: StateFlow<RecipeDetailUiState> =
        combine(
                recipePresentation,
                actions,
                completedStepIndexes,
                refreshState,
                presentationSettings,
            ) { presentation, recipeActions, completedSteps, refresh, (checklistEnabled, books) ->
                recipeDetailUiState(
                    recipe = presentation?.recipe,
                    actions = recipeActions,
                    serverUrl = presentation?.serverUrl.orEmpty(),
                    refresh = refresh,
                    imageIndex = presentation?.imageIndex,
                    cookbooks = books,
                    completedStepIndexes = completedSteps,
                    ingredientChecklistEnabled = checklistEnabled,
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                RecipeDetailUiState.Loading,
            )

    init {
        refresh(forceRefresh = false)
    }

    fun refresh() {
        refresh(forceRefresh = true)
    }

    private fun refresh(forceRefresh: Boolean) {
        if (refreshJob?.isActive == true) return
        refreshActions()
        refreshJob = viewModelScope.launch {
            _refreshState.value = RefreshState(isRefreshing = true)
            _refreshState.value =
                RefreshState(
                    errorMessage =
                        refreshErrorMessage(
                            recipeRepository.refreshRecipeDetail(
                                requestedRecipeId,
                                route.slug,
                                forceRefresh,
                            )
                        )
                )
        }
    }

    fun setFavorite(isFavorite: Boolean) {
        viewModelScope.launch {
            recipeActionRepository.setFavorite(effectiveRecipeId.value, route.slug, isFavorite)
        }
    }

    fun setRating(rating: Int) {
        require(rating in 1..5) { "Recipe rating must be between 1 and 5" }
        viewModelScope.launch {
            recipeActionRepository.setRating(effectiveRecipeId.value, route.slug, rating)
        }
    }

    fun setStepCompleted(stepIndex: Int, completed: Boolean) {
        require(stepIndex >= 0) { "Recipe step index must not be negative" }
        viewModelScope.launch {
            stepProgressRepository.setCompleted(effectiveRecipeId.value, stepIndex, completed)
        }
    }

    fun deleteRecipe(recipeId: String, slug: String) {
        if (_deleteState.value is RecipeDeleteUiState.Deleting) return
        lastDeleteTarget = recipeId to slug
        viewModelScope.launch {
            _deleteState.value = RecipeDeleteUiState.Deleting
            _deleteState.value =
                recipeRepository
                    .deleteRecipe(recipeId, slug)
                    .fold(
                        onSuccess = { RecipeDeleteUiState.Deleted },
                        onFailure = {
                            RecipeDeleteUiState.Failed(
                                "Couldn't delete the recipe. Your saved copy is still available; " +
                                    "check your connection and try again."
                            )
                        },
                    )
        }
    }

    fun retryDelete() {
        lastDeleteTarget?.let { (recipeId, slug) -> deleteRecipe(recipeId, slug) }
    }

    /** Records a durable "I made this" cooking event - see [RecipeTimelineRepository]'s kdoc. */
    fun recordMadeThis() {
        viewModelScope.launch { recipeTimelineRepository.recordMadeThis(effectiveRecipeId.value) }
    }

    private fun refreshActions() {
        viewModelScope.launch { recipeActionRepository.refreshFromServer() }
    }
}

private data class RecipeDetailPresentation(
    val recipe: RecipeDetailDto,
    val serverUrl: String,
    val imageIndex: RecipeImageIndex,
)

internal fun recipeDetailUiState(
    recipe: RecipeDetailDto?,
    actions: RecipeActionUiState,
    serverUrl: String,
    refresh: RefreshState,
    imageIndex: RecipeImageIndex? = null,
    cookbooks: List<CookbookEntity> = emptyList(),
    completedStepIndexes: Set<Int> = emptySet(),
    ingredientChecklistEnabled: Boolean = false,
): RecipeDetailUiState =
    when {
        recipe != null ->
            RecipeDetailUiState.Loaded(
                recipe = recipe,
                serverUrl = serverUrl,
                imageIndex = imageIndex ?: recipeImageIndex(serverUrl, recipe),
                actions = actions,
                cookbooks = cookbooks,
                completedStepIndexes = completedStepIndexes,
                ingredientChecklistEnabled = ingredientChecklistEnabled,
                refreshError = refresh.errorMessage,
            )
        refresh.isRefreshing -> RecipeDetailUiState.Loading
        else -> RecipeDetailUiState.Unavailable(refresh.errorMessage)
    }

internal fun decodeRecipeDetail(json: Json, rawJson: String): RecipeDetailDto? = runCatching {
    json.decodeFromString<RecipeDetailDto>(rawJson)
}
    .getOrNull()

internal fun recipeImageIndex(serverUrl: String, recipe: RecipeDetailDto): RecipeImageIndex {
    val coverUrl = recipeImageUrl(serverUrl, recipe.id, recipe.image)
    val instructionReferences =
        recipe.recipeInstructions.map { extractRecipeImageReferences(it.text, serverUrl) }
    val galleryUrls = buildList {
        coverUrl?.let(::add)
        instructionReferences
            .asSequence()
            .flatten()
            .map(RecipeImageReference::url)
            .distinct()
            .forEach(::add)
    }
    return RecipeImageIndex(coverUrl, galleryUrls, instructionReferences)
}

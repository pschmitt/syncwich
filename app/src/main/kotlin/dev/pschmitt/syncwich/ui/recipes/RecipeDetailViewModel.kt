package dev.pschmitt.syncwich.ui.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.api.dto.RecipeDetailDto
import dev.pschmitt.syncwich.data.db.entity.RecipeActionEntity
import dev.pschmitt.syncwich.data.repository.RecipeActionRepository
import dev.pschmitt.syncwich.data.repository.RecipeRepository
import dev.pschmitt.syncwich.data.repository.RecipeTimelineRepository
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import dev.pschmitt.syncwich.ui.common.RefreshState
import dev.pschmitt.syncwich.ui.common.refreshErrorMessage
import dev.pschmitt.syncwich.ui.navigation.Route
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
        val actions: RecipeActionUiState = RecipeActionUiState(),
        val refreshError: String? = null,
    ) : RecipeDetailUiState
}

data class RecipeActionUiState(
    val isFavorite: Boolean = false,
    val rating: Int? = null,
    val favoritePending: Boolean = false,
    val ratingPending: Boolean = false,
    val madeThisPending: Boolean = false,
)

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
    settingsRepository: SettingsRepository,
    private val json: Json,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.RecipeDetail>()
    private val _refreshState = MutableStateFlow(RefreshState())
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    private val detailJson: Flow<String?> =
        recipeRepository
            .observeRecipeDetail(route.recipeId)
            .map { it?.detailJson }
            .distinctUntilChanged()

    /** Starts with the screen/view-model, so cached detail decoding never waits for a refresh. */
    private val decodedRecipe: StateFlow<RecipeDetailDto?> =
        detailJson
            .map { rawJson -> rawJson?.let { decodeRecipeDetail(json, it) } }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val actions: Flow<RecipeActionUiState> =
        combine(
                recipeActionRepository.observe(route.recipeId),
                recipeTimelineRepository.observe(route.recipeId),
            ) { action, timelineEvents ->
                action.toUiState(madeThisPending = timelineEvents.any { it.pending })
            }
            .distinctUntilChanged()

    val uiState: StateFlow<RecipeDetailUiState> =
        combine(
                decodedRecipe,
                actions,
                settingsRepository.credentials,
                refreshState,
            ) { recipe, recipeActions, credentials, refresh ->
                recipeDetailUiState(recipe, recipeActions, credentials.serverUrl, refresh)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                RecipeDetailUiState.Loading,
            )

    init {
        refresh()
    }

    fun refresh() {
        refreshActions()
        viewModelScope.launch {
            _refreshState.value = RefreshState(isRefreshing = true)
            _refreshState.value =
                RefreshState(
                    errorMessage =
                        refreshErrorMessage(
                            recipeRepository.refreshRecipeDetail(route.recipeId, route.slug)
                        )
                )
        }
    }

    fun setFavorite(isFavorite: Boolean) {
        viewModelScope.launch {
            recipeActionRepository.setFavorite(route.recipeId, route.slug, isFavorite)
        }
    }

    fun setRating(rating: Int) {
        require(rating in 1..5) { "Recipe rating must be between 1 and 5" }
        viewModelScope.launch {
            recipeActionRepository.setRating(route.recipeId, route.slug, rating)
        }
    }

    /** Records a durable "I made this" cooking event - see [RecipeTimelineRepository]'s kdoc. */
    fun recordMadeThis() {
        viewModelScope.launch { recipeTimelineRepository.recordMadeThis(route.recipeId) }
    }

    private fun refreshActions() {
        viewModelScope.launch { recipeActionRepository.refreshFromServer() }
    }
}

internal fun recipeDetailUiState(
    recipe: RecipeDetailDto?,
    actions: RecipeActionUiState,
    serverUrl: String,
    refresh: RefreshState,
): RecipeDetailUiState =
    when {
        recipe != null ->
            RecipeDetailUiState.Loaded(
                recipe = recipe,
                serverUrl = serverUrl,
                actions = actions,
                refreshError = refresh.errorMessage,
            )
        refresh.isRefreshing -> RecipeDetailUiState.Loading
        else -> RecipeDetailUiState.Unavailable(refresh.errorMessage)
    }

internal fun decodeRecipeDetail(json: Json, rawJson: String): RecipeDetailDto? =
    runCatching { json.decodeFromString<RecipeDetailDto>(rawJson) }.getOrNull()

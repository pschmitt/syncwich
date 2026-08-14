package dev.pschmitt.syncwich.ui.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.api.dto.RecipeDetailDto
import dev.pschmitt.syncwich.data.repository.RecipeRepository
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
        val refreshError: String? = null,
    ) : RecipeDetailUiState
}

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

    private val decodedRecipe: Flow<RecipeDetailDto?> =
        detailJson
            .map { rawJson -> rawJson?.let { decodeRecipeDetail(json, it) } }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    val uiState: StateFlow<RecipeDetailUiState> =
        combine(
                decodedRecipe,
                settingsRepository.credentials,
                refreshState,
            ) { recipe, credentials, refresh ->
                when {
                    recipe != null ->
                        RecipeDetailUiState.Loaded(
                            recipe = recipe,
                            serverUrl = credentials.serverUrl,
                            refreshError = refresh.errorMessage,
                        )
                    refresh.isRefreshing -> RecipeDetailUiState.Loading
                    else -> RecipeDetailUiState.Unavailable(refresh.errorMessage)
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                RecipeDetailUiState.Loading,
            )

    init { refresh() }

    fun refresh() {
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
}

internal fun decodeRecipeDetail(json: Json, rawJson: String): RecipeDetailDto? =
    runCatching { json.decodeFromString<RecipeDetailDto>(rawJson) }.getOrNull()

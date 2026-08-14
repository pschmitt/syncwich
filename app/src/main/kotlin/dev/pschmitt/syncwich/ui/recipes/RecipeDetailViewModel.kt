package dev.pschmitt.syncwich.ui.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.api.dto.RecipeDetailDto
import dev.pschmitt.syncwich.data.repository.RecipeRepository
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import dev.pschmitt.syncwich.ui.navigation.Route
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

sealed interface RecipeDetailUiState {
    data object Loading : RecipeDetailUiState

    data class Loaded(val recipe: RecipeDetailDto, val serverUrl: String) : RecipeDetailUiState
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

    val uiState: StateFlow<RecipeDetailUiState> =
        combine(
                recipeRepository.observeRecipeDetail(route.recipeId),
                settingsRepository.credentials,
            ) { entity, credentials ->
                val recipe =
                    entity
                        ?.let {
                            runCatching { json.decodeFromString<RecipeDetailDto>(it.detailJson) }
                        }
                        ?.getOrNull()
                if (recipe != null) RecipeDetailUiState.Loaded(recipe, credentials.serverUrl)
                else RecipeDetailUiState.Loading
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                RecipeDetailUiState.Loading,
            )

    init {
        viewModelScope.launch { recipeRepository.refreshRecipeDetail(route.recipeId, route.slug) }
    }
}

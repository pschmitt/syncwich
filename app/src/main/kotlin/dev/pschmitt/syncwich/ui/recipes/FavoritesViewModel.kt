package dev.pschmitt.syncwich.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.data.repository.RecipeRepository
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import dev.pschmitt.syncwich.ui.common.RefreshState
import dev.pschmitt.syncwich.ui.common.refreshErrorMessage
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val recipes: List<RecipeSummaryEntity> = emptyList(),
    val serverUrl: String = "",
    val refreshState: RefreshState = RefreshState(),
)

/** Displays the favorite state already cached in Room; refresh only nudges the recipe cache. */
@HiltViewModel
class FavoritesViewModel
@Inject
constructor(
    private val recipeRepository: RecipeRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val refreshState = MutableStateFlow(RefreshState())

    val uiState: StateFlow<FavoritesUiState> =
        combine(
                recipeRepository.observeFavoriteRecipes(),
                settingsRepository.credentials,
                refreshState,
            ) { recipes, credentials, refresh ->
                FavoritesUiState(
                    recipes = recipes,
                    serverUrl = credentials.serverUrl,
                    refreshState = refresh,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), FavoritesUiState())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            refreshState.value = RefreshState(isRefreshing = true)
            refreshState.value =
                RefreshState(
                    errorMessage = refreshErrorMessage(recipeRepository.refreshRecipes())
                )
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

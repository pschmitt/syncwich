package dev.pschmitt.syncwich.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.data.repository.RecipeHistoryRepository
import dev.pschmitt.syncwich.data.repository.RecipeRepository
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import dev.pschmitt.syncwich.sync.SyncScheduler
import dev.pschmitt.syncwich.sync.SyncStatus
import dev.pschmitt.syncwich.sync.SyncStatusRepository
import dev.pschmitt.syncwich.sync.SyncStatusState
import dev.pschmitt.syncwich.ui.common.RefreshState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val recentlyViewedRecipes: List<RecipeSummaryEntity> = emptyList(),
    val recentlyAddedRecipes: List<RecipeSummaryEntity> = emptyList(),
    val recentlyCookedRecipes: List<RecipeSummaryEntity> = emptyList(),
    val favoriteRecipes: List<RecipeSummaryEntity> = emptyList(),
    val serverUrl: String = "",
    val refreshState: RefreshState = RefreshState(),
    val syncStatus: SyncStatus = SyncStatus(),
)

/**
 * Reads every dashboard section from Room first. Home refresh queues the existing full background
 * worker and is deliberately independent from the state flow, so a disconnected server never hides
 * recipes that are already cached.
 */
@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val recipeRepository: RecipeRepository,
    private val recipeHistoryRepository: RecipeHistoryRepository,
    settingsRepository: SettingsRepository,
    private val syncScheduler: SyncScheduler,
    syncStatusRepository: SyncStatusRepository,
) : ViewModel() {

    private val userRefreshRequested = MutableStateFlow(false)

    private val favoriteRecipes = recipeRepository.observeFavoriteRecipes()

    private val recipeSections =
        combine(recipeRepository.observeRecipes(), recipeHistoryRepository.recipeIds) {
            recipes,
            historyIds ->
            recipes to recipesForHistory(historyIds, recipes)
        }

    private val syncPresentation =
        combine(syncStatusRepository.status, userRefreshRequested) { syncStatus, userRefresh ->
            syncStatus to userRefresh
        }

    val uiState: StateFlow<HomeUiState> =
        combine(
                recipeSections,
                favoriteRecipes,
                settingsRepository.credentials,
                syncPresentation,
            ) {
                (recipes, recentlyViewed),
                favorites,
                credentials,
                (syncStatus, userRefresh) ->
                HomeUiState(
                    recentlyViewedRecipes = recentlyViewed,
                    recentlyAddedRecipes =
                        sortRecipesByDate(recipes, RecipeSummaryEntity::dateAdded),
                    recentlyCookedRecipes =
                        sortRecipesByDate(recipes, RecipeSummaryEntity::lastMade),
                    favoriteRecipes = sortFavoriteRecipes(favorites, MAX_PREVIEW),
                    serverUrl = credentials.serverUrl,
                    refreshState =
                        RefreshState(
                            // The Home sync card owns automatic-sync feedback. The pull-to-refresh
                            // indicator is reserved for a gesture explicitly initiated by the user.
                            isRefreshing = isHomePullToRefreshActive(syncStatus, userRefresh),
                            errorMessage =
                                syncStatus.errorMessage?.let {
                                    "Couldn't refresh. Showing saved data. Check your connection."
                                },
                        ),
                    syncStatus = syncStatus,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    fun refresh() {
        userRefreshRequested.value = true
        syncScheduler.syncAll()
    }

    private companion object {
        const val MAX_PREVIEW = 5
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

internal const val HOME_RECIPE_PREVIEW_LIMIT = 5

internal fun isHomePullToRefreshActive(status: SyncStatus, userRefreshRequested: Boolean): Boolean =
    userRefreshRequested && status.state == SyncStatusState.SYNCING

fun sortRecipesByDate(
    recipes: List<RecipeSummaryEntity>,
    date: (RecipeSummaryEntity) -> String?,
    limit: Int = HOME_RECIPE_PREVIEW_LIMIT,
): List<RecipeSummaryEntity> =
    recipes
        .sortedWith(
            compareBy<RecipeSummaryEntity> { date(it).isNullOrBlank() }
                .thenByDescending { date(it).orEmpty() }
                .thenBy { it.name.lowercase() }
        )
        .take(limit.coerceAtLeast(0))

fun sortFavoriteRecipes(
    recipes: List<RecipeSummaryEntity>,
    limit: Int = HOME_RECIPE_PREVIEW_LIMIT,
): List<RecipeSummaryEntity> =
    recipes.sortedBy { it.name.lowercase() }.take(limit.coerceAtLeast(0))

/** Resolves the ordered local history against cached summaries, omitting missing cache entries. */
fun recipesForHistory(
    historyIds: List<String>,
    recipes: List<RecipeSummaryEntity>,
    limit: Int = HOME_RECIPE_PREVIEW_LIMIT,
): List<RecipeSummaryEntity> {
    val recipesById = recipes.associateBy { it.id }
    return historyIds
        .mapNotNull { recipesById[it] }
        .distinctBy { it.id }
        .take(limit.coerceAtLeast(0))
}

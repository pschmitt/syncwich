package dev.pschmitt.syncwich.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.CookbookEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.data.repository.CookbookRepository
import dev.pschmitt.syncwich.data.repository.RecipeHistoryRepository
import dev.pschmitt.syncwich.data.repository.RecipeRepository
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import dev.pschmitt.syncwich.sync.SyncScheduler
import dev.pschmitt.syncwich.sync.SyncStatus
import dev.pschmitt.syncwich.sync.SyncStatusRepository
import dev.pschmitt.syncwich.sync.SyncStatusState
import dev.pschmitt.syncwich.ui.common.RefreshState
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val recentlyViewedRecipes: List<RecipeSummaryEntity> = emptyList(),
    val recentlyAddedRecipes: List<RecipeSummaryEntity> = emptyList(),
    val recentlyCookedRecipes: List<RecipeSummaryEntity> = emptyList(),
    val favoriteRecipes: List<RecipeSummaryEntity> = emptyList(),
    val favoriteCookbook: CookbookEntity? = null,
    val serverUrl: String = "",
    val refreshState: RefreshState = RefreshState(),
    val syncStatus: SyncStatus = SyncStatus(),
)

/**
 * Reads every dashboard section from Room first. Home refresh queues the existing full background
 * worker and is deliberately independent from the state flow, so a disconnected server never
 * hides recipes that are already cached.
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel
@Inject
constructor(
    private val recipeRepository: RecipeRepository,
    private val recipeHistoryRepository: RecipeHistoryRepository,
    private val cookbookRepository: CookbookRepository,
    settingsRepository: SettingsRepository,
    private val syncScheduler: SyncScheduler,
    syncStatusRepository: SyncStatusRepository,
) : ViewModel() {

    private val userRefreshRequested = MutableStateFlow(false)

    private val favoriteCookbook =
        cookbookRepository
            .observeCookbooks()
            .map(::findFavoriteCookbook)
            .distinctUntilChanged()

    private val favoriteRecipes =
        favoriteCookbook.flatMapLatest { cookbook ->
            cookbook?.let { cookbookRepository.observeCookbookRecipes(it.id) }
                ?: flowOf(emptyList())
        }

    private val recipeSections =
        combine(recipeRepository.observeRecipes(), recipeHistoryRepository.recipeIds) {
                recipes,
                historyIds,
            ->
            recipes to recipesForHistory(historyIds, recipes)
        }

    private val syncPresentation =
        combine(syncStatusRepository.status, userRefreshRequested) { syncStatus, userRefresh ->
            syncStatus to userRefresh
        }

    val uiState: StateFlow<HomeUiState> =
        combine(
                recipeSections,
                favoriteCookbook,
                favoriteRecipes,
                settingsRepository.credentials,
                syncPresentation,
            ) {
                (recipes, recentlyViewed),
                favoritesCookbook,
                favorites,
                credentials,
                (syncStatus, userRefresh),
                ->
                HomeUiState(
                    recentlyViewedRecipes = recentlyViewed,
                    recentlyAddedRecipes =
                        sortRecipesByDate(recipes, RecipeSummaryEntity::dateAdded),
                    recentlyCookedRecipes =
                        sortRecipesByDate(recipes, RecipeSummaryEntity::lastMade),
                    favoriteRecipes = favorites.sortedBy { it.name.lowercase() }.take(MAX_PREVIEW),
                    favoriteCookbook = favoritesCookbook,
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

    init {
        // Automatic startup sync is reported by HomeSyncStatusCard, not by the pull gesture's
        // indicator. This avoids showing a second refresh icon while the app is opening.
        syncScheduler.syncAll()
    }

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

fun findFavoriteCookbook(cookbooks: List<CookbookEntity>): CookbookEntity? =
    cookbooks.firstOrNull { cookbook ->
        cookbook.name.trim().equals("favorites", ignoreCase = true) ||
            cookbook.name.trim().equals("favourites", ignoreCase = true)
    }

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

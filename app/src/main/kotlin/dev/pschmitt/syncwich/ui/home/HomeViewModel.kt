package dev.pschmitt.syncwich.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.CookbookEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.data.repository.CookbookRepository
import dev.pschmitt.syncwich.data.repository.RecipeRepository
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import dev.pschmitt.syncwich.ui.common.RefreshState
import dev.pschmitt.syncwich.ui.common.refreshErrorMessage
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val recentlyAddedRecipes: List<RecipeSummaryEntity> = emptyList(),
    val recentlyCookedRecipes: List<RecipeSummaryEntity> = emptyList(),
    val favoriteRecipes: List<RecipeSummaryEntity> = emptyList(),
    val favoriteCookbook: CookbookEntity? = null,
    val serverUrl: String = "",
    val refreshState: RefreshState = RefreshState(),
)

/**
 * Reads every dashboard section from Room first. Refreshing only nudges the existing repositories
 * and is deliberately independent from the state flow, so a disconnected server never hides
 * recipes that are already cached.
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel
@Inject
constructor(
    private val recipeRepository: RecipeRepository,
    private val cookbookRepository: CookbookRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val refreshState = MutableStateFlow(RefreshState())

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

    val uiState: StateFlow<HomeUiState> =
        combine(
                recipeRepository.observeRecipes(),
                favoriteCookbook,
                favoriteRecipes,
                settingsRepository.credentials,
                refreshState,
            ) { recipes, favoritesCookbook, favorites, credentials, refresh ->
                HomeUiState(
                    recentlyAddedRecipes =
                        sortRecipesByDate(recipes, RecipeSummaryEntity::dateAdded),
                    recentlyCookedRecipes =
                        sortRecipesByDate(recipes, RecipeSummaryEntity::lastMade),
                    favoriteRecipes = favorites.sortedBy { it.name.lowercase() }.take(MAX_PREVIEW),
                    favoriteCookbook = favoritesCookbook,
                    serverUrl = credentials.serverUrl,
                    refreshState = refresh,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshState.value = RefreshState(isRefreshing = true)
            val results =
                coroutineScope {
                    listOf(
                            async { recipeRepository.refreshRecipes() },
                            async { cookbookRepository.refreshCookbooks() },
                        )
                        .awaitAll()
                }
            refreshState.value =
                RefreshState(errorMessage = results.firstNotNullOfOrNull(::refreshErrorMessage))
        }
    }

    private companion object {
        const val MAX_PREVIEW = 5
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

internal const val HOME_RECIPE_PREVIEW_LIMIT = 5

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

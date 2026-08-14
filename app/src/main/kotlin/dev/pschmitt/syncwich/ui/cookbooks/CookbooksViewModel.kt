package dev.pschmitt.syncwich.ui.cookbooks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.CookbookEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.data.repository.CookbookRepository
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import dev.pschmitt.syncwich.ui.common.RefreshState
import dev.pschmitt.syncwich.ui.common.refreshErrorMessage
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
class CookbooksViewModel
@Inject
constructor(
    private val cookbookRepository: CookbookRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _refreshState = MutableStateFlow(RefreshState())
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()
    private var refreshJob: Job? = null

    val cookbooks: StateFlow<List<CookbookEntity>> =
        combine(cookbookRepository.observeCookbooks(), searchQuery) { cookbooks, query ->
                filterCookbooksByQuery(cookbooks, query)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** Cached recipe summaries used for cookbook cards; full recipe details are never requested. */
    val recipePreviews: StateFlow<Map<String, List<RecipeSummaryEntity>>> =
        cookbookRepository
            .observeCookbooks()
            .flatMapLatest { cookbooks ->
                if (cookbooks.isEmpty()) {
                    flowOf(emptyMap())
                } else {
                    combine(cookbooks.map { cookbook ->
                        cookbookRepository.observeCookbookRecipes(cookbook.id)
                    }) { recipesByCookbook ->
                        cookbooks
                            .mapIndexed { index, cookbook ->
                                cookbook.id to recipesByCookbook[index]
                            }
                            .toMap()
                    }
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                emptyMap(),
            )

    /** Used to build a recipe's cover-image URL - see `RecipeSummaryEntity.image`'s kdoc. */
    val serverUrl: String
        get() = settingsRepository.credentials.value.serverUrl

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    init { refresh(forceRefresh = false) }

    fun refresh() = refresh(forceRefresh = true)

    private fun refresh(forceRefresh: Boolean) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _refreshState.value = RefreshState(isRefreshing = true)
            _refreshState.value =
                RefreshState(
                    errorMessage =
                        refreshErrorMessage(cookbookRepository.refreshCookbooks(forceRefresh))
                )
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

fun filterCookbooksByQuery(
    cookbooks: List<CookbookEntity>,
    query: String,
): List<CookbookEntity> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return cookbooks

    return cookbooks.filter { cookbook ->
        cookbook.name.contains(normalizedQuery, ignoreCase = true) ||
            cookbook.description.contains(normalizedQuery, ignoreCase = true)
    }
}

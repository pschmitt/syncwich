package dev.pschmitt.syncwich.ui.cookbooks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.CookbookEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.data.repository.CookbookRepository
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import dev.pschmitt.syncwich.ui.common.RefreshState
import dev.pschmitt.syncwich.ui.common.refreshErrorMessage
import dev.pschmitt.syncwich.ui.navigation.Route
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CookbookDeleteUiState {
    data object Idle : CookbookDeleteUiState

    data object Deleting : CookbookDeleteUiState

    data class Failed(val message: String) : CookbookDeleteUiState

    data object Deleted : CookbookDeleteUiState
}

@HiltViewModel
class CookbookDetailViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val cookbookRepository: CookbookRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.CookbookDetail>()
    private val requestedCookbookId = route.cookbookId
    private val _refreshState = MutableStateFlow(RefreshState())
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()
    private val _deleteState = MutableStateFlow<CookbookDeleteUiState>(CookbookDeleteUiState.Idle)
    val deleteState: StateFlow<CookbookDeleteUiState> = _deleteState.asStateFlow()
    private var refreshJob: Job? = null
    private var lastDeleteCookbookId: String? = null

    /** Used to build a recipe's cover-image URL - see `RecipeSummaryEntity.image`'s kdoc. */
    val serverUrl: String
        get() = settingsRepository.credentials.value.serverUrl

    val cookbook: StateFlow<CookbookEntity?> =
        (if (requestedCookbookId.isBlank()) {
                cookbookRepository.observeCookbookBySlug(route.slug)
            } else {
                cookbookRepository.observeCookbook(requestedCookbookId)
            })
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val recipes: StateFlow<List<RecipeSummaryEntity>> =
        cookbook
            .flatMapLatest { current ->
                current?.let { cookbookRepository.observeCookbookRecipes(it.id) }
                    ?: flowOf(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        refresh(forceRefresh = false)
    }

    fun refresh() = refresh(forceRefresh = true)

    fun deleteCookbook(cookbookId: String = requestedCookbookId) {
        if (_deleteState.value is CookbookDeleteUiState.Deleting) return
        if (cookbookId.isBlank()) return
        lastDeleteCookbookId = cookbookId
        viewModelScope.launch {
            _deleteState.value = CookbookDeleteUiState.Deleting
            _deleteState.value =
                cookbookRepository.deleteCookbook(cookbookId).fold(
                    onSuccess = { CookbookDeleteUiState.Deleted },
                    onFailure = {
                        CookbookDeleteUiState.Failed(
                            "Couldn't delete the cookbook. Your saved copy is still available; " +
                                "check your connection and try again."
                        )
                    },
                )
        }
    }

    fun retryDelete() {
        lastDeleteCookbookId?.let(::deleteCookbook)
    }

    private fun refresh(forceRefresh: Boolean) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _refreshState.value = RefreshState(isRefreshing = true)
            val result =
                if (requestedCookbookId.isBlank()) {
                    cookbookRepository
                        .refreshCookbooks(forceRefresh)
                        .fold(
                            onSuccess = {
                                val discovered = cookbook.first()
                                if (discovered == null) {
                                    Result.failure(IllegalStateException("Cookbook not found"))
                                } else {
                                    cookbookRepository.refreshCookbookRecipes(
                                        discovered.id,
                                        forceRefresh,
                                    )
                                }
                            },
                            onFailure = { Result.failure(it) },
                        )
                } else {
                    cookbookRepository.refreshCookbookRecipes(requestedCookbookId, forceRefresh)
                }
            _refreshState.value = RefreshState(errorMessage = refreshErrorMessage(result))
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CookbookDetailViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val cookbookRepository: CookbookRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val cookbookId = savedStateHandle.toRoute<Route.CookbookDetail>().cookbookId
    private val _refreshState = MutableStateFlow(RefreshState())
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    /** Used to build a recipe's cover-image URL - see `RecipeSummaryEntity.image`'s kdoc. */
    val serverUrl: String
        get() = settingsRepository.credentials.value.serverUrl

    val cookbook: StateFlow<CookbookEntity?> =
        cookbookRepository
            .observeCookbook(cookbookId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val recipes: StateFlow<List<RecipeSummaryEntity>> =
        cookbookRepository
            .observeCookbookRecipes(cookbookId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _refreshState.value = RefreshState(isRefreshing = true)
            _refreshState.value =
                RefreshState(errorMessage = refreshErrorMessage(cookbookRepository.refreshCookbooks()))
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

package dev.pschmitt.syncwich.ui.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.RecipeTimelineEventEntity
import dev.pschmitt.syncwich.data.repository.RecipeTimelineRepository
import dev.pschmitt.syncwich.ui.common.RefreshState
import dev.pschmitt.syncwich.ui.common.refreshErrorMessage
import dev.pschmitt.syncwich.ui.navigation.Route
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs [RecipeTimelineScreen] - a cache-first, read-only view of one recipe's confirmed
 * cooking-event history (see [RecipeTimelineRepository]'s kdoc for how that data was confirmed).
 */
@HiltViewModel
class RecipeTimelineViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, private val repository: RecipeTimelineRepository) :
    ViewModel() {

    private val recipeId: String = savedStateHandle.toRoute<Route.RecipeTimeline>().recipeId
    private val _refreshState = MutableStateFlow(RefreshState())
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    val events: StateFlow<List<RecipeTimelineEventEntity>> =
        repository
            .observe(recipeId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshState.value = RefreshState(isRefreshing = true)
            _refreshState.value =
                RefreshState(
                    errorMessage = refreshErrorMessage(repository.refreshFromServer(recipeId))
                )
        }
    }
}

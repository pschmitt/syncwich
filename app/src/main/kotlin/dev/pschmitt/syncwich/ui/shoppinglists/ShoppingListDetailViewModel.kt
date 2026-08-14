package dev.pschmitt.syncwich.ui.shoppinglists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.ShoppingListEntity
import dev.pschmitt.syncwich.data.db.entity.ShoppingListItemEntity
import dev.pschmitt.syncwich.data.repository.ShoppingListRepository
import dev.pschmitt.syncwich.ui.navigation.Route
import dev.pschmitt.syncwich.ui.common.RefreshState
import dev.pschmitt.syncwich.ui.common.refreshErrorMessage
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ShoppingListDetailViewModel
@Inject
constructor(private val repository: ShoppingListRepository, savedStateHandle: SavedStateHandle) :
    ViewModel() {

    private val listId: String = savedStateHandle.toRoute<Route.ShoppingListDetail>().listId
    private val _refreshState = MutableStateFlow(RefreshState())
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    val list: StateFlow<ShoppingListEntity?> =
        repository
            .observeList(listId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val items: StateFlow<List<ShoppingListItemEntity>> =
        repository
            .observeItems(listId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _refreshState.value = RefreshState(isRefreshing = true)
            _refreshState.value =
                RefreshState(errorMessage = refreshErrorMessage(repository.refreshListDetail(listId)))
        }
    }
}

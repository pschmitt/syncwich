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
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ShoppingListDetailViewModel
@Inject
constructor(private val repository: ShoppingListRepository, savedStateHandle: SavedStateHandle) :
    ViewModel() {

    private val listId: String = savedStateHandle.toRoute<Route.ShoppingListDetail>().listId

    val list: StateFlow<ShoppingListEntity?> =
        repository
            .observeList(listId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val items: StateFlow<List<ShoppingListItemEntity>> =
        repository
            .observeItems(listId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { repository.refreshListDetail(listId) }
    }
}

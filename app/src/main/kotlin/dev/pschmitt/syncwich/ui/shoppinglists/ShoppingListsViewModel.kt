package dev.pschmitt.syncwich.ui.shoppinglists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.ShoppingListEntity
import dev.pschmitt.syncwich.data.repository.ShoppingListRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ShoppingListsViewModel @Inject constructor(private val repository: ShoppingListRepository) :
    ViewModel() {

    val lists: StateFlow<List<ShoppingListEntity>> =
        repository
            .observeLists()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { repository.refreshLists() }
    }
}

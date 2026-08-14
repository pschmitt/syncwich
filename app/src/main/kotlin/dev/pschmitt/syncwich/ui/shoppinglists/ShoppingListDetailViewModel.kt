package dev.pschmitt.syncwich.ui.shoppinglists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.ShoppingListEntity
import dev.pschmitt.syncwich.data.db.entity.ShoppingListItemEntity
import dev.pschmitt.syncwich.data.repository.ShoppingListRepository
import dev.pschmitt.syncwich.ui.common.RefreshState
import dev.pschmitt.syncwich.ui.common.refreshErrorMessage
import dev.pschmitt.syncwich.ui.navigation.Route
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** In-screen "add item" dialog state (SW-24/SW-33), mirrors `MealPlanEditorState`'s shape. */
data class AddShoppingItemState(
    val isOpen: Boolean = false,
    val display: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ShoppingListDetailViewModel
@Inject
constructor(private val repository: ShoppingListRepository, savedStateHandle: SavedStateHandle) :
    ViewModel() {

    private val listId: String = savedStateHandle.toRoute<Route.ShoppingListDetail>().listId
    private val _refreshState = MutableStateFlow(RefreshState())
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    private val _addItemState = MutableStateFlow(AddShoppingItemState())
    val addItemState: StateFlow<AddShoppingItemState> = _addItemState.asStateFlow()

    val list: StateFlow<ShoppingListEntity?> =
        repository
            .observeList(listId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val items: StateFlow<List<ShoppingListItemEntity>> =
        repository
            .observeItems(listId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshState.value = RefreshState(isRefreshing = true)
            _refreshState.value =
                RefreshState(
                    errorMessage = refreshErrorMessage(repository.refreshListDetail(listId))
                )
        }
    }

    /**
     * Fire-and-forget, like `RecipeDetailViewModel.setFavorite`: [ShoppingListRepository]'s Room
     * write happens before any network call, so the checkbox already reflects the choice by the
     * time this suspend call returns; a failed sync is retried later, not surfaced here.
     */
    fun setChecked(itemId: String, checked: Boolean) {
        viewModelScope.launch { repository.setItemChecked(itemId, checked) }
    }

    /**
     * Fire-and-forget: a failed delete simply leaves the item cached, which is the correct state.
     */
    fun removeItem(itemId: String) {
        viewModelScope.launch { repository.removeItem(itemId) }
    }

    fun startAddItem() {
        _addItemState.value = AddShoppingItemState(isOpen = true)
    }

    fun onAddItemTextChange(value: String) {
        if (_addItemState.value.isSaving) return
        _addItemState.update { it.copy(display = value, errorMessage = null) }
    }

    fun dismissAddItem() {
        if (_addItemState.value.isSaving) return
        _addItemState.value = AddShoppingItemState()
    }

    fun confirmAddItem() {
        val state = _addItemState.value
        if (state.isSaving) return
        if (state.display.isBlank()) {
            _addItemState.update { it.copy(errorMessage = "Enter an item") }
            return
        }
        _addItemState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            repository
                .addItem(listId, state.display.trim())
                .fold(
                    onSuccess = { _addItemState.value = AddShoppingItemState() },
                    onFailure = {
                        _addItemState.update {
                            it.copy(
                                isSaving = false,
                                errorMessage =
                                    "Couldn't add item. Check your connection and try again.",
                            )
                        }
                    },
                )
        }
    }
}

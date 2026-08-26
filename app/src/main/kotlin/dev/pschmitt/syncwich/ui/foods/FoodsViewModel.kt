package dev.pschmitt.syncwich.ui.foods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.FoodEntity
import dev.pschmitt.syncwich.data.repository.FoodRepository
import dev.pschmitt.syncwich.ui.common.RefreshState
import dev.pschmitt.syncwich.ui.common.refreshErrorMessage
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FoodsViewModel @Inject constructor(private val foodRepository: FoodRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _refreshState = MutableStateFlow(RefreshState())
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    val foods: StateFlow<List<FoodEntity>> =
        combine(foodRepository.observeFoods(), searchQuery) { foods, query ->
                filterFoodsByQuery(foods, query)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        refresh()
    }

    fun onSearchQueryChange(value: String) {
        _searchQuery.value = value
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshState.value = RefreshState(isRefreshing = true)
            val result = foodRepository.refreshFoods()
            _refreshState.value = RefreshState(errorMessage = refreshErrorMessage(result))
        }
    }

    fun deleteFood(foodId: String) {
        viewModelScope.launch { foodRepository.deleteFood(foodId) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

internal fun filterFoodsByQuery(foods: List<FoodEntity>, query: String): List<FoodEntity> {
    if (query.isBlank()) return foods
    val trimmed = query.trim()
    return foods.filter {
        it.name.contains(trimmed, ignoreCase = true) ||
            it.pluralName?.contains(trimmed, ignoreCase = true) == true
    }
}

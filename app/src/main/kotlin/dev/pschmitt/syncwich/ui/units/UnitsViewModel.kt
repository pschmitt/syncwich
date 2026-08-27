package dev.pschmitt.syncwich.ui.units

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.UnitEntity
import dev.pschmitt.syncwich.data.repository.UnitRepository
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
class UnitsViewModel @Inject constructor(private val unitRepository: UnitRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _refreshState = MutableStateFlow(RefreshState())
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    val units: StateFlow<List<UnitEntity>> =
        combine(unitRepository.observeUnits(), searchQuery) { units, query ->
                filterUnitsByQuery(units, query)
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
            val result = unitRepository.refreshUnits()
            _refreshState.value = RefreshState(errorMessage = refreshErrorMessage(result))
        }
    }

    fun deleteUnit(unitId: String) {
        viewModelScope.launch { unitRepository.deleteUnit(unitId) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

internal fun filterUnitsByQuery(units: List<UnitEntity>, query: String): List<UnitEntity> {
    if (query.isBlank()) return units
    val trimmed = query.trim()
    return units.filter {
        it.name.contains(trimmed, ignoreCase = true) ||
            it.pluralName?.contains(trimmed, ignoreCase = true) == true ||
            it.abbreviation.contains(trimmed, ignoreCase = true)
    }
}

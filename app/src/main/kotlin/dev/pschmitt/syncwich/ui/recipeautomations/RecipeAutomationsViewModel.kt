package dev.pschmitt.syncwich.ui.recipeautomations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.RecipeAutomationEntity
import dev.pschmitt.syncwich.data.repository.RecipeAutomationRepository
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
class RecipeAutomationsViewModel
@Inject
constructor(private val recipeAutomationRepository: RecipeAutomationRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _refreshState = MutableStateFlow(RefreshState())
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    val automations: StateFlow<List<RecipeAutomationEntity>> =
        combine(recipeAutomationRepository.observeAutomations(), searchQuery) { automations, query
                ->
                filterAutomationsByQuery(automations, query)
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
            val result = recipeAutomationRepository.refreshAutomations()
            _refreshState.value = RefreshState(errorMessage = refreshErrorMessage(result))
        }
    }

    fun deleteAutomation(automationId: String) {
        viewModelScope.launch { recipeAutomationRepository.deleteAutomation(automationId) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

internal fun filterAutomationsByQuery(
    automations: List<RecipeAutomationEntity>,
    query: String,
): List<RecipeAutomationEntity> {
    if (query.isBlank()) return automations
    val trimmed = query.trim()
    return automations.filter {
        it.title.contains(trimmed, ignoreCase = true) || it.url.contains(trimmed, ignoreCase = true)
    }
}

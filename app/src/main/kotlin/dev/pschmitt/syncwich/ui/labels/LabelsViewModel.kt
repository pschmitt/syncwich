package dev.pschmitt.syncwich.ui.labels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.LabelEntity
import dev.pschmitt.syncwich.data.repository.LabelRepository
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
class LabelsViewModel @Inject constructor(private val labelRepository: LabelRepository) :
    ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _refreshState = MutableStateFlow(RefreshState())
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    val labels: StateFlow<List<LabelEntity>> =
        combine(labelRepository.observeLabels(), searchQuery) { labels, query ->
                filterLabelsByQuery(labels, query)
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
            val result = labelRepository.refreshLabels()
            _refreshState.value = RefreshState(errorMessage = refreshErrorMessage(result))
        }
    }

    fun deleteLabel(labelId: String) {
        viewModelScope.launch { labelRepository.deleteLabel(labelId) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

internal fun filterLabelsByQuery(labels: List<LabelEntity>, query: String): List<LabelEntity> {
    if (query.isBlank()) return labels
    val trimmed = query.trim()
    return labels.filter { it.name.contains(trimmed, ignoreCase = true) }
}

package dev.pschmitt.syncwich.ui.organizers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.ToolEntity
import dev.pschmitt.syncwich.data.repository.ToolRepository
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
class ToolsViewModel @Inject constructor(private val toolRepository: ToolRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _refreshState = MutableStateFlow(RefreshState())
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    val tools: StateFlow<List<ToolEntity>> =
        combine(toolRepository.observeTools(), searchQuery) { tools, query ->
                filterByQuery(tools, query, ToolEntity::name)
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
            val result = toolRepository.refreshTools()
            _refreshState.value = RefreshState(errorMessage = refreshErrorMessage(result))
        }
    }

    fun delete(toolId: String) {
        viewModelScope.launch { toolRepository.deleteTool(toolId) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

@Composable
fun ToolsScreen(
    onBack: () -> Unit,
    onToolClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: ToolsViewModel = hiltViewModel(),
) {
    val tools by viewModel.tools.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()

    SimpleCatalogScreen(
        title = "Tools",
        itemNounSingular = "tool",
        emptyIcon = Icons.Filled.Build,
        emptySubtitle = "Tools are kitchen equipment recipes can call for. Add one to get started.",
        items = tools.map { SimpleCatalogItem(it.id, it.name) },
        searchQuery = searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        refreshState = refreshState,
        onRefresh = viewModel::refresh,
        onBack = onBack,
        onItemClick = onToolClick,
        onCreateClick = onCreateClick,
        onDelete = viewModel::delete,
    )
}

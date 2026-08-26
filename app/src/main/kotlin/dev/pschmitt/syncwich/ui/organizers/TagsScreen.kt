package dev.pschmitt.syncwich.ui.organizers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Label
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.TagEntity
import dev.pschmitt.syncwich.data.repository.TagRepository
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
class TagsViewModel @Inject constructor(private val tagRepository: TagRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _refreshState = MutableStateFlow(RefreshState())
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    val tags: StateFlow<List<TagEntity>> =
        combine(tagRepository.observeTags(), searchQuery) { tags, query ->
                filterByQuery(tags, query, TagEntity::name)
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
            val result = tagRepository.refreshTags()
            _refreshState.value = RefreshState(errorMessage = refreshErrorMessage(result))
        }
    }

    fun delete(tagId: String) {
        viewModelScope.launch { tagRepository.deleteTag(tagId) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

@Composable
fun TagsScreen(
    onBack: () -> Unit,
    onTagClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: TagsViewModel = hiltViewModel(),
) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()

    SimpleCatalogScreen(
        title = "Tags",
        itemNounSingular = "tag",
        emptyIcon = Icons.Filled.Label,
        emptySubtitle = "Tags help you find recipes later. Add one to get started.",
        items = tags.map { SimpleCatalogItem(it.id, it.name) },
        searchQuery = searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        refreshState = refreshState,
        onRefresh = viewModel::refresh,
        onBack = onBack,
        onItemClick = onTagClick,
        onCreateClick = onCreateClick,
        onDelete = viewModel::delete,
    )
}

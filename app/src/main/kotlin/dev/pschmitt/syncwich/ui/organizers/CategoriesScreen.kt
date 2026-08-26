package dev.pschmitt.syncwich.ui.organizers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.CategoryEntity
import dev.pschmitt.syncwich.data.repository.CategoryRepository
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
class CategoriesViewModel @Inject constructor(private val categoryRepository: CategoryRepository) :
    ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _refreshState = MutableStateFlow(RefreshState())
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> =
        combine(categoryRepository.observeCategories(), searchQuery) { categories, query ->
                filterByQuery(categories, query, CategoryEntity::name)
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
            val result = categoryRepository.refreshCategories()
            _refreshState.value = RefreshState(errorMessage = refreshErrorMessage(result))
        }
    }

    fun delete(categoryId: String) {
        viewModelScope.launch { categoryRepository.deleteCategory(categoryId) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()

    SimpleCatalogScreen(
        title = "Categories",
        itemNounSingular = "category",
        emptyIcon = Icons.Filled.Category,
        emptySubtitle = "Categories organize your recipes. Add one to get started.",
        items = categories.map { SimpleCatalogItem(it.id, it.name) },
        searchQuery = searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        refreshState = refreshState,
        onRefresh = viewModel::refresh,
        onBack = onBack,
        onItemClick = onCategoryClick,
        onCreateClick = onCreateClick,
        onDelete = viewModel::delete,
    )
}

/** Case-insensitive substring filter shared by the SW-139 simple-catalog list screens. */
internal fun <T> filterByQuery(items: List<T>, query: String, name: (T) -> String): List<T> {
    if (query.isBlank()) return items
    val trimmed = query.trim()
    return items.filter { name(it).contains(trimmed, ignoreCase = true) }
}

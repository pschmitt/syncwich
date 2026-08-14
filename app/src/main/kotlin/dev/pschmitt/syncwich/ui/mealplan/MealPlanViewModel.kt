package dev.pschmitt.syncwich.ui.mealplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.MealPlanEntryEntity
import dev.pschmitt.syncwich.data.repository.MealPlanRepository
import dev.pschmitt.syncwich.ui.common.RefreshState
import dev.pschmitt.syncwich.ui.common.refreshErrorMessage
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MealPlanUiState(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val entries: List<MealPlanEntryEntity> = emptyList(),
    val refreshState: RefreshState = RefreshState(),
)

/**
 * In-screen add/edit state for [MealPlanScreen]'s meal-plan entry dialog (SW-24/SW-33). Mirrors
 * `CookbookEditorViewModel`'s draft/save-state split, but kept inline on `MealPlanScreen` rather
 * than a separate route since it edits one field of a still-visible day, not a whole standalone
 * object like a cookbook.
 */
data class MealPlanEditorState(
    val isOpen: Boolean = false,
    val editingId: Long? = null,
    val date: LocalDate = LocalDate.now(),
    val entryType: String = "dinner",
    val title: String = "",
    val text: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    val isEditing: Boolean
        get() = editingId != null
}

@HiltViewModel
class MealPlanViewModel @Inject constructor(private val mealPlanRepository: MealPlanRepository) :
    ViewModel() {

    private val weekStart = MutableStateFlow(LocalDate.now().with(DayOfWeek.MONDAY))
    private val refreshState = MutableStateFlow(RefreshState())

    private val _editorState = MutableStateFlow(MealPlanEditorState())
    val editorState: StateFlow<MealPlanEditorState> = _editorState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MealPlanUiState> =
        combine(weekStart, refreshState) { start, refresh -> start to refresh }
            .flatMapLatest { (start, refresh) ->
                val end = start.plusDays(6)
                mealPlanRepository.observeMealPlan(start, end).map { entries ->
                    MealPlanUiState(
                        weekStart = start,
                        weekEnd = end,
                        entries = entries,
                        refreshState = refresh,
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue =
                    MealPlanUiState(
                        weekStart = weekStart.value,
                        weekEnd = weekStart.value.plusDays(6),
                    ),
            )

    init {
        refresh()
    }

    fun showPreviousWeek() {
        weekStart.value = weekStart.value.minusWeeks(1)
        refresh()
    }

    fun showNextWeek() {
        weekStart.value = weekStart.value.plusWeeks(1)
        refresh()
    }

    fun showCurrentWeek() {
        weekStart.value = LocalDate.now().with(DayOfWeek.MONDAY)
        refresh()
    }

    fun refresh() {
        val start = weekStart.value
        val end = start.plusDays(6)
        viewModelScope.launch {
            refreshState.value = RefreshState(isRefreshing = true)
            refreshState.value =
                RefreshState(
                    errorMessage =
                        refreshErrorMessage(mealPlanRepository.refreshMealPlan(start, end))
                )
        }
    }

    fun startAddEntry(date: LocalDate) {
        _editorState.value = MealPlanEditorState(isOpen = true, date = date)
    }

    fun startEditEntry(entry: MealPlanEntryEntity) {
        _editorState.value =
            MealPlanEditorState(
                isOpen = true,
                editingId = entry.id,
                date = LocalDate.parse(entry.date),
                entryType = entry.entryType,
                title = entry.title,
                text = entry.text,
            )
    }

    fun dismissEditor() {
        if (_editorState.value.isSaving) return
        _editorState.value = MealPlanEditorState()
    }

    fun onEntryTypeChange(entryType: String) = updateEditor { copy(entryType = entryType) }

    fun onTitleChange(title: String) = updateEditor { copy(title = title) }

    fun onTextChange(text: String) = updateEditor { copy(text = text) }

    fun saveEntry() {
        val state = _editorState.value
        if (state.isSaving) return
        if (state.title.isBlank() && state.text.isBlank()) {
            _editorState.update { it.copy(errorMessage = "Enter a title or a note") }
            return
        }
        _editorState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val result =
                if (state.isEditing) {
                    mealPlanRepository.updateEntry(
                        id = requireNotNull(state.editingId),
                        date = state.date,
                        entryType = state.entryType,
                        title = state.title,
                        text = state.text,
                        recipeId = null,
                    )
                } else {
                    mealPlanRepository.createEntry(
                        date = state.date,
                        entryType = state.entryType,
                        title = state.title,
                        text = state.text,
                        recipeId = null,
                    )
                }
            result.fold(
                onSuccess = { _editorState.value = MealPlanEditorState() },
                onFailure = {
                    _editorState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage =
                                "Couldn't save. Your entry is still here; check your connection " +
                                    "and try again.",
                        )
                    }
                },
            )
        }
    }

    fun deleteEntry() {
        val id = _editorState.value.editingId ?: return
        if (_editorState.value.isSaving) return
        _editorState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            mealPlanRepository
                .deleteEntry(id)
                .fold(
                    onSuccess = { _editorState.value = MealPlanEditorState() },
                    onFailure = {
                        _editorState.update {
                            it.copy(
                                isSaving = false,
                                errorMessage =
                                    "Couldn't delete. Check your connection and try again.",
                            )
                        }
                    },
                )
        }
    }

    private fun updateEditor(update: MealPlanEditorState.() -> MealPlanEditorState) {
        if (_editorState.value.isSaving) return
        _editorState.update { it.update().copy(errorMessage = null) }
    }
}

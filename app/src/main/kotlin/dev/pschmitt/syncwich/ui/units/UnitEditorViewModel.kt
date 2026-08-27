package dev.pschmitt.syncwich.ui.units

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.repository.UnitRepository
import dev.pschmitt.syncwich.ui.navigation.Route
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface UnitEditorSaveState {
    data object Idle : UnitEditorSaveState

    data object Saving : UnitEditorSaveState

    data class Error(val message: String) : UnitEditorSaveState

    data object Saved : UnitEditorSaveState
}

@HiltViewModel
class UnitEditorViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, private val unitRepository: UnitRepository) :
    ViewModel() {

    private val route = savedStateHandle.toRoute<Route.UnitEditor>()
    private val unitId = route.unitId
    val isEditing: Boolean = unitId.isNotBlank()

    private val _draft = MutableStateFlow(UnitEditorDraft())
    val draft: StateFlow<UnitEditorDraft> = _draft.asStateFlow()

    private val _saveState = MutableStateFlow<UnitEditorSaveState>(UnitEditorSaveState.Idle)
    val saveState: StateFlow<UnitEditorSaveState> = _saveState.asStateFlow()

    private var draftTouched = false

    init {
        if (isEditing) {
            viewModelScope.launch {
                val cached = unitRepository.observeUnit(unitId).first()
                if (!draftTouched && cached != null) _draft.value = UnitEditorDraft.from(cached)
            }
        }
    }

    private fun updateDraft(block: (UnitEditorDraft) -> UnitEditorDraft) {
        if (_saveState.value is UnitEditorSaveState.Saving) return
        draftTouched = true
        _draft.value = block(_draft.value)
        _saveState.value = UnitEditorSaveState.Idle
    }

    fun onNameChange(value: String) = updateDraft { it.copy(name = value) }

    fun onPluralNameChange(value: String) = updateDraft { it.copy(pluralName = value) }

    fun onDescriptionChange(value: String) = updateDraft { it.copy(description = value) }

    fun onAbbreviationChange(value: String) = updateDraft { it.copy(abbreviation = value) }

    fun save() {
        if (_saveState.value is UnitEditorSaveState.Saving) return
        val draft = _draft.value
        val validationError = draft.validationError()
        if (validationError != null) {
            _saveState.value = UnitEditorSaveState.Error(validationError)
            return
        }
        _saveState.value = UnitEditorSaveState.Saving
        viewModelScope.launch {
            val name = draft.name.trim()
            val pluralName = draft.pluralName.trim().takeIf(String::isNotBlank)
            val description = draft.description.trim()
            val abbreviation = draft.abbreviation.trim()
            val result =
                if (isEditing) {
                    unitRepository.updateUnit(unitId, name, pluralName, description, abbreviation)
                } else {
                    unitRepository.createUnit(name, pluralName, description, abbreviation)
                }
            _saveState.value =
                result.fold(
                    onSuccess = { UnitEditorSaveState.Saved },
                    onFailure = {
                        UnitEditorSaveState.Error(
                            "Couldn't save unit. Your draft is still here; check your" +
                                " connection and try again."
                        )
                    },
                )
        }
    }
}

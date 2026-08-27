package dev.pschmitt.syncwich.ui.labels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.repository.LabelRepository
import dev.pschmitt.syncwich.ui.navigation.Route
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val DEFAULT_COLOR = "#959595"

sealed interface LabelEditorSaveState {
    data object Idle : LabelEditorSaveState

    data object Saving : LabelEditorSaveState

    data class Error(val message: String) : LabelEditorSaveState

    data object Saved : LabelEditorSaveState
}

@HiltViewModel
class LabelEditorViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, private val labelRepository: LabelRepository) :
    ViewModel() {

    private val route = savedStateHandle.toRoute<Route.LabelEditor>()
    private val labelId = route.labelId
    val isEditing: Boolean = labelId.isNotBlank()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _color = MutableStateFlow(DEFAULT_COLOR)
    val color: StateFlow<String> = _color.asStateFlow()

    private val _saveState = MutableStateFlow<LabelEditorSaveState>(LabelEditorSaveState.Idle)
    val saveState: StateFlow<LabelEditorSaveState> = _saveState.asStateFlow()

    private var touched = false

    init {
        if (isEditing) {
            viewModelScope.launch {
                val cached = labelRepository.observeLabel(labelId).first()
                if (!touched && cached != null) {
                    _name.value = cached.name
                    _color.value = cached.color
                }
            }
        }
    }

    fun onNameChange(value: String) {
        if (_saveState.value is LabelEditorSaveState.Saving) return
        touched = true
        _name.value = value
        _saveState.value = LabelEditorSaveState.Idle
    }

    fun onColorChange(value: String) {
        if (_saveState.value is LabelEditorSaveState.Saving) return
        touched = true
        _color.value = value
        _saveState.value = LabelEditorSaveState.Idle
    }

    fun save() {
        if (_saveState.value is LabelEditorSaveState.Saving) return
        val trimmedName = _name.value.trim()
        if (trimmedName.isEmpty()) {
            _saveState.value = LabelEditorSaveState.Error("Enter a label name")
            return
        }
        val trimmedColor = _color.value.trim().ifBlank { DEFAULT_COLOR }
        if (!isValidHexColor(trimmedColor)) {
            _saveState.value = LabelEditorSaveState.Error("Color must be a hex code like #959595")
            return
        }
        _saveState.value = LabelEditorSaveState.Saving
        viewModelScope.launch {
            val result =
                if (isEditing) {
                    labelRepository.updateLabel(labelId, trimmedName, trimmedColor)
                } else {
                    labelRepository.createLabel(trimmedName, trimmedColor)
                }
            _saveState.value =
                result.fold(
                    onSuccess = { LabelEditorSaveState.Saved },
                    onFailure = {
                        LabelEditorSaveState.Error(
                            "Couldn't save label. Your draft is still here; check your" +
                                " connection and try again."
                        )
                    },
                )
        }
    }
}

private fun isValidHexColor(value: String): Boolean =
    Regex("^#[0-9a-fA-F]{6}$").matches(value)

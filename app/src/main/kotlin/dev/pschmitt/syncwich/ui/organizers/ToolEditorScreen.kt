package dev.pschmitt.syncwich.ui.organizers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.repository.ToolRepository
import dev.pschmitt.syncwich.ui.navigation.Route
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class ToolEditorViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, private val toolRepository: ToolRepository) :
    ViewModel() {

    private val route = savedStateHandle.toRoute<Route.ToolEditor>()
    private val toolId = route.toolId
    val isEditing: Boolean = toolId.isNotBlank()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _saveState = MutableStateFlow<SimpleCatalogSaveState>(SimpleCatalogSaveState.Idle)
    val saveState: StateFlow<SimpleCatalogSaveState> = _saveState.asStateFlow()

    private var nameTouched = false

    init {
        if (isEditing) {
            viewModelScope.launch {
                val cached = toolRepository.observeTool(toolId).first()
                if (!nameTouched && cached != null) _name.value = cached.name
            }
        }
    }

    fun onNameChange(value: String) {
        if (_saveState.value is SimpleCatalogSaveState.Saving) return
        nameTouched = true
        _name.value = value
        _saveState.value = SimpleCatalogSaveState.Idle
    }

    fun save() {
        if (_saveState.value is SimpleCatalogSaveState.Saving) return
        val trimmed = _name.value.trim()
        if (trimmed.isEmpty()) {
            _saveState.value = SimpleCatalogSaveState.Error("Enter a tool name")
            return
        }
        _saveState.value = SimpleCatalogSaveState.Saving
        viewModelScope.launch {
            val result =
                if (isEditing) {
                    toolRepository.updateTool(toolId, trimmed)
                } else {
                    toolRepository.createTool(trimmed)
                }
            _saveState.value =
                result.fold(
                    onSuccess = { SimpleCatalogSaveState.Saved },
                    onFailure = {
                        SimpleCatalogSaveState.Error(
                            "Couldn't save tool. Your draft is still here; check your connection" +
                                " and try again."
                        )
                    },
                )
        }
    }
}

@Composable
fun ToolEditorScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: ToolEditorViewModel = hiltViewModel(),
) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val isSaving = saveState is SimpleCatalogSaveState.Saving

    LaunchedEffect(saveState) { if (saveState is SimpleCatalogSaveState.Saved) onSaved() }

    SimpleCatalogEditorScreen(
        itemNounSingular = "tool",
        isEditing = viewModel.isEditing,
        name = name,
        onNameChange = viewModel::onNameChange,
        isSaving = isSaving,
        validationMessage = (saveState as? SimpleCatalogSaveState.Error)?.message,
        onSave = viewModel::save,
        onBack = onBack,
    )
}

package dev.pschmitt.syncwich.ui.recipeautomations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.repository.RecipeAutomationRepository
import dev.pschmitt.syncwich.ui.navigation.Route
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal const val ACTION_TYPE_LINK = "link"
internal const val ACTION_TYPE_POST = "post"

sealed interface RecipeAutomationEditorSaveState {
    data object Idle : RecipeAutomationEditorSaveState

    data object Saving : RecipeAutomationEditorSaveState

    data class Error(val message: String) : RecipeAutomationEditorSaveState

    data object Saved : RecipeAutomationEditorSaveState
}

@HiltViewModel
class RecipeAutomationEditorViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeAutomationRepository: RecipeAutomationRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.RecipeAutomationEditor>()
    private val automationId = route.automationId
    val isEditing: Boolean = automationId.isNotBlank()

    private val _actionType = MutableStateFlow(ACTION_TYPE_LINK)
    val actionType: StateFlow<String> = _actionType.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _saveState =
        MutableStateFlow<RecipeAutomationEditorSaveState>(RecipeAutomationEditorSaveState.Idle)
    val saveState: StateFlow<RecipeAutomationEditorSaveState> = _saveState.asStateFlow()

    private var touched = false

    init {
        if (isEditing) {
            viewModelScope.launch {
                val cached = recipeAutomationRepository.observeAutomation(automationId).first()
                if (!touched && cached != null) {
                    _actionType.value = cached.actionType
                    _title.value = cached.title
                    _url.value = cached.url
                }
            }
        }
    }

    fun onActionTypeChange(value: String) {
        if (_saveState.value is RecipeAutomationEditorSaveState.Saving) return
        touched = true
        _actionType.value = value
        _saveState.value = RecipeAutomationEditorSaveState.Idle
    }

    fun onTitleChange(value: String) {
        if (_saveState.value is RecipeAutomationEditorSaveState.Saving) return
        touched = true
        _title.value = value
        _saveState.value = RecipeAutomationEditorSaveState.Idle
    }

    fun onUrlChange(value: String) {
        if (_saveState.value is RecipeAutomationEditorSaveState.Saving) return
        touched = true
        _url.value = value
        _saveState.value = RecipeAutomationEditorSaveState.Idle
    }

    fun save() {
        if (_saveState.value is RecipeAutomationEditorSaveState.Saving) return
        val trimmedTitle = _title.value.trim()
        val trimmedUrl = _url.value.trim()
        if (trimmedTitle.isEmpty()) {
            _saveState.value = RecipeAutomationEditorSaveState.Error("Enter a title")
            return
        }
        if (trimmedUrl.isEmpty()) {
            _saveState.value = RecipeAutomationEditorSaveState.Error("Enter a URL")
            return
        }
        _saveState.value = RecipeAutomationEditorSaveState.Saving
        viewModelScope.launch {
            val result =
                if (isEditing) {
                    recipeAutomationRepository.updateAutomation(
                        automationId,
                        _actionType.value,
                        trimmedTitle,
                        trimmedUrl,
                    )
                } else {
                    recipeAutomationRepository.createAutomation(
                        _actionType.value,
                        trimmedTitle,
                        trimmedUrl,
                    )
                }
            _saveState.value =
                result.fold(
                    onSuccess = { RecipeAutomationEditorSaveState.Saved },
                    onFailure = {
                        RecipeAutomationEditorSaveState.Error(
                            "Couldn't save recipe action. Your draft is still here; check your" +
                                " connection and try again."
                        )
                    },
                )
        }
    }
}

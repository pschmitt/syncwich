package dev.pschmitt.syncwich.ui.cookbooks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.repository.CookbookRepository
import dev.pschmitt.syncwich.ui.navigation.Route
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface CookbookEditorSaveState {
    data object Idle : CookbookEditorSaveState

    data object Saving : CookbookEditorSaveState

    data class Error(val message: String) : CookbookEditorSaveState

    data object Saved : CookbookEditorSaveState
}

/** Coordinates one explicit cookbook mutation without replacing the user's draft on failure. */
@HiltViewModel
class CookbookEditorViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val cookbookRepository: CookbookRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.CookbookEditor>()
    private val cookbookId = route.cookbookId
    val isEditing: Boolean = cookbookId.isNotBlank()

    private val _draft = MutableStateFlow(CookbookEditorDraft())
    val draft: StateFlow<CookbookEditorDraft> = _draft.asStateFlow()

    private val _saveState = MutableStateFlow<CookbookEditorSaveState>(CookbookEditorSaveState.Idle)
    val saveState: StateFlow<CookbookEditorSaveState> = _saveState.asStateFlow()

    private var draftTouched = false

    init {
        if (isEditing) loadCachedDraft()
    }

    fun onNameChange(value: String) = updateDraft { copy(name = value) }

    fun onDescriptionChange(value: String) = updateDraft { copy(description = value) }

    fun onQueryFilterChange(value: String) = updateDraft { copy(queryFilterString = value) }

    fun onPublicChange(value: Boolean) = updateDraft { copy(public = value) }

    fun save() {
        if (_saveState.value is CookbookEditorSaveState.Saving) return

        val draftSnapshot = _draft.value
        draftSnapshot.validationError()?.let { message ->
            _saveState.value = CookbookEditorSaveState.Error(message)
            return
        }
        if (isEditing && draftSnapshot.existingSlug.isNullOrBlank()) {
            _saveState.value =
                CookbookEditorSaveState.Error(
                    "This cookbook is not cached on this device. Sync it before editing."
                )
            return
        }

        _saveState.value = CookbookEditorSaveState.Saving
        viewModelScope.launch {
            val result =
                if (isEditing) {
                    cookbookRepository.updateCookbook(cookbookId, draftSnapshot.toRequest())
                } else {
                    cookbookRepository.createCookbook(draftSnapshot.toRequest())
                }
            _saveState.value =
                result.fold(
                    onSuccess = { CookbookEditorSaveState.Saved },
                    onFailure = {
                        CookbookEditorSaveState.Error(
                            "Couldn't save cookbook. Your draft is still here; check your " +
                                "connection and try again."
                        )
                    },
                )
        }
    }

    private fun loadCachedDraft() {
        viewModelScope.launch {
            val cachedCookbook = cookbookRepository.observeCookbook(cookbookId).first()
            if (!draftTouched && cachedCookbook != null) {
                _draft.value = CookbookEditorDraft.from(cachedCookbook)
            }
        }
    }

    private fun updateDraft(update: CookbookEditorDraft.() -> CookbookEditorDraft) {
        if (_saveState.value is CookbookEditorSaveState.Saving) return
        draftTouched = true
        _draft.value = _draft.value.update()
        _saveState.value = CookbookEditorSaveState.Idle
    }
}

package dev.pschmitt.syncwich.ui.foods

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.repository.FoodRepository
import dev.pschmitt.syncwich.ui.navigation.Route
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface FoodEditorSaveState {
    data object Idle : FoodEditorSaveState

    data object Saving : FoodEditorSaveState

    data class Error(val message: String) : FoodEditorSaveState

    data object Saved : FoodEditorSaveState
}

/** Coordinates one explicit food mutation without replacing the user's draft on failure. */
@HiltViewModel
class FoodEditorViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, private val foodRepository: FoodRepository) :
    ViewModel() {

    private val route = savedStateHandle.toRoute<Route.FoodEditor>()
    private val foodId = route.foodId
    val isEditing: Boolean = foodId.isNotBlank()

    private val _draft =
        MutableStateFlow(
            route.seedName?.takeIf { it.isNotBlank() }?.let(FoodEditorDraft::seeded)
                ?: FoodEditorDraft()
        )
    val draft: StateFlow<FoodEditorDraft> = _draft.asStateFlow()

    private val _saveState = MutableStateFlow<FoodEditorSaveState>(FoodEditorSaveState.Idle)
    val saveState: StateFlow<FoodEditorSaveState> = _saveState.asStateFlow()

    private var draftTouched = false

    init {
        if (isEditing) loadCachedDraft()
    }

    fun onNameChange(value: String) = updateDraft { copy(name = value) }

    fun onPluralNameChange(value: String) = updateDraft { copy(pluralName = value) }

    fun onDescriptionChange(value: String) = updateDraft { copy(description = value) }

    fun save() {
        if (_saveState.value is FoodEditorSaveState.Saving) return

        val draftSnapshot = _draft.value
        draftSnapshot.validationError()?.let { message ->
            _saveState.value = FoodEditorSaveState.Error(message)
            return
        }

        _saveState.value = FoodEditorSaveState.Saving
        viewModelScope.launch {
            val result =
                if (isEditing) {
                    foodRepository.updateFood(foodId, draftSnapshot.toRequest())
                } else {
                    foodRepository.createFood(draftSnapshot.toRequest())
                }
            _saveState.value =
                result.fold(
                    onSuccess = { FoodEditorSaveState.Saved },
                    onFailure = {
                        FoodEditorSaveState.Error(
                            "Couldn't save food. Your draft is still here; check your connection " +
                                "and try again."
                        )
                    },
                )
        }
    }

    private fun loadCachedDraft() {
        viewModelScope.launch {
            val cachedFood = foodRepository.observeFood(foodId).first()
            if (!draftTouched && cachedFood != null) {
                _draft.value = FoodEditorDraft.from(cachedFood)
            }
        }
    }

    private fun updateDraft(update: FoodEditorDraft.() -> FoodEditorDraft) {
        if (_saveState.value is FoodEditorSaveState.Saving) return
        draftTouched = true
        _draft.value = _draft.value.update()
        _saveState.value = FoodEditorSaveState.Idle
    }
}

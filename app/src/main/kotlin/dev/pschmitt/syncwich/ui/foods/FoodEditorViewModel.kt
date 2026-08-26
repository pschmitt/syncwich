package dev.pschmitt.syncwich.ui.foods

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.db.entity.FoodEntity
import dev.pschmitt.syncwich.data.repository.FoodRepository
import dev.pschmitt.syncwich.ui.navigation.Route
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FoodEditorSaveState {
    data object Idle : FoodEditorSaveState

    data object Saving : FoodEditorSaveState

    data class Error(val message: String) : FoodEditorSaveState

    data object Saved : FoodEditorSaveState
}

/**
 * Coordinates one explicit food mutation without replacing the user's draft on failure.
 *
 * A long-pressed recipe ingredient only ever carries [Route.FoodEditor.seedName] (no `foodId` -
 * there's no structured food reference to follow, see
 * [dev.pschmitt.syncwich.data.api.dto.FoodDto]'s kdoc), so that path alone would always create a
 * brand-new food - including a duplicate of one that already matches by name.
 * [resolveExistingFoodMatch] looks the seeded text up against the cached food dictionary first and
 * switches into a real edit of the matching entry when found.
 */
@HiltViewModel
class FoodEditorViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, private val foodRepository: FoodRepository) :
    ViewModel() {

    private val route = savedStateHandle.toRoute<Route.FoodEditor>()
    private val routeFoodId = route.foodId.takeIf(String::isNotBlank)

    private val _editingFoodId = MutableStateFlow(routeFoodId)
    val isEditing: StateFlow<Boolean> =
        _editingFoodId
            .map { it != null }
            .stateIn(viewModelScope, SharingStarted.Eagerly, routeFoodId != null)

    private val _draft =
        MutableStateFlow(
            routeFoodId?.let { FoodEditorDraft() }
                ?: route.seedName?.takeIf(String::isNotBlank)?.let(FoodEditorDraft::seeded)
                ?: FoodEditorDraft()
        )
    val draft: StateFlow<FoodEditorDraft> = _draft.asStateFlow()

    private val _saveState = MutableStateFlow<FoodEditorSaveState>(FoodEditorSaveState.Idle)
    val saveState: StateFlow<FoodEditorSaveState> = _saveState.asStateFlow()

    private var draftTouched = false

    init {
        if (routeFoodId != null) {
            loadCachedDraft(routeFoodId)
        } else {
            route.seedName?.takeIf(String::isNotBlank)?.let(::resolveExistingFoodMatch)
        }
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

        val targetFoodId = _editingFoodId.value
        _saveState.value = FoodEditorSaveState.Saving
        viewModelScope.launch {
            val result =
                if (targetFoodId != null) {
                    foodRepository.updateFood(targetFoodId, draftSnapshot.toRequest())
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

    private fun loadCachedDraft(foodId: String) {
        viewModelScope.launch {
            val cachedFood = foodRepository.observeFood(foodId).first()
            if (!draftTouched && cachedFood != null) {
                _draft.value = FoodEditorDraft.from(cachedFood)
            }
        }
    }

    private fun resolveExistingFoodMatch(seedName: String) {
        viewModelScope.launch {
            val match =
                findFoodMatch(foodRepository.observeFoods().first(), seedName) ?: return@launch
            if (draftTouched) return@launch
            _editingFoodId.value = match.id
            _draft.value = FoodEditorDraft.from(match)
        }
    }

    private fun updateDraft(update: FoodEditorDraft.() -> FoodEditorDraft) {
        if (_saveState.value is FoodEditorSaveState.Saving) return
        draftTouched = true
        _draft.value = _draft.value.update()
        _saveState.value = FoodEditorSaveState.Idle
    }
}

/**
 * Finds the cached food, if any, that a freeform ingredient line (e.g. "2 cups all-purpose flour")
 * most specifically names - a whole-word, case-insensitive match of the food's name or plural name
 * anywhere in the text. Ties (or no match) prefer not guessing: the longest matching name wins,
 * since it's the most specific; a genuine tie falls back to list order.
 */
internal fun findFoodMatch(foods: List<FoodEntity>, ingredientText: String): FoodEntity? {
    val haystack = ingredientText.lowercase()
    fun matches(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        return Regex("\\b${Regex.escape(name.lowercase())}\\b").containsMatchIn(haystack)
    }
    return foods
        .filter { matches(it.name) || matches(it.pluralName) }
        .maxByOrNull { maxOf(it.name.length, it.pluralName?.length ?: 0) }
}

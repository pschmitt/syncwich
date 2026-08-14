package dev.pschmitt.syncwich.ui.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pschmitt.syncwich.data.api.dto.RecipeInputDto
import dev.pschmitt.syncwich.data.repository.RecipeRepository
import dev.pschmitt.syncwich.ui.navigation.Route
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

sealed interface RecipeEditorSaveState {
    data object Idle : RecipeEditorSaveState

    data object Saving : RecipeEditorSaveState

    data class Error(val message: String) : RecipeEditorSaveState

    data object Saved : RecipeEditorSaveState
}

/**
 * Coordinates one explicit recipe mutation without replacing the user's draft on failure - mirrors
 * [dev.pschmitt.syncwich.ui.cookbooks.CookbookEditorViewModel]. An edit draft is seeded from the
 * cached `RecipeDetailEntity.detailJson` decoded as a [RecipeInputDto] via [decodeRecipeInput]
 * below (the mutation-side sibling of [decodeRecipeDetail] in `RecipeDetailViewModel.kt`) so
 * unedited fields survive the round trip through Mealie's single-recipe PUT route.
 */
@HiltViewModel
class RecipeEditorViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val json: Json,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.RecipeEditor>()
    private val recipeId = route.recipeId
    val isEditing: Boolean = recipeId.isNotBlank()

    private val _draft = MutableStateFlow(RecipeEditorDraft())
    val draft: StateFlow<RecipeEditorDraft> = _draft.asStateFlow()

    private val _saveState = MutableStateFlow<RecipeEditorSaveState>(RecipeEditorSaveState.Idle)
    val saveState: StateFlow<RecipeEditorSaveState> = _saveState.asStateFlow()

    private var draftTouched = false

    init {
        if (isEditing) loadCachedDraft()
    }

    fun onNameChange(value: String) = updateDraft { copy(name = value) }

    fun onDescriptionChange(value: String) = updateDraft { copy(description = value) }

    fun onYieldChange(value: String) = updateDraft { copy(recipeYield = value) }

    fun onPrepTimeChange(value: String) = updateDraft { copy(prepTime = value) }

    fun onCookTimeChange(value: String) = updateDraft { copy(cookTime = value) }

    fun onTotalTimeChange(value: String) = updateDraft { copy(totalTime = value) }

    fun onIngredientChange(index: Int, value: String) =
        updateDraft { withIngredientChanged(index, value) }

    fun onIngredientAdd() = updateDraft { withIngredientAdded() }

    fun onIngredientRemove(index: Int) = updateDraft { withIngredientRemoved(index) }

    fun onInstructionChange(index: Int, value: String) =
        updateDraft { withInstructionChanged(index, value) }

    fun onInstructionAdd() = updateDraft { withInstructionAdded() }

    fun onInstructionRemove(index: Int) = updateDraft { withInstructionRemoved(index) }

    fun save() {
        if (_saveState.value is RecipeEditorSaveState.Saving) return

        val draftSnapshot = _draft.value
        draftSnapshot.validationError()?.let { message ->
            _saveState.value = RecipeEditorSaveState.Error(message)
            return
        }
        if (isEditing && draftSnapshot.existingSlug.isNullOrBlank()) {
            _saveState.value =
                RecipeEditorSaveState.Error(
                    "This recipe is not cached on this device. Sync it before editing."
                )
            return
        }

        _saveState.value = RecipeEditorSaveState.Saving
        viewModelScope.launch {
            val result =
                if (isEditing) {
                    recipeRepository.updateRecipe(
                        draftSnapshot.existingSlug!!,
                        draftSnapshot.toUpdateRequest(),
                    )
                } else {
                    recipeRepository.createRecipe(draftSnapshot.toCreateRequest()).mapCatching {}
                }
            _saveState.value =
                result.fold(
                    onSuccess = { RecipeEditorSaveState.Saved },
                    onFailure = {
                        RecipeEditorSaveState.Error(
                            "Couldn't save recipe. Your draft is still here; check your " +
                                "connection and try again."
                        )
                    },
                )
        }
    }

    private fun loadCachedDraft() {
        viewModelScope.launch {
            val cachedDetail = recipeRepository.observeRecipeDetail(recipeId).first()
            val rawJson = cachedDetail?.detailJson
            if (!draftTouched && cachedDetail != null && rawJson != null) {
                val input = decodeRecipeInput(json, rawJson)
                if (input != null) {
                    _draft.value = RecipeEditorDraft.from(input, slug = cachedDetail.slug)
                }
            }
        }
    }

    private fun updateDraft(update: RecipeEditorDraft.() -> RecipeEditorDraft) {
        if (_saveState.value is RecipeEditorSaveState.Saving) return
        draftTouched = true
        _draft.value = _draft.value.update()
        _saveState.value = RecipeEditorSaveState.Idle
    }
}

/**
 * Decodes a cached recipe detail's raw JSON as the editable `Recipe-Input` envelope. Uses the same
 * lenient/ignore-unknown-keys [Json] instance as every other Mealie decode in this app (see
 * `NetworkModule`), so fields this editor doesn't model are simply carried through unread rather
 * than failing the decode.
 */
internal fun decodeRecipeInput(json: Json, rawJson: String): RecipeInputDto? =
    runCatching { json.decodeFromString<RecipeInputDto>(rawJson) }.getOrNull()

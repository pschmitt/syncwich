package dev.pschmitt.syncwich.ui.recipes

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pschmitt.syncwich.data.api.dto.RecipeInputDto
import dev.pschmitt.syncwich.data.repository.RecipeRepository
import dev.pschmitt.syncwich.ui.navigation.Route
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val LOCAL_MARKDOWN_IMAGE = Regex("""(!\[[^]]*]\()\s*((?:content|file)://[^)\s]+)(\s*\))""")

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
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.RecipeEditor>()
    private val recipeId = route.recipeId
    private val sharedAssetUri = route.sharedAssetUri
    val isEditing: Boolean = recipeId.isNotBlank()

    private val _draft = MutableStateFlow(RecipeEditorDraft())
    val draft: StateFlow<RecipeEditorDraft> = _draft.asStateFlow()

    private val _saveState = MutableStateFlow<RecipeEditorSaveState>(RecipeEditorSaveState.Idle)
    val saveState: StateFlow<RecipeEditorSaveState> = _saveState.asStateFlow()

    private var draftTouched = false

    init {
        sharedAssetUri?.let { cacheSelectedImage(it) { cachedUri -> withCoverImage(cachedUri) } }
        if (isEditing) loadCachedDraft()
    }

    fun onNameChange(value: String) = updateDraft { copy(name = value) }

    fun onDescriptionChange(value: String) = updateDraft { copy(description = value) }

    fun onYieldChange(value: String) = updateDraft { copy(recipeYield = value) }

    fun onPrepTimeChange(value: String) = updateDraft { copy(prepTime = value) }

    fun onCookTimeChange(value: String) = updateDraft { copy(cookTime = value) }

    fun onTotalTimeChange(value: String) = updateDraft { copy(totalTime = value) }

    fun onIngredientChange(index: Int, value: String) = updateDraft {
        withIngredientChanged(index, value)
    }

    fun onIngredientAdd() = updateDraft { withIngredientAdded() }

    fun onIngredientRemove(index: Int) = updateDraft { withIngredientRemoved(index) }

    fun onInstructionChange(index: Int, value: String) = updateDraft {
        withInstructionChanged(index, value)
    }

    fun onInstructionAdd() = updateDraft { withInstructionAdded() }

    fun onInstructionRemove(index: Int) = updateDraft { withInstructionRemoved(index) }

    fun onInstructionMove(from: Int, to: Int) = updateDraft { withInstructionMoved(from, to) }

    fun onDescriptionImage(uri: String) =
        cacheSelectedImage(uri) { cachedUri -> withDescriptionImage(cachedUri) }

    fun onInstructionImage(index: Int, uri: String) =
        cacheSelectedImage(uri) { cachedUri -> withInstructionImage(index, cachedUri) }

    fun onCoverImage(uri: String) =
        cacheSelectedImage(uri) { cachedUri -> withCoverImage(cachedUri) }

    fun onRemoveCoverImage() = updateDraft { withoutCoverImage() }

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
                    saveExistingRecipe(draftSnapshot)
                } else {
                    saveNewRecipe(draftSnapshot)
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

    private suspend fun saveNewRecipe(draft: RecipeEditorDraft): Result<Unit> = runCatching {
        val returnedReference = recipeRepository.createRecipe(draft.toCreateRequest()).getOrThrow()
        val slug =
            returnedReference.trim().trim('"').takeIf(String::isNotBlank)
                ?: error("Mealie did not return the created recipe slug")
        recipeRepository.refreshRecipeDetail("", slug, forceRefresh = true).getOrThrow()
        val cached =
            recipeRepository.observeRecipeDetailBySlug(slug).first()
                ?: error("The created recipe was not returned by Mealie")
        val input =
            decodeRecipeInput(json, cached.detailJson)
                ?: error("The created recipe could not be decoded")
        val enrichedDraft = draft.copy(existingSlug = slug, baseInput = input)
        val request = prepareRequest(enrichedDraft, slug).getOrThrow()
        syncCoverImage(enrichedDraft, slug).getOrThrow()
        recipeRepository.updateRecipe(slug, request).getOrThrow()
    }

    private suspend fun saveExistingRecipe(draft: RecipeEditorDraft): Result<Unit> = runCatching {
        val slug = draft.existingSlug ?: error("Recipe slug is missing")
        val request = prepareRequest(draft, slug).getOrThrow()
        syncCoverImage(draft, slug).getOrThrow()
        recipeRepository.updateRecipe(slug, request).getOrThrow()
    }

    private suspend fun prepareRequest(
        draft: RecipeEditorDraft,
        slug: String,
    ): Result<RecipeInputDto> = runCatching {
        val request = draft.toUpdateRequest()
        request.copy(
            description =
                replaceLocalImages(
                    request.description,
                    slug,
                    "Description image",
                    draft.baseInput?.id ?: recipeId,
                ),
            recipeInstructions =
                request.recipeInstructions.map { step ->
                    step.copy(
                        text =
                            replaceLocalImages(
                                step.text,
                                slug,
                                "Step image",
                                draft.baseInput?.id ?: recipeId,
                            )
                    )
                },
        )
    }

    private suspend fun replaceLocalImages(
        text: String,
        slug: String,
        name: String,
        imageRecipeId: String,
    ): String {
        var updated = text
        LOCAL_MARKDOWN_IMAGE.findAll(text).toList().asReversed().forEach { match ->
            val asset =
                recipeRepository
                    .uploadRecipeAsset(
                        slug,
                        name,
                        Uri.parse(match.groupValues[2]),
                        context.contentResolver,
                    )
                    .getOrThrow()
            val fileName = asset.fileName ?: error("Mealie did not return the uploaded asset name")
            val mediaPath = "/api/media/recipes/$imageRecipeId/assets/$fileName"
            updated =
                updated.replaceRange(
                    match.range,
                    match.groupValues[1] + mediaPath + match.groupValues[3],
                )
        }
        return updated
    }

    private suspend fun syncCoverImage(draft: RecipeEditorDraft, slug: String): Result<Unit> =
        when {
            draft.removeCoverImage -> recipeRepository.deleteRecipeImage(slug)
            draft.coverImageUri != null ->
                recipeRepository.updateRecipeImage(
                    slug,
                    Uri.parse(draft.coverImageUri),
                    context.contentResolver,
                )
            else -> Result.success(Unit)
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

    private fun cacheSelectedImage(
        sourceUri: String,
        update: RecipeEditorDraft.(String) -> RecipeEditorDraft,
    ) {
        viewModelScope.launch {
            val cachedUri =
                runCatching {
                    withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val source = Uri.parse(sourceUri)
                        val resolver = context.contentResolver
                        val mimeType = resolver.getType(source) ?: "image/jpeg"
                        val extension =
                            mimeType.substringAfter('/', "jpg").lowercase().replace("jpeg", "jpg")
                        val directory =
                            File(context.cacheDir, "recipe-editor-media").apply { mkdirs() }
                        val destination = File(directory, "${UUID.randomUUID()}.$extension")
                        resolver.openInputStream(source)?.use { input ->
                            destination.outputStream().use { output -> input.copyTo(output) }
                        } ?: error("Couldn't read the selected image")
                        require(destination.length() > 0) { "The selected image is empty" }
                        Uri.fromFile(destination).toString()
                    }
                }
                    .getOrElse {
                        _saveState.value =
                            RecipeEditorSaveState.Error("Couldn't read the selected image")
                        null
                    } ?: return@launch
            if (_saveState.value !is RecipeEditorSaveState.Saving) {
                draftTouched = true
                _draft.value = _draft.value.update(cachedUri)
                _saveState.value = RecipeEditorSaveState.Idle
            }
        }
    }
}

/**
 * Decodes a cached recipe detail's raw JSON as the editable `Recipe-Input` envelope. Uses the same
 * lenient/ignore-unknown-keys [Json] instance as every other Mealie decode in this app (see
 * `NetworkModule`), so fields this editor doesn't model are simply carried through unread rather
 * than failing the decode.
 */
internal fun decodeRecipeInput(json: Json, rawJson: String): RecipeInputDto? = runCatching {
    json.decodeFromString<RecipeInputDto>(rawJson)
}
    .getOrNull()

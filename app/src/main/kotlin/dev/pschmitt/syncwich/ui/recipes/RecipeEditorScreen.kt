package dev.pschmitt.syncwich.ui.recipes

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.pschmitt.syncwich.ui.common.CenteredContent
import dev.pschmitt.syncwich.ui.common.MarkdownEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditorScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeEditorViewModel = hiltViewModel(),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val isSaving = saveState is RecipeEditorSaveState.Saving
    val isImporting = importState is RecipeEditorImportState.Importing
    val isBusy = isSaving || isImporting
    val errorMessage = (saveState as? RecipeEditorSaveState.Error)?.message
    val importErrorMessage = (importState as? RecipeEditorImportState.Failed)?.message
    val context = LocalContext.current
    val cameraAvailable = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    var imageTarget by remember { mutableStateOf<RecipeEditorImageTarget?>(null) }
    var cameraOutputUri by remember { mutableStateOf<Uri?>(null) }
    val applyImageUri: (Uri) -> Unit = { uri ->
        when (val target = imageTarget) {
            RecipeEditorImageTarget.Cover -> viewModel.onCoverImage(uri.toString())
            RecipeEditorImageTarget.Description -> viewModel.onDescriptionImage(uri.toString())
            is RecipeEditorImageTarget.Instruction ->
                viewModel.onInstructionImage(target.index, uri.toString())
            null -> Unit
        }
        imageTarget = null
    }
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let(applyImageUri)
            imageTarget = null
        }
    val cameraCapture =
        rememberLauncherForActivityResult(RecipeEditorTakePictureContract) { captured ->
            val uri = cameraOutputUri
            if (captured && uri != null) applyImageUri(uri) else imageTarget = null
            cameraOutputUri = null
        }
    val startCameraCapture = {
        val outputUri = createRecipeEditorCameraUri(context)
        cameraOutputUri = outputUri
        cameraCapture.launch(outputUri)
    }
    val cameraPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted && imageTarget != null) startCameraCapture() else imageTarget = null
        }
    val requestCameraCapture: (RecipeEditorImageTarget) -> Unit = { target ->
        imageTarget = target
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        ) {
            startCameraCapture()
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(saveState) { if (saveState is RecipeEditorSaveState.Saved) onSaved() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            isImporting -> "Import recipe"
                            viewModel.isEditing -> "Edit recipe"
                            else -> "New recipe"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isBusy) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        CenteredContent(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier =
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text =
                        when {
                            isImporting -> "Mealie is parsing this URL and caching the recipe…"
                            importState is RecipeEditorImportState.Loaded ->
                                "Mealie imported this recipe. Review the details, then save any " +
                                    "changes explicitly."
                            viewModel.isEditing ->
                                "Changes are saved to Mealie explicitly. Your cached recipe " +
                                    "remains available if saving is unavailable."
                            else -> "Create a recipe in Mealie. Nothing is sent until you tap Save."
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Name") },
                    isError = errorMessage == "Enter a recipe name",
                    supportingText =
                        if (errorMessage == "Enter a recipe name") {
                            { Text(errorMessage) }
                        } else null,
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                )
                MarkdownEditor(
                    value = draft.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = "Description",
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    onAddImage = {
                        imageTarget = RecipeEditorImageTarget.Description
                        imagePicker.launch("image/*")
                    },
                    onCaptureImage =
                        if (cameraAvailable) {
                            { requestCameraCapture(RecipeEditorImageTarget.Description) }
                        } else null,
                )
                OutlinedTextField(
                    value = draft.categories,
                    onValueChange = viewModel::onCategoriesChange,
                    label = { Text("Categories") },
                    supportingText = { Text("Separate categories with commas") },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.tags,
                    onValueChange = viewModel::onTagsChange,
                    label = { Text("Tags") },
                    supportingText = { Text("Separate tags with commas") },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.tools,
                    onValueChange = viewModel::onToolsChange,
                    label = { Text("Required tools") },
                    supportingText = { Text("Separate tools with commas") },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = {
                            imageTarget = RecipeEditorImageTarget.Cover
                            imagePicker.launch("image/*")
                        },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null)
                        Text("Choose cover image", modifier = Modifier.padding(start = 8.dp))
                    }
                    if (cameraAvailable) {
                        OutlinedButton(
                            onClick = { requestCameraCapture(RecipeEditorImageTarget.Cover) },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                            Text("Take cover photo", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    if (viewModel.isEditing) {
                        OutlinedButton(
                            onClick = viewModel::onRemoveCoverImage,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                            Text("Remove cover", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
                draft.coverImageUri?.let { uri ->
                    AsyncImage(
                        model = Uri.parse(uri),
                        contentDescription = "Selected cover image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = draft.recipeYield,
                        onValueChange = viewModel::onYieldChange,
                        label = { Text("Yield") },
                        singleLine = true,
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = draft.prepTime,
                        onValueChange = viewModel::onPrepTimeChange,
                        label = { Text("Prep time") },
                        singleLine = true,
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = draft.cookTime,
                        onValueChange = viewModel::onCookTimeChange,
                        label = { Text("Cook time") },
                        singleLine = true,
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = draft.totalTime,
                        onValueChange = viewModel::onTotalTimeChange,
                        label = { Text("Total time") },
                        singleLine = true,
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f),
                    )
                }

                EditableTextList(
                    title = "Ingredients",
                    values = draft.ingredients,
                    itemLabel = "Ingredient",
                    enabled = !isBusy,
                    onValueChange = viewModel::onIngredientChange,
                    onAdd = viewModel::onIngredientAdd,
                    onRemove = viewModel::onIngredientRemove,
                    addLabel = "Add ingredient",
                )

                EditableTextList(
                    title = "Steps",
                    values = draft.instructions,
                    itemLabel = "Step",
                    enabled = !isBusy,
                    onValueChange = viewModel::onInstructionChange,
                    onAdd = viewModel::onInstructionAdd,
                    onRemove = viewModel::onInstructionRemove,
                    onMove = viewModel::onInstructionMove,
                    addLabel = "Add step",
                    singleLine = false,
                    onAddImage = { index ->
                        imageTarget = RecipeEditorImageTarget.Instruction(index)
                        imagePicker.launch("image/*")
                    },
                    onCaptureImage =
                        if (cameraAvailable) {
                            { index ->
                                requestCameraCapture(RecipeEditorImageTarget.Instruction(index))
                            }
                        } else null,
                    availableNotes = draft.notes,
                    linkedNoteReferenceIds = { index ->
                        draft.normalizedInstructionNoteReferences().getOrElse(index) { emptySet() }
                    },
                    onToggleNoteLink = viewModel::onStepNoteLinkToggled,
                )

                EditableNotesList(
                    notes = draft.notes,
                    enabled = !isBusy,
                    onTitleChange = viewModel::onNoteTitleChange,
                    onTextChange = viewModel::onNoteTextChange,
                    onAdd = viewModel::onNoteAdd,
                    onRemove = viewModel::onNoteRemove,
                )

                if (importErrorMessage != null) {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = importErrorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                if (errorMessage != null && errorMessage != "Enter a recipe name") {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                Button(
                    onClick = viewModel::save,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Text("Saving recipe…", modifier = Modifier.padding(start = 8.dp))
                    } else {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Text("Save recipe", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableTextList(
    title: String,
    values: List<String>,
    itemLabel: String,
    enabled: Boolean,
    onValueChange: (Int, String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    addLabel: String,
    singleLine: Boolean = true,
    onAddImage: ((Int) -> Unit)? = null,
    onCaptureImage: ((Int) -> Unit)? = null,
    onMove: ((Int, Int) -> Unit)? = null,
    availableNotes: List<RecipeEditorNote> = emptyList(),
    linkedNoteReferenceIds: (Int) -> Set<String> = { emptySet() },
    onToggleNoteLink: ((Int, String) -> Unit)? = null,
) {
    var noteLinkerIndex by remember { mutableStateOf<Int?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        values.forEachIndexed { index, value ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (singleLine) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { onValueChange(index, it) },
                        label = { Text("$itemLabel ${index + 1}") },
                        singleLine = true,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    MarkdownEditor(
                        value = value,
                        onValueChange = { onValueChange(index, it) },
                        label = "$itemLabel ${index + 1}",
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        onAddImage = onAddImage?.let { add -> { add(index) } },
                        onCaptureImage = onCaptureImage?.let { capture -> { capture(index) } },
                    )
                }
                if (onToggleNoteLink != null && availableNotes.isNotEmpty()) {
                    val linkedCount = linkedNoteReferenceIds(index).size
                    IconButton(onClick = { noteLinkerIndex = index }, enabled = enabled) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.StickyNote2,
                            contentDescription =
                                "Link notes to $itemLabel ${index + 1}" +
                                    if (linkedCount > 0) " ($linkedCount linked)" else "",
                            tint =
                                if (linkedCount > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (onMove == null) {
                    IconButton(
                        onClick = { onRemove(index) },
                        enabled = enabled,
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Remove $itemLabel ${index + 1}"
                            },
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                    }
                } else {
                    Column {
                        IconButton(
                            onClick = { onMove(index, index - 1) },
                            enabled = enabled && index > 0,
                            modifier =
                                Modifier.semantics {
                                    contentDescription = "Move $itemLabel ${index + 1} up"
                                },
                        ) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = null)
                        }
                        IconButton(
                            onClick = { onMove(index, index + 1) },
                            enabled = enabled && index < values.lastIndex,
                            modifier =
                                Modifier.semantics {
                                    contentDescription = "Move $itemLabel ${index + 1} down"
                                },
                        ) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = null)
                        }
                        IconButton(
                            onClick = { onRemove(index) },
                            enabled = enabled,
                            modifier =
                                Modifier.semantics {
                                    contentDescription = "Remove $itemLabel ${index + 1}"
                                },
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = onAdd, enabled = enabled) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(addLabel, modifier = Modifier.padding(start = 8.dp))
        }
    }
    val linkerIndex = noteLinkerIndex
    if (linkerIndex != null && onToggleNoteLink != null) {
        StepNoteLinkerDialog(
            stepLabel = "$itemLabel ${linkerIndex + 1}",
            notes = availableNotes,
            linkedReferenceIds = linkedNoteReferenceIds(linkerIndex),
            onToggle = { referenceId -> onToggleNoteLink(linkerIndex, referenceId) },
            onDismiss = { noteLinkerIndex = null },
        )
    }
}

@Composable
private fun StepNoteLinkerDialog(
    stepLabel: String,
    notes: List<RecipeEditorNote>,
    linkedReferenceIds: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Filled.StickyNote2, contentDescription = null) },
        title = { Text("Link notes to $stepLabel") },
        text = {
            Column {
                notes.forEach { note ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier.fillMaxWidth().clickable { onToggle(note.referenceId) },
                    ) {
                        Checkbox(
                            checked = note.referenceId in linkedReferenceIds,
                            onCheckedChange = { onToggle(note.referenceId) },
                        )
                        Text(
                            text =
                                note.title
                                    .ifBlank { note.text.ifBlank { "Untitled note" } },
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun EditableNotesList(
    notes: List<RecipeEditorNote>,
    enabled: Boolean,
    onTitleChange: (Int, String) -> Unit,
    onTextChange: (Int, String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Notes", style = MaterialTheme.typography.titleMedium)
        notes.forEachIndexed { index, note ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = note.title,
                            onValueChange = { onTitleChange(index, it) },
                            label = { Text("Note ${index + 1} title") },
                            singleLine = true,
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { onRemove(index) },
                            enabled = enabled,
                            modifier =
                                Modifier.semantics {
                                    contentDescription = "Remove note ${index + 1}"
                                },
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                        }
                    }
                    OutlinedTextField(
                        value = note.text,
                        onValueChange = { onTextChange(index, it) },
                        label = { Text("Note ${index + 1} text") },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        OutlinedButton(onClick = onAdd, enabled = enabled) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("Add note", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

private sealed interface RecipeEditorImageTarget {
    data object Cover : RecipeEditorImageTarget

    data object Description : RecipeEditorImageTarget

    data class Instruction(val index: Int) : RecipeEditorImageTarget
}

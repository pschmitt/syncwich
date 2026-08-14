package dev.pschmitt.syncwich.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pschmitt.syncwich.ui.common.CenteredContent

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
    val isSaving = saveState is RecipeEditorSaveState.Saving
    val errorMessage = (saveState as? RecipeEditorSaveState.Error)?.message

    LaunchedEffect(saveState) {
        if (saveState is RecipeEditorSaveState.Saved) onSaved()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "Edit recipe" else "New recipe") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isSaving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        CenteredContent(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
            Text(
                text =
                    if (viewModel.isEditing) {
                        "Changes are saved to Mealie explicitly. Your cached recipe remains " +
                            "available if saving is unavailable."
                    } else {
                        "Create a recipe in Mealie. Nothing is sent until you tap Save."
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
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = draft.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Description") },
                minLines = 3,
                maxLines = 6,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft.recipeYield,
                    onValueChange = viewModel::onYieldChange,
                    label = { Text("Yield") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = draft.prepTime,
                    onValueChange = viewModel::onPrepTimeChange,
                    label = { Text("Prep time") },
                    singleLine = true,
                    enabled = !isSaving,
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
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = draft.totalTime,
                    onValueChange = viewModel::onTotalTimeChange,
                    label = { Text("Total time") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                )
            }

            EditableTextList(
                title = "Ingredients",
                values = draft.ingredients,
                itemLabel = "Ingredient",
                enabled = !isSaving,
                onValueChange = viewModel::onIngredientChange,
                onAdd = viewModel::onIngredientAdd,
                onRemove = viewModel::onIngredientRemove,
                addLabel = "Add ingredient",
            )

            EditableTextList(
                title = "Steps",
                values = draft.instructions,
                itemLabel = "Step",
                enabled = !isSaving,
                onValueChange = viewModel::onInstructionChange,
                onAdd = viewModel::onInstructionAdd,
                onRemove = viewModel::onInstructionRemove,
                addLabel = "Add step",
                singleLine = false,
            )

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
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
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
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        values.forEachIndexed { index, value ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { onValueChange(index, it) },
                    label = { Text("$itemLabel ${index + 1}") },
                    singleLine = singleLine,
                    minLines = if (singleLine) 1 else 2,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onRemove(index) }, enabled = enabled) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Remove $itemLabel ${index + 1}",
                    )
                }
            }
        }
        OutlinedButton(onClick = onAdd, enabled = enabled) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(addLabel, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

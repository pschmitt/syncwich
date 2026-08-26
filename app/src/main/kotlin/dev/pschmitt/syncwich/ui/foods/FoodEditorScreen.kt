package dev.pschmitt.syncwich.ui.foods

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pschmitt.syncwich.ui.common.CenteredContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodEditorScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FoodEditorViewModel = hiltViewModel(),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val isSaving = saveState is FoodEditorSaveState.Saving
    val validationMessage = (saveState as? FoodEditorSaveState.Error)?.message
    val nameErrorMessage = validationMessage?.takeIf { it == "Enter a food name" }

    LaunchedEffect(saveState) { if (saveState is FoodEditorSaveState.Saved) onSaved() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "Edit food" else "New food") },
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
                modifier =
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text =
                        if (viewModel.isEditing) {
                            "Changes are saved to Mealie explicitly. Your cached food remains " +
                                "available if saving is unavailable."
                        } else {
                            "Add a food to Mealie's structured ingredient catalog. Nothing is " +
                                "sent until you tap Save."
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Name") },
                    isError = nameErrorMessage != null,
                    supportingText = nameErrorMessage?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.pluralName,
                    onValueChange = viewModel::onPluralNameChange,
                    label = { Text("Plural name (optional)") },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text("Description (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (validationMessage != null && nameErrorMessage == null) {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = validationMessage,
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
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Text("Saving food…", modifier = Modifier.padding(start = 8.dp))
                    } else {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Text("Save food", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

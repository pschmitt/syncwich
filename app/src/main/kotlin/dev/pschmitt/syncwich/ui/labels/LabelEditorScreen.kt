package dev.pschmitt.syncwich.ui.labels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pschmitt.syncwich.ui.common.CenteredContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelEditorScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LabelEditorViewModel = hiltViewModel(),
) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val color by viewModel.color.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val isEditing = viewModel.isEditing
    val isSaving = saveState is LabelEditorSaveState.Saving
    val validationMessage = (saveState as? LabelEditorSaveState.Error)?.message
    val nameErrorMessage = validationMessage?.takeIf { it == "Enter a label name" }
    val colorErrorMessage =
        validationMessage?.takeIf { it == "Color must be a hex code like #959595" }

    LaunchedEffect(saveState) { if (saveState is LabelEditorSaveState.Saved) onSaved() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit label" else "New label") },
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
                        if (isEditing) {
                            "Changes are saved to Mealie explicitly. Your cached label remains " +
                                "available if saving is unavailable."
                        } else {
                            "Add a color-coded label, shared across Mealie's foods and " +
                                "shopping-list items. Nothing is sent until you tap Save."
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = name,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier =
                            Modifier.size(40.dp)
                                .background(parseLabelColor(color), CircleShape)
                    )
                    OutlinedTextField(
                        value = color,
                        onValueChange = viewModel::onColorChange,
                        label = { Text("Color") },
                        placeholder = { Text("#959595") },
                        isError = colorErrorMessage != null,
                        supportingText = colorErrorMessage?.let { { Text(it) } },
                        singleLine = true,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (validationMessage != null &&
                    nameErrorMessage == null &&
                    colorErrorMessage == null
                ) {
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
                        Text("Saving label…", modifier = Modifier.padding(start = 8.dp))
                    } else {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Text("Save label", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

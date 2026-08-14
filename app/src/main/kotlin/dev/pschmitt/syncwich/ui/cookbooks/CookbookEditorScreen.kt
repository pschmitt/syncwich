package dev.pschmitt.syncwich.ui.cookbooks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
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
import androidx.compose.material3.Switch
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
fun CookbookEditorScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CookbookEditorViewModel = hiltViewModel(),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val isSaving = saveState is CookbookEditorSaveState.Saving
    val validationMessage = (saveState as? CookbookEditorSaveState.Error)?.message

    LaunchedEffect(saveState) {
        if (saveState is CookbookEditorSaveState.Saved) onSaved()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "Edit cookbook" else "New cookbook") },
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
                        "Changes are saved to Mealie explicitly. Your cached cookbook remains " +
                            "available if saving is unavailable."
                    } else {
                        "Create a cookbook in Mealie. Nothing is sent until you tap Save."
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = draft.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Name") },
                isError = validationMessage == "Enter a cookbook name",
                supportingText =
                    if (validationMessage == "Enter a cookbook name") {
                        { Text(validationMessage) }
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
            OutlinedTextField(
                value = draft.queryFilterString,
                onValueChange = viewModel::onQueryFilterChange,
                label = { Text("Recipe filter (optional)") },
                supportingText = { Text("Use Mealie's filter expression to choose matching recipes.") },
                minLines = 2,
                maxLines = 4,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Public, contentDescription = null)
                    Text("Public cookbook", modifier = Modifier.padding(start = 12.dp))
                }
                Switch(
                    checked = draft.public,
                    onCheckedChange = viewModel::onPublicChange,
                    enabled = !isSaving,
                )
            }
            if (validationMessage != null && validationMessage != "Enter a cookbook name") {
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
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Saving cookbook…", modifier = Modifier.padding(start = 8.dp))
                } else {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Text("Save cookbook", modifier = Modifier.padding(start = 8.dp))
                }
            }
            }
        }
    }
}

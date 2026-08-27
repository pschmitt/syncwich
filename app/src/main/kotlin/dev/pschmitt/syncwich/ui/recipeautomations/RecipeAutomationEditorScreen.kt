package dev.pschmitt.syncwich.ui.recipeautomations

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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pschmitt.syncwich.ui.common.CenteredContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeAutomationEditorScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeAutomationEditorViewModel = hiltViewModel(),
) {
    val actionType by viewModel.actionType.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val url by viewModel.url.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val isEditing = viewModel.isEditing
    val isSaving = saveState is RecipeAutomationEditorSaveState.Saving
    val validationMessage = (saveState as? RecipeAutomationEditorSaveState.Error)?.message
    val titleErrorMessage = validationMessage?.takeIf { it == "Enter a title" }
    val urlErrorMessage = validationMessage?.takeIf { it == "Enter a URL" }

    LaunchedEffect(saveState) {
        if (saveState is RecipeAutomationEditorSaveState.Saved) onSaved()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit recipe action" else "New recipe action") },
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
                            "Changes are saved to Mealie explicitly. Your cached recipe action " +
                                "remains available if saving is unavailable."
                        } else {
                            "Add a triggerable automation your household can run from a recipe " +
                                "(e.g. a webhook). Nothing is sent until you tap Save."
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = actionType == ACTION_TYPE_LINK,
                        onClick = { viewModel.onActionTypeChange(ACTION_TYPE_LINK) },
                        label = { Text("Link") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Link,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        enabled = !isSaving,
                    )
                    FilterChip(
                        selected = actionType == ACTION_TYPE_POST,
                        onClick = { viewModel.onActionTypeChange(ACTION_TYPE_POST) },
                        label = { Text("Post") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        enabled = !isSaving,
                    )
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text("Title") },
                    isError = titleErrorMessage != null,
                    supportingText = titleErrorMessage?.let { { Text(it) } },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = viewModel::onUrlChange,
                    label = { Text("URL") },
                    placeholder = { Text("https://example.com/webhook") },
                    isError = urlErrorMessage != null,
                    supportingText = urlErrorMessage?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (validationMessage != null &&
                    titleErrorMessage == null &&
                    urlErrorMessage == null
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
                        Text("Saving…", modifier = Modifier.padding(start = 8.dp))
                    } else {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Text("Save recipe action", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

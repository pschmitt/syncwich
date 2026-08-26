package dev.pschmitt.syncwich.ui.organizers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

/** Shared name-only create/edit form for Categories/Tags/Tools - see [SimpleCatalogItem]'s kdoc. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleCatalogEditorScreen(
    itemNounSingular: String,
    isEditing: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    isSaving: Boolean,
    validationMessage: String?,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    val capitalizedNoun = itemNounSingular.replaceFirstChar(Char::uppercase)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isEditing) "Edit $itemNounSingular" else "New $itemNounSingular")
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isSaving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text =
                    if (isEditing) {
                        "Changes are saved to Mealie explicitly. Your cached $itemNounSingular" +
                            " remains available if saving is unavailable."
                    } else {
                        "Add a $itemNounSingular to Mealie. Nothing is sent until you tap Save."
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Name") },
                isError = validationMessage != null,
                supportingText = validationMessage?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onSave, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Saving $itemNounSingular…", modifier = Modifier.padding(start = 8.dp))
                } else {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Text("Save $capitalizedNoun", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

package dev.pschmitt.syncwich.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown

/**
 * A Markdown-aware rich-text editor. The toolbar writes portable Markdown while the rendered
 * preview updates on every keystroke, so users do not need to mentally translate the stored
 * representation or switch screens to check formatting.
 */
@Composable
fun MarkdownEditor(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    minLines: Int = 4,
    onAddImage: (() -> Unit)? = null,
) {
    var preview by rememberSaveable { mutableStateOf(false) }
    var fieldValue by remember(value) { mutableStateOf(TextFieldValue(value)) }

    LaunchedEffect(value) { if (fieldValue.text != value) fieldValue = TextFieldValue(value) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Row {
                TextButton(onClick = { preview = false }, enabled = enabled || preview) {
                    Text("Edit")
                }
                TextButton(onClick = { preview = true }, enabled = enabled || !preview) {
                    Text("Preview")
                }
            }
        }
        if (preview) {
            Markdown(
                content = value.ifBlank { "Nothing to preview yet." },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                FormatButton("Bold", Icons.Filled.FormatBold) {
                    updateSelection(fieldValue, "**", "**", "bold text") { updated ->
                        fieldValue = updated
                        onValueChange(updated.text)
                    }
                }
                FormatButton("Italic", Icons.Filled.FormatItalic) {
                    updateSelection(fieldValue, "_", "_", "italic text") { updated ->
                        fieldValue = updated
                        onValueChange(updated.text)
                    }
                }
                FormatButton("Bulleted list", Icons.Filled.FormatListBulleted) {
                    updateSelection(fieldValue, "- ", "", "list item") { updated ->
                        fieldValue = updated
                        onValueChange(updated.text)
                    }
                }
                FormatButton("Numbered list", Icons.Filled.FormatListNumbered) {
                    updateSelection(fieldValue, "1. ", "", "list item") { updated ->
                        fieldValue = updated
                        onValueChange(updated.text)
                    }
                }
                FormatButton("Heading", Icons.Filled.FormatIndentIncrease) {
                    updateSelection(fieldValue, "## ", "", "heading") { updated ->
                        fieldValue = updated
                        onValueChange(updated.text)
                    }
                }
                FormatButton("Quote", Icons.Filled.FormatIndentIncrease) {
                    updateSelection(fieldValue, "> ", "", "quoted text") { updated ->
                        fieldValue = updated
                        onValueChange(updated.text)
                    }
                }
                FormatButton("Code", Icons.Filled.Code) {
                    updateSelection(fieldValue, "`", "`", "code") { updated ->
                        fieldValue = updated
                        onValueChange(updated.text)
                    }
                }
                FormatButton("Link", Icons.Filled.Link) {
                    updateSelection(fieldValue, "[", "](https://)", "link text") { updated ->
                        fieldValue = updated
                        onValueChange(updated.text)
                    }
                }
                if (onAddImage != null) {
                    FormatButton("Add image", Icons.Filled.Image, onClick = onAddImage)
                }
            }
            OutlinedTextField(
                value = fieldValue,
                onValueChange = {
                    fieldValue = it
                    onValueChange(it.text)
                },
                label = { Text(label) },
                enabled = enabled,
                minLines = minLines,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Live preview",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Markdown(
                content = value.ifBlank { "Nothing to preview yet." },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun FormatButton(
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) { Icon(icon, contentDescription = description) }
}

private fun updateSelection(
    value: TextFieldValue,
    prefix: String,
    suffix: String,
    placeholder: String,
    onUpdated: (TextFieldValue) -> Unit,
) {
    val selection = value.selection
    val selected = value.text.substring(selection.min, selection.max)
    val replacement = prefix + selected.ifBlank { placeholder } + suffix
    val start = selection.min
    val newText = value.text.replaceRange(selection.min, selection.max, replacement)
    val cursor = start + replacement.length - suffix.length
    onUpdated(TextFieldValue(newText, TextRange(cursor)))
}

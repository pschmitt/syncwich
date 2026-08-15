package dev.pschmitt.syncwich.ui.mealplan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tapas
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pschmitt.syncwich.data.db.entity.MealPlanEntryEntity
import dev.pschmitt.syncwich.ui.common.NavigationTitle
import dev.pschmitt.syncwich.ui.common.PlaceholderScreen
import dev.pschmitt.syncwich.ui.common.RefreshErrorBanner
import dev.pschmitt.syncwich.ui.navigation.TopLevelDestination
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanScreen(
    modifier: Modifier = Modifier,
    onRecipeClick: (String, String) -> Unit = { _, _ -> },
    onSettingsClick: () -> Unit = {},
    viewModel: MealPlanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()

    if (editorState.isOpen) {
        MealPlanEntryEditorDialog(
            state = editorState,
            onEntryTypeChange = viewModel::onEntryTypeChange,
            onTitleChange = viewModel::onTitleChange,
            onTextChange = viewModel::onTextChange,
            onSave = viewModel::saveEntry,
            onDelete = viewModel::deleteEntry,
            onDismiss = viewModel::dismissEditor,
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { NavigationTitle(TopLevelDestination.MEAL_PLAN.icon, "Meal Plan") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.refreshState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                WeekHeader(
                    weekStart = uiState.weekStart,
                    weekEnd = uiState.weekEnd,
                    isRefreshing = uiState.refreshState.isRefreshing,
                    onPreviousWeek = viewModel::showPreviousWeek,
                    onNextWeek = viewModel::showNextWeek,
                    onToday = viewModel::showCurrentWeek,
                )
                RefreshErrorBanner(
                    errorMessage = uiState.refreshState.errorMessage,
                    onRetry = viewModel::refresh,
                )
                if (uiState.entries.isEmpty()) {
                    PlaceholderScreen(
                        icon = Icons.Filled.CalendarMonth,
                        title =
                            if (uiState.refreshState.isRefreshing) "Loading meal plan"
                            else "Nothing planned this week",
                        subtitle =
                            if (uiState.refreshState.errorMessage != null) {
                                "Showing the saved meal plan. Connect to Mealie and try again to refresh it."
                            } else {
                                "Meal plan entries synced from your household's Mealie meal plan show up here."
                            },
                        modifier = Modifier.fillMaxSize(),
                        isLoading = uiState.refreshState.isRefreshing,
                        onRetry = viewModel::refresh,
                    )
                } else {
                    val entriesByDate = uiState.entries.groupBy { it.date }
                    val days =
                        generateSequence(uiState.weekStart) { it.plusDays(1) }.take(7).toList()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(days) { day ->
                            DayCard(
                                day = day,
                                entries = entriesByDate[day.toString()].orEmpty(),
                                onRecipeClick = onRecipeClick,
                                onAddEntry = { viewModel.startAddEntry(day) },
                                onEditEntry = viewModel::startEditEntry,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekHeader(
    weekStart: LocalDate,
    weekEnd: LocalDate,
    isRefreshing: Boolean,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onToday: () -> Unit,
) {
    val formatter = DateTimeFormatter.ofPattern("MMM d")
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPreviousWeek) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous week")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${weekStart.format(formatter)} - ${weekEnd.format(formatter)}",
                style = MaterialTheme.typography.titleMedium,
            )
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(start = 8.dp).size(16.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
        Row {
            IconButton(onClick = onToday) {
                Icon(Icons.Filled.Today, contentDescription = "Jump to this week")
            }
            IconButton(onClick = onNextWeek) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next week")
            }
        }
    }
}

@Composable
private fun DayCard(
    day: LocalDate,
    entries: List<MealPlanEntryEntity>,
    onRecipeClick: (String, String) -> Unit,
    onAddEntry: () -> Unit,
    onEditEntry: (MealPlanEntryEntity) -> Unit,
) {
    // Reads through LocalConfiguration (observable, recomposes on a config change) rather than
    // java.util.Locale.getDefault() - see Android Lint's NonObservableLocale check.
    val locale = LocalConfiguration.current.locales[0]
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = day.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = day.format(DateTimeFormatter.ofPattern("MMMM d")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onAddEntry) {
                    Icon(Icons.Filled.Add, contentDescription = "Add meal plan entry")
                }
            }
            if (entries.isEmpty()) {
                Text(
                    text = "Nothing planned",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    entries.forEach { entry -> MealPlanEntryRow(entry, onRecipeClick, onEditEntry) }
                }
            }
        }
    }
}

@Composable
private fun MealPlanEntryRow(
    entry: MealPlanEntryEntity,
    onRecipeClick: (String, String) -> Unit,
    onEditEntry: (MealPlanEntryEntity) -> Unit,
) {
    val clickableModifier =
        if (entry.recipeId != null && entry.recipeSlug != null) {
            Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button) {
                onRecipeClick(entry.recipeId, entry.recipeSlug)
            }
        } else {
            Modifier
        }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).then(clickableModifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier.size(32.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = entryTypeIcon(entry.entryType),
                    contentDescription = entry.entryType,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = entry.entryType.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text =
                        entry.recipeName
                            ?: entry.title.ifBlank { entry.text.ifBlank { "Untitled" } },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        IconButton(onClick = { onEditEntry(entry) }) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit meal plan entry")
        }
    }
}

private fun entryTypeIcon(entryType: String): ImageVector =
    when (entryType) {
        "breakfast" -> Icons.Filled.FreeBreakfast
        "lunch" -> Icons.Filled.LunchDining
        "dinner" -> Icons.Filled.DinnerDining
        "side" -> Icons.Filled.Tapas
        "snack" -> Icons.Filled.Cookie
        "drink" -> Icons.Filled.LocalBar
        "dessert" -> Icons.Filled.Icecream
        else -> Icons.Filled.Restaurant
    }

private val ENTRY_TYPES =
    listOf("breakfast", "lunch", "dinner", "side", "snack", "drink", "dessert")

/**
 * Add/edit dialog for one meal-plan entry (SW-24/SW-33). Only freeform title/note fields are
 * editable here - attaching an existing recipe would need a full recipe picker, out of scope for
 * this minimal add/edit flow; a recipe-linked entry synced from Mealie can still be deleted here,
 * it just can't be re-linked to a different recipe.
 */
@Composable
private fun MealPlanEntryEditorDialog(
    state: MealPlanEditorState,
    onEntryTypeChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (state.isEditing) "Edit meal plan entry"
                else
                    "Add meal plan entry for ${state.date.format(DateTimeFormatter.ofPattern("MMM d"))}"
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ENTRY_TYPES) { type ->
                        FilterChip(
                            selected = state.entryType == type,
                            onClick = { onEntryTypeChange(type) },
                            enabled = !state.isSaving,
                            label = { Text(type.replaceFirstChar { it.uppercase() }) },
                            leadingIcon = {
                                Icon(
                                    imageVector = entryTypeIcon(type),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }
                OutlinedTextField(
                    value = state.title,
                    onValueChange = onTitleChange,
                    label = { Text("Title") },
                    singleLine = true,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.text,
                    onValueChange = onTextChange,
                    label = { Text("Note") },
                    minLines = 2,
                    maxLines = 4,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !state.isSaving) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            Row {
                if (state.isEditing) {
                    TextButton(onClick = onDelete, enabled = !state.isSaving) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Text("Delete", modifier = Modifier.padding(start = 4.dp))
                    }
                }
                TextButton(onClick = onDismiss, enabled = !state.isSaving) { Text("Cancel") }
            }
        },
    )
}

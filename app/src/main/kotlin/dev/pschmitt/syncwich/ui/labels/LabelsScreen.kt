package dev.pschmitt.syncwich.ui.labels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pschmitt.syncwich.data.db.entity.LabelEntity
import dev.pschmitt.syncwich.ui.common.PlaceholderScreen
import dev.pschmitt.syncwich.ui.common.RefreshErrorBanner
import dev.pschmitt.syncwich.ui.common.SearchField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onLabelClick: (String) -> Unit = {},
    onCreateClick: () -> Unit = {},
    viewModel: LabelsViewModel = hiltViewModel(),
) {
    val labels by viewModel.labels.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    val pendingDeleteLabel =
        remember(labels, pendingDelete) { labels.find { it.id == pendingDelete } }
    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Labels") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New label") },
                expanded = !listState.canScrollBackward,
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = refreshState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                RefreshErrorBanner(refreshState.errorMessage, onRetry = viewModel::refresh)
                SearchField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = "Search labels",
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
                if (labels.isEmpty()) {
                    PlaceholderScreen(
                        icon = Icons.Filled.Label,
                        title = "No labels yet",
                        subtitle =
                            "Labels are Mealie's color-coded organizer, shared across foods and " +
                                "shopping-list items. Add one to get started.",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        items(labels, key = LabelEntity::id) { label ->
                            ListItem(
                                leadingContent = {
                                    androidx.compose.foundation.layout.Box(
                                        modifier =
                                            Modifier.size(24.dp)
                                                .background(
                                                    parseLabelColor(label.color),
                                                    CircleShape,
                                                )
                                    )
                                },
                                headlineContent = { Text(label.name) },
                                trailingContent = {
                                    IconButton(onClick = { pendingDelete = label.id }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Delete ${label.name}",
                                        )
                                    }
                                },
                                modifier =
                                    Modifier.fillMaxWidth().clickable { onLabelClick(label.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (pendingDeleteLabel != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete label?") },
            text = {
                Text(
                    "\"${pendingDeleteLabel.name}\" will be removed from Mealie. This can't be" +
                        " undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLabel(pendingDeleteLabel.id)
                        pendingDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

/** Falls back to a neutral gray for a color string Mealie didn't actually send as `#rrggbb`. */
internal fun parseLabelColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)

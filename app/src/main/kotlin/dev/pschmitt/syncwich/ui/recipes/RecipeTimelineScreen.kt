package dev.pschmitt.syncwich.ui.recipes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pschmitt.syncwich.data.db.entity.RecipeTimelineEventEntity
import dev.pschmitt.syncwich.ui.common.PlaceholderScreen
import dev.pschmitt.syncwich.ui.common.RefreshErrorBanner
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeTimelineScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeTimelineViewModel = hiltViewModel(),
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Cooking timeline") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = refreshState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                RefreshErrorBanner(
                    errorMessage = refreshState.errorMessage,
                    onRetry = viewModel::refresh,
                )
                if (events.isEmpty()) {
                    PlaceholderScreen(
                        icon = Icons.Filled.Timeline,
                        title =
                            if (refreshState.isRefreshing) "Loading timeline"
                            else "No cooking events yet",
                        subtitle =
                            "Tap \"I made this\" on the recipe to add the first entry, or check " +
                                "your connection and try again.",
                        modifier = Modifier.weight(1f),
                        isLoading = refreshState.isRefreshing,
                        onRetry = viewModel::refresh,
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(events, key = { it.localId }) { event ->
                            RecipeTimelineEventRow(event)
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeTimelineEventRow(event: RecipeTimelineEventEntity) {
    val message = event.eventMessage?.takeIf { it.isNotBlank() }
    val supportingText =
        listOfNotNull(formatTimelineTimestamp(event.timestamp).takeIf { it.isNotBlank() }, message)
            .joinToString(separator = " - ")

    ListItem(
        headlineContent = { Text(event.subject) },
        supportingContent = if (supportingText.isNotBlank()) ({ Text(supportingText) }) else null,
        leadingContent = {
            Icon(
                imageVector =
                    if (event.eventType == "system") Icons.Filled.Info
                    else Icons.Filled.RestaurantMenu,
                contentDescription = null,
            )
        },
        trailingContent =
            if (event.pending) {
                {
                    Icon(
                        imageVector = Icons.Filled.CloudOff,
                        contentDescription = "Saved offline; sync pending",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else null,
    )
}

internal fun formatTimelineTimestamp(epochMillis: Long): String =
    if (epochMillis <= 0L) ""
    else {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(epochMillis))
    }

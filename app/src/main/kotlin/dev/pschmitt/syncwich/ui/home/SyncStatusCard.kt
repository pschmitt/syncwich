package dev.pschmitt.syncwich.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.pschmitt.syncwich.sync.SyncStatus
import dev.pschmitt.syncwich.sync.SyncStatusState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.TimeUnit

/** Persistent Home feedback for the cache refresh; it never gates or replaces Room content. */
@Composable
fun HomeSyncStatusCard(
    status: SyncStatus,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (status.state) {
                SyncStatusState.SYNCING ->
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                SyncStatusState.ERROR,
                SyncStatusState.STALE,
                SyncStatusState.NEVER_SYNCED ->
                    Icon(
                        Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp),
                    )
                SyncStatusState.SYNCED ->
                    Icon(
                        Icons.Filled.CloudDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(syncStatusHeadline(status), style = MaterialTheme.typography.titleMedium)
                Text(
                    syncStatusDetails(status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

internal fun syncStatusHeadline(status: SyncStatus): String =
    when (status.state) {
        SyncStatusState.SYNCING -> status.currentMessage ?: "Syncing recipes…"
        SyncStatusState.ERROR -> "Sync failed"
        SyncStatusState.STALE -> "Cache may be stale"
        SyncStatusState.NEVER_SYNCED -> "Not synced yet"
        SyncStatusState.SYNCED -> "Recipes up to date"
    }

internal fun syncStatusDetails(status: SyncStatus, nowMillis: Long = System.currentTimeMillis()): String =
    when (status.state) {
        SyncStatusState.SYNCING -> "Updating the cache; saved recipes remain available."
        SyncStatusState.ERROR ->
            status.errorMessage ?: "Showing saved data. Check your connection and try again."
        SyncStatusState.STALE ->
            "${formatRelativeSyncTime(status.lastSyncAt, nowMillis)}. Sync when a connection is available."
        SyncStatusState.NEVER_SYNCED -> "No successful sync yet. Cached content will stay available offline."
        SyncStatusState.SYNCED -> formatRelativeSyncTime(status.lastSyncAt, nowMillis)
    }

internal fun formatRelativeSyncTime(lastSyncAt: Long?, nowMillis: Long): String {
    if (lastSyncAt == null) return "Last synced: never"
    val deltaMillis = (nowMillis - lastSyncAt).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(deltaMillis)
    val hours = TimeUnit.MILLISECONDS.toHours(deltaMillis)
    val days = TimeUnit.MILLISECONDS.toDays(deltaMillis)
    return when {
        minutes < 1 -> "Last synced just now"
        minutes < 60 -> "Last synced ${minutes}m ago"
        hours < 24 -> "Last synced ${hours}h ago"
        days < 7 -> "Last synced ${days}d ago"
        else ->
            "Last synced on " +
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochMilli(lastSyncAt))
    }
}

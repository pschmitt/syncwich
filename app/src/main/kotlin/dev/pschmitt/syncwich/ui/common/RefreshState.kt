package dev.pschmitt.syncwich.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** State for a best-effort network refresh that never replaces the Room-backed read path. */
data class RefreshState(val isRefreshing: Boolean = false, val errorMessage: String? = null)

/** A deliberately non-technical message for a failed background refresh. */
fun refreshErrorMessage(result: Result<Unit>): String? =
    result.exceptionOrNull()?.let { "Couldn't refresh. Showing saved data. Check your connection." }

@Composable
fun RefreshErrorBanner(
    errorMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (errorMessage == null) return

    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(text = errorMessage, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

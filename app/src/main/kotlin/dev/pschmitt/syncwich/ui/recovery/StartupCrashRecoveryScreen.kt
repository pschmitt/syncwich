package dev.pschmitt.syncwich.ui.recovery

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import dev.pschmitt.syncwich.R
import dev.pschmitt.syncwich.data.crash.StartupCrashReport

@Composable
fun StartupCrashRecoveryScreen(
    report: StartupCrashReport,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.syncwich_icon),
                contentDescription = "Syncwich app icon",
                modifier =
                    Modifier.size(88.dp)
                        .background(colorResource(R.color.icon_background), CircleShape)
                        .padding(6.dp),
            )
            Text(
                text = "Syncwich recovered from a startup crash",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text =
                    "The app saved a sanitized diagnostic report. You can copy or share it, then continue to Syncwich.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                SelectionContainer {
                    Text(
                        text = report.details,
                        style = MaterialTheme.typography.bodySmall,
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(16.dp)
                                .semantics { contentDescription = "Crash report" },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { copyCrashReport(context, report.details) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Copy")
                }
                Button(
                    onClick = { shareCrashReport(context, report.details) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Share")
                }
            }
            TextButton(onClick = onContinue) { Text("Continue") }
        }
    }
}

private fun copyCrashReport(context: Context, details: String) {
    context.getSystemService<ClipboardManager>()
        ?.setPrimaryClip(ClipData.newPlainText("Syncwich crash report", details))
}

private fun shareCrashReport(context: Context, details: String) {
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Syncwich startup crash report")
                putExtra(Intent.EXTRA_TEXT, details)
            },
            "Share crash report",
        )
    )
}

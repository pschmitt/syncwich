package dev.pschmitt.syncwich.ui.initialsync

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pschmitt.syncwich.R
import dev.pschmitt.syncwich.sync.InitialSyncProgress
import dev.pschmitt.syncwich.sync.InitialSyncStage
import dev.pschmitt.syncwich.ui.common.CenteredContent

@Composable
fun InitialSyncScreen(
    onFinished: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InitialSyncViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        when (uiState) {
            InitialSyncUiState.Completed -> onFinished()
            InitialSyncUiState.Cancelled -> onCancel()
            else -> Unit
        }
    }

    BackHandler { viewModel.cancel() }

    Scaffold(modifier = modifier) { innerPadding ->
        CenteredContent(modifier = Modifier.fillMaxSize().padding(innerPadding), maxWidth = 520.dp) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
            Icon(
                imageVector = Icons.Filled.Sync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.initial_sync_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.initial_sync_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            when (val state = uiState) {
                InitialSyncUiState.Starting -> {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.initial_sync_starting))
                }
                is InitialSyncUiState.Syncing -> SyncProgressContent(state.progress)
                is InitialSyncUiState.Failed -> {
                    Text(
                        text = stringResource(R.string.initial_sync_error, state.message),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = viewModel::retry, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.initial_sync_retry))
                    }
                    OutlinedButton(
                        onClick = viewModel::cancel,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.initial_sync_continue))
                    }
                }
                InitialSyncUiState.Completed,
                InitialSyncUiState.Cancelled -> Unit
            }

            if (uiState is InitialSyncUiState.Starting || uiState is InitialSyncUiState.Syncing) {
                OutlinedButton(onClick = viewModel::cancel, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.initial_sync_cancel))
                }
            }
            }
        }
    }
}

@Composable
private fun SyncProgressContent(progress: InitialSyncProgress) {
    val stageName =
        stringResource(
            when (progress.stage) {
                InitialSyncStage.Recipes -> R.string.initial_sync_stage_recipes
                InitialSyncStage.Categories -> R.string.initial_sync_stage_categories
                InitialSyncStage.Tags -> R.string.initial_sync_stage_tags
                InitialSyncStage.ShoppingLists -> R.string.initial_sync_stage_shopping_lists
                InitialSyncStage.Cookbooks -> R.string.initial_sync_stage_cookbooks
                InitialSyncStage.MealPlan -> R.string.initial_sync_stage_meal_plan
            }
        )
    val stageText =
        if (progress.completed) {
            stringResource(R.string.initial_sync_stage_complete, stageName)
        } else {
            stringResource(R.string.initial_sync_stage_syncing, stageName)
        }
    Text(stageText, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    LinearProgressIndicator(
        progress = progress.stageNumber.toFloat() / progress.totalStages,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text =
            progress.itemCount?.let {
                stringResource(R.string.initial_sync_count, it)
            } ?: stringResource(
                R.string.initial_sync_stage_position,
                progress.stageNumber,
                progress.totalStages,
            ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

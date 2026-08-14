package dev.pschmitt.syncwich.sync

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InitialSyncRunnerTest {

    @Test
    fun `reports each stage before and after its count is cached`() = runTest {
        val dataSource =
            FakeDataSource(counts = InitialSyncStage.entries.mapIndexed { i, _ -> i + 1 })
        val progress = mutableListOf<InitialSyncProgress>()

        val result = InitialSyncRunner(dataSource).run { progress += it }

        assertTrue(result.isSuccess)
        assertEquals(InitialSyncStage.entries, progress.filter { !it.completed }.map { it.stage })
        assertEquals(
            listOf(1, 2, 3, 4, 5, 6),
            progress.filter { it.completed }.map { it.itemCount },
        )
        assertEquals(
            listOf(1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6),
            progress.map { it.stageNumber },
        )
        assertTrue(progress.all { it.totalStages == InitialSyncStage.entries.size })
    }

    @Test
    fun `stops at a failed stage and leaves later stages untouched`() = runTest {
        val dataSource = FakeDataSource(failure = InitialSyncStage.Tags)
        val progress = mutableListOf<InitialSyncProgress>()

        val result = InitialSyncRunner(dataSource).run { progress += it }

        assertTrue(result.isFailure)
        assertEquals(
            InitialSyncStage.Tags,
            (result.exceptionOrNull() as InitialSyncException).stage,
        )
        assertEquals(
            listOf(InitialSyncStage.Recipes, InitialSyncStage.Categories, InitialSyncStage.Tags),
            dataSource.refreshed,
        )
        assertFalse(progress.any { it.stage == InitialSyncStage.Tags && it.completed })
    }

    @Test
    fun `cancellation propagates instead of becoming a sync failure`() = runTest {
        val dataSource = FakeDataSource(cancellation = InitialSyncStage.Categories)

        var cancelled = false
        try {
            InitialSyncRunner(dataSource).run {}
        } catch (_: CancellationException) {
            cancelled = true
        }

        assertTrue(cancelled)
    }

    private class FakeDataSource(
        private val counts: List<Int> = InitialSyncStage.entries.map { 0 },
        private val failure: InitialSyncStage? = null,
        private val cancellation: InitialSyncStage? = null,
    ) : InitialSyncDataSource {
        val refreshed = mutableListOf<InitialSyncStage>()

        override suspend fun refresh(stage: InitialSyncStage): Result<Int> {
            refreshed += stage
            if (stage == cancellation) throw CancellationException("cancelled")
            if (stage == failure) return Result.failure(IOException("network down"))
            return Result.success(counts[stage.ordinal])
        }
    }
}

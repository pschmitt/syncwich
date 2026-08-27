package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.LabelsApi
import dev.pschmitt.syncwich.data.api.dto.LabelCreateDto
import dev.pschmitt.syncwich.data.api.dto.LabelDto
import dev.pschmitt.syncwich.data.api.dto.LabelUpdateDto
import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import dev.pschmitt.syncwich.data.db.dao.LabelDao
import dev.pschmitt.syncwich.data.db.entity.LabelEntity
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the "hard requirement" from AGENTS.md's architecture section (see [FoodRepositoryTest]'s
 * equivalent coverage), plus [LabelUpdateDto]'s specific risk: unlike Categories/Tags/Tools,
 * Mealie's label `PUT` requires `groupId` in the request body itself, not just the
 * path - [RecipeAutomationRepository] has the identical shape/risk (`groupId` + `householdId`), so
 * this coverage stands in for both (SW-139).
 */
class LabelRepositoryTest {

    @Test
    fun `updateLabel round-trips groupId from the cached entity`() = runTest {
        val labelDao =
            FakeLabelDao(
                seed =
                    listOf(
                        LabelEntity(
                            id = "label-1",
                            groupId = "group-1",
                            name = "Old",
                            color = "#111111",
                        )
                    )
            )
        var capturedRequest: LabelUpdateDto? = null
        val labelsApi =
            FakeLabelsApi(
                updateResponse = { request ->
                    capturedRequest = request
                    LabelDto(
                        id = "label-1",
                        groupId = request.groupId,
                        name = request.name,
                        color = request.color,
                    )
                }
            )
        val repository = LabelRepository(labelsApi, labelDao)

        val result = repository.updateLabel("label-1", "New name", "#abcdef")

        assertTrue(result.isSuccess)
        assertEquals("group-1", capturedRequest?.groupId)
        assertEquals("label-1", capturedRequest?.id)
    }

    @Test
    fun `a failed updateLabel leaves the cached row untouched`() = runTest {
        val cached =
            LabelEntity(id = "keep-1", groupId = "group-1", name = "Keep me", color = "#959595")
        val labelDao = FakeLabelDao(seed = listOf(cached))
        val repository =
            LabelRepository(FakeLabelsApi(mutationFailure = IOException("offline")), labelDao)

        val result = repository.updateLabel("keep-1", "Changed", "#000000")

        assertTrue(result.isFailure)
        assertEquals(listOf(cached), labelDao.observeAll().first())
    }

    @Test
    fun `refreshLabels replaces the cache on success`() = runTest {
        val labelDao =
            FakeLabelDao(seed = listOf(LabelEntity("old-1", "group-1", "Old", "#111111")))
        val labelsApi =
            FakeLabelsApi(labels = listOf(LabelDto("new-1", "group-1", "New", "#222222")))
        val repository = LabelRepository(labelsApi, labelDao)

        val result = repository.refreshLabels()

        assertTrue(result.isSuccess)
        assertEquals(listOf("New"), labelDao.observeAll().first().map { it.name })
    }

    @Test
    fun `a failed refreshLabels leaves the cache untouched`() = runTest {
        val cached = listOf(LabelEntity("keep-1", "group-1", "Keep Me", "#959595"))
        val labelDao = FakeLabelDao(seed = cached)
        val repository =
            LabelRepository(FakeLabelsApi(failure = IOException("network down")), labelDao)

        val result = repository.refreshLabels()

        assertTrue(result.isFailure)
        assertEquals(cached, labelDao.observeAll().first())
    }

    private class FakeLabelDao(seed: List<LabelEntity> = emptyList()) : LabelDao {
        private val state = MutableStateFlow(seed)

        override fun observeAll(): Flow<List<LabelEntity>> = state

        override fun observeById(id: String): Flow<LabelEntity?> = state.map { list ->
            list.find { it.id == id }
        }

        override suspend fun upsertAll(labels: List<LabelEntity>) {
            val byId = state.value.associateBy { it.id }.toMutableMap()
            labels.forEach { byId[it.id] = it }
            state.value = byId.values.toList()
        }

        override suspend fun deleteById(id: String) {
            state.value = state.value.filterNot { it.id == id }
        }

        override suspend fun deleteAll() {
            state.value = emptyList()
        }
    }

    private class FakeLabelsApi(
        private val labels: List<LabelDto> = emptyList(),
        private val failure: Throwable? = null,
        private val updateResponse: ((LabelUpdateDto) -> LabelDto)? = null,
        private val mutationFailure: Throwable? = null,
    ) : LabelsApi {
        override suspend fun getLabels(page: Int, perPage: Int): PagedResponseDto<LabelDto> {
            failure?.let { throw it }
            return PagedResponseDto(1, labels.size, labels.size, 1, labels)
        }

        override suspend fun createLabel(request: LabelCreateDto): LabelDto =
            error("not used by LabelRepositoryTest")

        override suspend fun updateLabel(itemId: String, request: LabelUpdateDto): LabelDto {
            mutationFailure?.let { throw it }
            return updateResponse?.invoke(request) ?: error("not used by LabelRepositoryTest")
        }

        override suspend fun deleteLabel(itemId: String): ResponseBody {
            mutationFailure?.let { throw it }
            return "".toResponseBody()
        }
    }
}

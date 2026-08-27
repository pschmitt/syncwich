package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.UnitsApi
import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import dev.pschmitt.syncwich.data.api.dto.UnitDto
import dev.pschmitt.syncwich.data.api.dto.UnitMutationDto
import dev.pschmitt.syncwich.data.db.dao.UnitDao
import dev.pschmitt.syncwich.data.db.entity.UnitEntity
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the "hard requirement" from AGENTS.md's architecture section (see [FoodRepositoryTest]'s
 * equivalent coverage), plus [UnitDto]'s specific risk: a `PUT` that only sent the fields this
 * app's editor exposes would silently reset every other field
 * (fraction/useAbbreviation/pluralAbbreviation/aliases/standardQuantity/standardUnit/extras) to
 * this app's own Kotlin defaults, clobbering whatever the user had actually configured on Mealie.
 */
class UnitRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `updateUnit round-trips fields the editor does not expose`() = runTest {
        val serverOnlyFields =
            UnitDto(
                id = "unit-1",
                name = "Cup",
                pluralName = "Cups",
                description = "",
                abbreviation = "c",
                fraction = false,
                pluralAbbreviation = "cs",
                useAbbreviation = true,
                standardQuantity = 236.588,
                standardUnit = "milliliter",
            )
        val unitDao =
            FakeUnitDao(
                seed =
                    listOf(
                        UnitEntity(
                            id = "unit-1",
                            name = "Cup",
                            pluralName = "Cups",
                            description = "",
                            abbreviation = "c",
                            rawJson = json.encodeToString(UnitDto.serializer(), serverOnlyFields),
                        )
                    )
            )
        var capturedRequest: UnitMutationDto? = null
        val unitsApi =
            FakeUnitsApi(
                updateResponse = { request ->
                    capturedRequest = request
                    serverOnlyFields.copy(name = request.name)
                }
            )
        val repository = UnitRepository(unitsApi, unitDao, json)

        val result = repository.updateUnit("unit-1", "Metric Cup", "Cups", "", "c")

        assertTrue(result.isSuccess)
        val request = requireNotNull(capturedRequest)
        assertEquals("Metric Cup", request.name)
        assertEquals(false, request.fraction)
        assertEquals("cs", request.pluralAbbreviation)
        assertEquals(true, request.useAbbreviation)
        assertEquals(236.588, request.standardQuantity)
        assertEquals("milliliter", request.standardUnit)
    }

    @Test
    fun `a failed updateUnit leaves the cached row untouched`() = runTest {
        val cached =
            UnitEntity(
                id = "keep-1",
                name = "Keep me",
                pluralName = null,
                description = "",
                abbreviation = "",
                rawJson =
                    json.encodeToString(
                        UnitDto.serializer(),
                        UnitDto(id = "keep-1", name = "Keep me"),
                    ),
            )
        val unitDao = FakeUnitDao(seed = listOf(cached))
        val repository =
            UnitRepository(
                FakeUnitsApi(mutationFailure = IOException("offline")),
                unitDao,
                json,
            )

        val result = repository.updateUnit("keep-1", "Changed", null, "", "")

        assertTrue(result.isFailure)
        assertEquals(listOf(cached), unitDao.observeAll().first())
    }

    @Test
    fun `refreshUnits replaces the cache on success`() = runTest {
        val unitDao =
            FakeUnitDao(
                seed =
                    listOf(
                        UnitEntity(
                            "old-1",
                            "Old",
                            null,
                            "",
                            "",
                            json.encodeToString(UnitDto.serializer(), UnitDto("old-1", "Old")),
                        )
                    )
            )
        val unitsApi = FakeUnitsApi(units = listOf(UnitDto(id = "new-1", name = "New")))
        val repository = UnitRepository(unitsApi, unitDao, json)

        val result = repository.refreshUnits()

        assertTrue(result.isSuccess)
        assertEquals(listOf("New"), unitDao.observeAll().first().map { it.name })
    }

    private class FakeUnitDao(seed: List<UnitEntity> = emptyList()) : UnitDao {
        private val state = MutableStateFlow(seed)

        override fun observeAll(): Flow<List<UnitEntity>> = state

        override fun observeById(id: String): Flow<UnitEntity?> = state.map { list ->
            list.find { it.id == id }
        }

        override suspend fun upsertAll(units: List<UnitEntity>) {
            val byId = state.value.associateBy { it.id }.toMutableMap()
            units.forEach { byId[it.id] = it }
            state.value = byId.values.toList()
        }

        override suspend fun deleteById(id: String) {
            state.value = state.value.filterNot { it.id == id }
        }

        override suspend fun deleteAll() {
            state.value = emptyList()
        }
    }

    private class FakeUnitsApi(
        private val units: List<UnitDto> = emptyList(),
        private val failure: Throwable? = null,
        private val updateResponse: ((UnitMutationDto) -> UnitDto)? = null,
        private val mutationFailure: Throwable? = null,
    ) : UnitsApi {
        override suspend fun getUnits(page: Int, perPage: Int): PagedResponseDto<UnitDto> {
            failure?.let { throw it }
            return PagedResponseDto(1, units.size, units.size, 1, units)
        }

        override suspend fun createUnit(request: UnitMutationDto): UnitDto =
            error("not used by UnitRepositoryTest")

        override suspend fun updateUnit(itemId: String, request: UnitMutationDto): UnitDto {
            mutationFailure?.let { throw it }
            return updateResponse?.invoke(request) ?: error("not used by UnitRepositoryTest")
        }

        override suspend fun deleteUnit(itemId: String): ResponseBody =
            error("not used by UnitRepositoryTest")
    }
}

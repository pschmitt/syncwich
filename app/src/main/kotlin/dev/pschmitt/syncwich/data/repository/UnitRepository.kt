package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.UnitsApi
import dev.pschmitt.syncwich.data.api.dto.UnitDto
import dev.pschmitt.syncwich.data.api.dto.UnitMutationDto
import dev.pschmitt.syncwich.data.db.dao.UnitDao
import dev.pschmitt.syncwich.data.db.entity.UnitEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Cache-first, offline-first unit access - mirrors [CategoryRepository]'s shape, except
 * [updateUnit] must round-trip [UnitDto]'s unexposed fields (see its kdoc) rather than just
 * name/pluralName/description/abbreviation, so it decodes [UnitEntity.rawJson] instead of building
 * a bare mutation DTO from scratch.
 */
@Singleton
class UnitRepository
@Inject
constructor(
    private val unitsApi: UnitsApi,
    private val unitDao: UnitDao,
    private val json: Json,
) {

    fun observeUnits(): Flow<List<UnitEntity>> = unitDao.observeAll()

    fun observeUnit(unitId: String): Flow<UnitEntity?> = unitDao.observeById(unitId)

    suspend fun refreshUnits(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                    val allItems = mutableListOf<UnitDto>()
                    var page = 1
                    while (true) {
                        val response =
                            unitsApi.getUnits(page = page, perPage = UnitsApi.DEFAULT_PAGE_SIZE)
                        allItems += response.items
                        if (response.items.isEmpty() || page >= response.totalPages) break
                        page++
                    }
                    unitDao.replaceAll(allItems.map { it.toEntity() })
                }
                .onFailure { Timber.w(it, "Unit refresh failed; keeping cached data") }
        }

    suspend fun createUnit(
        name: String,
        pluralName: String?,
        description: String,
        abbreviation: String,
    ): Result<UnitEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                    val entity =
                        unitsApi
                            .createUnit(
                                UnitMutationDto(
                                    name = name,
                                    pluralName = pluralName,
                                    description = description,
                                    abbreviation = abbreviation,
                                )
                            )
                            .toEntity()
                    unitDao.upsertAll(listOf(entity))
                    entity
                }
                .onFailure { Timber.w(it, "Unit creation failed; keeping cached data") }
        }

    suspend fun updateUnit(
        unitId: String,
        name: String,
        pluralName: String?,
        description: String,
        abbreviation: String,
    ): Result<UnitEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                    val cached = unitDao.observeById(unitId).first()
                    val existing =
                        cached?.let { json.decodeFromString(UnitDto.serializer(), it.rawJson) }
                    val request =
                        UnitMutationDto(
                            name = name,
                            pluralName = pluralName,
                            description = description,
                            abbreviation = abbreviation,
                            extras = existing?.extras,
                            fraction = existing?.fraction ?: true,
                            pluralAbbreviation = existing?.pluralAbbreviation ?: "",
                            useAbbreviation = existing?.useAbbreviation ?: false,
                            aliases = existing?.aliases ?: emptyList(),
                            standardQuantity = existing?.standardQuantity,
                            standardUnit = existing?.standardUnit,
                        )
                    val entity = unitsApi.updateUnit(unitId, request).toEntity()
                    unitDao.upsertAll(listOf(entity))
                    entity
                }
                .onFailure { Timber.w(it, "Unit update failed for '$unitId'; keeping cached data") }
        }

    suspend fun deleteUnit(unitId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                    unitsApi.deleteUnit(unitId).use {}
                    unitDao.deleteById(unitId)
                }
                .onFailure { Timber.w(it, "Unit deletion failed for '$unitId'; keeping cached data") }
        }

    private fun UnitDto.toEntity() =
        UnitEntity(
            id = id,
            name = name,
            pluralName = pluralName,
            description = description,
            abbreviation = abbreviation,
            rawJson = json.encodeToString(UnitDto.serializer(), this),
        )
}

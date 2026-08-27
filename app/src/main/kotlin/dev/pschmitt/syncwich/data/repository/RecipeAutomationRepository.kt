package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.RecipeAutomationsApi
import dev.pschmitt.syncwich.data.api.dto.RecipeAutomationCreateDto
import dev.pschmitt.syncwich.data.api.dto.RecipeAutomationDto
import dev.pschmitt.syncwich.data.api.dto.RecipeAutomationSaveDto
import dev.pschmitt.syncwich.data.db.dao.RecipeAutomationDao
import dev.pschmitt.syncwich.data.db.entity.RecipeAutomationEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Cache-first, offline-first access to Mealie's household recipe-action automations - mirrors
 * [LabelRepository]'s shape, since [updateAutomation] similarly must round-trip
 * [RecipeAutomationEntity.groupId]/[RecipeAutomationEntity.householdId] into the request body (see
 * [RecipeAutomationSaveDto]'s kdoc).
 */
@Singleton
class RecipeAutomationRepository
@Inject
constructor(
    private val recipeAutomationsApi: RecipeAutomationsApi,
    private val recipeAutomationDao: RecipeAutomationDao,
) {

    fun observeAutomations(): Flow<List<RecipeAutomationEntity>> = recipeAutomationDao.observeAll()

    fun observeAutomation(automationId: String): Flow<RecipeAutomationEntity?> =
        recipeAutomationDao.observeById(automationId)

    suspend fun refreshAutomations(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val allItems = mutableListOf<RecipeAutomationDto>()
                var page = 1
                while (true) {
                    val response =
                        recipeAutomationsApi.getRecipeAutomations(
                            page = page,
                            perPage = RecipeAutomationsApi.DEFAULT_PAGE_SIZE,
                        )
                    allItems += response.items
                    if (response.items.isEmpty() || page >= response.totalPages) break
                    page++
                }
                recipeAutomationDao.replaceAll(allItems.map { it.toEntity() })
            }
                .onFailure { Timber.w(it, "Recipe-action refresh failed; keeping cached data") }
        }

    suspend fun createAutomation(
        actionType: String,
        title: String,
        url: String,
    ): Result<RecipeAutomationEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                val entity =
                    recipeAutomationsApi
                        .createRecipeAutomation(
                            RecipeAutomationCreateDto(
                                actionType = actionType,
                                title = title,
                                url = url,
                            )
                        )
                        .toEntity()
                recipeAutomationDao.upsertAll(listOf(entity))
                entity
            }
                .onFailure { Timber.w(it, "Recipe-action creation failed; keeping cached data") }
        }

    suspend fun updateAutomation(
        automationId: String,
        actionType: String,
        title: String,
        url: String,
    ): Result<RecipeAutomationEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cached =
                    recipeAutomationDao.observeById(automationId).first()
                        ?: error(
                            "No cached recipe action '$automationId' to round-trip " +
                                "groupId/householdId from"
                        )
                val entity =
                    recipeAutomationsApi
                        .updateRecipeAutomation(
                            automationId,
                            RecipeAutomationSaveDto(
                                actionType = actionType,
                                title = title,
                                url = url,
                                groupId = cached.groupId,
                                householdId = cached.householdId,
                            ),
                        )
                        .toEntity()
                recipeAutomationDao.upsertAll(listOf(entity))
                entity
            }
                .onFailure {
                    Timber.w(
                        it,
                        "Recipe-action update failed for '$automationId'; keeping cached data",
                    )
                }
        }

    suspend fun deleteAutomation(automationId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                recipeAutomationsApi.deleteRecipeAutomation(automationId).use {}
                recipeAutomationDao.deleteById(automationId)
            }
                .onFailure {
                    Timber.w(
                        it,
                        "Recipe-action deletion failed for '$automationId'; keeping cached data",
                    )
                }
        }

    private fun RecipeAutomationDto.toEntity() =
        RecipeAutomationEntity(
            id = id,
            actionType = actionType,
            title = title,
            url = url,
            groupId = groupId,
            householdId = householdId,
        )
}

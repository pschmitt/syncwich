package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.CookbooksApi
import dev.pschmitt.syncwich.data.api.RecipesApi
import dev.pschmitt.syncwich.data.api.dto.CookbookDto
import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import dev.pschmitt.syncwich.data.api.dto.RecipeSummaryDto
import dev.pschmitt.syncwich.data.db.dao.CookbookDao
import dev.pschmitt.syncwich.data.db.dao.RecipeDao
import dev.pschmitt.syncwich.data.db.entity.CookbookEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeCookbookCrossRef
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Cache-first, offline-first cookbook access - mirrors [RecipeRepository]/[CategoryRepository]'s
 * shape. A cookbook is a saved recipe filter, not an embedded recipe list (see [CookbookDto]'s
 * kdoc), so [refreshCookbooks] fetches the cookbook dictionary and then, per cookbook, the recipes
 * that currently match it - both cached so [observeCookbookRecipes] works fully offline. All network
 * calls happen before any Room write, so a failure partway through (a bad cookbook filter, a
 * dropped connection) leaves whatever was cached before completely untouched, same contract as
 * [RecipeRepository.refreshRecipes].
 */
@Singleton
class CookbookRepository
@Inject
constructor(
    private val cookbooksApi: CookbooksApi,
    private val recipesApi: RecipesApi,
    private val cookbookDao: CookbookDao,
    private val recipeDao: RecipeDao,
) {

    fun observeCookbooks(): Flow<List<CookbookEntity>> = cookbookDao.observeAll()

    fun observeCookbook(cookbookId: String): Flow<CookbookEntity?> =
        cookbookDao.observeById(cookbookId)

    fun observeCookbookRecipes(cookbookId: String): Flow<List<RecipeSummaryEntity>> =
        recipeDao.observeByCookbook(cookbookId)

    suspend fun refreshCookbooks(): Result<Unit> =
        runCatching {
                val cookbooks = fetchAllPages { page -> cookbooksApi.getCookbooks(page = page) }

                val recipeSummaries = mutableMapOf<String, RecipeSummaryEntity>()
                val cookbookRefs = mutableListOf<RecipeCookbookCrossRef>()
                cookbooks.forEach { cookbook ->
                    val recipes =
                        fetchAllPages { page ->
                            recipesApi.getRecipesByCookbook(cookbookId = cookbook.id, page = page)
                        }
                    recipes.forEach { recipe ->
                        recipeSummaries[recipe.id] = recipe.toEntity()
                        cookbookRefs +=
                            RecipeCookbookCrossRef(recipeId = recipe.id, cookbookId = cookbook.id)
                    }
                }

                // Non-destructive: recipes seen here are opportunistically kept fresh, but
                // RecipeRepository.refreshRecipes remains the sole authoritative full-list replace -
                // see its kdoc for the equivalent category/tag rationale.
                if (recipeSummaries.isNotEmpty()) recipeDao.upsertAll(recipeSummaries.values.toList())
                recipeDao.replaceCookbookCrossRefs(cookbookRefs)
                cookbookDao.replaceAll(cookbooks.map { it.toEntity() })
            }
            .onFailure { Timber.w(it, "Cookbook refresh failed; keeping cached data") }

    private suspend fun <T> fetchAllPages(
        loadPage: suspend (page: Int) -> PagedResponseDto<T>
    ): List<T> {
        val allItems = mutableListOf<T>()
        var page = 1
        while (true) {
            val response = loadPage(page)
            allItems += response.items
            if (response.items.isEmpty() || page >= response.totalPages) break
            page++
        }
        return allItems
    }

    private fun CookbookDto.toEntity() =
        CookbookEntity(
            id = id,
            name = name,
            slug = slug,
            description = description.orEmpty(),
            position = position,
        )

    private fun RecipeSummaryDto.toEntity() =
        RecipeSummaryEntity(
            id = id,
            slug = slug,
            name = name,
            description = description.orEmpty(),
            image = image,
            rating = rating,
            prepTime = prepTime,
            totalTime = totalTime,
            dateAdded = dateAdded,
            lastMade = lastMade,
        )
}

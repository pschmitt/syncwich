package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.CookbooksApi
import dev.pschmitt.syncwich.data.api.RecipesApi
import dev.pschmitt.syncwich.data.api.dto.CookbookDto
import dev.pschmitt.syncwich.data.api.dto.CreateCookbookDto
import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import dev.pschmitt.syncwich.data.api.dto.RecipeSummaryDto
import dev.pschmitt.syncwich.data.db.dao.CookbookDao
import dev.pschmitt.syncwich.data.db.dao.RecipeDao
import dev.pschmitt.syncwich.data.db.entity.CookbookEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeCookbookCrossRef
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Cache-first, offline-first cookbook access - mirrors [RecipeRepository]/[CategoryRepository]'s
 * shape. A cookbook is a saved recipe filter, not an embedded recipe list (see [CookbookDto]'s
 * kdoc), so [refreshCookbooks] fetches the cookbook dictionary and then, per cookbook, the recipes
 * that currently match it - both cached so [observeCookbookRecipes] works fully offline. All network
 * calls happen before any Room write, so a failure partway through (a bad cookbook filter, a
 * dropped connection) leaves whatever was cached before completely untouched, same contract as
 * [RecipeRepository.refreshRecipes]. Automatic refreshes are coalesced for a short freshness
 * window because Home, the cookbook list, and a restored navigation destination can all become
 * active around the same time.
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

    private val refreshMutex = Mutex()
    private var lastCookbookDictionaryRefreshAt: Long? = null
    private val lastCookbookRefreshAt = mutableMapOf<String, Long>()

    fun observeCookbooks(): Flow<List<CookbookEntity>> = cookbookDao.observeAll()

    fun observeHasCachedCookbooks(): Flow<Boolean> = observeCookbooks().map { it.isNotEmpty() }

    fun observeCookbook(cookbookId: String): Flow<CookbookEntity?> =
        cookbookDao.observeById(cookbookId)

    fun observeCookbookBySlug(slug: String): Flow<CookbookEntity?> =
        cookbookDao.observeBySlug(slug)

    fun observeCookbookRecipes(cookbookId: String): Flow<List<RecipeSummaryEntity>> =
        recipeDao.observeByCookbook(cookbookId)

    /**
     * Creates a cookbook and immediately caches the returned server object. A failed request
     * leaves the existing cookbook cache untouched, including when the device is offline.
     */
    suspend fun createCookbook(request: CreateCookbookDto): Result<CookbookEntity> =
        mutateCookbook { cookbooksApi.createCookbook(request) }

    /** Updates one cookbook using the v3.22.0 single-item PUT route and refreshes its Room row. */
    suspend fun updateCookbook(
        cookbookId: String,
        request: CreateCookbookDto,
    ): Result<CookbookEntity> = mutateCookbook { cookbooksApi.updateCookbook(cookbookId, request) }

    suspend fun refreshCookbooks(forceRefresh: Boolean = false): Result<Unit> =
        withContext(Dispatchers.IO) {
            refreshMutex.withLock {
                if (!forceRefresh && isFresh(lastCookbookDictionaryRefreshAt)) {
                    Timber.d("Skipping fresh cookbook refresh")
                    return@withLock Result.success(Unit)
                }
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
                                RecipeCookbookCrossRef(
                                    recipeId = recipe.id,
                                    cookbookId = cookbook.id,
                                )
                        }
                    }

                    // Non-destructive: recipes seen here are opportunistically kept fresh, but
                    // RecipeRepository.refreshRecipes remains the sole authoritative full-list replace -
                    // see its kdoc for the equivalent category/tag rationale.
                    if (recipeSummaries.isNotEmpty())
                        recipeDao.upsertAll(recipeSummaries.values.toList())
                    recipeDao.replaceCookbookCrossRefs(cookbookRefs)
                    cookbookDao.replaceAll(cookbooks.map { it.toEntity() })
                    val refreshedAt = System.currentTimeMillis()
                    lastCookbookDictionaryRefreshAt = refreshedAt
                    lastCookbookRefreshAt.clear()
                    cookbooks.forEach { lastCookbookRefreshAt[it.id] = refreshedAt }
                    Timber.d(
                        "Cookbook refresh cached ${cookbooks.size} cookbooks and " +
                            "${recipeSummaries.size} recipe summaries"
                    )
                }
                    .onFailure { Timber.w(it, "Cookbook refresh failed; keeping cached data") }
            }
        }

    /** Refreshes one cookbook's matching summaries without touching other cookbook membership. */
    suspend fun refreshCookbookRecipes(
        cookbookId: String,
        forceRefresh: Boolean = false,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            refreshMutex.withLock {
                if (!forceRefresh && isFresh(lastCookbookRefreshAt[cookbookId])) {
                    Timber.d("Skipping fresh cookbook recipe refresh for $cookbookId")
                    return@withLock Result.success(Unit)
                }
                runCatching {
                    val recipes =
                        fetchAllPages { page ->
                            recipesApi.getRecipesByCookbook(cookbookId = cookbookId, page = page)
                        }
                    val entities = recipes.map { it.toEntity() }
                    recipeDao.replaceCookbookRecipeCache(
                        cookbookId = cookbookId,
                        recipes = entities,
                        refs = recipes.map { RecipeCookbookCrossRef(it.id, cookbookId) },
                    )
                    lastCookbookRefreshAt[cookbookId] = System.currentTimeMillis()
                    Timber.d(
                        "Cookbook recipe refresh cached ${entities.size} summaries for $cookbookId"
                    )
                }
                    .onFailure {
                        Timber.w(it, "Cookbook recipe refresh failed for $cookbookId; keeping cache")
                    }
            }
        }

    private fun isFresh(lastRefreshAt: Long?): Boolean =
        lastRefreshAt != null &&
            System.currentTimeMillis() - lastRefreshAt < AUTOMATIC_REFRESH_WINDOW_MS

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
            public = public,
            queryFilterString = queryFilterString.orEmpty(),
        )

    private suspend fun mutateCookbook(
        request: suspend () -> CookbookDto,
    ): Result<CookbookEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                    val entity = request().toEntity()
                    cookbookDao.upsertAll(listOf(entity))
                    entity
                }
                .onFailure { Timber.w(it, "Cookbook mutation failed; keeping cached data") }
        }

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

    private companion object {
        const val AUTOMATIC_REFRESH_WINDOW_MS = 30_000L
    }
}

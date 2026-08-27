package dev.pschmitt.syncwich.data.repository

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import dev.pschmitt.syncwich.data.api.RecipesApi
import dev.pschmitt.syncwich.data.api.dto.CreateRecipeDto
import dev.pschmitt.syncwich.data.api.dto.OrganizerDto
import dev.pschmitt.syncwich.data.api.dto.RecipeAssetDto
import dev.pschmitt.syncwich.data.api.dto.RecipeDetailDto
import dev.pschmitt.syncwich.data.api.dto.RecipeInputDto
import dev.pschmitt.syncwich.data.api.dto.RecipeSummaryDto
import dev.pschmitt.syncwich.data.api.dto.ScrapeRecipeDto
import dev.pschmitt.syncwich.data.db.AppDatabase
import dev.pschmitt.syncwich.data.db.dao.RecipeActionDao
import dev.pschmitt.syncwich.data.db.dao.RecipeDao
import dev.pschmitt.syncwich.data.db.dao.RecipeStepProgressDao
import dev.pschmitt.syncwich.data.db.entity.CategoryEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeCategoryCrossRef
import dev.pschmitt.syncwich.data.db.entity.RecipeDetailEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeFoodCrossRef
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeTagCrossRef
import dev.pschmitt.syncwich.data.db.entity.RecipeToolCrossRef
import dev.pschmitt.syncwich.data.db.entity.TagEntity
import dev.pschmitt.syncwich.data.db.entity.ToolEntity
import dev.pschmitt.syncwich.data.image.selectRecipeImagePrefetchUrls
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

/**
 * Cache-first, offline-first recipe access - see AGENTS.md's architecture section. Every read is a
 * [Flow] straight from Room; [refreshRecipes]/[refreshRecipeDetail] are best-effort background
 * refreshes that upsert into Room on success and are silently logged (never thrown, never wipe the
 * existing cache) on failure. `CategoryRepository`/`TagRepository` mirror this same shape.
 */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class RecipeRepository
@Inject
constructor(
    private val recipesApi: RecipesApi,
    private val recipeDao: RecipeDao,
    private val recipeActionDao: RecipeActionDao,
    private val recipeStepProgressDao: RecipeStepProgressDao,
    private val database: AppDatabase,
    private val json: Json,
) {

    private val refreshMutex = Mutex()
    private var lastRecipeListRefreshAt: Long? = null
    private val lastDetailRefreshAt = mutableMapOf<String, Long>()

    fun observeRecipes(): Flow<List<RecipeSummaryEntity>> = recipeDao.observeAll()

    /** Returns cached recipe summaries whose existing per-user action state marks them favorite. */
    fun observeFavoriteRecipes(): Flow<List<RecipeSummaryEntity>> =
        recipeDao.observeAll().flatMapLatest { recipes ->
            if (recipes.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(recipes.map { recipe -> recipeActionDao.observe(recipe.id) }) { actions ->
                    recipes.filterIndexed { index, _ -> actions[index]?.isFavorite == true }
                }
            }
        }

    /** Returns cached favorite ids so list screens can decorate any matching recipe summary. */
    fun observeFavoriteRecipeIds(): Flow<List<String>> = recipeActionDao.observeFavoriteIds()

    /**
     * Sends the minimal `CreateRecipe` body. No cache is cleared on failure; a later refresh can
     * discover a successful server-side create without making an offline read unavailable.
     */
    suspend fun createRecipe(request: CreateRecipeDto): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(request.name.isNotBlank()) { "Recipe name must not be blank" }
                recipesApi.createRecipe(request).use { it.string() }
            }
                .onFailure { Timber.w(it, "Recipe create failed; keeping cached data") }
        }

    /** Sends one complete `Recipe-Input` through Mealie's single-recipe PUT route. */
    suspend fun updateRecipe(slug: String, request: RecipeInputDto): Result<Unit> =
        mutateRecipe(slug) { recipesApi.updateRecipe(slug, request) }

    /** Sends one complete `Recipe-Input` through Mealie's single-recipe PATCH route. */
    suspend fun patchRecipe(slug: String, request: RecipeInputDto): Result<Unit> =
        mutateRecipe(slug) { recipesApi.patchRecipe(slug, request) }

    /** Deletes the server recipe first; the offline cache is removed only after that succeeds. */
    suspend fun deleteRecipe(recipeId: String, slug: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                recipesApi.deleteRecipe(slug).use {}
                recipeDao.deleteRecipeCache(recipeId)
                recipeActionDao.delete(recipeId)
                recipeStepProgressDao.deleteForRecipe(recipeId)
            }
                .onFailure {
                    Timber.w(it, "Recipe deletion failed for '$slug'; keeping cached data")
                }
        }

    /**
     * Parses a URL through Mealie; no local cache is changed until the returned recipe is
     * refreshed.
     */
    suspend fun parseRecipeUrl(url: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(url.isNotBlank()) { "Recipe URL must not be blank" }
                recipesApi
                    .parseRecipeUrl(ScrapeRecipeDto(url = url.trim()))
                    .use { it.string().trim().trim('"') }
                    .also { require(it.isNotBlank()) { "Mealie returned no recipe reference" } }
            }
                .onFailure { Timber.w(it, "Recipe URL parsing failed; keeping cached data") }
        }

    suspend fun updateRecipeImage(
        slug: String,
        uri: Uri,
        contentResolver: ContentResolver,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val media = readLocalMedia(uri, contentResolver)
                val image =
                    MultipartBody.Part.createFormData(
                        "image",
                        media.fileName,
                        media.bytes.toRequestBody(media.mimeType.toMediaType()),
                    )
                recipesApi
                    .updateRecipeImage(
                        slug,
                        image,
                        media.extension.toRequestBody("text/plain".toMediaType()),
                    )
                    .use {}
            }
                .onFailure { Timber.w(it, "Recipe cover image update failed; keeping draft") }
        }

    suspend fun deleteRecipeImage(slug: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { recipesApi.deleteRecipeImage(slug).use {} }
                .onFailure { Timber.w(it, "Recipe cover image deletion failed") }
        }

    suspend fun uploadRecipeAsset(
        slug: String,
        name: String,
        uri: Uri,
        contentResolver: ContentResolver,
    ): Result<RecipeAssetDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val media = readLocalMedia(uri, contentResolver)
                val file =
                    MultipartBody.Part.createFormData(
                        "file",
                        media.fileName,
                        media.bytes.toRequestBody(media.mimeType.toMediaType()),
                    )
                recipesApi.uploadRecipeAsset(
                    slug,
                    name.toRequestBody("text/plain".toMediaType()),
                    "image".toRequestBody("text/plain".toMediaType()),
                    media.extension.toRequestBody("text/plain".toMediaType()),
                    file,
                )
            }
                .onFailure { Timber.w(it, "Recipe asset upload failed; keeping draft") }
        }

    fun observeRecipesByCategory(categoryId: String): Flow<List<RecipeSummaryEntity>> =
        recipeDao.observeByCategory(categoryId)

    fun observeRecipesByTag(tagId: String): Flow<List<RecipeSummaryEntity>> =
        recipeDao.observeByTag(tagId)

    fun observeRecipesByTool(toolId: String): Flow<List<RecipeSummaryEntity>> =
        recipeDao.observeByTool(toolId)

    fun observeRecipesByFood(foodId: String): Flow<List<RecipeSummaryEntity>> =
        recipeDao.observeByFood(foodId)

    /** The full recipe, decoded lazily by the caller - see [RecipeDetailEntity]'s kdoc. */
    fun observeRecipeDetail(recipeId: String): Flow<RecipeDetailEntity?> =
        recipeDao.observeDetail(recipeId)

    fun observeRecipeDetailBySlug(slug: String): Flow<RecipeDetailEntity?> =
        recipeDao.observeDetailBySlug(slug)

    /** Returns prefetch candidates from Room without making any network request. */
    suspend fun cachedRecipeImagePrefetchUrls(
        serverUrl: String,
        json: kotlinx.serialization.json.Json,
    ): List<String> =
        selectRecipeImagePrefetchUrls(
            serverUrl = serverUrl,
            recipes = recipeDao.getAll(),
            details = recipeDao.getAllDetails(),
            json = json,
        )

    /**
     * Fetches every page of `/api/recipes` and replaces the cached list + category/tag associations
     * in one transaction. A failure leaves whatever was cached before completely untouched -
     * callers should treat this as "refresh attempted", not "recipes are now guaranteed fresh".
     */
    suspend fun refreshRecipes(forceRefresh: Boolean = false): Result<Unit> =
        withContext(Dispatchers.IO) {
            refreshMutex.withLock {
                if (!forceRefresh && isFresh(lastRecipeListRefreshAt)) {
                    Timber.d("Skipping fresh recipe-list refresh")
                    return@withLock Result.success(Unit)
                }
                runCatching {
                    val allItems = mutableListOf<RecipeSummaryDto>()
                    var page = 1
                    while (true) {
                        val response =
                            recipesApi.getRecipes(
                                page = page,
                                perPage = RecipesApi.DEFAULT_PAGE_SIZE,
                            )
                        allItems += response.items
                        if (response.items.isEmpty() || page >= response.totalPages) break
                        page++
                    }

                    val categories = mutableMapOf<String, CategoryEntity>()
                    val tags = mutableMapOf<String, TagEntity>()
                    val tools = mutableMapOf<String, ToolEntity>()
                    val categoryRefs = mutableListOf<RecipeCategoryCrossRef>()
                    val tagRefs = mutableListOf<RecipeTagCrossRef>()
                    val toolRefs = mutableListOf<RecipeToolCrossRef>()
                    val recipes = allItems.map { dto ->
                        dto.recipeCategory.forEach { category ->
                            categories[category.id] = category.toCategoryEntity()
                            categoryRefs +=
                                RecipeCategoryCrossRef(recipeId = dto.id, categoryId = category.id)
                        }
                        dto.tags.forEach { tag ->
                            tags[tag.id] = tag.toTagEntity()
                            tagRefs += RecipeTagCrossRef(recipeId = dto.id, tagId = tag.id)
                        }
                        dto.tools.forEach { tool ->
                            tools[tool.id] = tool.toToolEntity()
                            toolRefs += RecipeToolCrossRef(recipeId = dto.id, toolId = tool.id)
                        }
                        dto.toEntity()
                    }

                    database.withTransaction {
                        recipeDao.deleteAll()
                        recipeDao.deleteAllCategoryCrossRefs()
                        recipeDao.deleteAllTagCrossRefs()
                        recipeDao.deleteAllToolCrossRefs()
                        // Non-destructive: unlike the recipe list itself, the category/tag/tool
                        // dictionaries
                        // are authoritatively refreshed by CategoryRepository/TagRepository/
                        // ToolRepository - upsert
                        // here only so a name/slug embedded in this response is never stale,
                        // without
                        // racing a delete of a category that has no recipes (and thus never shows
                        // up
                        // in this loop) out from under CategoryRepository's own refresh.
                        if (categories.isNotEmpty())
                            database.categoryDao().upsertAll(categories.values.toList())
                        if (tags.isNotEmpty()) database.tagDao().upsertAll(tags.values.toList())
                        if (tools.isNotEmpty()) database.toolDao().upsertAll(tools.values.toList())
                        recipeDao.upsertAll(recipes)
                        if (categoryRefs.isNotEmpty())
                            recipeDao.insertCategoryCrossRefs(categoryRefs)
                        if (tagRefs.isNotEmpty()) recipeDao.insertTagCrossRefs(tagRefs)
                        if (toolRefs.isNotEmpty()) recipeDao.insertToolCrossRefs(toolRefs)
                    }
                    lastRecipeListRefreshAt = System.currentTimeMillis()
                    Timber.d("Recipe-list refresh cached ${recipes.size} summaries")
                }
                    .onFailure { Timber.w(it, "Recipe list refresh failed; keeping cached data") }
            }
        }

    /** Fetches one recipe's full detail and caches its raw JSON - see [RecipeDetailEntity]. */
    suspend fun refreshRecipeDetail(
        recipeId: String,
        slug: String,
        forceRefresh: Boolean = false,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            refreshMutex.withLock {
                if (!forceRefresh && isFresh(lastDetailRefreshAt[recipeId])) {
                    Timber.d("Skipping fresh recipe-detail refresh for $recipeId")
                    return@withLock Result.success(Unit)
                }
                runCatching {
                    val body = recipesApi.getRecipeDetailRaw(slug).string()
                    val cachedId = recipeId.ifBlank {
                        json.decodeFromString<RecipeDetailDto>(body).id
                    }
                    recipeDao.upsertDetail(
                        RecipeDetailEntity(
                            id = cachedId,
                            slug = slug,
                            detailJson = body,
                            fetchedAt = System.currentTimeMillis(),
                        )
                    )
                    lastDetailRefreshAt[cachedId] = System.currentTimeMillis()
                    Timber.d("Recipe-detail refresh cached $cachedId")
                }
                    .onFailure {
                        Timber.w(
                            it,
                            "Recipe detail refresh failed for '$slug'; keeping cached data",
                        )
                    }
            }
        }

    /**
     * Populates the recipe<->food cross-ref that backs SW-142's "filter recipes by food" - unlike
     * category/tag/tool (embedded in `/api/recipes`'s list response), food references only exist in
     * each recipe's full detail, so this bulk-fetches every recipe whose cached detail is missing
     * or stale (its [RecipeDetailEntity.sourceUpdatedAt] doesn't match the summary's current
     * [RecipeSummaryEntity.dateUpdated]) - deliberately expensive (one request per changed recipe),
     * accepted per SW-142's documented tradeoff rather than left as an unused "unit"-style JSON
     * blob. Skips a recipe's fetch on failure and keeps whatever cross-refs already exist for it,
     * matching this repository's "never block/wipe on a bad run" contract.
     */
    suspend fun refreshRecipeFoodCrossRefs(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val summaries = recipeDao.getAll()
                val cachedDetails = recipeDao.getAllDetails().associateBy { it.id }
                val foodRefs = mutableListOf<RecipeFoodCrossRef>()
                for (summary in summaries) {
                    val cached = cachedDetails[summary.id]
                    val detailJson =
                        if (cached != null && cached.sourceUpdatedAt == summary.dateUpdated) {
                            cached.detailJson
                        } else {
                            runCatching { recipesApi.getRecipeDetailRaw(summary.slug).string() }
                                .onSuccess { body ->
                                    recipeDao.upsertDetail(
                                        RecipeDetailEntity(
                                            id = summary.id,
                                            slug = summary.slug,
                                            detailJson = body,
                                            fetchedAt = System.currentTimeMillis(),
                                            sourceUpdatedAt = summary.dateUpdated,
                                        )
                                    )
                                }
                                .onFailure {
                                    Timber.w(
                                        it,
                                        "Recipe detail fetch failed for '${summary.slug}' during" +
                                            " food cross-ref refresh; using stale/no cached detail",
                                    )
                                }
                                .getOrNull() ?: cached?.detailJson
                        }
                    detailJson ?: continue
                    runCatching { json.decodeFromString<RecipeDetailDto>(detailJson) }
                        .onSuccess { detail ->
                            detail.recipeIngredient.forEach { ingredient ->
                                ingredient.food?.id?.let { foodId ->
                                    foodRefs +=
                                        RecipeFoodCrossRef(recipeId = summary.id, foodId = foodId)
                                }
                            }
                        }
                        .onFailure {
                            Timber.w(it, "Couldn't decode cached detail for '${summary.slug}'")
                        }
                }
                database.withTransaction {
                    recipeDao.deleteAllFoodCrossRefs()
                    if (foodRefs.isNotEmpty()) recipeDao.insertFoodCrossRefs(foodRefs)
                }
                Timber.d("Recipe-food cross-ref refresh cached ${foodRefs.size} references")
            }
                .onFailure {
                    Timber.w(it, "Recipe food cross-ref refresh failed; keeping cached data")
                }
        }

    private fun isFresh(lastRefreshAt: Long?): Boolean =
        lastRefreshAt != null &&
            System.currentTimeMillis() - lastRefreshAt < AUTOMATIC_REFRESH_WINDOW_MS

    private fun OrganizerDto.toCategoryEntity() = CategoryEntity(id = id, name = name, slug = slug)

    private suspend fun mutateRecipe(
        slug: String,
        request: suspend () -> okhttp3.ResponseBody,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { request().use {} }
                .onFailure {
                    Timber.w(it, "Recipe mutation failed for '$slug'; keeping cached data")
                }
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
            dateUpdated = dateUpdated,
            lastMade = lastMade,
        )

    private companion object {
        const val AUTOMATIC_REFRESH_WINDOW_MS = 30_000L
    }
}

private data class LocalMedia(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
    val extension: String,
)

private fun readLocalMedia(uri: Uri, contentResolver: ContentResolver): LocalMedia {
    val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
    val extension = mimeType.substringAfter('/', "jpg").lowercase().replace("jpeg", "jpg").take(8)
    val fileName =
        uri.lastPathSegment?.substringAfterLast('/')?.takeIf(String::isNotBlank)
            ?: "syncwich-image.$extension"
    val bytes =
        if (uri.scheme == "file") {
            FileInputStream(uri.path ?: error("The selected image has no file path")).use {
                it.readBytes()
            }
        } else {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Couldn't read the selected image")
        }
    require(bytes.isNotEmpty()) { "The selected image is empty" }
    return LocalMedia(bytes, fileName, mimeType, extension)
}

private fun OrganizerDto.toTagEntity() = TagEntity(id = id, name = name, slug = slug)

private fun OrganizerDto.toToolEntity() = ToolEntity(id = id, name = name, slug = slug)

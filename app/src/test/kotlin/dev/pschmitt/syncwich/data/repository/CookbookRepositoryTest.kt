package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.CookbooksApi
import dev.pschmitt.syncwich.data.api.RecipesApi
import dev.pschmitt.syncwich.data.api.dto.CookbookDto
import dev.pschmitt.syncwich.data.api.dto.CreateCookbookDto
import dev.pschmitt.syncwich.data.api.dto.CreateRecipeDto
import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import dev.pschmitt.syncwich.data.api.dto.RecipeInputDto
import dev.pschmitt.syncwich.data.api.dto.RecipeSummaryDto
import dev.pschmitt.syncwich.data.api.dto.ScrapeRecipeDto
import dev.pschmitt.syncwich.data.db.dao.CookbookDao
import dev.pschmitt.syncwich.data.db.dao.RecipeDao
import dev.pschmitt.syncwich.data.db.entity.CookbookEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeCategoryCrossRef
import dev.pschmitt.syncwich.data.db.entity.RecipeCookbookCrossRef
import dev.pschmitt.syncwich.data.db.entity.RecipeDetailEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeTagCrossRef
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the "hard requirement" from AGENTS.md's architecture section: a failed refresh must
 * never clear or block what's already cached - see [CategoryRepositoryTest]'s equivalent coverage.
 * Cookbooks additionally cache their matching recipes (via [RecipeCookbookCrossRef]), so both the
 * cookbook dictionary and that membership cache are asserted here.
 */
class CookbookRepositoryTest {

    @Test
    fun `successful cookbook create caches the complete returned entity`() = runTest {
        val cookbookDao = FakeCookbookDao()
        val created =
            CookbookDto(
                id = "created-1",
                name = "Quick dinners",
                slug = "quick-dinners",
                description = "Fast meals",
                position = 3,
                public = true,
                queryFilterString = "tags.id IN [\"quick\"]",
            )
        val repository =
            CookbookRepository(
                FakeCookbooksApi(createResponse = created),
                FakeRecipesApi(),
                cookbookDao,
                FakeRecipeDao(),
            )

        val result = repository.createCookbook(CreateCookbookDto("Quick dinners"))

        assertTrue(result.isSuccess)
        assertEquals(
            CookbookEntity(
                id = "created-1",
                name = "Quick dinners",
                slug = "quick-dinners",
                description = "Fast meals",
                position = 3,
                public = true,
                queryFilterString = "tags.id IN [\"quick\"]",
            ),
            cookbookDao.observeAll().first().single(),
        )
    }

    @Test
    fun `failed cookbook create leaves the existing cache untouched`() = runTest {
        val cached = CookbookEntity("keep-1", "Keep me", "keep-me", "", 0)
        val cookbookDao = FakeCookbookDao(seed = listOf(cached))
        val repository =
            CookbookRepository(
                FakeCookbooksApi(mutationFailure = IOException("offline")),
                FakeRecipesApi(),
                cookbookDao,
                FakeRecipeDao(),
            )

        val result = repository.createCookbook(CreateCookbookDto("New cookbook"))

        assertTrue(result.isFailure)
        assertEquals(listOf(cached), cookbookDao.observeAll().first())
    }

    @Test
    fun `failed cookbook delete leaves the cached cookbook and membership untouched`() = runTest {
        val cached = CookbookEntity("keep-1", "Keep me", "keep-me", "", 0)
        val cookbookDao = FakeCookbookDao(seed = listOf(cached))
        val recipeDao =
            FakeRecipeDao(
                seedRecipes = listOf(recipe("recipe-1")),
                seedCookbookRefs = listOf(RecipeCookbookCrossRef("recipe-1", "keep-1")),
            )
        val repository =
            CookbookRepository(
                FakeCookbooksApi(mutationFailure = IOException("offline")),
                FakeRecipesApi(),
                cookbookDao,
                recipeDao,
            )

        val result = repository.deleteCookbook("keep-1")

        assertTrue(result.isFailure)
        assertEquals(listOf(cached), cookbookDao.observeAll().first())
        assertEquals(
            listOf("recipe-1"),
            recipeDao.observeByCookbook("keep-1").first().map { it.id },
        )
    }

    @Test
    fun `successful cookbook delete removes its dictionary row and membership`() = runTest {
        val cookbookDao =
            FakeCookbookDao(seed = listOf(CookbookEntity("remove-1", "Remove me", "remove", "", 0)))
        val recipeDao =
            FakeRecipeDao(
                seedRecipes = listOf(recipe("recipe-1")),
                seedCookbookRefs = listOf(RecipeCookbookCrossRef("recipe-1", "remove-1")),
            )
        val repository =
            CookbookRepository(
                FakeCookbooksApi(),
                FakeRecipesApi(),
                cookbookDao,
                recipeDao,
            )

        assertTrue(repository.deleteCookbook("remove-1").isSuccess)
        assertEquals(emptyList<CookbookEntity>(), cookbookDao.observeAll().first())
        assertEquals(
            emptyList<RecipeSummaryEntity>(),
            recipeDao.observeByCookbook("remove-1").first(),
        )
    }

    @Test
    fun `refreshCookbooks replaces the cookbook and recipe-membership cache on success`() =
        runTest {
            val cookbookDao =
                FakeCookbookDao(seed = listOf(CookbookEntity("old-1", "Old", "old", "", 0)))
            val recipeDao = FakeRecipeDao()
            val cookbooksApi =
                FakeCookbooksApi(
                    cookbooks = listOf(CookbookDto("new-1", "New", "new", "", 0, false))
                )
            val recipesApi =
                FakeRecipesApi(
                    byCookbook =
                        mapOf("new-1" to listOf(RecipeSummaryDto("r1", "r1-slug", "Recipe One")))
                )
            val repository = CookbookRepository(cookbooksApi, recipesApi, cookbookDao, recipeDao)

            val result = repository.refreshCookbooks()

            assertTrue(result.isSuccess)
            assertEquals(listOf("New"), cookbookDao.observeAll().first().map { it.name })
            assertEquals(
                listOf("Recipe One"),
                recipeDao.observeByCookbook("new-1").first().map { it.name },
            )
        }

    @Test
    fun `a failed refreshCookbooks leaves the cache untouched`() = runTest {
        val cachedCookbooks = listOf(CookbookEntity("keep-1", "Keep Me", "keep-me", "", 0))
        val cookbookDao = FakeCookbookDao(seed = cachedCookbooks)
        val cachedRefs = listOf(RecipeCookbookCrossRef("r-keep", "keep-1"))
        val cachedRecipes =
            listOf(
                RecipeSummaryEntity(
                    id = "r-keep",
                    slug = "r-keep-slug",
                    name = "Keep Recipe",
                    description = "",
                    image = null,
                    rating = null,
                    prepTime = null,
                    totalTime = null,
                    dateAdded = null,
                    lastMade = null,
                )
            )
        val recipeDao = FakeRecipeDao(seedRecipes = cachedRecipes, seedCookbookRefs = cachedRefs)
        val cookbooksApi = FakeCookbooksApi(failure = IOException("network down"))
        val recipesApi = FakeRecipesApi()
        val repository = CookbookRepository(cookbooksApi, recipesApi, cookbookDao, recipeDao)

        val result = repository.refreshCookbooks()

        assertTrue(result.isFailure)
        assertEquals(cachedCookbooks, cookbookDao.observeAll().first())
        assertEquals(
            listOf("Keep Recipe"),
            recipeDao.observeByCookbook("keep-1").first().map { it.name },
        )
    }

    @Test
    fun `automatic cookbook refreshes reuse the recent cache and detail refresh is targeted`() =
        runTest {
            val cookbooks =
                listOf(
                    CookbookDto("one", "One", "one", "", 0, false),
                    CookbookDto("two", "Two", "two", "", 1, false),
                )
            val cookbooksApi = FakeCookbooksApi(cookbooks = cookbooks)
            val recipesApi =
                FakeRecipesApi(
                    byCookbook =
                        mapOf(
                            "one" to listOf(RecipeSummaryDto("r1", "r1", "One recipe")),
                            "two" to listOf(RecipeSummaryDto("r2", "r2", "Two recipe")),
                        )
                )
            val repository =
                CookbookRepository(cookbooksApi, recipesApi, FakeCookbookDao(), FakeRecipeDao())

            repository.refreshCookbooks()
            repository.refreshCookbooks()
            assertEquals(1, cookbooksApi.requestCount)
            assertEquals(1, recipesApi.requestCount("one"))
            assertEquals(1, recipesApi.requestCount("two"))

            val targetedApi =
                FakeRecipesApi(
                    byCookbook =
                        mapOf(
                            "one" to listOf(RecipeSummaryDto("r1", "r1", "One recipe")),
                            "two" to listOf(RecipeSummaryDto("r2", "r2", "Two recipe")),
                        )
                )
            val targetedRepository =
                CookbookRepository(
                    FakeCookbooksApi(),
                    targetedApi,
                    FakeCookbookDao(),
                    FakeRecipeDao(),
                )
            targetedRepository.refreshCookbookRecipes("one")
            targetedRepository.refreshCookbookRecipes("one")
            assertEquals(
                "A restored cookbook detail only refreshes its own membership",
                1,
                targetedApi.requestCount("one"),
            )
            assertEquals(0, targetedApi.requestCount("two"))
        }

    @Test
    fun `explicit cookbook refresh bypasses freshness while staying targeted`() = runTest {
        val recipesApi =
            FakeRecipesApi(
                byCookbook =
                    mapOf("current" to listOf(RecipeSummaryDto("r1", "r1", "Current recipe")))
            )
        val repository =
            CookbookRepository(
                FakeCookbooksApi(),
                recipesApi,
                FakeCookbookDao(),
                FakeRecipeDao(),
            )

        repository.refreshCookbookRecipes("current")
        repository.refreshCookbookRecipes("current", forceRefresh = true)

        assertEquals(2, recipesApi.requestCount("current"))
        assertEquals(0, recipesApi.requestCount("other"))
    }

    private class FakeCookbookDao(seed: List<CookbookEntity> = emptyList()) : CookbookDao {
        private val state = MutableStateFlow(seed)

        override fun observeAll(): Flow<List<CookbookEntity>> = state

        override fun observeById(id: String): Flow<CookbookEntity?> = state.map { list ->
            list.find { it.id == id }
        }

        override fun observeBySlug(slug: String): Flow<CookbookEntity?> = state.map { list ->
            list.find { it.slug == slug }
        }

        override fun observeByRecipe(recipeId: String): Flow<List<CookbookEntity>> =
            flowOf(emptyList())

        override suspend fun upsertAll(cookbooks: List<CookbookEntity>) {
            val byId = state.value.associateBy { it.id }.toMutableMap()
            cookbooks.forEach { byId[it.id] = it }
            state.value = byId.values.toList()
        }

        override suspend fun deleteById(id: String) {
            state.value = state.value.filterNot { it.id == id }
        }

        override suspend fun deleteAll() {
            state.value = emptyList()
        }
    }

    private class FakeRecipeDao(
        seedRecipes: List<RecipeSummaryEntity> = emptyList(),
        seedCookbookRefs: List<RecipeCookbookCrossRef> = emptyList(),
    ) : RecipeDao {
        private val recipes = MutableStateFlow(seedRecipes.associateBy { it.id })
        private val cookbookRefs = MutableStateFlow(seedCookbookRefs)

        override fun observeAll(): Flow<List<RecipeSummaryEntity>> = recipes.map {
            it.values.toList()
        }

        override fun observeByCategory(categoryId: String): Flow<List<RecipeSummaryEntity>> =
            error("not used by CookbookRepository")

        override fun observeByTag(tagId: String): Flow<List<RecipeSummaryEntity>> =
            error("not used by CookbookRepository")

        override fun observeDetail(id: String): Flow<RecipeDetailEntity?> =
            error("not used by CookbookRepository")

        override fun observeDetailBySlug(slug: String): Flow<RecipeDetailEntity?> =
            error("not used by CookbookRepository")

        override suspend fun getAll(): List<RecipeSummaryEntity> = recipes.value.values.toList()

        override suspend fun getAllDetails(): List<RecipeDetailEntity> = emptyList()

        override fun observeByCookbook(cookbookId: String): Flow<List<RecipeSummaryEntity>> =
            combine(cookbookRefs, recipes) { refs, recipesById ->
                refs.filter { it.cookbookId == cookbookId }.mapNotNull { recipesById[it.recipeId] }
            }

        override suspend fun upsertAll(recipes: List<RecipeSummaryEntity>) {
            val byId = this.recipes.value.toMutableMap()
            recipes.forEach { byId[it.id] = it }
            this.recipes.value = byId
        }

        override suspend fun upsertDetail(detail: RecipeDetailEntity) {
            error("not used by CookbookRepository")
        }

        override suspend fun deleteSummary(recipeId: String) {
            recipes.value = recipes.value - recipeId
        }

        override suspend fun deleteDetail(recipeId: String) = Unit

        override suspend fun deleteCategoryCrossRefs(recipeId: String) = Unit

        override suspend fun deleteTagCrossRefs(recipeId: String) = Unit

        override suspend fun deleteRecipeCookbookCrossRefs(recipeId: String) {
            cookbookRefs.value = cookbookRefs.value.filterNot { it.recipeId == recipeId }
        }

        override suspend fun deleteRecipeCache(recipeId: String) {
            deleteSummary(recipeId)
            deleteDetail(recipeId)
            deleteCategoryCrossRefs(recipeId)
            deleteTagCrossRefs(recipeId)
            deleteRecipeCookbookCrossRefs(recipeId)
        }

        override suspend fun insertCategoryCrossRefs(refs: List<RecipeCategoryCrossRef>) {
            error("not used by CookbookRepository")
        }

        override suspend fun insertTagCrossRefs(refs: List<RecipeTagCrossRef>) {
            error("not used by CookbookRepository")
        }

        override suspend fun insertCookbookCrossRefs(refs: List<RecipeCookbookCrossRef>) {
            cookbookRefs.value = cookbookRefs.value + refs
        }

        override suspend fun deleteAll() {
            error("not used by CookbookRepository")
        }

        override suspend fun deleteAllCategoryCrossRefs() {
            error("not used by CookbookRepository")
        }

        override suspend fun deleteAllTagCrossRefs() {
            error("not used by CookbookRepository")
        }

        override suspend fun deleteAllCookbookCrossRefs() {
            cookbookRefs.value = emptyList()
        }

        override suspend fun deleteCookbookCrossRefs(cookbookId: String) {
            cookbookRefs.value = cookbookRefs.value.filterNot { it.cookbookId == cookbookId }
        }

        override suspend fun replaceCookbookRecipeCache(
            cookbookId: String,
            recipes: List<RecipeSummaryEntity>,
            refs: List<RecipeCookbookCrossRef>,
        ) {
            upsertAll(recipes)
            deleteCookbookCrossRefs(cookbookId)
            insertCookbookCrossRefs(refs)
        }
    }

    private class FakeCookbooksApi(
        private val cookbooks: List<CookbookDto> = emptyList(),
        private val failure: Throwable? = null,
        private val createResponse: CookbookDto? = null,
        private val updateResponse: CookbookDto? = null,
        private val mutationFailure: Throwable? = null,
    ) : CookbooksApi {
        var requestCount = 0
            private set

        override suspend fun createCookbook(request: CreateCookbookDto): CookbookDto =
            mutationFailure?.let { throw it }
                ?: createResponse
                ?: error("not used by CookbookRepositoryTest")

        override suspend fun updateCookbook(
            itemId: String,
            request: CreateCookbookDto,
        ): CookbookDto =
            mutationFailure?.let { throw it }
                ?: updateResponse
                ?: error("not used by CookbookRepositoryTest")

        override suspend fun deleteCookbook(itemId: String): ResponseBody {
            mutationFailure?.let { throw it }
            return "".toResponseBody()
        }

        override suspend fun getCookbooks(page: Int, perPage: Int): PagedResponseDto<CookbookDto> {
            requestCount++
            failure?.let { throw it }
            return PagedResponseDto(1, cookbooks.size, cookbooks.size, 1, cookbooks)
        }
    }

    private class FakeRecipesApi(
        private val byCookbook: Map<String, List<RecipeSummaryDto>> = emptyMap()
    ) : RecipesApi {
        private val cookbookRequestCounts = mutableMapOf<String, Int>()

        fun requestCount(cookbookId: String): Int = cookbookRequestCounts[cookbookId] ?: 0

        override suspend fun createRecipe(request: CreateRecipeDto): ResponseBody =
            error("not used by CookbookRepositoryTest")

        override suspend fun parseRecipeUrl(request: ScrapeRecipeDto): ResponseBody =
            error("not used by CookbookRepositoryTest")

        override suspend fun updateRecipe(slug: String, request: RecipeInputDto): ResponseBody =
            error("not used by CookbookRepositoryTest")

        override suspend fun patchRecipe(slug: String, request: RecipeInputDto): ResponseBody =
            error("not used by CookbookRepositoryTest")

        override suspend fun updateRecipeImage(
            slug: String,
            image: okhttp3.MultipartBody.Part,
            extension: okhttp3.RequestBody,
        ): ResponseBody = error("not used by CookbookRepositoryTest")

        override suspend fun deleteRecipeImage(slug: String): ResponseBody =
            error("not used by CookbookRepositoryTest")

        override suspend fun deleteRecipe(slug: String): ResponseBody =
            error("not used by CookbookRepositoryTest")

        override suspend fun uploadRecipeAsset(
            slug: String,
            name: okhttp3.RequestBody,
            icon: okhttp3.RequestBody,
            extension: okhttp3.RequestBody,
            file: okhttp3.MultipartBody.Part,
        ): dev.pschmitt.syncwich.data.api.dto.RecipeAssetDto =
            error("not used by CookbookRepositoryTest")

        override suspend fun getRecipes(
            page: Int,
            perPage: Int,
        ): PagedResponseDto<RecipeSummaryDto> = error("not used by CookbookRepository")

        override suspend fun getRecipeDetailRaw(slug: String): ResponseBody =
            error("not used by CookbookRepository")

        override suspend fun getRecipesByCookbook(
            cookbookId: String,
            page: Int,
            perPage: Int,
        ): PagedResponseDto<RecipeSummaryDto> {
            cookbookRequestCounts[cookbookId] = requestCount(cookbookId) + 1
            val items = byCookbook[cookbookId].orEmpty()
            return PagedResponseDto(1, items.size, items.size, 1, items)
        }
    }

    private companion object {
        fun recipe(id: String) =
            RecipeSummaryEntity(
                id = id,
                slug = id,
                name = id,
                description = "",
                image = null,
                rating = null,
                prepTime = null,
                totalTime = null,
                dateAdded = null,
                lastMade = null,
            )
    }
}

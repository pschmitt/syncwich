package dev.pschmitt.syncwich.sync

import dev.pschmitt.syncwich.data.repository.CategoryRepository
import dev.pschmitt.syncwich.data.repository.CookbookRepository
import dev.pschmitt.syncwich.data.repository.MealPlanRepository
import dev.pschmitt.syncwich.data.repository.RecipeRepository
import dev.pschmitt.syncwich.data.repository.ShoppingListRepository
import dev.pschmitt.syncwich.data.repository.TagRepository
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first

enum class InitialSyncStage {
    Recipes,
    Categories,
    Tags,
    ShoppingLists,
    Cookbooks,
    MealPlan,
}

data class InitialSyncProgress(
    val stage: InitialSyncStage,
    val stageNumber: Int,
    val totalStages: Int,
    val itemCount: Int? = null,
    val completed: Boolean = false,
)

class InitialSyncException(val stage: InitialSyncStage, cause: Throwable) : Exception(cause)

interface InitialSyncDataSource {
    suspend fun refresh(stage: InitialSyncStage): Result<Int>
}

/** Runs the first cache fill in a cancellable, observable sequence of entity-type stages. */
@Singleton
class InitialSyncRunner @Inject constructor(private val dataSource: InitialSyncDataSource) {

    suspend fun run(onProgress: suspend (InitialSyncProgress) -> Unit): Result<Unit> {
        val stages = InitialSyncStage.entries
        for ((index, stage) in stages.withIndex()) {
            coroutineContext.ensureActive()
            val stageNumber = index + 1
            onProgress(
                InitialSyncProgress(
                    stage = stage,
                    stageNumber = stageNumber,
                    totalStages = stages.size,
                )
            )

            val result = dataSource.refresh(stage)
            coroutineContext.ensureActive()
            val itemCount = result.exceptionOrNull()?.let {
                return Result.failure(InitialSyncException(stage, it))
            } ?: result.getOrThrow()
            onProgress(
                InitialSyncProgress(
                    stage = stage,
                    stageNumber = stageNumber,
                    totalStages = stages.size,
                    itemCount = itemCount,
                    completed = true,
                )
            )
        }
        return Result.success(Unit)
    }
}

/** Adapts the existing cache-first repositories to the initial-sync stage contract. */
@Singleton
class RepositoryInitialSyncDataSource
@Inject
constructor(
    private val recipeRepository: RecipeRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val cookbookRepository: CookbookRepository,
    private val mealPlanRepository: MealPlanRepository,
) : InitialSyncDataSource {

    override suspend fun refresh(stage: InitialSyncStage): Result<Int> =
        when (stage) {
            InitialSyncStage.Recipes ->
                refreshAndCount(
                    refresh = { recipeRepository.refreshRecipes(forceRefresh = true) },
                    count = { recipeRepository.observeRecipes().first().size },
                )
            InitialSyncStage.Categories ->
                refreshAndCount(
                    refresh = { categoryRepository.refreshCategories() },
                    count = { categoryRepository.observeCategories().first().size },
                )
            InitialSyncStage.Tags ->
                refreshAndCount(
                    refresh = { tagRepository.refreshTags() },
                    count = { tagRepository.observeTags().first().size },
                )
            InitialSyncStage.ShoppingLists ->
                refreshAndCount(
                    refresh = { shoppingListRepository.refreshLists() },
                    count = { shoppingListRepository.observeLists().first().size },
                )
            InitialSyncStage.Cookbooks ->
                refreshAndCount(
                    refresh = { cookbookRepository.refreshCookbooks(forceRefresh = true) },
                    count = { cookbookRepository.observeCookbooks().first().size },
                )
            InitialSyncStage.MealPlan -> {
                val start = LocalDate.now().with(DayOfWeek.MONDAY)
                val end = start.plusWeeks(3).minusDays(1)
                refreshAndCount(
                    refresh = { mealPlanRepository.refreshMealPlan(start, end) },
                    count = { mealPlanRepository.observeMealPlan(start, end).first().size },
                )
            }
        }

    private suspend fun refreshAndCount(
        refresh: suspend () -> Result<Unit>,
        count: suspend () -> Int,
    ): Result<Int> {
        val refreshResult = refresh()
        refreshResult.exceptionOrNull()?.let { return Result.failure(it) }
        // Do not wrap this in runCatching: cancellation must propagate out of a foreground sync.
        return Result.success(count())
    }
}

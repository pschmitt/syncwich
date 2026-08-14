package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.db.dao.RecipeStepProgressDao
import dev.pschmitt.syncwich.data.db.entity.RecipeStepProgressEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RecipeStepProgressRepository @Inject constructor(private val dao: RecipeStepProgressDao) {
    fun observeCompleted(recipeId: String): Flow<Set<Int>> =
        dao.observeCompleted(recipeId).map { rows -> rows.mapTo(mutableSetOf()) { it.stepIndex } }

    suspend fun setCompleted(recipeId: String, stepIndex: Int, completed: Boolean) {
        if (completed) {
            dao.upsert(
                RecipeStepProgressEntity(
                    recipeId = recipeId,
                    stepIndex = stepIndex,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        } else {
            dao.delete(recipeId, stepIndex)
        }
    }
}

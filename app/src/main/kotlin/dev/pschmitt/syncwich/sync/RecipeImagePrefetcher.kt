package dev.pschmitt.syncwich.sync

import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pschmitt.syncwich.data.image.ImagePrefetchStats
import dev.pschmitt.syncwich.data.image.prefetchImageUrls
import dev.pschmitt.syncwich.data.repository.RecipeRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber

/** Prefetches only URLs discoverable from Room, writing them through Coil's bounded disk cache. */
@Singleton
class RecipeImagePrefetcher
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val recipeRepository: RecipeRepository,
    private val json: Json,
) {

    suspend fun prefetchCachedRecipeImages(serverUrl: String): ImagePrefetchStats =
        withContext(Dispatchers.IO) {
            val urls = recipeRepository.cachedRecipeImagePrefetchUrls(serverUrl, json)
            val stats =
                prefetchImageUrls(urls) { url ->
                    val result =
                        imageLoader.execute(
                            ImageRequest.Builder(context)
                                .data(url)
                                .memoryCachePolicy(CachePolicy.DISABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build()
                        )
                    when (result) {
                        is SuccessResult -> true
                        is ErrorResult -> {
                            Timber.w(result.throwable, "Recipe image prefetch failed for $url")
                            false
                        }
                    }
                }
            Timber.d(
                "Recipe image prefetch: ${stats.succeeded}/${stats.attempted} succeeded" +
                    " (${stats.failed} failed)"
            )
            stats
        }

    private val imageLoader: ImageLoader
        get() = SingletonImageLoader.get(context)
}

package dev.pschmitt.syncwich.data.image

import dev.pschmitt.syncwich.data.api.dto.RecipeDetailDto
import dev.pschmitt.syncwich.data.api.recipeImageUrl
import dev.pschmitt.syncwich.data.db.entity.RecipeDetailEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json

private const val MAX_MARKDOWN_IMAGE_URL_LENGTH = 2_048

/** Extracts the conservative subset of Markdown image destinations that we can prefetch safely. */
fun extractMarkdownImageUrls(markdown: String): List<String> {
    val urls = linkedSetOf<String>()
    MARKDOWN_IMAGE.findAll(markdown).forEach { match ->
        val candidate = match.groups[1]?.value ?: match.groups[2]?.value
        if (candidate != null && isSafeRecipeImageUrl(candidate)) urls += candidate
    }
    return urls.toList()
}

/**
 * Selects image URLs from the cache only. Covers are included for every cached summary; inline
 * images are deliberately capped because recipe Markdown is user-controlled and can contain an
 * arbitrary number of image destinations.
 *
 * Invalid detail JSON is ignored so a malformed cached record cannot block the rest of sync.
 */
fun selectRecipeImagePrefetchUrls(
    serverUrl: String,
    recipes: List<RecipeSummaryEntity>,
    details: List<RecipeDetailEntity>,
    json: Json,
    maxInlineImagesPerRecipe: Int = DEFAULT_MAX_INLINE_IMAGES_PER_RECIPE,
    maxInlineImages: Int = DEFAULT_MAX_INLINE_IMAGES,
): List<String> {
    if (serverUrl.isBlank() || maxInlineImagesPerRecipe <= 0 || maxInlineImages <= 0) {
        return recipes.mapNotNull { recipeImageUrl(serverUrl, it.id, it.image) }
    }

    val urls = linkedSetOf<String>()
    recipes.forEach { recipeImageUrl(serverUrl, it.id, it.image)?.let(urls::add) }

    var inlineImageCount = 0
    details.forEach { detail ->
        if (inlineImageCount >= maxInlineImages) return@forEach

        val recipe =
            runCatching { json.decodeFromString<RecipeDetailDto>(detail.detailJson) }
                .getOrNull() ?: return@forEach

        val remainingForRecipe =
            minOf(maxInlineImagesPerRecipe, maxInlineImages - inlineImageCount)
        val recipeUrls =
            recipe.recipeInstructions
                .asSequence()
                .flatMap { extractMarkdownImageUrls(it.text).asSequence() }
                .distinct()
                .take(remainingForRecipe)
                .toList()
        recipeUrls.forEach { urls += it }
        inlineImageCount += recipeUrls.size
    }
    return urls.toList()
}

/** Outcome of one bounded prefetch pass. Individual image failures are intentionally non-fatal. */
data class ImagePrefetchStats(val attempted: Int, val succeeded: Int, val failed: Int)

/** Runs image work with bounded concurrency and lets later URLs proceed after one failure. */
suspend fun prefetchImageUrls(
    urls: List<String>,
    maxConcurrent: Int = DEFAULT_MAX_CONCURRENT_PREFETCHES,
    prefetch: suspend (String) -> Boolean,
): ImagePrefetchStats {
    require(maxConcurrent > 0) { "maxConcurrent must be positive" }

    val outcomes =
        urls.distinct().chunked(maxConcurrent).flatMap { batch ->
            coroutineScope {
                batch.map { url ->
                    async {
                        try {
                            prefetch(url)
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (_: Exception) {
                            false
                        }
                    }
                }.awaitAll()
            }
        }
    val succeeded = outcomes.count { it }
    return ImagePrefetchStats(
        attempted = outcomes.size,
        succeeded = succeeded,
        failed = outcomes.size - succeeded,
    )
}

/** Returns whether a Markdown image destination is safe to load as a remote recipe image. */
fun isSafeRecipeImageUrl(candidate: String): Boolean {
    if (candidate.length > MAX_MARKDOWN_IMAGE_URL_LENGTH) return false
    val uri = runCatching { URI(candidate) }.getOrNull() ?: return false
    return uri.scheme?.lowercase() in setOf("http", "https") &&
        !uri.host.isNullOrBlank() &&
        uri.userInfo == null
}

private val MARKDOWN_IMAGE =
    Regex(
        """!\[[^\]\r\n]*\]\(\s*(?:<([^>\r\n]+)>|([^\s)\"']+))(?:(?:\s+)(?:\"[^\"\r\n]*\"|'[^'\r\n]*'|\([^\)\r\n]*\)))?\s*\)"""
    )

const val DEFAULT_MAX_INLINE_IMAGES_PER_RECIPE = 8
const val DEFAULT_MAX_INLINE_IMAGES = 256
const val DEFAULT_MAX_CONCURRENT_PREFETCHES = 4

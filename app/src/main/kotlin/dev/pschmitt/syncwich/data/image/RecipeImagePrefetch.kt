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

private const val MAX_RECIPE_IMAGE_URL_LENGTH = 2_048

data class RecipeImageReference(val url: String, val altText: String? = null)

/** Extracts the conservative subset of Markdown image destinations that we can prefetch safely. */
fun extractMarkdownImageUrls(markdown: String): List<String> {
    val urls = linkedSetOf<String>()
    markdownImageCandidates(markdown).forEach { (candidate, _) ->
        if (isSafeRecipeImageUrl(candidate)) urls += candidate
    }
    return urls.toList()
}

/**
 * Extracts Markdown and HTML image elements from recipe content and resolves relative Mealie media
 * paths against the configured server. Relative and protocol-relative destinations are accepted
 * only when they resolve to that same server; absolute HTTP(S) destinations retain the existing
 * safe-image policy.
 */
fun extractRecipeImageReferences(content: String, serverUrl: String): List<RecipeImageReference> {
    if (serverUrl.isBlank()) return emptyList()

    val references = linkedMapOf<String, RecipeImageReference>()
    markdownImageCandidates(content).forEach { (candidate, altText) ->
        resolveRecipeImageUrl(serverUrl, candidate)?.let { url ->
            references.putIfAbsent(url, RecipeImageReference(url, altText))
        }
    }
    HTML_IMAGE.findAll(content).forEach { imageMatch ->
        val attributes = imageMatch.groupValues[1]
        val srcMatch =
            HTML_ATTRIBUTE.findAll(attributes).firstOrNull { it.groupValues[1].equals("src", true) }
        val candidate = srcMatch?.let(::attributeValue) ?: return@forEach
        val altText =
            HTML_ATTRIBUTE.findAll(attributes)
                .firstOrNull { it.groupValues[1].equals("alt", true) }
                ?.let(::attributeValue)
                ?.trim()
                ?.take(MAX_ALT_TEXT_LENGTH)
                ?.takeIf(String::isNotBlank)
        resolveRecipeImageUrl(serverUrl, candidate)?.let { url ->
            references.putIfAbsent(url, RecipeImageReference(url, altText))
        }
    }
    return references.values.toList()
}

/** Resolves a recipe image destination while rejecting malformed or cross-server relative URLs. */
fun resolveRecipeImageUrl(serverUrl: String, candidate: String): String? {
    val trimmedCandidate = decodeHtmlAttribute(candidate).trim()
    if (trimmedCandidate.isEmpty() || trimmedCandidate.length > MAX_RECIPE_IMAGE_URL_LENGTH) {
        return null
    }
    val base =
        runCatching { URI(serverUrl.trimEnd('/') + "/") }
            .getOrNull()
            ?.takeIf {
                it.scheme?.lowercase() in setOf("http", "https") && !it.host.isNullOrBlank()
            } ?: return null
    val candidateUri = runCatching { URI(trimmedCandidate) }.getOrNull() ?: return null
    val resolved = runCatching { base.resolve(candidateUri) }.getOrNull() ?: return null
    if (!isSafeRecipeImageUrl(resolved.toString())) return null
    if (
        !candidateUri.isAbsolute && candidateUri.rawAuthority != null && !sameOrigin(base, resolved)
    ) {
        return null
    }
    return resolved.toString()
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
            runCatching { json.decodeFromString<RecipeDetailDto>(detail.detailJson) }.getOrNull()
                ?: return@forEach

        val remainingForRecipe = minOf(maxInlineImagesPerRecipe, maxInlineImages - inlineImageCount)
        val recipeUrls =
            recipe.recipeInstructions
                .asSequence()
                .flatMap {
                    extractRecipeImageReferences(it.text, serverUrl)
                        .asSequence()
                        .map(RecipeImageReference::url)
                }
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
                batch
                    .map { url ->
                        async {
                            try {
                                prefetch(url)
                            } catch (cancellation: CancellationException) {
                                throw cancellation
                            } catch (_: Exception) {
                                false
                            }
                        }
                    }
                    .awaitAll()
            }
        }
    val succeeded = outcomes.count { it }
    return ImagePrefetchStats(
        attempted = outcomes.size,
        succeeded = succeeded,
        failed = outcomes.size - succeeded,
    )
}

/** Returns whether an image destination is safe to load as a remote recipe image. */
fun isSafeRecipeImageUrl(candidate: String): Boolean {
    if (candidate.length > MAX_RECIPE_IMAGE_URL_LENGTH) return false
    val uri = runCatching { URI(candidate) }.getOrNull() ?: return false
    return uri.scheme?.lowercase() in setOf("http", "https") &&
        !uri.host.isNullOrBlank() &&
        uri.userInfo == null
}

private fun sameOrigin(first: URI, second: URI): Boolean =
    first.scheme.equals(second.scheme, ignoreCase = true) &&
        first.host.equals(second.host, ignoreCase = true) &&
        effectivePort(first) == effectivePort(second)

private fun effectivePort(uri: URI): Int =
    if (uri.port != -1) uri.port else if (uri.scheme.equals("https", true)) 443 else 80

private fun markdownImageCandidates(markdown: String): List<Pair<String, String?>> =
    MARKDOWN_IMAGE.findAll(markdown)
        .mapNotNull { match ->
            val candidate =
                match.groups[2]?.value ?: match.groups[3]?.value ?: return@mapNotNull null
            val altText = match.groups[1]?.value?.trim()?.take(MAX_ALT_TEXT_LENGTH)
            candidate to altText?.takeIf(String::isNotBlank)
        }
        .toList()

private fun attributeValue(match: MatchResult): String =
    match.groupValues.drop(2).firstOrNull(String::isNotEmpty).orEmpty()

private fun decodeHtmlAttribute(value: String): String =
    value
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)

private val MARKDOWN_IMAGE =
    Regex(
        """!\[([^\]\r\n]*)\]\(\s*(?:<([^>\r\n]+)>|([^\s)\"']+))(?:(?:\s+)(?:\"[^\"\r\n]*\"|'[^'\r\n]*'|\([^\)\r\n]*\)))?\s*\)"""
    )

private val HTML_IMAGE = Regex("""<img\b([^>]*)>""", setOf(RegexOption.IGNORE_CASE))

private val HTML_ATTRIBUTE =
    Regex("""\b([a-zA-Z_:][a-zA-Z0-9_.:-]*)\s*=\s*(?:\"([^\"]*)\"|'([^']*)'|([^\s>]+))""")

const val DEFAULT_MAX_INLINE_IMAGES_PER_RECIPE = 8
const val DEFAULT_MAX_INLINE_IMAGES = 256
const val DEFAULT_MAX_CONCURRENT_PREFETCHES = 4

private const val MAX_ALT_TEXT_LENGTH = 240

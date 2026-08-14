package dev.pschmitt.syncwich.data.api

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Builds a recipe's cover image URL - confirmed live against a v3.22.0 Mealie instance (SW-3
 * verification): `GET {serverUrl}/api/media/recipes/{recipeId}/images/min-original.webp`, served
 * with no auth required on the verification instance, though Coil's request still carries whatever
 * `Authorization` header the app's OkHttpClient adds since a locked-down deployment may require it.
 * The Mealie image version is included as `?v=...` in the returned URL so it also becomes Coil's
 * cache key.
 *
 * Mealie's `image` field is normally a short cache-busting version, but some imported recipes
 * expose the literal string `"no image"` even though the media endpoint still serves a real
 * uploaded cover. Only a null/blank field is treated as absent; skipping the sentinel caused
 * imported covers such as `bananengemuse` to disappear without even making the valid media call.
 */
fun recipeImageUrl(serverUrl: String, recipeId: String, image: String?): String? {
    val imageVersion = image?.trim()
    if (imageVersion.isNullOrBlank()) return null

    val baseUrl = serverUrl.trimEnd('/').toHttpUrlOrNull() ?: return null
    val explicitImageUrl = imageVersion.toHttpUrlOrNull()
    if (explicitImageUrl != null) {
        return explicitImageUrl
            .takeIf {
                it.scheme == baseUrl.scheme && it.host == baseUrl.host && it.port == baseUrl.port
            }
            ?.toString()
    }
    val imageFileName =
        imageVersion.takeIf {
            it == "original.webp" || it == "min-original.webp" || it == "tiny-original.webp"
        } ?: "min-original.webp"
    return baseUrl
        .newBuilder()
        .addPathSegments("api/media/recipes/$recipeId/images/$imageFileName")
        // Mealie changes this value when the cover changes. Including it in Coil's model gives
        // each server-side image version its own memory/disk-cache key while repeated renders of
        // the same version still reuse the exact same cached entry.
        .addQueryParameter("v", imageVersion)
        .build()
        .toString()
}

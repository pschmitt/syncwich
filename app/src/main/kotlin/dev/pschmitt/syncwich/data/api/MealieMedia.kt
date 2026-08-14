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
 * Mealie's `image` field on both the recipe list and detail responses is `null` *or* the literal
 * string `"no image"` when a recipe has no cover - never a URL fragment itself, just a
 * cache-busting marker that a real image exists. Both cases mean "don't render anything".
 */
fun recipeImageUrl(serverUrl: String, recipeId: String, image: String?): String? {
    val imageVersion = image?.trim()
    if (imageVersion.isNullOrBlank() || imageVersion == "no image") return null

    val baseUrl = serverUrl.trimEnd('/').toHttpUrlOrNull() ?: return null
    return baseUrl
        .newBuilder()
        .addPathSegments("api/media/recipes/$recipeId/images/min-original.webp")
        // Mealie changes this value when the cover changes. Including it in Coil's model gives
        // each server-side image version its own memory/disk-cache key while repeated renders of
        // the same version still reuse the exact same cached entry.
        .addQueryParameter("v", imageVersion)
        .build()
        .toString()
}

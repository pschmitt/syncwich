package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.settings.SettingsRepository
import javax.inject.Inject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Retrofit is built once against a placeholder host; this interceptor rewrites those Retrofit
 * requests to the user's currently configured Mealie server so changing it in Settings doesn't
 * require rebuilding the whole Retrofit/OkHttp stack. Absolute Coil URLs are deliberately left
 * alone: rewriting them used to duplicate a path-prefixed server URL and could turn valid media
 * requests into 404s.
 */
class DynamicBaseUrlInterceptor
@Inject
constructor(private val settingsRepository: SettingsRepository) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val configured =
            settingsRepository.credentials.value.serverUrl.toHttpUrlOrNull()
                ?: return chain.proceed(request)

        val rewrittenUrl = rewriteDynamicBaseUrl(request.url, configured)
        return chain.proceed(request.newBuilder().url(rewrittenUrl).build())
    }
}

internal const val MEALIE_PLACEHOLDER_HOST = "mealie.invalid"

internal fun rewriteDynamicBaseUrl(requestUrl: okhttp3.HttpUrl, configured: okhttp3.HttpUrl) =
    if (requestUrl.host != MEALIE_PLACEHOLDER_HOST) {
        requestUrl
    } else {
        requestUrl
            .newBuilder()
            .scheme(configured.scheme)
            .host(configured.host)
            .port(configured.port)
            .encodedPath(joinServerPath(configured.encodedPath, requestUrl.encodedPath))
            .build()
    }

private fun joinServerPath(serverPath: String, requestPath: String): String {
    val base = serverPath.trimEnd('/')
    val suffix = "/${requestPath.trimStart('/')}"
    return if (base.isEmpty()) suffix else base + suffix
}

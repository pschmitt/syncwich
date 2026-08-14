package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.settings.SettingsRepository
import javax.inject.Inject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Retrofit is built once against a placeholder host; this interceptor rewrites every request to the
 * user's currently configured Mealie server so changing it in Settings doesn't require rebuilding
 * the whole Retrofit/OkHttp stack. Same technique as the sibling nyetbox app's
 * `DynamicBaseUrlInterceptor`.
 */
class DynamicBaseUrlInterceptor
@Inject
constructor(private val settingsRepository: SettingsRepository) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val configured =
            settingsRepository.credentials.value.serverUrl.toHttpUrlOrNull()
                ?: return chain.proceed(request)

        val rewrittenUrl =
            request.url
                .newBuilder()
                .scheme(configured.scheme)
                .host(configured.host)
                .port(configured.port)
                .encodedPath(configured.encodedPath.trimEnd('/') + request.url.encodedPath)
                .build()
        return chain.proceed(request.newBuilder().url(rewrittenUrl).build())
    }
}

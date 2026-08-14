package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.settings.SettingsRepository
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/** Adds `Authorization: Bearer <token>` from the currently stored Mealie API token, if any. */
class AuthInterceptor @Inject constructor(private val settingsRepository: SettingsRepository) :
    Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = settingsRepository.credentials.value.apiToken
        val request = chain.request()
        val authorized =
            if (token.isBlank()) request
            else request.newBuilder().header("Authorization", "Bearer $token").build()
        return chain.proceed(authorized)
    }
}

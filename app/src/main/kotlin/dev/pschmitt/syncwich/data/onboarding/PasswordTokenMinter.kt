package dev.pschmitt.syncwich.data.onboarding

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dev.pschmitt.syncwich.data.api.AuthApi
import dev.pschmitt.syncwich.data.api.dto.LongLiveTokenRequestDto
import dev.pschmitt.syncwich.di.ValidationClient
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit

/**
 * The username/password onboarding path: exchanges a username/password for a short-lived JWT via
 * `/api/auth/token`, then immediately uses that JWT to mint a real long-lived Mealie API token via
 * `/api/users/api-tokens` - that token is the only thing ever returned to the caller (and, from
 * there, the only thing [dev.pschmitt.syncwich.data.settings.SettingsRepository] persists). The
 * password and JWT live only as local `val`s for the duration of this one request chain; neither is
 * ever logged (the [ValidationClient] OkHttpClient's `HttpLoggingInterceptor` redacts the
 * `Authorization` header, and request bodies aren't logged in release builds) or written to disk.
 *
 * Builds its own per-call [Retrofit]/[AuthApi] against the *entered* (not-yet-saved) server URL,
 * for the same reason [OnboardingValidator] builds its own requests rather than reusing the app's
 * normal Retrofit stack: that stack's interceptors read the currently *saved* connection, which
 * during onboarding is exactly what's being set up.
 */
@Singleton
class PasswordTokenMinter
@Inject
constructor(@ValidationClient private val client: OkHttpClient, private val json: Json) {

    suspend fun mintToken(
        serverUrl: String,
        username: String,
        password: String,
        tokenName: String,
    ): Result<String> {
        val baseUrl =
            serverUrl.trim().trimEnd('/').toHttpUrlOrNull()?.let { url ->
                // Retrofit requires a directory-style base URL, while users naturally enter
                // `https://mealie.example.com` without a trailing slash.
                url.newBuilder().encodedPath(url.encodedPath.trimEnd('/') + "/").build()
            } ?: return Result.failure(OnboardingValidationException(OnboardingError.MalformedUrl))

        val api = authApi(baseUrl)

        return try {
            withContext(Dispatchers.IO) {
                val accessToken =
                    try {
                        api.login(username.trim(), password).accessToken
                    } catch (e: HttpException) {
                        throw OnboardingValidationException(e.toLoginError(), e)
                    }
                val token =
                    try {
                        api.createApiToken(
                                "Bearer $accessToken",
                                LongLiveTokenRequestDto(
                                    name = tokenName,
                                    integrationId = "generic",
                                ),
                            )
                            .token
                    } catch (e: HttpException) {
                        throw OnboardingValidationException(
                            OnboardingError.ServerError(e.code()),
                            e,
                        )
                    }
                Result.success(token)
            }
        } catch (e: OnboardingValidationException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(OnboardingValidationException(OnboardingError.Unreachable, e))
        }
    }

    private fun authApi(baseUrl: HttpUrl): AuthApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApi::class.java)

    private fun HttpException.toLoginError(): OnboardingError =
        if (code() == 401 || code() == 403) OnboardingError.Unauthorized
        else OnboardingError.ServerError(code())
}

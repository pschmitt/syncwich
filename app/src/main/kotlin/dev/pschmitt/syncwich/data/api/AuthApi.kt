package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.api.dto.LongLiveTokenRequestDto
import dev.pschmitt.syncwich.data.api.dto.LongLiveTokenResponseDto
import dev.pschmitt.syncwich.data.api.dto.PasswordLoginResponseDto
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Password-login (`/api/auth/token`) and long-lived-token-minting (`/api/users/api-tokens`)
 * endpoints - only ever called against a fresh, unsaved Retrofit instance built at onboarding time
 * (see `PasswordTokenMinter`), never through the app's normal authenticated Retrofit stack, since
 * the JWT `login` returns expires in ~48h and is never persisted (see AGENTS.md).
 */
interface AuthApi {
    @FormUrlEncoded
    @POST("api/auth/token")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
    ): PasswordLoginResponseDto

    @POST("api/users/api-tokens")
    suspend fun createApiToken(
        @Header("Authorization") bearerToken: String,
        @Body request: LongLiveTokenRequestDto,
    ): LongLiveTokenResponseDto
}

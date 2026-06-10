package com.sac.acessibilidade.spotify.auth

import android.net.Uri
import com.sac.acessibilidade.spotify.auth.model.SpotifyTokenResponse
import com.sac.acessibilidade.spotify.auth.model.SpotifyTokens
import com.sac.acessibilidade.spotify.di.UnauthenticatedOkHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyAuthRepositoryImpl
    @Inject
    constructor(
        private val tokenStore: SpotifyTokenStore,
        @UnauthenticatedOkHttpClient private val okHttpClient: OkHttpClient,
        private val json: Json,
    ) : SpotifyAuthRepository {
        private val _authCodeFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
        override val authCodeFlow: SharedFlow<String> = _authCodeFlow.asSharedFlow()

        override fun buildAuthUri(
            clientId: String,
            redirectUri: String,
        ): Uri {
            val verifier = PkceUtils.generateVerifier()
            tokenStore.savePkceVerifier(verifier)
            val challenge = PkceUtils.generateChallenge(verifier)
            val state = generateState()
            tokenStore.saveOAuthState(state)
            return Uri.parse(SpotifyAuthConstants.AUTH_URL).buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("redirect_uri", redirectUri)
                .appendQueryParameter("code_challenge_method", "S256")
                .appendQueryParameter("code_challenge", challenge)
                .appendQueryParameter("scope", SpotifyAuthConstants.SCOPES.joinToString(" "))
                .appendQueryParameter("state", state)
                .build()
        }

        override suspend fun exchangeCode(
            code: String,
            clientId: String,
            redirectUri: String,
        ): Result<SpotifyTokens> =
            withContext(Dispatchers.IO) {
                val verifier =
                    tokenStore.getPkceVerifier()
                        ?: return@withContext Result.failure(IllegalStateException("PKCE verifier ausente"))

                val body =
                    FormBody.Builder()
                        .add("grant_type", "authorization_code")
                        .add("code", code)
                        .add("redirect_uri", redirectUri)
                        .add("client_id", clientId)
                        .add("code_verifier", verifier)
                        .build()

                executeTokenRequest(body).also { if (it.isSuccess) tokenStore.clearPkceVerifier() }
            }

        override suspend fun refreshAccessToken(clientId: String): Result<SpotifyTokens> =
            withContext(Dispatchers.IO) {
                val refreshToken =
                    tokenStore.getRefreshToken()
                        ?: return@withContext Result.failure(IllegalStateException("Refresh token ausente"))

                val body =
                    FormBody.Builder()
                        .add("grant_type", "refresh_token")
                        .add("refresh_token", refreshToken)
                        .add("client_id", clientId)
                        .build()

                executeTokenRequest(body)
            }

        override fun notifyAuthCode(code: String) {
            _authCodeFlow.tryEmit(code)
        }

        override fun isLoggedIn(): Boolean = tokenStore.isTokenValid()

        private fun executeTokenRequest(body: FormBody): Result<SpotifyTokens> {
            val request =
                Request.Builder()
                    .url(SpotifyAuthConstants.TOKEN_URL)
                    .post(body)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .build()

            return runCatching {
                val response = okHttpClient.newCall(request).execute()
                val responseBody =
                    response.body?.string()
                        ?: throw IOException("Resposta vazia do servidor")
                if (!response.isSuccessful) {
                    throw IOException("Falha na troca de token: HTTP ${response.code}")
                }
                val tokenResponse = json.decodeFromString<SpotifyTokenResponse>(responseBody)
                val tokens =
                    SpotifyTokens(
                        accessToken = tokenResponse.accessToken,
                        refreshToken =
                            tokenResponse.refreshToken
                                ?: tokenStore.getRefreshToken().orEmpty(),
                        expiresAt = System.currentTimeMillis() + tokenResponse.expiresIn * 1_000L,
                    )
                tokenStore.saveTokens(tokens)
                tokens
            }
        }

        override fun verifyAndConsumeState(receivedState: String): Boolean {
            val saved = tokenStore.consumeOAuthState() ?: return false
            return saved == receivedState
        }

        private fun generateState(): String {
            val bytes = ByteArray(16)
            SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

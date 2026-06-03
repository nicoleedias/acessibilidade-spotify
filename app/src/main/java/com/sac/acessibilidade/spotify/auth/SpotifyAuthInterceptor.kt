package com.sac.acessibilidade.spotify.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SpotifyAuthInterceptor
    @Inject
    constructor(
        private val tokenStore: SpotifyTokenStore,
        private val authRepository: SpotifyAuthRepository,
        @Named("spotify_client_id") private val clientId: String,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val token = tokenStore.getAccessToken() ?: throw IOException("Não autenticado")
            val response = chain.proceed(withBearer(chain.request(), token))

            if (response.code == 401) {
                response.close()
                val newToken =
                    runBlocking {
                        authRepository.refreshAccessToken(clientId).getOrThrow().accessToken
                    }
                return chain.proceed(withBearer(chain.request(), newToken))
            }

            if (response.code == 429) {
                response.close()
                val retryAfterMs = (response.header("Retry-After")?.toLongOrNull() ?: 1L) * 1_000L
                Thread.sleep(retryAfterMs)
                return chain.proceed(withBearer(chain.request(), token))
            }

            return response
        }

        private fun withBearer(
            request: Request,
            token: String,
        ): Request = request.newBuilder().header("Authorization", "Bearer $token").build()
    }

package com.sac.acessibilidade.spotify.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyCommandRepositoryImpl
    @Inject
    constructor(
        private val api: SpotifyPlayerApi,
    ) : SpotifyCommandRepository {
        override suspend fun play(): Result<Unit> = command { api.play() }

        override suspend fun pause(): Result<Unit> = command { api.pause() }

        override suspend fun skipToNext(): Result<Unit> = command { api.skipToNext() }

        override suspend fun skipToPrevious(): Result<Unit> = command { api.skipToPrevious() }

        override suspend fun setVolume(percent: Int): Result<Unit> = command { api.setVolume(percent.coerceIn(0, 100)) }

        private suspend fun command(block: suspend () -> Response<Unit>): Result<Unit> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val response = block()
                    when (response.code()) {
                        204 -> Unit
                        403 -> throw IOException("Spotify Premium necessário para controle de reprodução")
                        404 -> throw IOException("Nenhum dispositivo ativo. Abra o Spotify no celular primeiro.")
                        else -> if (!response.isSuccessful) throw IOException("HTTP ${response.code()}")
                    }
                }
            }
    }

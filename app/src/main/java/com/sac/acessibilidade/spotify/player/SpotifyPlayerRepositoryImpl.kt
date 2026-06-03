package com.sac.acessibilidade.spotify.player

import com.sac.acessibilidade.spotify.player.model.CurrentlyPlayingResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyPlayerRepositoryImpl
    @Inject
    constructor(
        private val api: SpotifyPlayerApi,
    ) : SpotifyPlayerRepository {
        override suspend fun getCurrentlyPlaying(): Result<CurrentlyPlayingResponse?> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val response = api.getCurrentlyPlaying()
                    when {
                        response.code() == 204 -> null
                        response.isSuccessful -> response.body()
                        else -> throw IOException("HTTP ${response.code()}")
                    }
                }
            }
    }

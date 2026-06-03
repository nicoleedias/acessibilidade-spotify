package com.sac.acessibilidade.spotify.player

interface SpotifyCommandRepository {
    suspend fun play(): Result<Unit>

    suspend fun pause(): Result<Unit>

    suspend fun skipToNext(): Result<Unit>

    suspend fun skipToPrevious(): Result<Unit>

    suspend fun setVolume(percent: Int): Result<Unit>
}

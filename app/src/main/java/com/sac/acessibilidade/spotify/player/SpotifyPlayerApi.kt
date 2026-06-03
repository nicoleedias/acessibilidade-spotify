package com.sac.acessibilidade.spotify.player

import com.sac.acessibilidade.spotify.player.model.CurrentlyPlayingResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Query

interface SpotifyPlayerApi {
    @GET("me/player/currently-playing")
    suspend fun getCurrentlyPlaying(): Response<CurrentlyPlayingResponse>

    @HTTP(method = "PUT", path = "me/player/play", hasBody = false)
    suspend fun play(): Response<Unit>

    @HTTP(method = "PUT", path = "me/player/pause", hasBody = false)
    suspend fun pause(): Response<Unit>

    @HTTP(method = "POST", path = "me/player/next", hasBody = false)
    suspend fun skipToNext(): Response<Unit>

    @HTTP(method = "POST", path = "me/player/previous", hasBody = false)
    suspend fun skipToPrevious(): Response<Unit>

    @HTTP(method = "PUT", path = "me/player/volume", hasBody = false)
    suspend fun setVolume(
        @Query("volume_percent") volumePercent: Int,
    ): Response<Unit>
}

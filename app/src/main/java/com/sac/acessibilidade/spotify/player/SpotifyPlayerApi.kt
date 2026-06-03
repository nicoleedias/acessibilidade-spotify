package com.sac.acessibilidade.spotify.player

import com.sac.acessibilidade.spotify.player.model.CurrentlyPlayingResponse
import retrofit2.Response
import retrofit2.http.GET

interface SpotifyPlayerApi {
    @GET("me/player/currently-playing")
    suspend fun getCurrentlyPlaying(): Response<CurrentlyPlayingResponse>
}

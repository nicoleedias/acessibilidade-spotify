package com.sac.acessibilidade.spotify.player

import com.sac.acessibilidade.spotify.player.model.CurrentlyPlayingResponse
import com.sac.acessibilidade.spotify.player.model.UserProfileResponse
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface SpotifyPlayerApi {
    @GET("me")
    suspend fun getUserProfile(): Response<UserProfileResponse>

    @GET("me/player/currently-playing")
    suspend fun getCurrentlyPlaying(): Response<CurrentlyPlayingResponse>

    // OkHttp 4 exige body em PUT/POST — passamos body vazio explicitamente
    @PUT("me/player/play")
    suspend fun play(
        @Body body: RequestBody,
    ): Response<Unit>

    @PUT("me/player/pause")
    suspend fun pause(
        @Body body: RequestBody,
    ): Response<Unit>

    @POST("me/player/next")
    suspend fun skipToNext(
        @Body body: RequestBody,
    ): Response<Unit>

    @POST("me/player/previous")
    suspend fun skipToPrevious(
        @Body body: RequestBody,
    ): Response<Unit>

    @PUT("me/player/volume")
    suspend fun setVolume(
        @Query("volume_percent") volumePercent: Int,
        @Body body: RequestBody,
    ): Response<Unit>
}

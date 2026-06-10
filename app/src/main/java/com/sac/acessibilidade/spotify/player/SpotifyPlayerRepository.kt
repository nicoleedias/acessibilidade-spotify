package com.sac.acessibilidade.spotify.player

import com.sac.acessibilidade.spotify.player.model.CurrentlyPlayingResponse
import com.sac.acessibilidade.spotify.player.model.UserProfileResponse

interface SpotifyPlayerRepository {
    suspend fun getCurrentlyPlaying(): Result<CurrentlyPlayingResponse?>

    suspend fun getUserProfile(): Result<UserProfileResponse>
}

package com.sac.acessibilidade.spotify.player

import com.sac.acessibilidade.spotify.player.model.CurrentlyPlayingResponse

interface SpotifyPlayerRepository {
    suspend fun getCurrentlyPlaying(): Result<CurrentlyPlayingResponse?>
}

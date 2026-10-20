package com.sac.acessibilidade.spotify.player

import com.sac.acessibilidade.spotify.player.model.PlaylistItem
import com.sac.acessibilidade.spotify.player.model.PlaylistTrack

/** Leitura da biblioteca do usuário e reprodução a partir dela. */
interface SpotifyLibraryRepository {
    suspend fun getPlaylists(): Result<List<PlaylistItem>>

    suspend fun getPlaylistTracks(playlistId: String): Result<List<PlaylistTrack>>

    /**
     * Toca [contextUri] a partir da faixa em [startIndex].
     * O índice é posicional dentro do contexto, como a API do Spotify espera.
     */
    suspend fun playFromContext(
        contextUri: String,
        startIndex: Int,
    ): Result<Unit>
}

package com.sac.acessibilidade.spotify.player.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Resposta de `GET /me/playlists`. */
@Serializable
data class PlaylistsResponse(
    @SerialName("items") val items: List<PlaylistItem> = emptyList(),
)

@Serializable
data class PlaylistItem(
    @SerialName("id") val id: String,
    @SerialName("uri") val uri: String,
    @SerialName("name") val name: String,
    @SerialName("images") val images: List<AlbumImage> = emptyList(),
    @SerialName("tracks") val tracks: PlaylistTracksRef? = null,
) {
    fun bestImageUrl(targetWidth: Int = 300): String? = images.bestUrl(targetWidth)

    val trackCount: Int get() = tracks?.total ?: 0
}

@Serializable
data class PlaylistTracksRef(
    @SerialName("total") val total: Int = 0,
)

/** Resposta de `GET /playlists/{id}/tracks`. */
@Serializable
data class PlaylistTracksResponse(
    @SerialName("items") val items: List<PlaylistTrackEntry> = emptyList(),
)

/**
 * O Spotify envolve cada faixa num objeto com metadados de quando foi adicionada;
 * `track` vem nulo para itens indisponíveis na região ou removidos do catálogo.
 */
@Serializable
data class PlaylistTrackEntry(
    @SerialName("track") val track: PlaylistTrack? = null,
)

@Serializable
data class PlaylistTrack(
    @SerialName("uri") val uri: String,
    @SerialName("name") val name: String,
    @SerialName("artists") val artists: List<ArtistItem> = emptyList(),
    @SerialName("album") val album: AlbumItem? = null,
) {
    val artistNames: String get() = artists.joinToString(", ") { it.name }
}

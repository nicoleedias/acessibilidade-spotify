package com.sac.acessibilidade.spotify.player.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrentlyPlayingResponse(
    @SerialName("is_playing") val isPlaying: Boolean,
    @SerialName("item") val item: TrackItem? = null,
)

@Serializable
data class TrackItem(
    @SerialName("name") val name: String,
    @SerialName("artists") val artists: List<ArtistItem> = emptyList(),
    @SerialName("album") val album: AlbumItem? = null,
)

@Serializable
data class ArtistItem(
    @SerialName("name") val name: String,
)

@Serializable
data class AlbumItem(
    @SerialName("images") val images: List<AlbumImage> = emptyList(),
) {
    fun bestImageUrl(targetWidth: Int = 300): String? =
        images
            .filter { it.width != null }
            .minByOrNull { kotlin.math.abs((it.width ?: 0) - targetWidth) }
            ?.url
            ?: images.firstOrNull()?.url
}

@Serializable
data class AlbumImage(
    @SerialName("url") val url: String,
    @SerialName("width") val width: Int? = null,
    @SerialName("height") val height: Int? = null,
)

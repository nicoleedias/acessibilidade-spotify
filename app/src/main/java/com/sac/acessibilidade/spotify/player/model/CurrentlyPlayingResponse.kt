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
)

@Serializable
data class ArtistItem(
    @SerialName("name") val name: String,
)

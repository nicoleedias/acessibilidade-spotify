package com.sac.acessibilidade.spotify.player.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponse(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("id") val id: String,
)

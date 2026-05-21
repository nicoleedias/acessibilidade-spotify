package com.sac.acessibilidade.spotify.auth.model

data class SpotifyTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
) {
    fun isExpired(): Boolean = System.currentTimeMillis() >= expiresAt
}

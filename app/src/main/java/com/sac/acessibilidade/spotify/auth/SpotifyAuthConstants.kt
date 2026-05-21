package com.sac.acessibilidade.spotify.auth

internal object SpotifyAuthConstants {
    const val AUTH_URL = "https://accounts.spotify.com/authorize"
    const val TOKEN_URL = "https://accounts.spotify.com/api/token"

    val SCOPES =
        listOf(
            "user-read-playback-state",
            "user-modify-playback-state",
            "user-read-currently-playing",
        )
}

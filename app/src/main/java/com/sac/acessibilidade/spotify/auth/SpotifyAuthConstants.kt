package com.sac.acessibilidade.spotify.auth

internal object SpotifyAuthConstants {
    const val AUTH_URL = "https://accounts.spotify.com/authorize"
    const val TOKEN_URL = "https://accounts.spotify.com/api/token"

    val SCOPES =
        listOf(
            "user-read-playback-state",
            "user-modify-playback-state",
            "user-read-currently-playing",
            // Necessário para listar as playlists da conta na tela de Biblioteca.
            // Alterar esta lista invalida tokens antigos: o usuário precisa entrar de novo.
            "playlist-read-private",
        )
}

package com.sac.acessibilidade.ui.screens

data class HomeUiState(
    val userName: String? = null,
    val nowPlayingTitle: String? = null,
    val nowPlayingArtist: String? = null,
    val isPlaying: Boolean = false,
)

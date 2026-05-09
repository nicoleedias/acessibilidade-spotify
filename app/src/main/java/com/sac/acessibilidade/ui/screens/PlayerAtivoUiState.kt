package com.sac.acessibilidade.ui.screens

data class PlayerAtivoUiState(
    val trackTitle: String = "Nome da Música",
    val trackArtist: String = "Nome do Artista",
    val isPlaying: Boolean = true,
    val lastGestureName: String = "",
    val lastGestureAction: String = "",
    val hasDetectedGesture: Boolean = false,
)

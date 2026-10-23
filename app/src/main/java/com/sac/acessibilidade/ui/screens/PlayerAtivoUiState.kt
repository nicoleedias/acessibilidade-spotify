package com.sac.acessibilidade.ui.screens

data class PlayerAtivoUiState(
    val trackTitle: String = "",
    val trackArtist: String = "",
    val albumArtUrl: String? = null,
    val isPlaying: Boolean = false,
    val volumePercent: Int = 50,
    val isLoading: Boolean = true,
    val error: String? = null,
    val commandError: String? = null,
    val lastGestureName: String = "",
    val lastGestureAction: String = "",
    val hasDetectedGesture: Boolean = false,
    /**
     * Nenhum gesto tem ação associada. O mapeamento começa vazio de propósito,
     * então sem este aviso o usuário abriria o rastreamento, moveria a cabeça e
     * nada aconteceria — sem explicação nenhuma.
     */
    val hasNoMapping: Boolean = false,
)

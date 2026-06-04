package com.sac.acessibilidade.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sac.acessibilidade.domain.gesture.Gesture
import com.sac.acessibilidade.domain.gesture.SpotifyAction
import com.sac.acessibilidade.spotify.player.SpotifyCommandRepository
import com.sac.acessibilidade.spotify.player.SpotifyPlayerRepository
import com.sac.acessibilidade.vision.GestureProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerAtivoViewModel
    @Inject
    constructor(
        private val playerRepository: SpotifyPlayerRepository,
        private val commandRepository: SpotifyCommandRepository,
        val gestureProcessor: GestureProcessor,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PlayerAtivoUiState())
        val uiState: StateFlow<PlayerAtivoUiState> = _uiState.asStateFlow()

        // Mapeamento padrão gesto → ação (UC03: será lido do Room quando implementado)
        private val defaultMapping =
            mapOf(
                Gesture.TILT_HEAD_RIGHT to SpotifyAction.VOLUME_UP,
                Gesture.TILT_HEAD_LEFT to SpotifyAction.VOLUME_DOWN,
                Gesture.TURN_FACE_RIGHT to SpotifyAction.NEXT_TRACK,
                Gesture.TURN_FACE_LEFT to SpotifyAction.PREVIOUS_TRACK,
                Gesture.BLINK_RIGHT_EYE to SpotifyAction.PLAY_PAUSE,
            )

        init {
            startPolling()
            gestureProcessor.initialize()
            collectGestures()
        }

        // ── Gestos ───────────────────────────────────────────────────────────

        private fun collectGestures() {
            viewModelScope.launch {
                gestureProcessor.gestureFlow.collect { gesture ->
                    val action = defaultMapping[gesture] ?: return@collect
                    showGestureFeedback(gesture, action)
                    executeAction(action)
                }
            }
        }

        private fun showGestureFeedback(
            gesture: Gesture,
            action: SpotifyAction,
        ) {
            _uiState.update {
                it.copy(
                    lastGestureName = gesture.displayName(),
                    lastGestureAction = action.displayName(),
                    hasDetectedGesture = true,
                )
            }
            viewModelScope.launch {
                delay(FEEDBACK_DURATION_MS)
                _uiState.update { it.copy(hasDetectedGesture = false) }
            }
        }

        private fun executeAction(action: SpotifyAction) {
            when (action) {
                SpotifyAction.PLAY_PAUSE -> togglePlayPause()
                SpotifyAction.NEXT_TRACK -> skipToNext()
                SpotifyAction.PREVIOUS_TRACK -> skipToPrevious()
                SpotifyAction.VOLUME_UP -> volumeUp()
                SpotifyAction.VOLUME_DOWN -> volumeDown()
            }
        }

        // ── Polling da faixa ─────────────────────────────────────────────────

        private fun startPolling() {
            viewModelScope.launch {
                while (isActive) {
                    fetchCurrentTrack()
                    delay(POLL_INTERVAL_MS)
                }
            }
        }

        private suspend fun fetchCurrentTrack() {
            playerRepository.getCurrentlyPlaying().fold(
                onSuccess = { response ->
                    if (response == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                trackTitle = "Nada tocando",
                                trackArtist = "",
                                albumArtUrl = null,
                                isPlaying = false,
                                error = null,
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                trackTitle = response.item?.name ?: "Desconhecido",
                                trackArtist = response.item?.artists?.firstOrNull()?.name ?: "",
                                albumArtUrl = response.item?.album?.bestImageUrl(300),
                                isPlaying = response.isPlaying,
                                error = null,
                            )
                        }
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Erro ao buscar música")
                    }
                },
            )
        }

        // ── Comandos manuais (botões da tela) ────────────────────────────────

        fun togglePlayPause() {
            viewModelScope.launch {
                val cmd = if (_uiState.value.isPlaying) commandRepository::pause else commandRepository::play
                cmd().onFailure { e -> showCommandError(e.message) }
            }
        }

        fun skipToNext() {
            viewModelScope.launch {
                commandRepository.skipToNext().onFailure { e -> showCommandError(e.message) }
            }
        }

        fun skipToPrevious() {
            viewModelScope.launch {
                commandRepository.skipToPrevious().onFailure { e -> showCommandError(e.message) }
            }
        }

        fun volumeUp() = adjustVolume(+VOLUME_STEP)

        fun volumeDown() = adjustVolume(-VOLUME_STEP)

        fun dismissCommandError() {
            _uiState.update { it.copy(commandError = null) }
        }

        private fun adjustVolume(delta: Int) {
            viewModelScope.launch {
                val newVolume = (_uiState.value.volumePercent + delta).coerceIn(0, 100)
                commandRepository.setVolume(newVolume)
                    .onSuccess { _uiState.update { it.copy(volumePercent = newVolume) } }
                    .onFailure { e -> showCommandError(e.message) }
            }
        }

        private fun showCommandError(message: String?) {
            _uiState.update { it.copy(commandError = message ?: "Erro ao executar comando") }
        }

        override fun onCleared() {
            super.onCleared()
            gestureProcessor.release()
        }

        companion object {
            private const val POLL_INTERVAL_MS = 5_000L
            private const val VOLUME_STEP = 5
            private const val FEEDBACK_DURATION_MS = 2_000L
        }
    }

private fun Gesture.displayName(): String =
    when (this) {
        Gesture.TILT_HEAD_RIGHT -> "Inclinar Direita"
        Gesture.TILT_HEAD_LEFT -> "Inclinar Esquerda"
        Gesture.TILT_HEAD_UP -> "Inclinar Cima"
        Gesture.TILT_HEAD_DOWN -> "Inclinar Baixo"
        Gesture.TURN_FACE_RIGHT -> "Virar Direita"
        Gesture.TURN_FACE_LEFT -> "Virar Esquerda"
        Gesture.BLINK_RIGHT_EYE -> "Piscar Olho Direito"
        Gesture.BLINK_LEFT_EYE -> "Piscar Olho Esquerdo"
    }

private fun SpotifyAction.displayName(): String =
    when (this) {
        SpotifyAction.PLAY_PAUSE -> "Play / Pause"
        SpotifyAction.NEXT_TRACK -> "Próxima Faixa"
        SpotifyAction.PREVIOUS_TRACK -> "Faixa Anterior"
        SpotifyAction.VOLUME_UP -> "Volume +"
        SpotifyAction.VOLUME_DOWN -> "Volume -"
    }

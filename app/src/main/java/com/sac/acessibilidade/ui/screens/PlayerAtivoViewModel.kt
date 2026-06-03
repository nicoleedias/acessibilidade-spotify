package com.sac.acessibilidade.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sac.acessibilidade.spotify.player.SpotifyCommandRepository
import com.sac.acessibilidade.spotify.player.SpotifyPlayerRepository
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
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PlayerAtivoUiState())
        val uiState: StateFlow<PlayerAtivoUiState> = _uiState.asStateFlow()

        init {
            startPolling()
        }

        // ── Polling ──────────────────────────────────────────────────────────

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
                                trackTitle = "Nada tocando no momento",
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

        // ── Comandos (chamados pelos gestos ou pelos botões manuais) ─────────

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

        companion object {
            private const val POLL_INTERVAL_MS = 5_000L
            private const val VOLUME_STEP = 5
        }
    }

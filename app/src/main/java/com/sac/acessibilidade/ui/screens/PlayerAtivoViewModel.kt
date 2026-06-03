package com.sac.acessibilidade.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PlayerAtivoUiState())
        val uiState: StateFlow<PlayerAtivoUiState> = _uiState.asStateFlow()

        init {
            startPolling()
        }

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
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Erro ao buscar música",
                        )
                    }
                },
            )
        }

        companion object {
            private const val POLL_INTERVAL_MS = 5_000L
        }
    }

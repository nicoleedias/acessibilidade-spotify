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
class HomeViewModel
    @Inject
    constructor(
        private val playerRepository: SpotifyPlayerRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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
            playerRepository.getCurrentlyPlaying()
                .onSuccess { response ->
                    _uiState.update {
                        if (response == null) {
                            it.copy(nowPlayingTitle = null, nowPlayingArtist = null, isPlaying = false)
                        } else {
                            it.copy(
                                nowPlayingTitle = response.item?.name,
                                nowPlayingArtist = response.item?.artists?.firstOrNull()?.name,
                                isPlaying = response.isPlaying,
                            )
                        }
                    }
                }
        }

        companion object {
            private const val POLL_INTERVAL_MS = 10_000L
        }
    }

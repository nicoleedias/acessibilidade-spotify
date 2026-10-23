package com.sac.acessibilidade.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sac.acessibilidade.domain.gesture.Gesture
import com.sac.acessibilidade.spotify.player.SpotifyLibraryRepository
import com.sac.acessibilidade.spotify.player.model.PlaylistItem
import com.sac.acessibilidade.vision.GestureProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Navegação da biblioteca por gestos da cabeça.
 *
 * Aqui os gestos têm significado fixo de navegação, e não o mapeamento que o
 * usuário escolheu para controlar a reprodução: nesta tela ele não está tocando
 * música, está escolhendo o que tocar. São os mesmos movimentos que ele já
 * calibrou, então nada precisa ser recalibrado.
 *
 * - virar o rosto para a direita: próximo item
 * - virar o rosto para a esquerda: item anterior
 * - aceno: abre a playlist, ou toca a faixa
 * - inclinar a cabeça para baixo: volta um nível
 */
@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val libraryRepository: SpotifyLibraryRepository,
        val gestureProcessor: GestureProcessor,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LibraryUiState())
        val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

        private var openPlaylist: PlaylistItem? = null

        init {
            gestureProcessor.initialize()
            gestureProcessor.resetBaseline()
            loadPlaylists()
            collectGestures()
            observeBlinkReliability()
        }

        // ── Carga ────────────────────────────────────────────────────────────

        private fun loadPlaylists() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                libraryRepository
                    .getPlaylists()
                    .onSuccess { lista ->
                        _uiState.update {
                            it.copy(playlists = lista, focusedIndex = 0, isLoading = false)
                        }
                    }.onFailure { erro ->
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = erro.message ?: ERRO_PADRAO)
                        }
                    }
            }
        }

        // ── Gestos ───────────────────────────────────────────────────────────

        private fun collectGestures() {
            gestureProcessor.gestureFlow
                .onEach { gesture ->
                    when (gesture) {
                        Gesture.TURN_FACE_RIGHT -> focusNext()
                        Gesture.TURN_FACE_LEFT -> focusPrevious()
                        Gesture.NOD -> select()
                        Gesture.TILT_HEAD_DOWN -> back()
                        else -> Unit
                    }
                }.launchIn(viewModelScope)
        }

        private fun observeBlinkReliability() {
            gestureProcessor.blinkReliable
                .onEach { confiavel -> _uiState.update { it.copy(blinkReliable = confiavel) } }
                .launchIn(viewModelScope)
        }

        // ── Navegação ────────────────────────────────────────────────────────

        /**
         * Avança circularmente. Numa lista longa operada por gesto, parar no fim
         * deixaria o usuário repetindo um movimento sem efeito e sem explicação;
         * o indicador de posição evita que ele dê voltas sem perceber.
         */
        fun focusNext() {
            val total = _uiState.value.itemCount
            if (total == 0) return
            _uiState.update { it.copy(focusedIndex = (it.focusedIndex + 1) % total) }
        }

        fun focusPrevious() {
            val total = _uiState.value.itemCount
            if (total == 0) return
            _uiState.update { it.copy(focusedIndex = (it.focusedIndex - 1 + total) % total) }
        }

        fun select() {
            val state = _uiState.value
            if (state.itemCount == 0) return
            when (state.level) {
                LibraryLevel.PLAYLISTS -> openFocusedPlaylist()
                LibraryLevel.TRACKS -> playFocusedTrack()
            }
        }

        private fun openFocusedPlaylist() {
            val state = _uiState.value
            val playlist = state.playlists.getOrNull(state.focusedIndex) ?: return
            openPlaylist = playlist
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                libraryRepository
                    .getPlaylistTracks(playlist.id)
                    .onSuccess { faixas ->
                        _uiState.update {
                            it.copy(
                                level = LibraryLevel.TRACKS,
                                tracks = faixas,
                                focusedIndex = 0,
                                openPlaylistName = playlist.name,
                                isLoading = false,
                            )
                        }
                    }.onFailure { erro ->
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = erro.message ?: ERRO_PADRAO)
                        }
                    }
            }
        }

        /**
         * Toca a playlist inteira a partir da faixa escolhida, em vez da faixa
         * solta: assim a reprodução continua sozinha, que é o que importa para
         * quem não vai interagir a cada música.
         */
        private fun playFocusedTrack() {
            val contexto = openPlaylist?.uri ?: return
            val indice = _uiState.value.focusedIndex
            viewModelScope.launch {
                libraryRepository
                    .playFromContext(contexto, indice)
                    .onFailure { erro ->
                        _uiState.update { it.copy(errorMessage = erro.message ?: ERRO_PADRAO) }
                    }
            }
        }

        /**
         * Volta das faixas para as playlists, deixando o foco na playlist que
         * estava aberta — não no começo da lista.
         *
         * @return false quando já está no nível das playlists, para a tela saber
         *   que o botão voltar do sistema deve encerrar a tela.
         */
        fun back(): Boolean {
            val state = _uiState.value
            if (state.level == LibraryLevel.PLAYLISTS) return false
            val indiceDaPlaylist =
                state.playlists.indexOfFirst { it.id == openPlaylist?.id }.coerceAtLeast(0)
            openPlaylist = null
            _uiState.update {
                it.copy(
                    level = LibraryLevel.PLAYLISTS,
                    tracks = emptyList(),
                    focusedIndex = indiceDaPlaylist,
                    openPlaylistName = null,
                    errorMessage = null,
                )
            }
            return true
        }

        fun dismissError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        fun retry() {
            if (_uiState.value.level == LibraryLevel.PLAYLISTS) loadPlaylists() else back()
        }

        private companion object {
            const val ERRO_PADRAO = "Não foi possível carregar sua biblioteca."
        }
    }

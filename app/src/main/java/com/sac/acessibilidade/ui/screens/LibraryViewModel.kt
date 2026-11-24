package com.sac.acessibilidade.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sac.acessibilidade.domain.gesture.Gesture
import com.sac.acessibilidade.domain.gesture.GestureMappingRepository
import com.sac.acessibilidade.domain.gesture.SpotifyAction
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
 * Navegação da biblioteca por gestos, usando o mesmo mapeamento da tela de
 * configuração: o usuário aprende um conjunto de gestos só.
 *
 * - "Próxima Faixa": próximo item (playlist ou faixa)
 * - "Faixa Anterior": item anterior
 * - "Tocar / Pausar": abre a playlist em foco, ou toca a faixa em foco
 * - "Voltar": volta um nível
 */
@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val libraryRepository: SpotifyLibraryRepository,
        private val gestureMappingRepository: GestureMappingRepository,
        val gestureProcessor: GestureProcessor,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LibraryUiState())
        val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

        private var openPlaylist: PlaylistItem? = null
        private var currentMapping: Map<Gesture, SpotifyAction?> = emptyMap()

        init {
            gestureProcessor.initialize()
            gestureProcessor.resetBaseline()
            loadPlaylists()
            observeMappings()
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

        private fun observeMappings() {
            gestureMappingRepository
                .observeMappings()
                .onEach { mapping ->
                    currentMapping = mapping
                    val atribuidas = mapping.values.toSet()
                    val faltando = ACOES_DA_BIBLIOTECA.filterNot { it in atribuidas }
                    _uiState.update { it.copy(missingActions = faltando) }
                }
                .launchIn(viewModelScope)
        }

        private fun collectGestures() {
            gestureProcessor.gestureFlow
                .onEach { gesture ->
                    when (currentMapping[gesture]) {
                        SpotifyAction.NEXT_TRACK -> focusNext()
                        SpotifyAction.PREVIOUS_TRACK -> focusPrevious()
                        SpotifyAction.PLAY_PAUSE -> select()
                        SpotifyAction.BACK -> back()
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
            /** Ações que a navegação da biblioteca usa; sem gesto, só os botões da tela as alcançam. */
            val ACOES_DA_BIBLIOTECA =
                listOf(
                    SpotifyAction.NEXT_TRACK,
                    SpotifyAction.PREVIOUS_TRACK,
                    SpotifyAction.PLAY_PAUSE,
                    SpotifyAction.BACK,
                )

            const val ERRO_PADRAO = "Não foi possível carregar sua biblioteca."
        }
    }

package com.sac.acessibilidade.ui.screens

import com.sac.acessibilidade.spotify.player.model.PlaylistItem
import com.sac.acessibilidade.spotify.player.model.PlaylistTrack

/** Nível em que a navegação está: a lista de playlists ou as faixas de uma delas. */
enum class LibraryLevel {
    PLAYLISTS,
    TRACKS,
}

/**
 * Estado da navegação da biblioteca.
 *
 * Um item por vez fica em foco, porque a tela é operada por gestos da cabeça:
 * não há como apontar para um alvo, só avançar, voltar e confirmar.
 */
data class LibraryUiState(
    val level: LibraryLevel = LibraryLevel.PLAYLISTS,
    val playlists: List<PlaylistItem> = emptyList(),
    val tracks: List<PlaylistTrack> = emptyList(),
    val focusedIndex: Int = 0,
    val openPlaylistName: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** Falso quando a piscada está bloqueada por oclusão (ver BlinkReliabilityMonitor). */
    val blinkReliable: Boolean = true,
) {
    val itemCount: Int
        get() = if (level == LibraryLevel.PLAYLISTS) playlists.size else tracks.size

    val isEmpty: Boolean get() = !isLoading && itemCount == 0

    /** Título do item em foco, ou null quando a lista está vazia ou carregando. */
    val focusedTitle: String?
        get() =
            when (level) {
                LibraryLevel.PLAYLISTS -> playlists.getOrNull(focusedIndex)?.name
                LibraryLevel.TRACKS -> tracks.getOrNull(focusedIndex)?.name
            }

    val focusedSubtitle: String?
        get() =
            when (level) {
                LibraryLevel.PLAYLISTS -> playlists.getOrNull(focusedIndex)?.let(::trackCountLabel)
                LibraryLevel.TRACKS -> tracks.getOrNull(focusedIndex)?.artistNames?.ifBlank { null }
            }

    val focusedImageUrl: String?
        get() =
            when (level) {
                LibraryLevel.PLAYLISTS -> playlists.getOrNull(focusedIndex)?.bestImageUrl()
                LibraryLevel.TRACKS -> tracks.getOrNull(focusedIndex)?.album?.bestImageUrl()
            }

    /** "3 de 25" — mantém a noção de onde se está sem precisar mostrar a lista toda. */
    val positionLabel: String?
        get() = if (itemCount == 0) null else "${focusedIndex + 1} de $itemCount"

    private fun trackCountLabel(playlist: PlaylistItem): String? =
        when (playlist.trackCount) {
            0 -> null
            1 -> "1 música"
            else -> "${playlist.trackCount} músicas"
        }
}

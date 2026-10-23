package com.sac.acessibilidade.ui.screens

import com.sac.acessibilidade.domain.gesture.Gesture
import com.sac.acessibilidade.spotify.player.SpotifyLibraryRepository
import com.sac.acessibilidade.spotify.player.model.PlaylistItem
import com.sac.acessibilidade.spotify.player.model.PlaylistTrack
import com.sac.acessibilidade.spotify.player.model.PlaylistTracksRef
import com.sac.acessibilidade.util.MainDispatcherRule
import com.sac.acessibilidade.vision.GestureProcessor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Navegação da biblioteca por gestos (UC01).
 *
 * O que importa aqui: o usuário percorre a lista com um item em foco por vez,
 * e cada gesto tem um efeito previsível. Nenhum teste toca em rede ou câmera.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: SpotifyLibraryRepository
    private lateinit var gestureProcessor: GestureProcessor
    private lateinit var gestures: MutableSharedFlow<Gesture>
    private lateinit var blinkReliable: MutableStateFlow<Boolean>

    private fun playlist(
        id: String,
        nome: String,
        total: Int = 10,
    ) = PlaylistItem(
        id = id,
        uri = "spotify:playlist:$id",
        name = nome,
        tracks = PlaylistTracksRef(total),
    )

    private fun faixa(nome: String) = PlaylistTrack(uri = "spotify:track:$nome", name = nome)

    private val tresPlaylists =
        listOf(playlist("a", "Foco"), playlist("b", "Treino"), playlist("c", "Domingo"))

    private val duasFaixas = listOf(faixa("primeira"), faixa("segunda"))

    @Before
    fun setUp() {
        gestures = MutableSharedFlow(extraBufferCapacity = 8)
        blinkReliable = MutableStateFlow(true)

        gestureProcessor = mockk(relaxed = true)
        every { gestureProcessor.gestureFlow } returns gestures
        every { gestureProcessor.blinkReliable } returns blinkReliable

        repository = mockk(relaxed = true)
        coEvery { repository.getPlaylists() } returns Result.success(tresPlaylists)
        coEvery { repository.getPlaylistTracks(any()) } returns Result.success(duasFaixas)
        coEvery { repository.playFromContext(any(), any()) } returns Result.success(Unit)
    }

    private fun viewModel() = LibraryViewModel(repository, gestureProcessor)

    // ── Carga inicial ────────────────────────────────────────────────────

    @Test
    fun `abre na lista de playlists da conta`() =
        runTest {
            val vm = viewModel()
            runCurrent()

            val estado = vm.uiState.value
            assertEquals(LibraryLevel.PLAYLISTS, estado.level)
            assertEquals(listOf("Foco", "Treino", "Domingo"), estado.playlists.map { it.name })
            assertEquals("Foco", estado.focusedTitle)
            assertFalse(estado.isLoading)
        }

    @Test
    fun `falha ao carregar vira mensagem na tela`() =
        runTest {
            coEvery { repository.getPlaylists() } returns
                Result.failure(RuntimeException("Sessão expirada. Entre no Spotify novamente."))

            val vm = viewModel()
            runCurrent()

            assertEquals("Sessão expirada. Entre no Spotify novamente.", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `conta sem playlists fica vazia e sem erro`() =
        runTest {
            coEvery { repository.getPlaylists() } returns Result.success(emptyList())

            val vm = viewModel()
            runCurrent()

            assertTrue(vm.uiState.value.isEmpty)
            assertNull(vm.uiState.value.errorMessage)
            assertNull(vm.uiState.value.focusedTitle)
        }

    // ── Navegação com um item em foco ────────────────────────────────────

    @Test
    fun `avancar move o foco para o proximo item`() =
        runTest {
            val vm = viewModel()
            runCurrent()

            vm.focusNext()

            assertEquals("Treino", vm.uiState.value.focusedTitle)
        }

    @Test
    fun `avancar no ultimo item volta ao primeiro`() =
        runTest {
            val vm = viewModel()
            runCurrent()

            repeat(3) { vm.focusNext() }

            assertEquals("Foco", vm.uiState.value.focusedTitle)
        }

    @Test
    fun `voltar no primeiro item vai para o ultimo`() =
        runTest {
            val vm = viewModel()
            runCurrent()

            vm.focusPrevious()

            assertEquals("Domingo", vm.uiState.value.focusedTitle)
        }

    /** Sem itens não há para onde ir — navegar não pode quebrar nem mover índice. */
    @Test
    fun `navegar em lista vazia nao faz nada`() =
        runTest {
            coEvery { repository.getPlaylists() } returns Result.success(emptyList())
            val vm = viewModel()
            runCurrent()

            vm.focusNext()
            vm.focusPrevious()

            assertEquals(0, vm.uiState.value.focusedIndex)
        }

    @Test
    fun `indicador de posicao acompanha o foco`() =
        runTest {
            val vm = viewModel()
            runCurrent()
            assertEquals("1 de 3", vm.uiState.value.positionLabel)

            vm.focusNext()

            assertEquals("2 de 3", vm.uiState.value.positionLabel)
        }

    // ── Entrar e sair de uma playlist ────────────────────────────────────

    @Test
    fun `selecionar uma playlist abre as faixas dela`() =
        runTest {
            val vm = viewModel()
            runCurrent()

            vm.focusNext()
            vm.select()
            runCurrent()

            val estado = vm.uiState.value
            assertEquals(LibraryLevel.TRACKS, estado.level)
            assertEquals("Treino", estado.openPlaylistName)
            assertEquals(listOf("primeira", "segunda"), estado.tracks.map { it.name })
            assertEquals(0, estado.focusedIndex)
            coVerify { repository.getPlaylistTracks("b") }
        }

    @Test
    fun `falha ao abrir a playlist vira mensagem e mantem o nivel`() =
        runTest {
            coEvery { repository.getPlaylistTracks(any()) } returns
                Result.failure(RuntimeException("Nenhum dispositivo ativo."))
            val vm = viewModel()
            runCurrent()

            vm.select()
            runCurrent()

            assertEquals(LibraryLevel.PLAYLISTS, vm.uiState.value.level)
            assertEquals("Nenhum dispositivo ativo.", vm.uiState.value.errorMessage)
        }

    /** Ao voltar, o foco precisa cair na playlist que estava aberta, não no topo. */
    @Test
    fun `voltar devolve o foco a playlist que estava aberta`() =
        runTest {
            val vm = viewModel()
            runCurrent()
            vm.focusNext()
            vm.focusNext()
            vm.select()
            runCurrent()

            val tinhaVoltado = vm.back()

            val estado = vm.uiState.value
            assertTrue(tinhaVoltado)
            assertEquals(LibraryLevel.PLAYLISTS, estado.level)
            assertEquals("Domingo", estado.focusedTitle)
            assertTrue(estado.tracks.isEmpty())
        }

    @Test
    fun `voltar na lista de playlists sinaliza que a tela deve sair`() =
        runTest {
            val vm = viewModel()
            runCurrent()

            assertFalse(vm.back())
        }

    // ── Reprodução ───────────────────────────────────────────────────────

    /**
     * Toca a PLAYLIST a partir da faixa escolhida, e não a faixa isolada:
     * a reprodução precisa continuar sozinha depois que a música acabar.
     */
    @Test
    fun `selecionar uma faixa toca a playlist a partir dela`() =
        runTest {
            val vm = viewModel()
            runCurrent()
            vm.select()
            runCurrent()

            vm.focusNext()
            vm.select()
            runCurrent()

            coVerify { repository.playFromContext("spotify:playlist:a", 1) }
        }

    @Test
    fun `primeira faixa toca a playlist do inicio`() =
        runTest {
            val vm = viewModel()
            runCurrent()
            vm.select()
            runCurrent()

            vm.select()
            runCurrent()

            coVerify { repository.playFromContext("spotify:playlist:a", 0) }
        }

    @Test
    fun `falha ao tocar vira mensagem sem sair da lista`() =
        runTest {
            coEvery { repository.playFromContext(any(), any()) } returns
                Result.failure(RuntimeException("Spotify Premium necessário"))
            val vm = viewModel()
            runCurrent()
            vm.select()
            runCurrent()

            vm.select()
            runCurrent()

            assertEquals("Spotify Premium necessário", vm.uiState.value.errorMessage)
            assertEquals(LibraryLevel.TRACKS, vm.uiState.value.level)
        }

    @Test
    fun `dispensar erro limpa a mensagem`() =
        runTest {
            coEvery { repository.getPlaylists() } returns Result.failure(RuntimeException("falhou"))
            val vm = viewModel()
            runCurrent()

            vm.dismissError()

            assertNull(vm.uiState.value.errorMessage)
        }

    // ── Gestos ───────────────────────────────────────────────────────────

    @Test
    fun `virar o rosto para a direita avanca na lista`() =
        runTest {
            val vm = viewModel()
            runCurrent()

            gestures.emit(Gesture.TURN_FACE_RIGHT)
            runCurrent()

            assertEquals("Treino", vm.uiState.value.focusedTitle)
        }

    @Test
    fun `virar o rosto para a esquerda retrocede na lista`() =
        runTest {
            val vm = viewModel()
            runCurrent()
            gestures.emit(Gesture.TURN_FACE_RIGHT)
            runCurrent()

            gestures.emit(Gesture.TURN_FACE_LEFT)
            runCurrent()

            assertEquals("Foco", vm.uiState.value.focusedTitle)
        }

    @Test
    fun `aceno abre a playlist em foco`() =
        runTest {
            val vm = viewModel()
            runCurrent()

            gestures.emit(Gesture.NOD)
            runCurrent()

            assertEquals(LibraryLevel.TRACKS, vm.uiState.value.level)
        }

    @Test
    fun `inclinar para baixo volta um nivel`() =
        runTest {
            val vm = viewModel()
            runCurrent()
            gestures.emit(Gesture.NOD)
            runCurrent()

            gestures.emit(Gesture.TILT_HEAD_DOWN)
            runCurrent()

            assertEquals(LibraryLevel.PLAYLISTS, vm.uiState.value.level)
        }

    /** Gestos fora do vocabulário desta tela não podem ter efeito colateral. */
    @Test
    fun `gesto sem funcao na biblioteca e ignorado`() =
        runTest {
            val vm = viewModel()
            runCurrent()

            gestures.emit(Gesture.TILT_HEAD_RIGHT)
            gestures.emit(Gesture.BLINK_LEFT_EYE)
            runCurrent()

            assertEquals(0, vm.uiState.value.focusedIndex)
            assertEquals(LibraryLevel.PLAYLISTS, vm.uiState.value.level)
        }

    // ── Piscada bloqueada por oclusão ────────────────────────────────────

    @Test
    fun `piscada bloqueada chega ao estado da tela`() =
        runTest {
            val vm = viewModel()
            runCurrent()
            assertTrue(vm.uiState.value.blinkReliable)

            blinkReliable.value = false
            runCurrent()

            assertFalse(vm.uiState.value.blinkReliable)
        }
}

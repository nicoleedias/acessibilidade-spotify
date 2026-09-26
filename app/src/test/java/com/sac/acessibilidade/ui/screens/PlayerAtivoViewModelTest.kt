package com.sac.acessibilidade.ui.screens

import androidx.lifecycle.viewModelScope
import com.sac.acessibilidade.domain.gesture.Gesture
import com.sac.acessibilidade.domain.gesture.GestureMappingRepository
import com.sac.acessibilidade.domain.gesture.SpotifyAction
import com.sac.acessibilidade.domain.gesture.displayName
import com.sac.acessibilidade.spotify.player.SpotifyCommandRepository
import com.sac.acessibilidade.spotify.player.SpotifyPlayerRepository
import com.sac.acessibilidade.spotify.player.model.AlbumImage
import com.sac.acessibilidade.spotify.player.model.AlbumItem
import com.sac.acessibilidade.spotify.player.model.ArtistItem
import com.sac.acessibilidade.spotify.player.model.CurrentlyPlayingResponse
import com.sac.acessibilidade.spotify.player.model.TrackItem
import com.sac.acessibilidade.util.MainDispatcherRule
import com.sac.acessibilidade.util.invokeOnCleared
import com.sac.acessibilidade.vision.GestureProcessor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
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
 * UC01 — Controlar Reprodução de Mídia via Gestos.
 *
 * Este é o teste que valida a regra central do projeto: o gesto detectado é
 * traduzido em comando **lendo o mapeamento do banco**, nunca uma tabela fixa.
 * Um gesto sem ação mapeada tem de ser ignorado em silêncio.
 *
 * Como a ViewModel também faz polling infinito de 5 s, aqui vale a mesma regra
 * do `HomeViewModelTest`: `runCurrent()` e `advanceTimeBy()`, nunca
 * `advanceUntilIdle()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerAtivoViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var playerRepository: SpotifyPlayerRepository
    private lateinit var commandRepository: SpotifyCommandRepository
    private lateinit var mappingRepository: GestureMappingRepository
    private lateinit var gestureProcessor: GestureProcessor
    private lateinit var gestures: MutableSharedFlow<Gesture>
    private lateinit var mappings: MutableStateFlow<Map<Gesture, SpotifyAction?>>

    private val defaults =
        mapOf(
            Gesture.TILT_HEAD_RIGHT to SpotifyAction.VOLUME_UP,
            Gesture.TILT_HEAD_LEFT to SpotifyAction.VOLUME_DOWN,
            Gesture.TILT_HEAD_UP to null,
            Gesture.TILT_HEAD_DOWN to null,
            Gesture.TURN_FACE_RIGHT to SpotifyAction.NEXT_TRACK,
            Gesture.TURN_FACE_LEFT to SpotifyAction.PREVIOUS_TRACK,
            Gesture.NOD to SpotifyAction.PLAY_PAUSE,
            Gesture.BLINK_RIGHT_EYE to null,
            Gesture.BLINK_LEFT_EYE to null,
        )

    @Before
    fun setUp() {
        gestures = MutableSharedFlow(extraBufferCapacity = 8)
        mappings = MutableStateFlow(defaults)

        gestureProcessor = mockk(relaxed = true)
        every { gestureProcessor.gestureFlow } returns gestures

        mappingRepository = mockk(relaxed = true)
        every { mappingRepository.observeMappings() } returns mappings

        playerRepository = mockk()
        coEvery { playerRepository.getCurrentlyPlaying() } returns Result.success(null)

        commandRepository = mockk()
        coEvery { commandRepository.play() } returns Result.success(Unit)
        coEvery { commandRepository.pause() } returns Result.success(Unit)
        coEvery { commandRepository.skipToNext() } returns Result.success(Unit)
        coEvery { commandRepository.skipToPrevious() } returns Result.success(Unit)
        coEvery { commandRepository.setVolume(any()) } returns Result.success(Unit)
    }

    private fun viewModel() =
        PlayerAtivoViewModel(
            playerRepository = playerRepository,
            commandRepository = commandRepository,
            gestureMappingRepository = mappingRepository,
            gestureProcessor = gestureProcessor,
        )

    private fun PlayerAtivoViewModel.encerrar() = viewModelScope.cancel()

    private fun faixa(tocando: Boolean = true) =
        CurrentlyPlayingResponse(
            isPlaying = tocando,
            item =
                TrackItem(
                    name = "Garota de Ipanema",
                    artists = listOf(ArtistItem(name = "Tom Jobim")),
                    album = AlbumItem(images = listOf(AlbumImage(url = "capa300", width = 300))),
                ),
        )

    // ── Ciclo de vida da câmera ─────────────────────────────────────────────

    @Test
    fun `inicializa o processador e recaptura a pose neutra ao abrir`() =
        runTest {
            val vm = viewModel()
            runCurrent()

            verify(exactly = 1) { gestureProcessor.initialize() }
            verify(exactly = 1) { gestureProcessor.resetBaseline() }
            vm.encerrar()
        }

    // ── Despacho gesto → comando (UC01) ─────────────────────────────────────

    @Test
    fun `aceno dispara tocar quando esta pausado`() =
        runTest {
            val vm = viewModel()
            runCurrent()

            gestures.emit(Gesture.NOD)
            runCurrent()

            coVerify(exactly = 1) { commandRepository.play() }
            coVerify(exactly = 0) { commandRepository.pause() }
            vm.encerrar()
        }

    @Test
    fun `aceno dispara pausar quando esta tocando`() =
        runTest {
            coEvery { playerRepository.getCurrentlyPlaying() } returns Result.success(faixa(tocando = true))
            val vm = viewModel()
            runCurrent()

            gestures.emit(Gesture.NOD)
            runCurrent()

            coVerify(exactly = 1) { commandRepository.pause() }
            coVerify(exactly = 0) { commandRepository.play() }
            vm.encerrar()
        }

    @Test
    fun `virar rosto a direita avanca a faixa`() =
        runTest {
            val vm = viewModel()
            runCurrent()

            gestures.emit(Gesture.TURN_FACE_RIGHT)
            runCurrent()

            coVerify(exactly = 1) { commandRepository.skipToNext() }
            vm.encerrar()
        }

    @Test
    fun `virar rosto a esquerda volta a faixa`() =
        runTest {
            val vm = viewModel()
            runCurrent()

            gestures.emit(Gesture.TURN_FACE_LEFT)
            runCurrent()

            coVerify(exactly = 1) { commandRepository.skipToPrevious() }
            vm.encerrar()
        }

    @Test
    fun `inclinar a direita aumenta o volume em cinco`() =
        runTest {
            val vm = viewModel()
            runCurrent()

            gestures.emit(Gesture.TILT_HEAD_RIGHT)
            runCurrent()

            coVerify(exactly = 1) { commandRepository.setVolume(55) }
            assertEquals(55, vm.uiState.value.volumePercent)
            vm.encerrar()
        }

    @Test
    fun `inclinar a esquerda diminui o volume em cinco`() =
        runTest {
            val vm = viewModel()
            runCurrent()

            gestures.emit(Gesture.TILT_HEAD_LEFT)
            runCurrent()

            coVerify(exactly = 1) { commandRepository.setVolume(45) }
            assertEquals(45, vm.uiState.value.volumePercent)
            vm.encerrar()
        }

    // ── Gestos sem ação ─────────────────────────────────────────────────────

    @Test
    fun `gesto sem acao mapeada e ignorado`() =
        runTest {
            val vm = viewModel()
            runCurrent()

            gestures.emit(Gesture.BLINK_LEFT_EYE)
            gestures.emit(Gesture.TILT_HEAD_UP)
            runCurrent()

            coVerify(exactly = 0) { commandRepository.play() }
            coVerify(exactly = 0) { commandRepository.pause() }
            coVerify(exactly = 0) { commandRepository.skipToNext() }
            coVerify(exactly = 0) { commandRepository.skipToPrevious() }
            coVerify(exactly = 0) { commandRepository.setVolume(any()) }
            assertFalse(vm.uiState.value.hasDetectedGesture)
            vm.encerrar()
        }

    @Test
    fun `mapeamento customizado do usuario e respeitado`() =
        runTest {
            // Regra do projeto: o mapeamento vem do banco, nunca hardcoded.
            mappings.value = mapOf(Gesture.BLINK_LEFT_EYE to SpotifyAction.NEXT_TRACK)
            val vm = viewModel()
            runCurrent()

            gestures.emit(Gesture.BLINK_LEFT_EYE)
            runCurrent()

            coVerify(exactly = 1) { commandRepository.skipToNext() }
            vm.encerrar()
        }

    @Test
    fun `remapeamento em tempo de execucao muda o comando disparado`() =
        runTest {
            val vm = viewModel()
            runCurrent()

            gestures.emit(Gesture.NOD)
            runCurrent()
            coVerify(exactly = 1) { commandRepository.play() }

            mappings.value = mapOf(Gesture.NOD to SpotifyAction.NEXT_TRACK)
            runCurrent()
            gestures.emit(Gesture.NOD)
            runCurrent()

            coVerify(exactly = 1) { commandRepository.skipToNext() }
            vm.encerrar()
        }

    // ── Feedback visual do gesto ────────────────────────────────────────────

    @Test
    fun `gesto reconhecido exibe nome do gesto e da acao`() =
        runTest {
            val vm = viewModel()
            runCurrent()

            gestures.emit(Gesture.TURN_FACE_RIGHT)
            runCurrent()

            assertTrue(vm.uiState.value.hasDetectedGesture)
            assertEquals(Gesture.TURN_FACE_RIGHT.displayName(), vm.uiState.value.lastGestureName)
            assertEquals(SpotifyAction.NEXT_TRACK.displayName(), vm.uiState.value.lastGestureAction)
            vm.encerrar()
        }

    @Test
    fun `feedback do gesto some depois de dois segundos`() =
        runTest {
            val vm = viewModel()
            runCurrent()
            gestures.emit(Gesture.TURN_FACE_RIGHT)
            runCurrent()
            assertTrue(vm.uiState.value.hasDetectedGesture)

            advanceTimeBy(2_000)
            runCurrent()

            assertFalse(vm.uiState.value.hasDetectedGesture)
            vm.encerrar()
        }

    @Test
    fun `feedback continua visivel antes de dois segundos`() =
        runTest {
            val vm = viewModel()
            runCurrent()
            gestures.emit(Gesture.TURN_FACE_RIGHT)
            runCurrent()

            advanceTimeBy(1_500)
            runCurrent()

            assertTrue(vm.uiState.value.hasDetectedGesture)
            vm.encerrar()
        }

    // ── Saturação de volume ─────────────────────────────────────────────────

    @Test
    fun `volume satura em cem`() =
        runTest {
            val vm = viewModel()
            runCurrent()

            repeat(15) {
                vm.volumeUp()
                runCurrent()
            }

            assertEquals(100, vm.uiState.value.volumePercent)
            coVerify(exactly = 0) { commandRepository.setVolume(more(100)) }
            vm.encerrar()
        }

    @Test
    fun `volume satura em zero`() =
        runTest {
            val vm = viewModel()
            runCurrent()

            repeat(15) {
                vm.volumeDown()
                runCurrent()
            }

            assertEquals(0, vm.uiState.value.volumePercent)
            coVerify(exactly = 0) { commandRepository.setVolume(less(0)) }
            vm.encerrar()
        }

    @Test
    fun `volume nao muda no estado quando o comando falha`() =
        runTest {
            coEvery { commandRepository.setVolume(any()) } returns
                Result.failure(java.io.IOException("Nenhum dispositivo ativo"))
            val vm = viewModel()
            runCurrent()
            val antes = vm.uiState.value.volumePercent

            vm.volumeUp()
            runCurrent()

            assertEquals(antes, vm.uiState.value.volumePercent)
            vm.encerrar()
        }

    // ── Erros de comando ────────────────────────────────────────────────────

    @Test
    fun `falha de comando vira mensagem de erro na tela`() =
        runTest {
            coEvery { commandRepository.skipToNext() } returns
                Result.failure(java.io.IOException("Spotify Premium necessário"))
            val vm = viewModel()
            runCurrent()

            vm.skipToNext()
            runCurrent()

            assertEquals("Spotify Premium necessário", vm.uiState.value.commandError)
            vm.encerrar()
        }

    @Test
    fun `falha sem mensagem usa texto padrao`() =
        runTest {
            coEvery { commandRepository.skipToPrevious() } returns Result.failure(RuntimeException())
            val vm = viewModel()
            runCurrent()

            vm.skipToPrevious()
            runCurrent()

            assertEquals("Erro ao executar comando", vm.uiState.value.commandError)
            vm.encerrar()
        }

    @Test
    fun `dispensar erro limpa a mensagem`() =
        runTest {
            coEvery { commandRepository.skipToNext() } returns Result.failure(RuntimeException("boom"))
            val vm = viewModel()
            runCurrent()
            vm.skipToNext()
            runCurrent()

            vm.dismissCommandError()

            assertNull(vm.uiState.value.commandError)
            vm.encerrar()
        }

    // ── Polling da faixa ────────────────────────────────────────────────────

    @Test
    fun `faixa tocando preenche titulo artista e capa`() =
        runTest {
            coEvery { playerRepository.getCurrentlyPlaying() } returns Result.success(faixa())
            val vm = viewModel()
            runCurrent()

            val estado = vm.uiState.value
            assertEquals("Garota de Ipanema", estado.trackTitle)
            assertEquals("Tom Jobim", estado.trackArtist)
            assertEquals("capa300", estado.albumArtUrl)
            assertTrue(estado.isPlaying)
            assertFalse(estado.isLoading)
            vm.encerrar()
        }

    @Test
    fun `nada tocando exibe mensagem propria`() =
        runTest {
            coEvery { playerRepository.getCurrentlyPlaying() } returns Result.success(null)
            val vm = viewModel()
            runCurrent()

            assertEquals("Nada tocando", vm.uiState.value.trackTitle)
            assertEquals("", vm.uiState.value.trackArtist)
            assertNull(vm.uiState.value.albumArtUrl)
            assertFalse(vm.uiState.value.isPlaying)
            vm.encerrar()
        }

    @Test
    fun `falha na busca da faixa vira erro na tela`() =
        runTest {
            coEvery { playerRepository.getCurrentlyPlaying() } returns
                Result.failure(java.io.IOException("Sem rede"))
            val vm = viewModel()
            runCurrent()

            assertEquals("Sem rede", vm.uiState.value.error)
            assertFalse(vm.uiState.value.isLoading)
            vm.encerrar()
        }

    @Test
    fun `polling refaz a consulta a cada cinco segundos`() =
        runTest {
            val vm = viewModel()
            runCurrent()
            coVerify(exactly = 1) { playerRepository.getCurrentlyPlaying() }

            advanceTimeBy(5_000)
            runCurrent()
            coVerify(exactly = 2) { playerRepository.getCurrentlyPlaying() }
            vm.encerrar()
        }

    // ── Encerramento ────────────────────────────────────────────────────────

    @Test
    fun `liberar a viewmodel libera a camera`() =
        runTest {
            val vm = viewModel()
            runCurrent()

            vm.invokeOnCleared()

            verify(exactly = 1) { gestureProcessor.release() }
            vm.encerrar()
        }
}

package com.sac.acessibilidade.ui.screens

import com.sac.acessibilidade.domain.gesture.Gesture
import com.sac.acessibilidade.domain.gesture.GestureMappingRepository
import com.sac.acessibilidade.domain.gesture.NO_ACTION_LABEL
import com.sac.acessibilidade.domain.gesture.SpotifyAction
import com.sac.acessibilidade.domain.gesture.displayName
import com.sac.acessibilidade.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * UC03 — Customizar Mapeamento de Comandos.
 *
 * A ViewModel é a ponte entre os rótulos em português da tela e os enums do
 * domínio. Um erro de conversão aqui salva "sem ação" sem o usuário perceber.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GestureConfigViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: GestureMappingRepository
    private lateinit var mappingsFlow: MutableStateFlow<Map<Gesture, SpotifyAction?>>

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
        mappingsFlow = MutableStateFlow(defaults)
        repository = mockk(relaxed = true)
        every { repository.observeMappings() } returns mappingsFlow
    }

    private fun viewModel() = GestureConfigViewModel(repository)

    // ── Carga inicial ───────────────────────────────────────────────────────

    @Test
    fun `estado inicial lista os nove gestos na ordem do enum`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()

            val linhas = vm.uiState.value.mappings
            assertEquals(9, linhas.size)
            assertEquals(Gesture.entries, linhas.map { it.gesture })
        }

    @Test
    fun `cada linha exibe o rotulo do gesto e da acao mapeada`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()

            val nod = vm.uiState.value.mappings.first { it.gesture == Gesture.NOD }
            assertEquals(Gesture.NOD.displayName(), nod.gestureName)
            assertEquals(SpotifyAction.PLAY_PAUSE.displayName(), nod.selectedAction)
        }

    @Test
    fun `gesto sem acao exibe o rotulo de sem acao`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()

            val piscar = vm.uiState.value.mappings.first { it.gesture == Gesture.BLINK_LEFT_EYE }
            assertEquals(NO_ACTION_LABEL, piscar.selectedAction)
        }

    @Test
    fun `mudanca no repositorio reflete no estado`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()

            mappingsFlow.value = mapOf(Gesture.NOD to SpotifyAction.NEXT_TRACK)
            advanceUntilIdle()

            val nod = vm.uiState.value.mappings.first { it.gesture == Gesture.NOD }
            assertEquals(SpotifyAction.NEXT_TRACK.displayName(), nod.selectedAction)
            // Gestos ausentes do mapa viram "sem ação", não somem da lista
            assertEquals(9, vm.uiState.value.mappings.size)
            assertEquals(
                NO_ACTION_LABEL,
                vm.uiState.value.mappings.first { it.gesture == Gesture.TILT_HEAD_RIGHT }.selectedAction,
            )
        }

    // ── Edição ──────────────────────────────────────────────────────────────

    @Test
    fun `atualizar mapeamento altera apenas a linha do gesto informado`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()
            val antes = vm.uiState.value.mappings

            vm.updateMapping(Gesture.TILT_HEAD_UP, SpotifyAction.NEXT_TRACK.displayName())

            val depois = vm.uiState.value.mappings
            assertEquals(
                SpotifyAction.NEXT_TRACK.displayName(),
                depois.first { it.gesture == Gesture.TILT_HEAD_UP }.selectedAction,
            )
            // Todas as outras linhas ficaram intactas
            val outrasAntes = antes.filter { it.gesture != Gesture.TILT_HEAD_UP }
            val outrasDepois = depois.filter { it.gesture != Gesture.TILT_HEAD_UP }
            assertEquals(outrasAntes, outrasDepois)
        }

    @Test
    fun `atualizar para sem acao e permitido`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()

            vm.updateMapping(Gesture.NOD, NO_ACTION_LABEL)

            assertEquals(
                NO_ACTION_LABEL,
                vm.uiState.value.mappings.first { it.gesture == Gesture.NOD }.selectedAction,
            )
        }

    // ── Salvar ──────────────────────────────────────────────────────────────

    @Test
    fun `salvar converte os rotulos de volta para enums do dominio`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()
            vm.updateMapping(Gesture.TILT_HEAD_UP, SpotifyAction.NEXT_TRACK.displayName())

            val slot = slot<Map<Gesture, SpotifyAction?>>()
            vm.save {}
            advanceUntilIdle()

            coVerify { repository.saveMappings(capture(slot)) }
            assertEquals(SpotifyAction.NEXT_TRACK, slot.captured[Gesture.TILT_HEAD_UP])
            assertEquals(SpotifyAction.PLAY_PAUSE, slot.captured[Gesture.NOD])
            assertEquals(9, slot.captured.size)
        }

    @Test
    fun `salvar converte sem acao em nulo`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()
            vm.updateMapping(Gesture.NOD, NO_ACTION_LABEL)

            val slot = slot<Map<Gesture, SpotifyAction?>>()
            vm.save {}
            advanceUntilIdle()

            coVerify { repository.saveMappings(capture(slot)) }
            assertTrue(slot.captured.containsKey(Gesture.NOD))
            assertNull(slot.captured[Gesture.NOD])
        }

    @Test
    fun `salvar invoca o callback de conclusao`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()
            var concluido = false

            vm.save { concluido = true }
            advanceUntilIdle()

            assertTrue(concluido)
        }

    @Test
    fun `estado de salvando volta a falso ao terminar`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()

            vm.save {}
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isSaving)
        }

    // ── Restaurar padrões ───────────────────────────────────────────────────

    @Test
    fun `restaurar padroes delega ao repositorio`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()

            vm.restoreDefaults()
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.restoreDefaults() }
        }
}

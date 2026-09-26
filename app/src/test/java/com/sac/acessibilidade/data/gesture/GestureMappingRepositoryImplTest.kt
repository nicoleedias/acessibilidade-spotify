package com.sac.acessibilidade.data.gesture

import com.sac.acessibilidade.domain.gesture.Gesture
import com.sac.acessibilidade.domain.gesture.SpotifyAction
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Regras de UC03: o mapeamento gesto→ação é 100% customizável e mora no banco.
 * Este teste isola a tradução entidade↔enum e a política de defaults;
 * `GestureMappingRepositoryIntegrationTest` (androidTest) cobre o Room real.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GestureMappingRepositoryImplTest {
    private lateinit var dao: GestureMappingDao
    private lateinit var repository: GestureMappingRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = GestureMappingRepositoryImpl(dao)
    }

    // ── Defaults quando o banco está vazio ──────────────────────────────────

    @Test
    fun `banco vazio devolve os mapeamentos padrao`() =
        runTest {
            every { dao.observeAll() } returns flowOf(emptyList())

            val mappings = repository.observeMappings().first()

            assertEquals(SpotifyAction.VOLUME_UP, mappings[Gesture.TILT_HEAD_RIGHT])
            assertEquals(SpotifyAction.VOLUME_DOWN, mappings[Gesture.TILT_HEAD_LEFT])
            assertEquals(SpotifyAction.NEXT_TRACK, mappings[Gesture.TURN_FACE_RIGHT])
            assertEquals(SpotifyAction.PREVIOUS_TRACK, mappings[Gesture.TURN_FACE_LEFT])
            assertEquals(SpotifyAction.PLAY_PAUSE, mappings[Gesture.NOD])
        }

    @Test
    fun `defaults cobrem os nove gestos deixando quatro sem acao`() =
        runTest {
            every { dao.observeAll() } returns flowOf(emptyList())

            val mappings = repository.observeMappings().first()

            assertEquals(9, mappings.size)
            assertEquals(Gesture.entries.toSet(), mappings.keys)
            assertNull(mappings[Gesture.TILT_HEAD_UP])
            assertNull(mappings[Gesture.TILT_HEAD_DOWN])
            assertNull(mappings[Gesture.BLINK_RIGHT_EYE])
            assertNull(mappings[Gesture.BLINK_LEFT_EYE])
        }

    @Test
    fun `defaults usam cada acao no maximo uma vez`() =
        runTest {
            every { dao.observeAll() } returns flowOf(emptyList())

            val acoes = repository.observeMappings().first().values.filterNotNull()

            assertEquals("Uma ação foi mapeada para dois gestos: $acoes", acoes.size, acoes.toSet().size)
        }

    // ── Leitura do que está persistido ──────────────────────────────────────

    @Test
    fun `mapeamento salvo tem prioridade sobre os defaults`() =
        runTest {
            every { dao.observeAll() } returns
                flowOf(
                    listOf(
                        GestureMappingEntity(gesture = "NOD", action = "NEXT_TRACK"),
                        GestureMappingEntity(gesture = "BLINK_LEFT_EYE", action = "PLAY_PAUSE"),
                    ),
                )

            val mappings = repository.observeMappings().first()

            // NOD deixou de ser PLAY_PAUSE (o default) e virou NEXT_TRACK
            assertEquals(SpotifyAction.NEXT_TRACK, mappings[Gesture.NOD])
            assertEquals(SpotifyAction.PLAY_PAUSE, mappings[Gesture.BLINK_LEFT_EYE])
            assertEquals(2, mappings.size)
        }

    @Test
    fun `acao nula persistida vira gesto sem acao`() =
        runTest {
            every { dao.observeAll() } returns
                flowOf(listOf(GestureMappingEntity(gesture = "TILT_HEAD_RIGHT", action = null)))

            val mappings = repository.observeMappings().first()

            assertTrue(mappings.containsKey(Gesture.TILT_HEAD_RIGHT))
            assertNull(mappings[Gesture.TILT_HEAD_RIGHT])
        }

    @Test
    fun `nome de gesto desconhecido no banco propaga excecao`() {
        // Comportamento ATUAL, documentado: renomear uma constante do enum
        // sem migrar a tabela derruba a tela de configuração.
        every { dao.observeAll() } returns
            flowOf(listOf(GestureMappingEntity(gesture = "GESTO_QUE_NAO_EXISTE", action = null)))

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.observeMappings().first() }
        }
    }

    // ── Escrita ─────────────────────────────────────────────────────────────

    @Test
    fun `salvar converte enums em entidades preservando nulos`() =
        runTest {
            val slot = slot<List<GestureMappingEntity>>()
            coEvery { dao.upsertAll(capture(slot)) } just Runs

            repository.saveMappings(
                mapOf(
                    Gesture.NOD to SpotifyAction.PLAY_PAUSE,
                    Gesture.TILT_HEAD_UP to null,
                ),
            )

            val salvas = slot.captured.associateBy { it.gesture }
            assertEquals(2, salvas.size)
            assertEquals("PLAY_PAUSE", salvas.getValue("NOD").action)
            assertNull(salvas.getValue("TILT_HEAD_UP").action)
        }

    @Test
    fun `salvar mapeamento vazio nao quebra`() =
        runTest {
            val slot = slot<List<GestureMappingEntity>>()
            coEvery { dao.upsertAll(capture(slot)) } just Runs

            repository.saveMappings(emptyMap())

            assertTrue(slot.captured.isEmpty())
        }

    @Test
    fun `restaurar padroes limpa a tabela antes de gravar`() =
        runTest {
            // A ordem importa: inverter apagaria os defaults recém-inseridos e
            // deixaria o usuário sem nenhum gesto funcionando.
            repository.restoreDefaults()

            coVerifyOrder {
                dao.clearAll()
                dao.upsertAll(any())
            }
        }

    @Test
    fun `restaurar padroes grava os nove gestos`() =
        runTest {
            val slot = slot<List<GestureMappingEntity>>()
            coEvery { dao.upsertAll(capture(slot)) } just Runs

            repository.restoreDefaults()

            assertEquals(9, slot.captured.size)
            assertEquals(
                Gesture.entries.map { it.name }.toSet(),
                slot.captured.map { it.gesture }.toSet(),
            )
            coVerify(exactly = 1) { dao.clearAll() }
        }
}

package com.sac.acessibilidade.data.gesture

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sac.acessibilidade.data.NodifyDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Teste de integração do DAO contra um Room real (em memória).
 *
 * Valida a persistência que sustenta UC03: sem isso, o mapeamento customizado
 * do usuário não sobrevive ao fechamento do app.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class GestureMappingDaoTest {
    private lateinit var database: NodifyDatabase
    private lateinit var dao: GestureMappingDao

    @Before
    fun criarBanco() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database =
            Room
                .inMemoryDatabaseBuilder(context, NodifyDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.gestureMappingDao()
    }

    @After
    fun fecharBanco() {
        database.close()
    }

    @Test
    fun bancoNovoEstaVazio() =
        runTest {
            assertTrue(dao.observeAll().first().isEmpty())
        }

    @Test
    fun gravaELeMapeamentos() =
        runTest {
            dao.upsertAll(
                listOf(
                    GestureMappingEntity(gesture = "NOD", action = "PLAY_PAUSE"),
                    GestureMappingEntity(gesture = "TURN_FACE_RIGHT", action = "NEXT_TRACK"),
                ),
            )

            val salvos = dao.observeAll().first().associate { it.gesture to it.action }

            assertEquals(2, salvos.size)
            assertEquals("PLAY_PAUSE", salvos["NOD"])
            assertEquals("NEXT_TRACK", salvos["TURN_FACE_RIGHT"])
        }

    @Test
    fun upsertDoMesmoGestoSubstituiEmVezDeDuplicar() =
        runTest {
            // O gesto é a chave primária: remapear não pode criar duas linhas.
            dao.upsertAll(listOf(GestureMappingEntity(gesture = "NOD", action = "PLAY_PAUSE")))
            dao.upsertAll(listOf(GestureMappingEntity(gesture = "NOD", action = "NEXT_TRACK")))

            val salvos = dao.observeAll().first()

            assertEquals(1, salvos.size)
            assertEquals("NEXT_TRACK", salvos.first().action)
        }

    @Test
    fun acaoNulaSobreviveAoRoundTrip() =
        runTest {
            // "Gesto sem ação" é um estado válido de UC03, não ausência de linha.
            dao.upsertAll(listOf(GestureMappingEntity(gesture = "BLINK_LEFT_EYE", action = null)))

            val salvos = dao.observeAll().first()

            assertEquals(1, salvos.size)
            assertEquals("BLINK_LEFT_EYE", salvos.first().gesture)
            assertNull(salvos.first().action)
        }

    @Test
    fun limparApagaTodasAsLinhas() =
        runTest {
            dao.upsertAll(
                listOf(
                    GestureMappingEntity(gesture = "NOD", action = "PLAY_PAUSE"),
                    GestureMappingEntity(gesture = "TILT_HEAD_LEFT", action = "VOLUME_DOWN"),
                ),
            )

            dao.clearAll()

            assertTrue(dao.observeAll().first().isEmpty())
        }

    @Test
    fun fluxoEmiteNovoValorAposEscrita() =
        runTest {
            // `observeAll` é um Flow reativo: a tela de configuração depende
            // dele para refletir "restaurar padrões" sem recarregar.
            assertTrue(dao.observeAll().first().isEmpty())

            dao.upsertAll(listOf(GestureMappingEntity(gesture = "NOD", action = "PLAY_PAUSE")))

            assertEquals(1, dao.observeAll().first().size)
        }
}

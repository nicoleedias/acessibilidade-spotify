package com.sac.acessibilidade.data.gesture

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sac.acessibilidade.data.NodifyDatabase
import com.sac.acessibilidade.domain.gesture.Gesture
import com.sac.acessibilidade.domain.gesture.SpotifyAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integração ponta a ponta da camada de dados de UC03: repositório real sobre
 * Room real. É a evidência de que "o mapeamento customizado do usuário persiste
 * entre reinícios do app" — sem mock nenhum no caminho.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class GestureMappingRepositoryIntegrationTest {
    private lateinit var database: NodifyDatabase
    private lateinit var repository: GestureMappingRepositoryImpl

    @Before
    fun criarBanco() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database =
            Room
                .inMemoryDatabaseBuilder(context, NodifyDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = GestureMappingRepositoryImpl(database.gestureMappingDao())
    }

    @After
    fun fecharBanco() {
        database.close()
    }

    @Test
    fun primeiroUsoEntregaOsMapeamentosPadrao() =
        runTest {
            val mappings = repository.observeMappings().first()

            assertEquals(9, mappings.size)
            assertEquals(SpotifyAction.VOLUME_UP, mappings[Gesture.TILT_HEAD_RIGHT])
            assertEquals(SpotifyAction.VOLUME_DOWN, mappings[Gesture.TILT_HEAD_LEFT])
            assertEquals(SpotifyAction.NEXT_TRACK, mappings[Gesture.TURN_FACE_RIGHT])
            assertEquals(SpotifyAction.PREVIOUS_TRACK, mappings[Gesture.TURN_FACE_LEFT])
            assertEquals(SpotifyAction.PLAY_PAUSE, mappings[Gesture.NOD])
        }

    @Test
    fun mapeamentoCustomizadoPersisteEEhLidoDeVolta() =
        runTest {
            val customizado =
                mapOf(
                    Gesture.BLINK_LEFT_EYE to SpotifyAction.PLAY_PAUSE,
                    Gesture.TILT_HEAD_UP to SpotifyAction.NEXT_TRACK,
                    Gesture.NOD to null,
                )

            repository.saveMappings(customizado)

            assertEquals(customizado, repository.observeMappings().first())
        }

    @Test
    fun novaInstanciaDoRepositorioLeOMesmoBanco() =
        runTest {
            // Simula reabrir o app: outro repositório, mesmo arquivo de banco.
            repository.saveMappings(mapOf(Gesture.BLINK_RIGHT_EYE to SpotifyAction.NEXT_TRACK))

            val outroRepositorio = GestureMappingRepositoryImpl(database.gestureMappingDao())

            assertEquals(
                SpotifyAction.NEXT_TRACK,
                outroRepositorio.observeMappings().first()[Gesture.BLINK_RIGHT_EYE],
            )
        }

    @Test
    fun gestoSemAcaoPersisteComoLinhaComAcaoNula() =
        runTest {
            repository.saveMappings(mapOf(Gesture.NOD to null))

            val mappings = repository.observeMappings().first()

            // Continua sendo uma linha explícita — não volta para o default
            assertEquals(1, mappings.size)
            assertNull(mappings[Gesture.NOD])
        }

    @Test
    fun restaurarPadroesDescartaACustomizacao() =
        runTest {
            repository.saveMappings(mapOf(Gesture.BLINK_LEFT_EYE to SpotifyAction.PLAY_PAUSE))

            repository.restoreDefaults()

            val mappings = repository.observeMappings().first()
            assertEquals(9, mappings.size)
            assertEquals(SpotifyAction.PLAY_PAUSE, mappings[Gesture.NOD])
            assertNull(mappings[Gesture.BLINK_LEFT_EYE])
        }

    @Test
    fun restaurarPadroesNaoDeixaLinhasOrfas() =
        runTest {
            // Um mapeamento antigo com um gesto que não está nos defaults tem de
            // sumir; senão `clearAll` teria rodado depois do `upsertAll`.
            repository.saveMappings(
                mapOf(
                    Gesture.TILT_HEAD_DOWN to SpotifyAction.VOLUME_UP,
                    Gesture.BLINK_RIGHT_EYE to SpotifyAction.NEXT_TRACK,
                ),
            )

            repository.restoreDefaults()

            val mappings = repository.observeMappings().first()
            assertEquals(9, mappings.size)
            assertNull(mappings[Gesture.TILT_HEAD_DOWN])
            assertNull(mappings[Gesture.BLINK_RIGHT_EYE])
        }

    @Test
    fun cadaAcaoApareceNoMaximoUmaVezAposRestaurar() =
        runTest {
            repository.restoreDefaults()

            val acoes = repository.observeMappings().first().values.filterNotNull()

            assertEquals(acoes.size, acoes.toSet().size)
        }
}

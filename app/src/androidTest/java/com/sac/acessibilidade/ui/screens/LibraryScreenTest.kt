package com.sac.acessibilidade.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sac.acessibilidade.spotify.player.model.PlaylistItem
import com.sac.acessibilidade.spotify.player.model.PlaylistTrack
import com.sac.acessibilidade.spotify.player.model.PlaylistTracksRef
import com.sac.acessibilidade.ui.theme.NodifyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Teste funcional da tela de biblioteca.
 *
 * A tela é state-hoisted: recebe estado e lambdas, sem ViewModel, Hilt, rede ou
 * câmera. Aqui se verifica o que o usuário vê e o que cada controle dispara.
 */
@RunWith(AndroidJUnit4::class)
class LibraryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun playlist(
        id: String,
        nome: String,
        total: Int = 12,
    ) = PlaylistItem(
        id = id,
        uri = "spotify:playlist:$id",
        name = nome,
        tracks = PlaylistTracksRef(total),
    )

    private val estadoComPlaylists =
        LibraryUiState(
            playlists = listOf(playlist("a", "Foco Total"), playlist("b", "Treino")),
            focusedIndex = 0,
            isLoading = false,
        )

    private fun render(
        uiState: LibraryUiState,
        onPrevious: () -> Unit = {},
        onNext: () -> Unit = {},
        onSelect: () -> Unit = {},
        onBack: () -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        composeRule.setContent {
            NodifyTheme {
                LibraryScreen(uiState, onPrevious, onNext, onSelect, onBack, onRetry)
            }
        }
    }

    // ── Item em foco ─────────────────────────────────────────────────────

    @Test
    fun exibeONomeDaPlaylistEmFoco() {
        render(estadoComPlaylists)

        composeRule.onNodeWithText("Foco Total").assertIsDisplayed()
    }

    @Test
    fun exibeAContagemDeMusicasDaPlaylist() {
        render(estadoComPlaylists)

        composeRule.onNodeWithText("12 músicas").assertIsDisplayed()
    }

    @Test
    fun exibeAPosicaoNaLista() {
        render(estadoComPlaylists)

        composeRule.onNodeWithText("1 de 2").assertIsDisplayed()
    }

    @Test
    fun focoEmOutroIndiceMostraOItemCorrespondente() {
        render(estadoComPlaylists.copy(focusedIndex = 1))

        composeRule.onNodeWithText("Treino").assertIsDisplayed()
        composeRule.onNodeWithText("2 de 2").assertIsDisplayed()
    }

    @Test
    fun dentroDaPlaylistOCabecalhoMostraONomeDela() {
        render(
            LibraryUiState(
                level = LibraryLevel.TRACKS,
                tracks = listOf(PlaylistTrack(uri = "spotify:track:1", name = "Primeira")),
                openPlaylistName = "Foco Total",
                isLoading = false,
            ),
        )

        composeRule.onNodeWithText("Foco Total").assertIsDisplayed()
        composeRule.onNodeWithText("Primeira").assertIsDisplayed()
    }

    // ── Controles manuais (caminho alternativo ao gesto) ─────────────────

    @Test
    fun botaoProximoDisparaOCallback() {
        var chamou = false
        render(estadoComPlaylists, onNext = { chamou = true })

        composeRule.onNodeWithContentDescription("Próximo item").performClick()

        assertTrue(chamou)
    }

    @Test
    fun botaoAnteriorDisparaOCallback() {
        var chamou = false
        render(estadoComPlaylists, onPrevious = { chamou = true })

        composeRule.onNodeWithContentDescription("Item anterior").performClick()

        assertTrue(chamou)
    }

    @Test
    fun botaoCentralAbreAPlaylistNoNivelDeListas() {
        var chamou = false
        render(estadoComPlaylists, onSelect = { chamou = true })

        composeRule.onNodeWithContentDescription("Abrir playlist").performClick()

        assertTrue(chamou)
    }

    /** No nível das faixas, o mesmo botão central passa a significar "Tocar". */
    @Test
    fun botaoCentralTocaNoNivelDeFaixas() {
        var chamou = false
        render(
            LibraryUiState(
                level = LibraryLevel.TRACKS,
                tracks = listOf(PlaylistTrack(uri = "spotify:track:1", name = "Primeira")),
                isLoading = false,
            ),
            onSelect = { chamou = true },
        )

        composeRule.onNodeWithContentDescription("Tocar").performClick()

        assertTrue(chamou)
    }

    @Test
    fun botaoVoltarDisparaOCallback() {
        var chamou = false
        render(estadoComPlaylists, onBack = { chamou = true })

        composeRule.onNodeWithContentDescription("Voltar").performClick()

        assertTrue(chamou)
    }

    // ── Estados da tela ──────────────────────────────────────────────────

    @Test
    fun carregandoNaoMostraControlesDeNavegacao() {
        render(LibraryUiState(isLoading = true))

        composeRule.onNodeWithContentDescription("Próximo item").assertDoesNotExist()
    }

    @Test
    fun contaSemPlaylistsExibeMensagemPropria() {
        render(LibraryUiState(isLoading = false))

        composeRule.onNodeWithText("Nenhuma playlist encontrada nesta conta.").assertIsDisplayed()
    }

    @Test
    fun erroExibeAMensagemEOBotaoDeTentarDeNovo() {
        render(LibraryUiState(isLoading = false, errorMessage = "Sessão expirada."))

        composeRule.onNodeWithText("Sessão expirada.").assertIsDisplayed()
        composeRule.onNodeWithText("Tentar de novo").assertIsDisplayed()
    }

    @Test
    fun tentarDeNovoDisparaOCallback() {
        var chamou = false
        render(LibraryUiState(isLoading = false, errorMessage = "falhou"), onRetry = { chamou = true })

        composeRule.onNodeWithContentDescription("Tentar de novo").performClick()

        assertTrue(chamou)
    }

    // ── Piscada bloqueada por oclusão ────────────────────────────────────

    @Test
    fun piscadaBloqueadaExibeOAviso() {
        render(estadoComPlaylists.copy(blinkReliable = false))

        composeRule
            .onNodeWithText(
                "A piscada não está sendo reconhecida. Se estiver de óculos escuros, use os outros gestos.",
            ).assertIsDisplayed()
    }

    @Test
    fun semProblemaDePiscadaNaoExibeAviso() {
        render(estadoComPlaylists)

        composeRule
            .onNodeWithText(
                "A piscada não está sendo reconhecida. Se estiver de óculos escuros, use os outros gestos.",
            ).assertDoesNotExist()
    }

    // ── Dica de gestos ───────────────────────────────────────────────────

    @Test
    fun dicaDeGestosMudaConformeONivel() {
        render(estadoComPlaylists)
        composeRule.onNodeWithText("Vire o rosto para navegar · Aceno para abrir").assertIsDisplayed()
    }

    // ── Acessibilidade ───────────────────────────────────────────────────

    /** Alvos de toque devem respeitar o mínimo do Material: 48dp. */
    @Test
    fun todosOsControlesRespeitamOAlvoMinimoDeToque() {
        render(estadoComPlaylists)

        listOf("Voltar", "Item anterior", "Abrir playlist", "Próximo item").forEach { rotulo ->
            val node = composeRule.onNodeWithContentDescription(rotulo).fetchSemanticsNode()
            val largura = node.size.width
            val altura = node.size.height
            val minimoPx = 48 * composeRule.density.density
            assertTrue(
                "$rotulo mede ${largura}x$altura px, abaixo de 48dp",
                largura >= minimoPx && altura >= minimoPx,
            )
        }
    }

    @Test
    fun aCapaTemDescricaoParaLeitorDeTela() {
        render(estadoComPlaylists)

        assertEquals(
            1,
            composeRule.onAllNodesWithContentDescription("Capa").fetchSemanticsNodes().size,
        )
    }
}

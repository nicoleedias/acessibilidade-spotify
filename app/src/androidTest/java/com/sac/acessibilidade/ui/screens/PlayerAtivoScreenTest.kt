package com.sac.acessibilidade.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sac.acessibilidade.ui.theme.NodifyTheme
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Teste funcional da tela de UC01 — Controlar Reprodução de Mídia via Gestos.
 *
 * Passando `gestureProcessor = null` a tela usa o placeholder de câmera, então
 * o teste roda sem permissão de câmera, sem MediaPipe e sem hardware.
 */
@RunWith(AndroidJUnit4::class)
class PlayerAtivoScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    /**
     * `TrackingHeader` usa `rememberInfiniteTransition` (o ponto verde pulsante).
     * Com o avanço automático do relógio, `waitForIdle` nunca considera a tela
     * ociosa e todo teste desta classe estouraria o tempo limite. Desligar o
     * avanço automático mantém a recomposição funcionando sem rodar a animação.
     */
    @Before
    fun congelarAnimacoes() {
        composeRule.mainClock.autoAdvance = false
    }

    private fun tocando(
        titulo: String = "Garota de Ipanema",
        artista: String = "Tom Jobim",
        isPlaying: Boolean = true,
    ) = PlayerAtivoUiState(
        trackTitle = titulo,
        trackArtist = artista,
        isPlaying = isPlaying,
        isLoading = false,
    )

    private fun montarTela(
        estado: PlayerAtivoUiState = tocando(),
        onStopTracking: () -> Unit = {},
        onPlayPauseClick: () -> Unit = {},
        onSkipNextClick: () -> Unit = {},
        onSkipPreviousClick: () -> Unit = {},
        onVolumeUpClick: () -> Unit = {},
        onVolumeDownClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            NodifyTheme {
                PlayerAtivoScreen(
                    uiState = estado,
                    gestureProcessor = null,
                    onStopTracking = onStopTracking,
                    onPlayPauseClick = onPlayPauseClick,
                    onSkipNextClick = onSkipNextClick,
                    onSkipPreviousClick = onSkipPreviousClick,
                    onVolumeUpClick = onVolumeUpClick,
                    onVolumeDownClick = onVolumeDownClick,
                )
            }
        }
    }

    // ── Renderização da faixa ───────────────────────────────────────────────

    @Test
    fun exibeTituloEArtistaDaFaixa() {
        montarTela()

        composeRule.onNodeWithText("Garota de Ipanema").assertIsDisplayed()
        composeRule.onNodeWithText("Tom Jobim").assertIsDisplayed()
    }

    @Test
    fun tocandoExibeOBotaoDePausar() {
        montarTela(estado = tocando(isPlaying = true))

        composeRule.onNodeWithContentDescription("Pausar reprodução").assertIsDisplayed()
    }

    @Test
    fun pausadoExibeOBotaoDeRetomar() {
        montarTela(estado = tocando(isPlaying = false))

        composeRule.onNodeWithContentDescription("Retomar reprodução").assertIsDisplayed()
    }

    @Test
    fun exibeOPercentualDeVolume() {
        montarTela(estado = tocando().copy(volumePercent = 65))

        composeRule.onNodeWithText("65%").assertIsDisplayed()
    }

    @Test
    fun carregandoNaoExibeOsControlesDeFaixa() {
        montarTela(estado = PlayerAtivoUiState(isLoading = true))

        composeRule.onNodeWithContentDescription("Próxima faixa").assertDoesNotExist()
    }

    @Test
    fun erroDeBuscaSubstituiOCartaoDaFaixa() {
        montarTela(estado = PlayerAtivoUiState(isLoading = false, error = "Sem conexão"))

        composeRule.onNodeWithText("Sem conexão").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Próxima faixa").assertDoesNotExist()
    }

    // ── Controles manuais (alternativa acessível aos gestos) ────────────────

    @Test
    fun botaoTocarPausarNotificaATela() {
        var clicou = false
        montarTela(onPlayPauseClick = { clicou = true })

        composeRule.onNodeWithContentDescription("Pausar reprodução").performClick()

        assertTrue(clicou)
    }

    @Test
    fun botaoProximaFaixaNotificaATela() {
        var clicou = false
        montarTela(onSkipNextClick = { clicou = true })

        composeRule.onNodeWithContentDescription("Próxima faixa").performClick()

        assertTrue(clicou)
    }

    @Test
    fun botaoFaixaAnteriorNotificaATela() {
        var clicou = false
        montarTela(onSkipPreviousClick = { clicou = true })

        composeRule.onNodeWithContentDescription("Faixa anterior").performClick()

        assertTrue(clicou)
    }

    @Test
    fun botoesDeVolumeNotificamATela() {
        var subiu = false
        var desceu = false
        montarTela(onVolumeUpClick = { subiu = true }, onVolumeDownClick = { desceu = true })

        composeRule.onNodeWithContentDescription("Volume +5").performClick()
        composeRule.onNodeWithContentDescription("Volume -5").performClick()

        assertTrue(subiu)
        assertTrue(desceu)
    }

    @Test
    fun botaoPararRastreamentoNotificaATela() {
        var parou = false
        montarTela(onStopTracking = { parou = true })

        composeRule
            .onNodeWithContentDescription("Parar o rastreamento de gestos e voltar ao início")
            .performClick()

        assertTrue(parou)
    }

    // ── Feedback visual do gesto ────────────────────────────────────────────

    @Test
    fun gestoDetectadoExibeAPilulaComGestoEAcao() {
        // Requisito de acessibilidade: o usuário precisa ver o que o app "viu".
        montarTela(
            estado =
                tocando().copy(
                    hasDetectedGesture = true,
                    lastGestureName = "Aceno (sim)",
                    lastGestureAction = "Tocar / Pausar",
                ),
        )

        composeRule
            .onNodeWithContentDescription("Aceno (sim) → Tocar / Pausar")
            .assertIsDisplayed()
    }

    @Test
    fun semGestoDetectadoAPilulaNaoAparece() {
        montarTela(
            estado =
                tocando().copy(
                    hasDetectedGesture = false,
                    lastGestureName = "Aceno (sim)",
                    lastGestureAction = "Tocar / Pausar",
                ),
        )

        composeRule
            .onNodeWithContentDescription("Aceno (sim) → Tocar / Pausar")
            .assertDoesNotExist()
    }

    // ── Erro de comando ─────────────────────────────────────────────────────

    @Test
    fun erroDeComandoApareceSemEsconderOsControles() {
        montarTela(
            estado = tocando().copy(commandError = "Spotify Premium necessário para controle de reprodução"),
        )

        composeRule
            .onNodeWithText("Spotify Premium necessário para controle de reprodução")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Próxima faixa").assertIsDisplayed()
    }

    @Test
    fun semErroDeComandoNadaEhExibido() {
        montarTela(estado = tocando().copy(commandError = null))

        composeRule
            .onNodeWithText("Spotify Premium necessário para controle de reprodução")
            .assertDoesNotExist()
    }

    // ── Câmera ──────────────────────────────────────────────────────────────

    @Test
    fun feedDaCameraTemDescricaoParaLeitorDeTela() {
        montarTela()

        composeRule
            .onNodeWithContentDescription("Feed da câmera frontal para rastreamento de gestos")
            .assertExists()
    }
}

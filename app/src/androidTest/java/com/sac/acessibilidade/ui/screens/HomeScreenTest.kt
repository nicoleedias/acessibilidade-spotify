package com.sac.acessibilidade.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sac.acessibilidade.ui.theme.NodifyTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Teste funcional do painel inicial: saudação, navegação e faixa atual. */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun montarTela(
        estado: HomeUiState = HomeUiState(),
        onStartTrackingClick: () -> Unit = {},
        onCalibrateClick: () -> Unit = {},
        onConfigureGesturesClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            NodifyTheme {
                HomeScreen(
                    uiState = estado,
                    onStartTrackingClick = onStartTrackingClick,
                    onCalibrateClick = onCalibrateClick,
                    onConfigureGesturesClick = onConfigureGesturesClick,
                )
            }
        }
    }

    // ── Saudação ────────────────────────────────────────────────────────────

    @Test
    fun exibeONomeDoUsuarioNaSaudacao() {
        montarTela(estado = HomeUiState(userName = "Pedro"))

        composeRule.onNodeWithText("Olá, Pedro").assertIsDisplayed()
    }

    @Test
    fun semNomeUsaOTratamentoGenerico() {
        montarTela(estado = HomeUiState(userName = null))

        composeRule.onNodeWithText("Olá, Usuário").assertIsDisplayed()
    }

    @Test
    fun exibeOStatusDeConexaoComOSpotify() {
        montarTela()

        composeRule.onNodeWithText("Spotify Conectado").assertIsDisplayed()
    }

    // ── Navegação ───────────────────────────────────────────────────────────

    @Test
    fun exibeOsTresCaminhosPrincipais() {
        montarTela()

        composeRule.onNodeWithContentDescription("Iniciar Rastreamento").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Calibrar Movimentos").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Configurar Gestos").assertIsDisplayed()
    }

    @Test
    fun iniciarRastreamentoNotificaATela() {
        var clicou = false
        montarTela(onStartTrackingClick = { clicou = true })

        composeRule.onNodeWithContentDescription("Iniciar Rastreamento").performClick()

        assertTrue(clicou)
    }

    @Test
    fun calibrarNotificaATela() {
        var clicou = false
        montarTela(onCalibrateClick = { clicou = true })

        composeRule.onNodeWithContentDescription("Calibrar Movimentos").performClick()

        assertTrue(clicou)
    }

    @Test
    fun configurarGestosNotificaATela() {
        var clicou = false
        montarTela(onConfigureGesturesClick = { clicou = true })

        composeRule.onNodeWithContentDescription("Configurar Gestos").performClick()

        assertTrue(clicou)
    }

    // ── Faixa atual ─────────────────────────────────────────────────────────

    @Test
    fun faixaTocandoApareceNoMiniCartao() {
        montarTela(
            estado =
                HomeUiState(
                    nowPlayingTitle = "Garota de Ipanema",
                    nowPlayingArtist = "Tom Jobim",
                    isPlaying = true,
                ),
        )

        composeRule.onNodeWithText("Tocando agora").assertIsDisplayed()
        composeRule.onNodeWithText("Garota de Ipanema").assertIsDisplayed()
        composeRule.onNodeWithText("Tom Jobim").assertIsDisplayed()
    }

    @Test
    fun semFaixaOMiniCartaoNaoApareceu() {
        montarTela(estado = HomeUiState(nowPlayingTitle = null))

        composeRule.onNodeWithText("Tocando agora").assertDoesNotExist()
    }
}

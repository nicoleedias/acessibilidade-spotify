package com.sac.acessibilidade.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sac.acessibilidade.ui.theme.NodifyTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Teste funcional da porta de entrada do app (UC01 — autenticação). */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun montarTela(
        estado: LoginUiState = LoginUiState.Idle,
        onConnectClick: () -> Unit = {},
        onErrorDismiss: () -> Unit = {},
    ) {
        composeRule.setContent {
            NodifyTheme {
                LoginScreen(
                    uiState = estado,
                    onConnectClick = onConnectClick,
                    onErrorDismiss = onErrorDismiss,
                )
            }
        }
    }

    // ── Estado ocioso ───────────────────────────────────────────────────────

    @Test
    fun estadoOciosoExibeOBotaoDeConectar() {
        montarTela()

        composeRule.onNodeWithText("Bem-vindo").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Conectar com Spotify").assertIsEnabled()
    }

    @Test
    fun exibeOAvisoDeQueASenhaNaoEhSalva() {
        // Transparência sobre o fluxo OAuth faz parte do requisito de privacidade.
        montarTela()

        composeRule
            .onNodeWithText(
                "Não salvamos sua senha. Você será redirecionado para o ambiente seguro do Spotify.",
            ).assertIsDisplayed()
    }

    @Test
    fun conectarNotificaATela() {
        var clicou = false
        montarTela(onConnectClick = { clicou = true })

        composeRule.onNodeWithContentDescription("Conectar com Spotify").performClick()

        assertTrue(clicou)
    }

    // ── Estado carregando ───────────────────────────────────────────────────

    @Test
    fun carregandoDesabilitaOBotao() {
        montarTela(estado = LoginUiState.Loading)

        composeRule.onNodeWithContentDescription("Conectar com Spotify").assertIsNotEnabled()
    }

    @Test
    fun carregandoIgnoraCliquesRepetidos() {
        // Evita disparar duas trocas de código OAuth com o mesmo `state`.
        var cliques = 0
        montarTela(estado = LoginUiState.Loading, onConnectClick = { cliques++ })

        composeRule.onNodeWithContentDescription("Conectar com Spotify").performClick()

        assertTrue(cliques == 0)
    }

    // ── Estado de erro ──────────────────────────────────────────────────────

    @Test
    fun erroExibeAMensagemAoUsuario() {
        montarTela(estado = LoginUiState.Error("Falha ao conectar com o Spotify. Tente novamente."))

        composeRule
            .onNodeWithText("Falha ao conectar com o Spotify. Tente novamente.")
            .assertIsDisplayed()
    }

    @Test
    fun tentarDeNovoLimpaOErroAntesDeReconectar() {
        var limpou = false
        var conectou = false
        montarTela(
            estado = LoginUiState.Error("Falha"),
            onConnectClick = { conectou = true },
            onErrorDismiss = { limpou = true },
        )

        composeRule.onNodeWithContentDescription("Conectar com Spotify").performClick()

        assertTrue(limpou)
        assertTrue(conectou)
    }

    @Test
    fun estadoOciosoNaoExibeMensagemDeErro() {
        var limpou = false
        montarTela(onErrorDismiss = { limpou = true })

        composeRule.onNodeWithContentDescription("Conectar com Spotify").performClick()

        // Sem erro na tela, `onErrorDismiss` não deve ser chamado
        assertFalse(limpou)
    }
}

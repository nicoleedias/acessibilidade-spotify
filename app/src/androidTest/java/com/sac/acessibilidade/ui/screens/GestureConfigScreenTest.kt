package com.sac.acessibilidade.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sac.acessibilidade.domain.gesture.Gesture
import com.sac.acessibilidade.domain.gesture.NO_ACTION_LABEL
import com.sac.acessibilidade.domain.gesture.SpotifyAction
import com.sac.acessibilidade.domain.gesture.displayName
import com.sac.acessibilidade.ui.theme.NodifyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Teste funcional da tela de UC03 — Customizar Mapeamento de Comandos.
 *
 * A tela é totalmente state-hoisted, então dá para exercitá-la com estado
 * sintético e lambdas: sem ViewModel, sem Hilt, sem banco, sem rede.
 */
@RunWith(AndroidJUnit4::class)
class GestureConfigScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun linhas(): List<GestureMappingUi> =
        Gesture.entries.map { gesture ->
            GestureMappingUi(
                gesture = gesture,
                gestureName = gesture.displayName(),
                selectedAction =
                    when (gesture) {
                        Gesture.NOD -> SpotifyAction.PLAY_PAUSE.displayName()
                        Gesture.TURN_FACE_RIGHT -> SpotifyAction.NEXT_TRACK.displayName()
                        else -> NO_ACTION_LABEL
                    },
                icon = Icons.Default.Face,
            )
        }

    private fun montarTela(
        estado: GestureConfigUiState = GestureConfigUiState(mappings = linhas()),
        onMappingChanged: (Gesture, String) -> Unit = { _, _ -> },
        onSaveClick: () -> Unit = {},
        onRestoreDefaults: () -> Unit = {},
        onBack: () -> Unit = {},
    ) {
        composeRule.setContent {
            NodifyTheme {
                GestureConfigScreen(
                    uiState = estado,
                    onMappingChanged = onMappingChanged,
                    onSaveClick = onSaveClick,
                    onRestoreDefaults = onRestoreDefaults,
                    onBack = onBack,
                )
            }
        }
    }

    // ── Renderização ────────────────────────────────────────────────────────

    @Test
    fun exibeOsNoveGestosDoVocabulario() {
        montarTela()

        Gesture.entries.forEach { gesture ->
            composeRule
                .onNodeWithText(gesture.displayName())
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun exibeAAcaoMapeadaDeCadaGesto() {
        montarTela()

        composeRule
            .onNodeWithContentDescription(
                "Ação para ${Gesture.NOD.displayName()}: ${SpotifyAction.PLAY_PAUSE.displayName()}. Toque para alterar",
            ).performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun gestoSemAcaoExibeORotuloDeSemAcao() {
        montarTela()

        composeRule
            .onNodeWithContentDescription(
                "Ação para ${Gesture.BLINK_LEFT_EYE.displayName()}: $NO_ACTION_LABEL. Toque para alterar",
            ).performScrollTo()
            .assertIsDisplayed()
    }

    // ── Edição do mapeamento ────────────────────────────────────────────────

    @Test
    fun escolherUmaAcaoNotificaOGestoEORotuloCorretos() {
        val recebidos = mutableListOf<Pair<Gesture, String>>()
        montarTela(onMappingChanged = { g, a -> recebidos += g to a })

        // Abre o seletor do gesto "Aceno (sim)"
        composeRule
            .onNodeWithContentDescription(
                "Ação para ${Gesture.NOD.displayName()}: ${SpotifyAction.PLAY_PAUSE.displayName()}. Toque para alterar",
            ).performScrollTo()
            .performClick()

        // Escolhe "Próxima Faixa" na lista aberta
        composeRule
            .onNodeWithContentDescription(SpotifyAction.NEXT_TRACK.displayName())
            .performScrollTo()
            .performClick()

        assertEquals(1, recebidos.size)
        assertEquals(Gesture.NOD, recebidos.first().first)
        assertEquals(SpotifyAction.NEXT_TRACK.displayName(), recebidos.first().second)
    }

    @Test
    fun podeEscolherSemAcaoParaDesativarUmGesto() {
        // Acessibilidade: o usuário precisa poder desligar um gesto que não
        // consegue executar de forma confiável.
        val recebidos = mutableListOf<Pair<Gesture, String>>()
        montarTela(onMappingChanged = { g, a -> recebidos += g to a })

        composeRule
            .onNodeWithContentDescription(
                "Ação para ${Gesture.NOD.displayName()}: ${SpotifyAction.PLAY_PAUSE.displayName()}. Toque para alterar",
            ).performScrollTo()
            .performClick()

        composeRule
            .onNodeWithContentDescription(NO_ACTION_LABEL)
            .performScrollTo()
            .performClick()

        assertEquals(NO_ACTION_LABEL, recebidos.single().second)
    }

    // ── Ações da barra ──────────────────────────────────────────────────────

    @Test
    fun botaoSalvarNotificaAConclusao() {
        var salvou = false
        montarTela(onSaveClick = { salvou = true })

        composeRule.onNodeWithContentDescription("Salvar Configurações").performClick()

        assertTrue(salvou)
    }

    @Test
    fun botaoSalvarFicaDesabilitadoEnquantoSalva() {
        montarTela(estado = GestureConfigUiState(mappings = linhas(), isSaving = true))

        composeRule.onNodeWithContentDescription("Salvar Configurações").assertIsNotEnabled()
    }

    @Test
    fun botaoSalvarFicaHabilitadoQuandoOcioso() {
        montarTela()

        composeRule.onNodeWithContentDescription("Salvar Configurações").assertIsEnabled()
    }

    @Test
    fun restaurarPadroesNotificaATela() {
        var restaurou = false
        montarTela(onRestoreDefaults = { restaurou = true })

        composeRule.onNodeWithText("Restaurar padrões").performClick()

        assertTrue(restaurou)
    }

    @Test
    fun botaoVoltarNotificaATela() {
        var voltou = false
        montarTela(onBack = { voltou = true })

        composeRule.onNodeWithContentDescription("Voltar").performClick()

        assertTrue(voltou)
    }
}

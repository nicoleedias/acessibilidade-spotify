package com.sac.acessibilidade.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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

    /**
     * A tela usa LazyColumn: item fora da área visível não é composto e nem sequer
     * existe na árvore de semântica, então `performScrollTo()` não o encontra.
     * É preciso pedir à lista que role até ele, o que o compõe.
     */
    private fun rolarAte(matcher: SemanticsMatcher) {
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(matcher)
    }

    private fun descricaoDoSeletor(
        gesture: Gesture,
        acao: String,
    ) = "Ação para ${gesture.displayName()}: $acao. Toque para alterar"

    // ── Renderização ────────────────────────────────────────────────────────

    @Test
    fun exibeOsNoveGestosDoVocabulario() {
        montarTela()

        Gesture.entries.forEach { gesture ->
            rolarAte(hasText(gesture.displayName()))
            composeRule.onNodeWithText(gesture.displayName()).assertIsDisplayed()
        }
    }

    @Test
    fun exibeAAcaoMapeadaDeCadaGesto() {
        montarTela()

        val descricao = descricaoDoSeletor(Gesture.NOD, SpotifyAction.PLAY_PAUSE.displayName())
        rolarAte(hasContentDescription(descricao))

        composeRule.onNodeWithContentDescription(descricao).assertIsDisplayed()
    }

    @Test
    fun gestoSemAcaoExibeORotuloDeSemAcao() {
        montarTela()

        val descricao = descricaoDoSeletor(Gesture.BLINK_LEFT_EYE, NO_ACTION_LABEL)
        rolarAte(hasContentDescription(descricao))

        composeRule.onNodeWithContentDescription(descricao).assertIsDisplayed()
    }

    // ── Edição do mapeamento ────────────────────────────────────────────────

    @Test
    fun escolherUmaAcaoNotificaOGestoEORotuloCorretos() {
        val recebidos = mutableListOf<Pair<Gesture, String>>()
        montarTela(onMappingChanged = { g, a -> recebidos += g to a })

        // Abre o seletor do gesto "Aceno (sim)"
        val descricao = descricaoDoSeletor(Gesture.NOD, SpotifyAction.PLAY_PAUSE.displayName())
        rolarAte(hasContentDescription(descricao))
        composeRule.onNodeWithContentDescription(descricao).performClick()

        // Escolhe uma ação LIVRE. "Próxima Faixa" não serve: no estado de teste ela
        // já pertence a "Virar para Direita" e por isso aparece bloqueada.
        composeRule
            .onNodeWithContentDescription(SpotifyAction.VOLUME_UP.displayName())
            .performClick()

        assertEquals(1, recebidos.size)
        assertEquals(Gesture.NOD, recebidos.first().first)
        assertEquals(SpotifyAction.VOLUME_UP.displayName(), recebidos.first().second)
    }

    // ── Exclusividade de ação (RN02) ────────────────────────────────────────

    /**
     * Ação já usada por outro gesto aparece no menu, mas desabilitada e dizendo
     * quem a ocupa. Some da lista seria pior: o usuário não saberia que a ação
     * existe nem onde foi parar.
     */
    @Test
    fun acaoUsadaPorOutroGestoApareceBloqueadaComODono() {
        montarTela()

        val seletor = descricaoDoSeletor(Gesture.NOD, SpotifyAction.PLAY_PAUSE.displayName())
        rolarAte(hasContentDescription(seletor))
        composeRule.onNodeWithContentDescription(seletor).performClick()

        val bloqueada =
            "${SpotifyAction.NEXT_TRACK.displayName()}, já usada por ${Gesture.TURN_FACE_RIGHT.displayName()}"
        composeRule.onNodeWithContentDescription(bloqueada).assertIsDisplayed()
    }

    @Test
    fun tocarEmAcaoBloqueadaNaoAlteraOMapeamento() {
        val recebidos = mutableListOf<Pair<Gesture, String>>()
        montarTela(onMappingChanged = { g, a -> recebidos += g to a })

        val seletor = descricaoDoSeletor(Gesture.NOD, SpotifyAction.PLAY_PAUSE.displayName())
        rolarAte(hasContentDescription(seletor))
        composeRule.onNodeWithContentDescription(seletor).performClick()

        val bloqueada =
            "${SpotifyAction.NEXT_TRACK.displayName()}, já usada por ${Gesture.TURN_FACE_RIGHT.displayName()}"
        composeRule.onNodeWithContentDescription(bloqueada).performClick()

        assertTrue("Ação bloqueada não deveria alterar nada: $recebidos", recebidos.isEmpty())
    }

    /** Desligar um gesto precisa estar sempre ao alcance, nunca bloqueado. */
    @Test
    fun semAcaoNuncaApareceBloqueada() {
        montarTela()

        val seletor = descricaoDoSeletor(Gesture.NOD, SpotifyAction.PLAY_PAUSE.displayName())
        rolarAte(hasContentDescription(seletor))
        composeRule.onNodeWithContentDescription(seletor).performClick()

        composeRule.onNodeWithContentDescription(NO_ACTION_LABEL).assertIsDisplayed()
    }

    @Test
    fun podeEscolherSemAcaoParaDesativarUmGesto() {
        // Acessibilidade: o usuário precisa poder desligar um gesto que não
        // consegue executar de forma confiável.
        val recebidos = mutableListOf<Pair<Gesture, String>>()
        montarTela(onMappingChanged = { g, a -> recebidos += g to a })

        val descricao = descricaoDoSeletor(Gesture.NOD, SpotifyAction.PLAY_PAUSE.displayName())
        rolarAte(hasContentDescription(descricao))
        composeRule.onNodeWithContentDescription(descricao).performClick()

        composeRule.onNodeWithContentDescription(NO_ACTION_LABEL).performClick()

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

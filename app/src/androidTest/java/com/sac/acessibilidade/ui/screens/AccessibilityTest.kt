package com.sac.acessibilidade.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sac.acessibilidade.domain.gesture.Gesture
import com.sac.acessibilidade.domain.gesture.NO_ACTION_LABEL
import com.sac.acessibilidade.domain.gesture.displayName
import com.sac.acessibilidade.ui.theme.NodifyTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testes de acessibilidade das telas — requisito não-funcional do projeto.
 *
 * O usuário-alvo tem limitação motora e pode depender de leitor de tela. Duas
 * regras são verificadas de forma automática aqui:
 *
 * 1. Todo elemento clicável expõe um rótulo para o TalkBack (`contentDescription`
 *    ou texto), senão o leitor anuncia apenas "botão".
 * 2. Todo alvo de toque tem no mínimo 48dp × 48dp (Material Design), senão fica
 *    inatingível para quem tem controle motor fino reduzido.
 *
 * O que **não** dá para automatizar (contraste percebido, tempo real de
 * navegação com TalkBack ligado) está no roteiro de `Docs/testes-usabilidade.md`.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val alvoMinimo = 48.dp

    private fun ComposeContentTestRule.nosClicaveis(): SemanticsNodeInteractionCollection =
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))

    /** Todo clicável precisa anunciar alguma coisa ao leitor de tela. */
    private fun ComposeContentTestRule.assertTodosClicaveisTemRotulo() {
        val nos = nosClicaveis().fetchSemanticsNodes()
        assertTrue("A tela não tem nenhum elemento clicável", nos.isNotEmpty())

        val semRotulo =
            nos.filter { no ->
                val descricao = no.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
                val texto = no.config.getOrNull(SemanticsProperties.Text).orEmpty()
                descricao.none { it.isNotBlank() } && texto.none { it.text.isNotBlank() }
            }

        assertTrue(
            "Elementos clicáveis sem rótulo para o TalkBack: " +
                semRotulo.map { it.id },
            semRotulo.isEmpty(),
        )
    }

    /** Todo alvo de toque precisa caber no dedo. */
    private fun ComposeContentTestRule.assertAlvosDeToqueTemQuarentaEOitoDp() {
        val total = nosClicaveis().fetchSemanticsNodes().size
        repeat(total) { indice ->
            nosClicaveis()[indice]
                .assertHeightIsAtLeast(alvoMinimo)
                .assertWidthIsAtLeast(alvoMinimo)
        }
    }

    // ── Login ───────────────────────────────────────────────────────────────

    @Test
    fun loginTemRotulosParaLeitorDeTela() {
        composeRule.setContent { NodifyTheme { LoginScreen() } }

        composeRule.assertTodosClicaveisTemRotulo()
    }

    @Test
    fun loginTemAlvosDeToqueAdequados() {
        composeRule.setContent { NodifyTheme { LoginScreen() } }

        composeRule.assertAlvosDeToqueTemQuarentaEOitoDp()
    }

    // ── Home ────────────────────────────────────────────────────────────────

    @Test
    fun homeTemRotulosParaLeitorDeTela() {
        composeRule.setContent { NodifyTheme { HomeScreen(uiState = HomeUiState(userName = "Pedro")) } }

        composeRule.assertTodosClicaveisTemRotulo()
    }

    @Test
    fun homeTemAlvosDeToqueAdequados() {
        composeRule.setContent { NodifyTheme { HomeScreen(uiState = HomeUiState(userName = "Pedro")) } }

        composeRule.assertAlvosDeToqueTemQuarentaEOitoDp()
    }

    // ── Configuração de gestos ──────────────────────────────────────────────

    @Test
    fun configuracaoDeGestosTemRotulosParaLeitorDeTela() {
        composeRule.setContent {
            NodifyTheme {
                GestureConfigScreen(
                    uiState = GestureConfigUiState(mappings = linhasDeGesto()),
                    onMappingChanged = { _, _ -> },
                    onSaveClick = {},
                    onRestoreDefaults = {},
                    onBack = {},
                )
            }
        }

        composeRule.assertTodosClicaveisTemRotulo()
    }

    @Test
    fun seletorDeAcaoAnunciaGestoEAcaoAtual() {
        // O anúncio precisa dizer qual gesto está sendo configurado — só
        // "Tocar / Pausar" não diz nada a quem navega com TalkBack.
        composeRule.setContent {
            NodifyTheme {
                GestureConfigScreen(
                    uiState = GestureConfigUiState(mappings = linhasDeGesto()),
                    onMappingChanged = { _, _ -> },
                    onSaveClick = {},
                    onRestoreDefaults = {},
                    onBack = {},
                )
            }
        }

        val descricoes =
            composeRule
                .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
                .fetchSemanticsNodes()
                .flatMap { it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() }

        assertTrue(
            "Nenhum seletor anuncia o gesto que está sendo configurado",
            descricoes.any { it.startsWith("Ação para ${Gesture.TILT_HEAD_RIGHT.displayName()}:") },
        )
    }

    // ── Rastreamento ativo ──────────────────────────────────────────────────

    @Test
    fun rastreamentoAtivoTemRotulosParaLeitorDeTela() {
        composeRule.mainClock.autoAdvance = false // ponto pulsante é animação infinita
        composeRule.setContent {
            NodifyTheme {
                PlayerAtivoScreen(
                    uiState =
                        PlayerAtivoUiState(
                            trackTitle = "Garota de Ipanema",
                            trackArtist = "Tom Jobim",
                            isLoading = false,
                        ),
                    gestureProcessor = null,
                )
            }
        }

        composeRule.assertTodosClicaveisTemRotulo()
    }

    @Test
    fun rastreamentoAtivoTemAlvosDeToqueAdequados() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            NodifyTheme {
                PlayerAtivoScreen(
                    uiState =
                        PlayerAtivoUiState(
                            trackTitle = "Garota de Ipanema",
                            trackArtist = "Tom Jobim",
                            isLoading = false,
                        ),
                    gestureProcessor = null,
                )
            }
        }

        composeRule.assertAlvosDeToqueTemQuarentaEOitoDp()
    }

    private fun linhasDeGesto(): List<GestureMappingUi> =
        Gesture.entries.map { gesture ->
            GestureMappingUi(
                gesture = gesture,
                gestureName = gesture.displayName(),
                selectedAction = NO_ACTION_LABEL,
                icon = Icons.Default.Face,
            )
        }
}

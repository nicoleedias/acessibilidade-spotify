package com.sac.acessibilidade.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import com.sac.acessibilidade.domain.gesture.Gesture
import com.sac.acessibilidade.domain.gesture.NO_ACTION_LABEL
import com.sac.acessibilidade.domain.gesture.SpotifyAction
import com.sac.acessibilidade.domain.gesture.displayName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exclusividade de ação na configuração de gestos (UC03, RN02).
 *
 * Uma ação pertence a no máximo um gesto. Se "Próxima Faixa" já está em
 * "Virar para Direita", nenhum outro gesto pode escolhê-la enquanto ela não for
 * liberada — senão dois movimentos disputariam o mesmo comando e o usuário não
 * teria como saber qual venceria.
 */
class GestureConfigUiStateTest {
    private fun linha(
        gesture: Gesture,
        acao: String = NO_ACTION_LABEL,
    ) = GestureMappingUi(
        gesture = gesture,
        gestureName = gesture.displayName(),
        selectedAction = acao,
        icon = Icons.Default.Face,
    )

    /** Todos os nove gestos, com as atribuições informadas e o resto sem ação. */
    private fun estado(vararg atribuicoes: Pair<Gesture, SpotifyAction>): GestureConfigUiState {
        val mapa = atribuicoes.toMap()
        return GestureConfigUiState(
            mappings = Gesture.entries.map { linha(it, mapa[it]?.displayName() ?: NO_ACTION_LABEL) },
        )
    }

    // ── Quem ocupa cada ação ─────────────────────────────────────────────

    @Test
    fun `acao atribuida aponta para o gesto que a usa`() {
        val state = estado(Gesture.TURN_FACE_RIGHT to SpotifyAction.NEXT_TRACK)

        assertEquals(
            Gesture.TURN_FACE_RIGHT.displayName(),
            state.actionOwners[SpotifyAction.NEXT_TRACK.displayName()],
        )
    }

    /** Vários gestos podem estar sem ação ao mesmo tempo — isso não é conflito. */
    @Test
    fun `sem acao nunca conta como ocupada`() {
        val state = estado()

        assertTrue(state.actionOwners.isEmpty())
        assertNull(state.actionOwners[NO_ACTION_LABEL])
    }

    // ── Bloqueio ─────────────────────────────────────────────────────────

    @Test
    fun `acao usada por outro gesto fica bloqueada e informa o dono`() {
        val state = estado(Gesture.TURN_FACE_RIGHT to SpotifyAction.NEXT_TRACK)

        val dono = state.blockedBy(Gesture.NOD, SpotifyAction.NEXT_TRACK.displayName())

        assertEquals(Gesture.TURN_FACE_RIGHT.displayName(), dono)
    }

    /** Sem isto o usuário não conseguiria ver qual é a escolha atual dele próprio. */
    @Test
    fun `a propria acao do gesto continua selecionavel`() {
        val state = estado(Gesture.NOD to SpotifyAction.PLAY_PAUSE)

        assertNull(state.blockedBy(Gesture.NOD, SpotifyAction.PLAY_PAUSE.displayName()))
    }

    @Test
    fun `acao livre nao e bloqueada`() {
        val state = estado(Gesture.NOD to SpotifyAction.PLAY_PAUSE)

        assertNull(state.blockedBy(Gesture.TILT_HEAD_UP, SpotifyAction.VOLUME_UP.displayName()))
    }

    /** Desligar um gesto precisa estar sempre disponível. */
    @Test
    fun `sem acao nunca e bloqueada para ninguem`() {
        val state =
            estado(
                Gesture.NOD to SpotifyAction.PLAY_PAUSE,
                Gesture.TURN_FACE_RIGHT to SpotifyAction.NEXT_TRACK,
            )

        Gesture.entries.forEach { gesture ->
            assertNull(
                "NO_ACTION bloqueada para $gesture",
                state.blockedBy(gesture, NO_ACTION_LABEL),
            )
        }
    }

    @Test
    fun `liberar a acao de um gesto desbloqueia para os demais`() {
        val ocupada = estado(Gesture.TURN_FACE_RIGHT to SpotifyAction.NEXT_TRACK)
        assertEquals(
            Gesture.TURN_FACE_RIGHT.displayName(),
            ocupada.blockedBy(Gesture.NOD, SpotifyAction.NEXT_TRACK.displayName()),
        )

        val liberada = estado()

        assertNull(liberada.blockedBy(Gesture.NOD, SpotifyAction.NEXT_TRACK.displayName()))
    }

    @Test
    fun `cada acao atribuida bloqueia apenas a si mesma`() {
        val state =
            estado(
                Gesture.NOD to SpotifyAction.PLAY_PAUSE,
                Gesture.TURN_FACE_RIGHT to SpotifyAction.NEXT_TRACK,
            )

        assertNull(state.blockedBy(Gesture.TILT_HEAD_UP, SpotifyAction.VOLUME_UP.displayName()))
        assertEquals(
            Gesture.NOD.displayName(),
            state.blockedBy(Gesture.TILT_HEAD_UP, SpotifyAction.PLAY_PAUSE.displayName()),
        )
    }

    // ── Progresso da configuração ────────────────────────────────────────

    @Test
    fun `contagem reflete apenas as acoes atribuidas`() {
        val state =
            estado(
                Gesture.NOD to SpotifyAction.PLAY_PAUSE,
                Gesture.TURN_FACE_RIGHT to SpotifyAction.NEXT_TRACK,
            )

        assertEquals(2, state.assignedCount)
        assertEquals(SpotifyAction.entries.size, state.totalActions)
    }

    @Test
    fun `primeiro uso e sinalizado como sem nenhum mapeamento`() {
        assertTrue(estado().hasNoMapping)
    }

    @Test
    fun `um gesto configurado ja tira o estado de vazio`() {
        assertFalse(estado(Gesture.NOD to SpotifyAction.PLAY_PAUSE).hasNoMapping)
    }

    /** Lista ainda não carregada não é "sem mapeamento" — não há o que avisar. */
    @Test
    fun `lista vazia enquanto carrega nao dispara o aviso`() {
        assertFalse(GestureConfigUiState().hasNoMapping)
    }
}

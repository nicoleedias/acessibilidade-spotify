package com.sac.acessibilidade.domain.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Os rótulos são o contrato entre a tela de mapeamento (UC03) e o domínio:
 * `GestureConfigViewModel.save()` converte o rótulo escolhido de volta para
 * [SpotifyAction] via [toSpotifyActionOrNull]. Qualquer quebra desse round-trip
 * faz o app salvar "sem ação" silenciosamente.
 */
class GestureDisplayNamesTest {
    // ── Round-trip rótulo ↔ ação ────────────────────────────────────────────

    @Test
    fun `toda acao volta a si mesma pelo rotulo`() {
        SpotifyAction.entries.forEach { action ->
            assertEquals(action, action.displayName().toSpotifyActionOrNull())
        }
    }

    @Test
    fun `rotulo de sem acao nao mapeia para nenhuma acao`() {
        assertNull(NO_ACTION_LABEL.toSpotifyActionOrNull())
    }

    @Test
    fun `rotulo desconhecido nao mapeia para nenhuma acao`() {
        assertNull("Rótulo inexistente".toSpotifyActionOrNull())
        assertNull("".toSpotifyActionOrNull())
    }

    @Test
    fun `conversao de rotulo e sensivel a maiusculas e espacos`() {
        // Documenta a comparação exata de `toSpotifyActionOrNull`: o rótulo tem
        // de vir de `displayName()`, nunca ser digitado à mão em outro ponto.
        val exato = SpotifyAction.PLAY_PAUSE.displayName()
        assertNull(exato.lowercase().toSpotifyActionOrNull())
        assertNull(exato.trim().replace(" ", "").toSpotifyActionOrNull())
    }

    // ── Exaustividade e unicidade ───────────────────────────────────────────

    @Test
    fun `todos os nove gestos possuem rotulo nao vazio`() {
        assertEquals(9, Gesture.entries.size)
        Gesture.entries.forEach { gesture ->
            assertTrue(
                "Gesto ${gesture.name} está sem rótulo",
                gesture.displayName().isNotBlank(),
            )
        }
    }

    @Test
    fun `todas as cinco acoes possuem rotulo nao vazio`() {
        assertEquals(5, SpotifyAction.entries.size)
        SpotifyAction.entries.forEach { action ->
            assertTrue(
                "Ação ${action.name} está sem rótulo",
                action.displayName().isNotBlank(),
            )
        }
    }

    @Test
    fun `rotulos de gesto sao unicos`() {
        val labels = Gesture.entries.map { it.displayName() }
        assertEquals("Há rótulos de gesto duplicados: $labels", labels.size, labels.toSet().size)
    }

    @Test
    fun `rotulos de acao sao unicos`() {
        // Rótulo duplicado faria toSpotifyActionOrNull devolver sempre a primeira
        // ação da lista, remapeando o gesto do usuário para o comando errado.
        val labels = SpotifyAction.entries.map { it.displayName() }
        assertEquals("Há rótulos de ação duplicados: $labels", labels.size, labels.toSet().size)
    }

    @Test
    fun `nenhuma acao usa o rotulo reservado de sem acao`() {
        assertTrue(SpotifyAction.entries.none { it.displayName() == NO_ACTION_LABEL })
    }
}

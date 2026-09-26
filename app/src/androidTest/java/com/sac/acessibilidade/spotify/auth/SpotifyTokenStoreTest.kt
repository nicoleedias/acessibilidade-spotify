package com.sac.acessibilidade.spotify.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sac.acessibilidade.spotify.auth.model.SpotifyTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integração com `EncryptedSharedPreferences` (só funciona com o Keystore do
 * dispositivo, por isso é teste instrumentado).
 *
 * Cobre dois pontos sensíveis: o armazenamento cifrado dos tokens OAuth e o
 * consumo de uso único do parâmetro `state`, que é a proteção contra replay/CSRF
 * no redirect do Spotify.
 */
@RunWith(AndroidJUnit4::class)
class SpotifyTokenStoreTest {
    private lateinit var store: SpotifyTokenStore

    @Before
    fun limparCofre() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        store = SpotifyTokenStore(context)
        store.clearAll()
    }

    private fun tokens(expiresAt: Long) =
        SpotifyTokens(
            accessToken = "access-abc",
            refreshToken = "refresh-xyz",
            expiresAt = expiresAt,
        )

    // ── Tokens ──────────────────────────────────────────────────────────────

    @Test
    fun cofreVazioNaoDevolveTokens() {
        assertNull(store.getTokens())
        assertNull(store.getAccessToken())
        assertNull(store.getRefreshToken())
    }

    @Test
    fun gravaELeTokens() {
        val esperado = tokens(expiresAt = Long.MAX_VALUE)

        store.saveTokens(esperado)

        assertEquals(esperado, store.getTokens())
        assertEquals("access-abc", store.getAccessToken())
        assertEquals("refresh-xyz", store.getRefreshToken())
    }

    @Test
    fun tokenComValidadeFuturaEhValido() {
        store.saveTokens(tokens(expiresAt = System.currentTimeMillis() + 60_000L))

        assertTrue(store.isTokenValid())
    }

    @Test
    fun tokenVencidoNaoEhValido() {
        store.saveTokens(tokens(expiresAt = System.currentTimeMillis() - 1_000L))

        assertFalse(store.isTokenValid())
    }

    @Test
    fun cofreVazioNaoTemTokenValido() {
        assertFalse(store.isTokenValid())
    }

    @Test
    fun novoLoginSobrescreveOsTokensAnteriores() {
        store.saveTokens(tokens(expiresAt = 1L))

        store.saveTokens(
            SpotifyTokens(accessToken = "novo", refreshToken = "novo-refresh", expiresAt = 2L),
        )

        assertEquals("novo", store.getAccessToken())
        assertEquals("novo-refresh", store.getRefreshToken())
    }

    // ── PKCE ────────────────────────────────────────────────────────────────

    @Test
    fun gravaLeELimpaOVerificadorPkce() {
        assertNull(store.getPkceVerifier())

        store.savePkceVerifier("verificador-de-teste")
        assertEquals("verificador-de-teste", store.getPkceVerifier())

        store.clearPkceVerifier()
        assertNull(store.getPkceVerifier())
    }

    @Test
    fun limparVerificadorNaoApagaOsTokens() {
        store.saveTokens(tokens(expiresAt = Long.MAX_VALUE))
        store.savePkceVerifier("verificador")

        store.clearPkceVerifier()

        assertEquals("access-abc", store.getAccessToken())
    }

    // ── Proteção contra replay do `state` ───────────────────────────────────

    @Test
    fun estadoOAuthEhDeUsoUnico() {
        // Uma segunda leitura tem de vir vazia: é isso que faz um redirect
        // repetido (replay) falhar em `verifyAndConsumeState`.
        store.saveOAuthState("estado-aleatorio")

        assertEquals("estado-aleatorio", store.consumeOAuthState())
        assertNull(store.consumeOAuthState())
    }

    @Test
    fun consumirEstadoInexistenteDevolveNulo() {
        assertNull(store.consumeOAuthState())
    }

    @Test
    fun novoEstadoSobrescreveOAnterior() {
        store.saveOAuthState("primeiro")
        store.saveOAuthState("segundo")

        assertEquals("segundo", store.consumeOAuthState())
    }

    // ── Direito de eliminação (LGPD) ────────────────────────────────────────

    @Test
    fun limparTudoRemoveTokensVerificadorEEstado() {
        store.saveTokens(tokens(expiresAt = Long.MAX_VALUE))
        store.savePkceVerifier("verificador")
        store.saveOAuthState("estado")

        store.clearAll()

        assertNull(store.getTokens())
        assertNull(store.getPkceVerifier())
        assertNull(store.consumeOAuthState())
        assertFalse(store.isTokenValid())
    }
}

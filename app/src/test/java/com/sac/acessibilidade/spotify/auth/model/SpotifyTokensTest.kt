package com.sac.acessibilidade.spotify.auth.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `isExpired()` decide se o `SpotifyAuthInterceptor` renova o token antes de
 * cada chamada. Um erro de fronteira aqui gera um 401 evitável no meio de um
 * comando de gesto — exatamente o que quebra a meta de latência de 500 ms.
 */
class SpotifyTokensTest {
    private fun tokens(expiresAt: Long) =
        SpotifyTokens(
            accessToken = "access",
            refreshToken = "refresh",
            expiresAt = expiresAt,
        )

    @Test
    fun `token com validade no passado esta expirado`() {
        assertTrue(tokens(expiresAt = 0L).isExpired())
    }

    @Test
    fun `token com validade distante nao esta expirado`() {
        assertFalse(tokens(expiresAt = Long.MAX_VALUE).isExpired())
    }

    @Test
    fun `token que expira exatamente agora conta como expirado`() {
        // A comparação é `>=`: no instante exato do vencimento já renovamos.
        val agora = System.currentTimeMillis()
        assertTrue(tokens(expiresAt = agora).isExpired())
    }

    @Test
    fun `token com folga de um minuto ainda e valido`() {
        val daquiUmMinuto = System.currentTimeMillis() + 60_000L
        assertFalse(tokens(expiresAt = daquiUmMinuto).isExpired())
    }

    @Test
    fun `campos do token sao preservados`() {
        val t = tokens(expiresAt = 123L)
        assertTrue(t.accessToken == "access")
        assertTrue(t.refreshToken == "refresh")
        assertTrue(t.expiresAt == 123L)
    }
}

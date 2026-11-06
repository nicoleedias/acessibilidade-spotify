package com.sac.acessibilidade.spotify.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyForbiddenTest {
    private fun body(
        reason: String,
        message: String = "Player command failed",
    ) = """{"error":{"status":403,"message":"$message","reason":"$reason"}}"""

    @Test
    fun `corpo ausente mantem a mensagem de Premium`() {
        assertEquals(SpotifyForbidden.PREMIUM_REQUIRED, SpotifyForbidden.messageFor(null))
    }

    @Test
    fun `PREMIUM_REQUIRED explica que falta Premium`() {
        assertEquals(SpotifyForbidden.PREMIUM_REQUIRED, SpotifyForbidden.messageFor(body("PREMIUM_REQUIRED")))
    }

    @Test
    fun `volume bloqueado no dispositivo nao culpa a conta`() {
        val message = SpotifyForbidden.messageFor(body("VOLUME_CONTROL_DISALLOW", "Cannot control device volume"))
        assertEquals(SpotifyForbidden.VOLUME_DISALLOWED, message)
    }

    @Test
    fun `motivo desconhecido mostra o motivo real`() {
        val message = SpotifyForbidden.messageFor(body("SOMETHING_NEW", "Nope"))
        assertTrue(message, message.contains("SOMETHING_NEW") && message.contains("Nope"))
    }

    @Test
    fun `corpo que nao e json cai na mensagem de Premium`() {
        assertEquals(SpotifyForbidden.PREMIUM_REQUIRED, SpotifyForbidden.messageFor("<html>403</html>"))
    }
}

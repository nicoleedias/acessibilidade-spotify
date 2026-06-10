package com.sac.acessibilidade.spotify.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.util.Base64

class PkceUtilsTest {
    @Test
    fun `generateVerifier retorna string base64url sem padding com comprimento valido`() {
        val verifier = PkceUtils.generateVerifier()
        // 64 bytes → base64url sem padding = 86 caracteres
        assertEquals(86, verifier.length)
        assertTrue("Não deve conter '+' (base64 padrão)", !verifier.contains('+'))
        assertTrue("Não deve conter '/' (base64 padrão)", !verifier.contains('/'))
        assertTrue("Não deve conter '=' (padding)", !verifier.contains('='))
    }

    @Test
    fun `generateVerifier retorna valores diferentes a cada chamada`() {
        val v1 = PkceUtils.generateVerifier()
        val v2 = PkceUtils.generateVerifier()
        assertTrue("Dois verifiers não devem ser iguais", v1 != v2)
    }

    @Test
    fun `generateChallenge retorna sha256 base64url do verifier`() {
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        val challenge = PkceUtils.generateChallenge(verifier)

        // Verifica manualmente: SHA-256 do verifier, base64url sem padding
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(verifier.toByteArray(Charsets.US_ASCII))
        val expected = Base64.getUrlEncoder().withoutPadding().encodeToString(hash)

        assertEquals(expected, challenge)
    }

    @Test
    fun `generateChallenge nao contem caracteres invalidos para URL`() {
        val verifier = PkceUtils.generateVerifier()
        val challenge = PkceUtils.generateChallenge(verifier)
        assertTrue(!challenge.contains('+'))
        assertTrue(!challenge.contains('/'))
        assertTrue(!challenge.contains('='))
    }
}

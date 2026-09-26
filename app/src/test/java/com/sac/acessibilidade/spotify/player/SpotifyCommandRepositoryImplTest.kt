package com.sac.acessibilidade.spotify.player

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Tradução de respostas HTTP do Spotify em `Result` de domínio (UC01).
 *
 * As mensagens de erro são exibidas direto ao usuário na `PlayerAtivoScreen`,
 * então o texto exato faz parte do contrato: um 403 precisa explicar que falta
 * Premium, e um 404 precisa dizer para abrir o Spotify — não "HTTP 404".
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SpotifyCommandRepositoryImplTest {
    private lateinit var api: SpotifyPlayerApi
    private lateinit var repository: SpotifyCommandRepositoryImpl

    @Before
    fun setUp() {
        api = mockk()
        repository = SpotifyCommandRepositoryImpl(api)
    }

    private fun sucesso204(): Response<Unit> = Response.success(204, Unit)

    private fun erro(code: Int): Response<Unit> =
        Response.error(code, "".toResponseBody("application/json".toMediaType()))

    // ── Caminho feliz ───────────────────────────────────────────────────────

    @Test
    fun `resposta 204 vira sucesso`() =
        runTest {
            coEvery { api.play(any()) } returns sucesso204()

            assertTrue(repository.play().isSuccess)
        }

    @Test
    fun `resposta 200 tambem vira sucesso`() =
        runTest {
            // Alguns endpoints devolvem 200 em vez de 204; cai no ramo `else`
            // e passa por `isSuccessful`.
            coEvery { api.pause(any()) } returns Response.success(200, Unit)

            assertTrue(repository.pause().isSuccess)
        }

    @Test
    fun `cada comando chama o endpoint correspondente`() =
        runTest {
            coEvery { api.play(any()) } returns sucesso204()
            coEvery { api.pause(any()) } returns sucesso204()
            coEvery { api.skipToNext(any()) } returns sucesso204()
            coEvery { api.skipToPrevious(any()) } returns sucesso204()

            repository.play()
            repository.pause()
            repository.skipToNext()
            repository.skipToPrevious()

            coVerify(exactly = 1) { api.play(any()) }
            coVerify(exactly = 1) { api.pause(any()) }
            coVerify(exactly = 1) { api.skipToNext(any()) }
            coVerify(exactly = 1) { api.skipToPrevious(any()) }
        }

    // ── Mapeamento de erros ─────────────────────────────────────────────────

    @Test
    fun `403 explica que falta Spotify Premium`() =
        runTest {
            coEvery { api.play(any()) } returns erro(403)

            val result = repository.play()

            assertTrue(result.isFailure)
            assertEquals(
                "Spotify Premium necessário para controle de reprodução",
                result.exceptionOrNull()?.message,
            )
        }

    @Test
    fun `404 orienta a abrir o Spotify no celular`() =
        runTest {
            coEvery { api.skipToNext(any()) } returns erro(404)

            val result = repository.skipToNext()

            assertTrue(result.isFailure)
            assertEquals(
                "Nenhum dispositivo ativo. Abra o Spotify no celular primeiro.",
                result.exceptionOrNull()?.message,
            )
        }

    @Test
    fun `outros erros expoem o codigo HTTP`() =
        runTest {
            coEvery { api.pause(any()) } returns erro(500)

            val result = repository.pause()

            assertTrue(result.isFailure)
            assertEquals("HTTP 500", result.exceptionOrNull()?.message)
        }

    @Test
    fun `excecao de rede vira Result de falha e nao propaga`() =
        runTest {
            // Sem conexão o app não pode crashar no meio de um gesto
            coEvery { api.skipToPrevious(any()) } throws java.io.IOException("Sem rede")

            val result = repository.skipToPrevious()

            assertTrue(result.isFailure)
            assertEquals("Sem rede", result.exceptionOrNull()?.message)
        }

    // ── Volume ──────────────────────────────────────────────────────────────

    @Test
    fun `volume acima de 100 e saturado em 100`() =
        runTest {
            val slot = mutableListOf<Int>()
            coEvery { api.setVolume(capture(slot), any<RequestBody>()) } returns sucesso204()

            repository.setVolume(150)

            assertEquals(listOf(100), slot)
        }

    @Test
    fun `volume abaixo de zero e saturado em zero`() =
        runTest {
            val slot = mutableListOf<Int>()
            coEvery { api.setVolume(capture(slot), any<RequestBody>()) } returns sucesso204()

            repository.setVolume(-5)

            assertEquals(listOf(0), slot)
        }

    @Test
    fun `volume dentro da faixa e repassado sem alteracao`() =
        runTest {
            val slot = mutableListOf<Int>()
            coEvery { api.setVolume(capture(slot), any<RequestBody>()) } returns sucesso204()

            repository.setVolume(0)
            repository.setVolume(45)
            repository.setVolume(100)

            assertEquals(listOf(0, 45, 100), slot)
        }

    @Test
    fun `falha ao ajustar volume tambem e mapeada`() =
        runTest {
            coEvery { api.setVolume(any(), any<RequestBody>()) } returns erro(403)

            val result = repository.setVolume(50)

            assertTrue(result.isFailure)
            assertEquals(
                "Spotify Premium necessário para controle de reprodução",
                result.exceptionOrNull()?.message,
            )
        }
}

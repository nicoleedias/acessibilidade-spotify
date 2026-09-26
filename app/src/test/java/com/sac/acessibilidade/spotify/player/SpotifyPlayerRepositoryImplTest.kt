package com.sac.acessibilidade.spotify.player

import com.sac.acessibilidade.spotify.player.model.AlbumImage
import com.sac.acessibilidade.spotify.player.model.AlbumItem
import com.sac.acessibilidade.spotify.player.model.ArtistItem
import com.sac.acessibilidade.spotify.player.model.CurrentlyPlayingResponse
import com.sac.acessibilidade.spotify.player.model.TrackItem
import com.sac.acessibilidade.spotify.player.model.UserProfileResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/** Leitura de estado do player (UC01): faixa atual e perfil do usuário. */
@OptIn(ExperimentalCoroutinesApi::class)
class SpotifyPlayerRepositoryImplTest {
    private lateinit var api: SpotifyPlayerApi
    private lateinit var repository: SpotifyPlayerRepositoryImpl

    @Before
    fun setUp() {
        api = mockk()
        repository = SpotifyPlayerRepositoryImpl(api)
    }

    private fun <T> erro(code: Int): Response<T> =
        Response.error(code, "".toResponseBody("application/json".toMediaType()))

    /**
     * 204 com corpo nulo. Precisa do overload que recebe a resposta bruta do
     * OkHttp: `Response.success(204, null)` é ambíguo para o compilador Kotlin.
     */
    private fun semConteudo204(): Response<CurrentlyPlayingResponse> =
        Response.success(
            null,
            okhttp3.Response
                .Builder()
                .code(204)
                .message("No Content")
                .protocol(Protocol.HTTP_1_1)
                .request(
                    Request
                        .Builder()
                        .url("https://api.spotify.com/v1/me/player/currently-playing")
                        .build(),
                ).build(),
        )

    private fun faixa() =
        CurrentlyPlayingResponse(
            isPlaying = true,
            item =
                TrackItem(
                    name = "Garota de Ipanema",
                    artists = listOf(ArtistItem(name = "Tom Jobim")),
                    album = AlbumItem(images = listOf(AlbumImage(url = "capa", width = 300))),
                ),
        )

    // ── Faixa atual ─────────────────────────────────────────────────────────

    @Test
    fun `204 significa nada tocando e devolve nulo com sucesso`() =
        runTest {
            // 204 é o "silêncio" do Spotify — não é erro.
            coEvery { api.getCurrentlyPlaying() } returns semConteudo204()

            val result = repository.getCurrentlyPlaying()

            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }

    @Test
    fun `200 devolve a faixa desserializada`() =
        runTest {
            coEvery { api.getCurrentlyPlaying() } returns Response.success(faixa())

            val result = repository.getCurrentlyPlaying()

            assertTrue(result.isSuccess)
            assertEquals("Garota de Ipanema", result.getOrNull()?.item?.name)
            assertEquals("Tom Jobim", result.getOrNull()?.item?.artists?.firstOrNull()?.name)
            assertEquals(true, result.getOrNull()?.isPlaying)
        }

    @Test
    fun `erro HTTP na faixa atual vira falha com o codigo`() =
        runTest {
            coEvery { api.getCurrentlyPlaying() } returns erro(500)

            val result = repository.getCurrentlyPlaying()

            assertTrue(result.isFailure)
            assertEquals("HTTP 500", result.exceptionOrNull()?.message)
        }

    @Test
    fun `excecao de rede na faixa atual nao propaga`() =
        runTest {
            coEvery { api.getCurrentlyPlaying() } throws java.io.IOException("Timeout")

            val result = repository.getCurrentlyPlaying()

            assertTrue(result.isFailure)
            assertEquals("Timeout", result.exceptionOrNull()?.message)
        }

    // ── Perfil ──────────────────────────────────────────────────────────────

    @Test
    fun `perfil valido e devolvido`() =
        runTest {
            coEvery { api.getUserProfile() } returns
                Response.success(UserProfileResponse(displayName = "Pedro", id = "pedro123"))

            val result = repository.getUserProfile()

            assertTrue(result.isSuccess)
            assertEquals("Pedro", result.getOrNull()?.displayName)
            assertEquals("pedro123", result.getOrNull()?.id)
        }

    @Test
    fun `perfil com corpo vazio vira falha explicita`() =
        runTest {
            coEvery { api.getUserProfile() } returns Response.success(null)

            val result = repository.getUserProfile()

            assertTrue(result.isFailure)
            assertEquals("Perfil vazio", result.exceptionOrNull()?.message)
        }

    @Test
    fun `401 no perfil vira falha com o codigo`() =
        runTest {
            coEvery { api.getUserProfile() } returns erro(401)

            val result = repository.getUserProfile()

            assertTrue(result.isFailure)
            assertEquals("HTTP 401", result.exceptionOrNull()?.message)
        }
}

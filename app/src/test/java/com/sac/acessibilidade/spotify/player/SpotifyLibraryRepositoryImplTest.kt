package com.sac.acessibilidade.spotify.player

import com.sac.acessibilidade.spotify.player.model.PlaylistItem
import com.sac.acessibilidade.spotify.player.model.PlaylistTrack
import com.sac.acessibilidade.spotify.player.model.PlaylistTrackEntry
import com.sac.acessibilidade.spotify.player.model.PlaylistTracksRef
import com.sac.acessibilidade.spotify.player.model.PlaylistTracksResponse
import com.sac.acessibilidade.spotify.player.model.PlaylistsResponse
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/** Leitura da biblioteca e reprodução a partir de uma playlist (UC01). */
@OptIn(ExperimentalCoroutinesApi::class)
class SpotifyLibraryRepositoryImplTest {
    private lateinit var api: SpotifyPlayerApi
    private lateinit var repository: SpotifyLibraryRepositoryImpl

    @Before
    fun setUp() {
        api = mockk()
        repository = SpotifyLibraryRepositoryImpl(api)
    }

    private fun <T> erro(code: Int): Response<T> =
        Response.error(code, "".toResponseBody("application/json".toMediaType()))

    private fun playlist(
        id: String,
        nome: String,
        total: Int = 0,
    ) = PlaylistItem(
        id = id,
        uri = "spotify:playlist:$id",
        name = nome,
        tracks = PlaylistTracksRef(total),
    )

    private fun faixa(nome: String) = PlaylistTrack(uri = "spotify:track:$nome", name = nome)

    private fun corpoDe(body: RequestBody): String = Buffer().also { body.writeTo(it) }.readUtf8()

    // ── listagem de playlists ────────────────────────────────────────────

    @Test
    fun `playlists da conta sao devolvidas na ordem recebida`() =
        runTest {
            coEvery { api.getPlaylists(any()) } returns
                Response.success(
                    PlaylistsResponse(listOf(playlist("a", "Foco"), playlist("b", "Treino"))),
                )

            val resultado = repository.getPlaylists().getOrThrow()

            assertEquals(listOf("Foco", "Treino"), resultado.map { it.name })
        }

    @Test
    fun `conta sem playlists devolve lista vazia e nao erro`() =
        runTest {
            coEvery { api.getPlaylists(any()) } returns Response.success(PlaylistsResponse())

            assertTrue(repository.getPlaylists().getOrThrow().isEmpty())
        }

    @Test
    fun `sessao expirada vira mensagem para reautenticar`() =
        runTest {
            coEvery { api.getPlaylists(any()) } returns erro(401)

            val erro = repository.getPlaylists().exceptionOrNull()

            assertEquals("Sessão expirada. Entre no Spotify novamente.", erro?.message)
        }

    @Test
    fun `contagem de faixas vem do bloco tracks da playlist`() =
        runTest {
            coEvery { api.getPlaylists(any()) } returns
                Response.success(PlaylistsResponse(listOf(playlist("a", "Foco", total = 37))))

            assertEquals(37, repository.getPlaylists().getOrThrow().first().trackCount)
        }

    // ── faixas de uma playlist ───────────────────────────────────────────

    @Test
    fun `faixas da playlist sao devolvidas na ordem`() =
        runTest {
            coEvery { api.getPlaylistTracks(any(), any()) } returns
                Response.success(
                    PlaylistTracksResponse(
                        listOf(PlaylistTrackEntry(faixa("uma")), PlaylistTrackEntry(faixa("outra"))),
                    ),
                )

            val nomes = repository.getPlaylistTracks("a").getOrThrow().map { it.name }

            assertEquals(listOf("uma", "outra"), nomes)
        }

    /**
     * O Spotify devolve `track: null` para faixa indisponível na região ou removida
     * do catálogo. Mantê-la na lista criaria um item sem nome e sem URI, que ao ser
     * selecionado falharia na reprodução.
     */
    @Test
    fun `faixa indisponivel e descartada em vez de virar item vazio`() =
        runTest {
            coEvery { api.getPlaylistTracks(any(), any()) } returns
                Response.success(
                    PlaylistTracksResponse(
                        listOf(
                            PlaylistTrackEntry(faixa("disponivel")),
                            PlaylistTrackEntry(null),
                            PlaylistTrackEntry(faixa("outra")),
                        ),
                    ),
                )

            val nomes = repository.getPlaylistTracks("a").getOrThrow().map { it.name }

            assertEquals(listOf("disponivel", "outra"), nomes)
        }

    // ── reprodução a partir de um contexto ───────────────────────────────

    @Test
    fun `tocar playlist envia o contexto e a posicao escolhida`() =
        runTest {
            val corpo = slot<RequestBody>()
            coEvery { api.play(capture(corpo)) } returns Response.success(204, Unit)

            repository.playFromContext("spotify:playlist:abc", startIndex = 4).getOrThrow()

            val json = corpoDe(corpo.captured)
            assertTrue(json.contains("\"context_uri\":\"spotify:playlist:abc\""))
            assertTrue(json.contains("\"position\":4"))
        }

    @Test
    fun `tocar do inicio usa posicao zero`() =
        runTest {
            val corpo = slot<RequestBody>()
            coEvery { api.play(capture(corpo)) } returns Response.success(204, Unit)

            repository.playFromContext("spotify:playlist:abc", startIndex = 0).getOrThrow()

            assertTrue(corpoDe(corpo.captured).contains("\"position\":0"))
        }

    @Test
    fun `conta sem Premium vira mensagem explicando a exigencia`() =
        runTest {
            coEvery { api.play(any()) } returns erro(403)

            val erro = repository.playFromContext("spotify:playlist:abc", 0).exceptionOrNull()

            assertEquals("Spotify Premium necessário para controle de reprodução", erro?.message)
        }

    @Test
    fun `sem dispositivo ativo orienta a abrir o Spotify`() =
        runTest {
            coEvery { api.play(any()) } returns erro(404)

            val erro = repository.playFromContext("spotify:playlist:abc", 0).exceptionOrNull()

            assertEquals("Nenhum dispositivo ativo. Abra o Spotify no celular primeiro.", erro?.message)
        }
}

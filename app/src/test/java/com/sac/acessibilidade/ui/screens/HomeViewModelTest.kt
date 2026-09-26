package com.sac.acessibilidade.ui.screens

import androidx.lifecycle.viewModelScope
import com.sac.acessibilidade.spotify.player.SpotifyPlayerRepository
import com.sac.acessibilidade.spotify.player.model.ArtistItem
import com.sac.acessibilidade.spotify.player.model.CurrentlyPlayingResponse
import com.sac.acessibilidade.spotify.player.model.TrackItem
import com.sac.acessibilidade.spotify.player.model.UserProfileResponse
import com.sac.acessibilidade.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Dashboard. No `init` busca o perfil uma vez e entra num laço **infinito** de
 * polling da faixa a cada 10 s.
 *
 * Por causa desse laço, nenhum teste aqui pode usar `advanceUntilIdle()`: a fila
 * do scheduler nunca esvazia e o teste rodaria para sempre. O padrão correto é
 * `runCurrent()` para executar o que está agendado agora e `advanceTimeBy(n)`
 * seguido de `runCurrent()` para avançar um ciclo de polling por vez.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: SpotifyPlayerRepository

    @Before
    fun setUp() {
        repository = mockk()
        coEvery { repository.getUserProfile() } returns
            Result.success(UserProfileResponse(displayName = "Pedro", id = "pedro123"))
        coEvery { repository.getCurrentlyPlaying() } returns Result.success(null)
    }

    private fun faixa(
        nome: String,
        artista: String,
        tocando: Boolean,
    ) = CurrentlyPlayingResponse(
        isPlaying = tocando,
        item = TrackItem(name = nome, artists = listOf(ArtistItem(name = artista))),
    )

    /** Encerra o laço de polling, como faria o `onCleared` no app real. */
    private fun HomeViewModel.encerrar() = viewModelScope.cancel()

    // ── Perfil ──────────────────────────────────────────────────────────────

    @Test
    fun `nome de exibicao do perfil vai para o estado`() =
        runTest {
            val vm = HomeViewModel(repository)
            runCurrent()

            assertEquals("Pedro", vm.uiState.value.userName)
            vm.encerrar()
        }

    @Test
    fun `perfil sem nome de exibicao cai para o id`() =
        runTest {
            coEvery { repository.getUserProfile() } returns
                Result.success(UserProfileResponse(displayName = null, id = "pedro123"))

            val vm = HomeViewModel(repository)
            runCurrent()

            assertEquals("pedro123", vm.uiState.value.userName)
            vm.encerrar()
        }

    @Test
    fun `falha ao buscar o perfil deixa o nome nulo sem crashar`() =
        runTest {
            coEvery { repository.getUserProfile() } returns Result.failure(java.io.IOException("HTTP 401"))

            val vm = HomeViewModel(repository)
            runCurrent()

            assertNull(vm.uiState.value.userName)
            vm.encerrar()
        }

    // ── Faixa atual ─────────────────────────────────────────────────────────

    @Test
    fun `faixa tocando vai para o estado`() =
        runTest {
            coEvery { repository.getCurrentlyPlaying() } returns
                Result.success(faixa("Garota de Ipanema", "Tom Jobim", tocando = true))

            val vm = HomeViewModel(repository)
            runCurrent()

            assertEquals("Garota de Ipanema", vm.uiState.value.nowPlayingTitle)
            assertEquals("Tom Jobim", vm.uiState.value.nowPlayingArtist)
            assertEquals(true, vm.uiState.value.isPlaying)
            vm.encerrar()
        }

    @Test
    fun `nada tocando limpa titulo e artista`() =
        runTest {
            coEvery { repository.getCurrentlyPlaying() } returns Result.success(null)

            val vm = HomeViewModel(repository)
            runCurrent()

            assertNull(vm.uiState.value.nowPlayingTitle)
            assertNull(vm.uiState.value.nowPlayingArtist)
            assertFalse(vm.uiState.value.isPlaying)
            vm.encerrar()
        }

    @Test
    fun `faixa sem artistas nao quebra o estado`() =
        runTest {
            coEvery { repository.getCurrentlyPlaying() } returns
                Result.success(
                    CurrentlyPlayingResponse(
                        isPlaying = true,
                        item = TrackItem(name = "Faixa solta", artists = emptyList()),
                    ),
                )

            val vm = HomeViewModel(repository)
            runCurrent()

            assertEquals("Faixa solta", vm.uiState.value.nowPlayingTitle)
            assertNull(vm.uiState.value.nowPlayingArtist)
            vm.encerrar()
        }

    @Test
    fun `falha ao buscar a faixa e ignorada silenciosamente`() =
        runTest {
            // Comportamento ATUAL: `fetchCurrentTrack` só trata `onSuccess`.
            // A Home não tem campo de erro, então a falha some. Documentado aqui
            // para que uma mudança futura seja deliberada.
            coEvery { repository.getCurrentlyPlaying() } returns
                Result.failure(java.io.IOException("Sem rede"))

            val vm = HomeViewModel(repository)
            runCurrent()

            assertNull(vm.uiState.value.nowPlayingTitle)
            assertFalse(vm.uiState.value.isPlaying)
            vm.encerrar()
        }

    // ── Polling ─────────────────────────────────────────────────────────────

    @Test
    fun `polling refaz a consulta a cada dez segundos`() =
        runTest {
            val vm = HomeViewModel(repository)
            runCurrent()
            coVerify(exactly = 1) { repository.getCurrentlyPlaying() }

            advanceTimeBy(10_000)
            runCurrent()
            coVerify(exactly = 2) { repository.getCurrentlyPlaying() }

            advanceTimeBy(10_000)
            runCurrent()
            coVerify(exactly = 3) { repository.getCurrentlyPlaying() }
            vm.encerrar()
        }

    @Test
    fun `polling nao dispara antes do intervalo completo`() =
        runTest {
            val vm = HomeViewModel(repository)
            runCurrent()

            advanceTimeBy(9_000)
            runCurrent()

            coVerify(exactly = 1) { repository.getCurrentlyPlaying() }
            vm.encerrar()
        }

    @Test
    fun `perfil e buscado uma unica vez e nao entra no polling`() =
        runTest {
            val vm = HomeViewModel(repository)
            runCurrent()

            advanceTimeBy(30_000)
            runCurrent()

            coVerify(exactly = 1) { repository.getUserProfile() }
            vm.encerrar()
        }

    @Test
    fun `polling encerrado para de consultar`() =
        runTest {
            val vm = HomeViewModel(repository)
            runCurrent()
            vm.encerrar()

            advanceTimeBy(30_000)
            runCurrent()

            coVerify(exactly = 1) { repository.getCurrentlyPlaying() }
        }
}

package com.sac.acessibilidade.ui.screens

import android.net.Uri
import com.sac.acessibilidade.spotify.auth.SpotifyAuthRepository
import com.sac.acessibilidade.spotify.auth.model.SpotifyTokens
import com.sac.acessibilidade.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * UC01 — autenticação OAuth 2.0 com PKCE.
 *
 * A ViewModel reage ao código de autorização que chega pelo deep link
 * `sac://callback` e traduz o resultado da troca de token numa máquina de
 * estados que a tela consome. Não precisa de Robolectric: `Uri` só é repassada,
 * nunca inspecionada.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: SpotifyAuthRepository
    private lateinit var authCodes: MutableSharedFlow<String>

    @Before
    fun setUp() {
        authCodes = MutableSharedFlow(extraBufferCapacity = 1)
        repository = mockk(relaxed = true)
        every { repository.authCodeFlow } returns authCodes
        every { repository.isLoggedIn() } returns false
    }

    private fun viewModel() = LoginViewModel(repository)

    private fun tokens() =
        SpotifyTokens(
            accessToken = "access",
            refreshToken = "refresh",
            expiresAt = Long.MAX_VALUE,
        )

    // ── Estado inicial ──────────────────────────────────────────────────────

    @Test
    fun `estado inicial e ocioso`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()

            assertEquals(LoginUiState.Idle, vm.uiState.value)
        }

    @Test
    fun `ja logado reflete o repositorio`() =
        runTest {
            every { repository.isLoggedIn() } returns true

            assertTrue(viewModel().isAlreadyLoggedIn)
        }

    @Test
    fun `nao logado reflete o repositorio`() =
        runTest {
            every { repository.isLoggedIn() } returns false

            assertFalse(viewModel().isAlreadyLoggedIn)
        }

    // ── URI de autorização ──────────────────────────────────────────────────

    @Test
    fun `uri de autorizacao vem do repositorio`() =
        runTest {
            val uri = mockk<Uri>()
            every { repository.buildAuthUri(any(), any()) } returns uri

            assertSame(uri, viewModel().buildAuthUri())
            verify(exactly = 1) { repository.buildAuthUri(any(), any()) }
        }

    // ── Troca de código por token ───────────────────────────────────────────

    @Test
    fun `codigo recebido resulta em sucesso`() =
        runTest {
            coEvery { repository.exchangeCode(any(), any(), any()) } returns Result.success(tokens())
            val vm = viewModel()
            advanceUntilIdle()

            authCodes.emit("codigo-valido")
            advanceUntilIdle()

            assertEquals(LoginUiState.Success, vm.uiState.value)
            coVerify(exactly = 1) { repository.exchangeCode("codigo-valido", any(), any()) }
        }

    @Test
    fun `falha na troca vira estado de erro com a mensagem`() =
        runTest {
            coEvery { repository.exchangeCode(any(), any(), any()) } returns
                Result.failure(java.io.IOException("invalid_grant"))
            val vm = viewModel()
            advanceUntilIdle()

            authCodes.emit("codigo-invalido")
            advanceUntilIdle()

            assertEquals(LoginUiState.Error("invalid_grant"), vm.uiState.value)
        }

    @Test
    fun `falha sem mensagem usa texto padrao`() =
        runTest {
            coEvery { repository.exchangeCode(any(), any(), any()) } returns
                Result.failure(RuntimeException())
            val vm = viewModel()
            advanceUntilIdle()

            authCodes.emit("codigo")
            advanceUntilIdle()

            assertEquals(LoginUiState.Error("Falha na autenticação"), vm.uiState.value)
        }

    @Test
    fun `segundo codigo reprocessa a troca`() =
        runTest {
            coEvery { repository.exchangeCode(any(), any(), any()) } returns
                Result.failure(java.io.IOException("primeira falha"))
            val vm = viewModel()
            advanceUntilIdle()

            authCodes.emit("codigo-1")
            advanceUntilIdle()
            assertTrue(vm.uiState.value is LoginUiState.Error)

            coEvery { repository.exchangeCode(any(), any(), any()) } returns Result.success(tokens())
            authCodes.emit("codigo-2")
            advanceUntilIdle()

            assertEquals(LoginUiState.Success, vm.uiState.value)
            coVerify(exactly = 2) { repository.exchangeCode(any(), any(), any()) }
        }

    // ── Limpeza de erro ─────────────────────────────────────────────────────

    @Test
    fun `limpar erro volta ao estado ocioso`() =
        runTest {
            coEvery { repository.exchangeCode(any(), any(), any()) } returns
                Result.failure(java.io.IOException("boom"))
            val vm = viewModel()
            advanceUntilIdle()
            authCodes.emit("codigo")
            advanceUntilIdle()
            assertTrue(vm.uiState.value is LoginUiState.Error)

            vm.clearError()

            assertEquals(LoginUiState.Idle, vm.uiState.value)
        }
}

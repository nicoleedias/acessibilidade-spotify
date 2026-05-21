package com.sac.acessibilidade.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sac.acessibilidade.BuildConfig
import com.sac.acessibilidade.spotify.auth.SpotifyAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val authRepository: SpotifyAuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
        val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

        val isAlreadyLoggedIn: Boolean get() = authRepository.isLoggedIn()

        init {
            viewModelScope.launch {
                authRepository.authCodeFlow.collect { code ->
                    exchangeCode(code)
                }
            }
        }

        fun buildAuthUri(): Uri =
            authRepository.buildAuthUri(
                clientId = BuildConfig.SPOTIFY_CLIENT_ID,
                redirectUri = BuildConfig.SPOTIFY_REDIRECT_URI,
            )

        private fun exchangeCode(code: String) {
            viewModelScope.launch {
                _uiState.value = LoginUiState.Loading
                authRepository.exchangeCode(
                    code = code,
                    clientId = BuildConfig.SPOTIFY_CLIENT_ID,
                    redirectUri = BuildConfig.SPOTIFY_REDIRECT_URI,
                ).fold(
                    onSuccess = { _uiState.value = LoginUiState.Success },
                    onFailure = { _uiState.value = LoginUiState.Error(it.message ?: "Falha na autenticação") },
                )
            }
        }

        fun clearError() {
            _uiState.value = LoginUiState.Idle
        }
    }

sealed interface LoginUiState {
    data object Idle : LoginUiState

    data object Loading : LoginUiState

    data object Success : LoginUiState

    data class Error(val message: String) : LoginUiState
}

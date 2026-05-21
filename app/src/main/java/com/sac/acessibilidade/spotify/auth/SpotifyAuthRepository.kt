package com.sac.acessibilidade.spotify.auth

import android.net.Uri
import com.sac.acessibilidade.spotify.auth.model.SpotifyTokens
import kotlinx.coroutines.flow.SharedFlow

interface SpotifyAuthRepository {
    /** Emits the auth code received via the OAuth redirect URI. */
    val authCodeFlow: SharedFlow<String>

    /** Builds the Spotify authorization URI and persists the PKCE verifier. */
    fun buildAuthUri(
        clientId: String,
        redirectUri: String,
    ): Uri

    /** Exchanges the auth code for access/refresh tokens. */
    suspend fun exchangeCode(
        code: String,
        clientId: String,
        redirectUri: String,
    ): Result<SpotifyTokens>

    /** Refreshes the access token using the stored refresh token. */
    suspend fun refreshAccessToken(clientId: String): Result<SpotifyTokens>

    /** Called by MainActivity when the redirect URI intent arrives. */
    fun notifyAuthCode(code: String)

    fun isLoggedIn(): Boolean
}

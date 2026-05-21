package com.sac.acessibilidade.spotify.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.sac.acessibilidade.spotify.auth.model.SpotifyTokens
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyTokenStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs =
            EncryptedSharedPreferences.create(
                context,
                "spotify_tokens",
                MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )

        fun saveTokens(tokens: SpotifyTokens) {
            prefs.edit()
                .putString(KEY_ACCESS_TOKEN, tokens.accessToken)
                .putString(KEY_REFRESH_TOKEN, tokens.refreshToken)
                .putLong(KEY_EXPIRES_AT, tokens.expiresAt)
                .apply()
        }

        fun getTokens(): SpotifyTokens? {
            val access = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
            val refresh = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
            val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
            return SpotifyTokens(access, refresh, expiresAt)
        }

        fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

        fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

        fun isTokenValid(): Boolean {
            val access = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return false
            val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
            return access.isNotBlank() && System.currentTimeMillis() < expiresAt
        }

        fun savePkceVerifier(verifier: String) {
            prefs.edit().putString(KEY_PKCE_VERIFIER, verifier).apply()
        }

        fun getPkceVerifier(): String? = prefs.getString(KEY_PKCE_VERIFIER, null)

        fun clearPkceVerifier() {
            prefs.edit().remove(KEY_PKCE_VERIFIER).apply()
        }

        fun clearAll() {
            prefs.edit().clear().apply()
        }

        private companion object {
            const val KEY_ACCESS_TOKEN = "access_token"
            const val KEY_REFRESH_TOKEN = "refresh_token"
            const val KEY_EXPIRES_AT = "expires_at"
            const val KEY_PKCE_VERIFIER = "pkce_verifier"
        }
    }

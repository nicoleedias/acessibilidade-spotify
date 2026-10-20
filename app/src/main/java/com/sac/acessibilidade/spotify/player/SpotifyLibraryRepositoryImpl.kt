package com.sac.acessibilidade.spotify.player

import com.sac.acessibilidade.spotify.player.model.PlaylistItem
import com.sac.acessibilidade.spotify.player.model.PlaylistTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyLibraryRepositoryImpl
    @Inject
    constructor(
        private val api: SpotifyPlayerApi,
    ) : SpotifyLibraryRepository {
        override suspend fun getPlaylists(): Result<List<PlaylistItem>> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val response = api.getPlaylists()
                    if (response.isSuccessful) {
                        response.body()?.items.orEmpty()
                    } else {
                        throw IOException(messageFor(response.code()))
                    }
                }
            }

        override suspend fun getPlaylistTracks(playlistId: String): Result<List<PlaylistTrack>> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val response = api.getPlaylistTracks(playlistId)
                    if (response.isSuccessful) {
                        // track nulo = faixa indisponível na região ou removida do catálogo
                        response.body()?.items?.mapNotNull { it.track }.orEmpty()
                    } else {
                        throw IOException(messageFor(response.code()))
                    }
                }
            }

        override suspend fun playFromContext(
            contextUri: String,
            startIndex: Int,
        ): Result<Unit> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val response = api.play(contextBody(contextUri, startIndex))
                    when (response.code()) {
                        NO_CONTENT -> Unit
                        FORBIDDEN -> throw IOException(PREMIUM_REQUIRED)
                        NOT_FOUND -> throw IOException(NO_ACTIVE_DEVICE)
                        else -> if (!response.isSuccessful) throw IOException("HTTP ${response.code()}")
                    }
                }
            }

        /** Corpo serializado — montar o JSON por concatenação quebraria com aspas no URI. */
        private fun contextBody(
            contextUri: String,
            startIndex: Int,
        ) = Json
            .encodeToString(
                JsonObject.serializer(),
                buildJsonObject {
                    put("context_uri", contextUri)
                    put("offset", JsonObject(mapOf("position" to JsonPrimitive(startIndex))))
                },
            ).toRequestBody(JSON_MEDIA_TYPE)

        private fun messageFor(code: Int): String =
            when (code) {
                FORBIDDEN -> PREMIUM_REQUIRED
                NOT_FOUND -> NO_ACTIVE_DEVICE
                UNAUTHORIZED -> "Sessão expirada. Entre no Spotify novamente."
                else -> "HTTP $code"
            }

        private companion object {
            val JSON_MEDIA_TYPE = "application/json".toMediaType()
            const val NO_CONTENT = 204
            const val UNAUTHORIZED = 401
            const val FORBIDDEN = 403
            const val NOT_FOUND = 404
            const val PREMIUM_REQUIRED = "Spotify Premium necessário para controle de reprodução"
            const val NO_ACTIVE_DEVICE = "Nenhum dispositivo ativo. Abra o Spotify no celular primeiro."
        }
    }

package com.sac.acessibilidade.spotify.player

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Traduz um HTTP 403 do Spotify em mensagem para o usuário.
 *
 * O 403 não significa só "falta Premium": o Spotify o usa também quando o
 * dispositivo não aceita controle de volume pela API (celulares costumam recusar)
 * e em restrições do modo de desenvolvimento. A causa real vem no corpo, em
 * `error.reason`; tratar todo 403 como Premium mandava usuários Premium atrás de
 * um problema que não tinham.
 */
object SpotifyForbidden {
    const val PREMIUM_REQUIRED = "Spotify Premium necessário para controle de reprodução"
    const val VOLUME_DISALLOWED =
        "Este dispositivo não permite ajustar o volume pelo app. Use os botões de volume do celular."

    private const val REASON_PREMIUM = "PREMIUM_REQUIRED"
    private const val REASON_VOLUME = "VOLUME_CONTROL_DISALLOW"

    fun messageFor(errorBody: String?): String {
        val error = parseError(errorBody) ?: return PREMIUM_REQUIRED
        val reason = error["reason"]?.jsonPrimitive?.contentOrNull
        val detail = error["message"]?.jsonPrimitive?.contentOrNull
        return when {
            reason == null && detail == null -> PREMIUM_REQUIRED
            reason == REASON_PREMIUM -> PREMIUM_REQUIRED
            reason == REASON_VOLUME -> VOLUME_DISALLOWED
            else -> "O Spotify recusou o comando (${reason ?: "403"}): ${detail ?: "sem detalhes"}"
        }
    }

    private fun parseError(body: String?): JsonObject? =
        runCatching {
            Json
                .parseToJsonElement(body.orEmpty())
                .jsonObject["error"]
                ?.jsonObject
        }.getOrNull()
}

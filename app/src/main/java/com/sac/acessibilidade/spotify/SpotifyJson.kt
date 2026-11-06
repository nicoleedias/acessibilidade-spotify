package com.sac.acessibilidade.spotify

import kotlinx.serialization.json.Json

/**
 * Configuração única de JSON para a Web API do Spotify.
 *
 * `coerceInputValues`: o Spotify devolve `null` em campos que o app modela como
 * lista/valor com padrão (ex.: `"images": null` em playlists sem capa). Com a
 * coerção, o `null` vira o valor padrão em vez de derrubar a tela da biblioteca.
 */
val spotifyJson: Json =
    Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

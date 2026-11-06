package com.sac.acessibilidade.spotify

import com.sac.acessibilidade.spotify.player.model.PlaylistsResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpotifyJsonTest {
    @Test
    fun `playlist com images nulo nao quebra o parse`() {
        val body =
            """{"items":[{"id":"1","uri":"spotify:playlist:1","name":"Sem capa","images":null,
            |"tracks":{"total":3}}]}
            """.trimMargin()

        val response = spotifyJson.decodeFromString<PlaylistsResponse>(body)

        assertEquals(1, response.items.size)
        assertEquals(emptyList<Any>(), response.items[0].images)
        assertNull(response.items[0].bestImageUrl())
        assertEquals(3, response.items[0].trackCount)
    }
}

package com.sac.acessibilidade.spotify.player.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Seleção da capa do álbum exibida em `PlayerAtivoScreen` e `HomeScreen`.
 * O Spotify devolve a mesma capa em várias resoluções; baixar a maior
 * desperdiça banda e memória num app que já roda visão computacional.
 */
class AlbumItemTest {
    private fun album(vararg images: AlbumImage) = AlbumItem(images = images.toList())

    // ── Escolha por proximidade ─────────────────────────────────────────────

    @Test
    fun `escolhe a imagem de largura exata quando existe`() {
        val item =
            album(
                AlbumImage(url = "g", width = 640),
                AlbumImage(url = "m", width = 300),
                AlbumImage(url = "p", width = 64),
            )
        assertEquals("m", item.bestImageUrl(300))
    }

    @Test
    fun `escolhe a largura mais proxima quando nao ha exata`() {
        val item =
            album(
                AlbumImage(url = "g", width = 640),
                AlbumImage(url = "p", width = 64),
            )
        // |640-300| = 340 vs |64-300| = 236 → a menor vence
        assertEquals("p", item.bestImageUrl(300))
    }

    @Test
    fun `respeita o alvo informado e nao o default`() {
        val item =
            album(
                AlbumImage(url = "g", width = 640),
                AlbumImage(url = "m", width = 300),
            )
        assertEquals("g", item.bestImageUrl(640))
    }

    @Test
    fun `usa 300 como alvo default`() {
        val item =
            album(
                AlbumImage(url = "g", width = 640),
                AlbumImage(url = "m", width = 300),
            )
        assertEquals("m", item.bestImageUrl())
    }

    @Test
    fun `empate entre larguras equidistantes escolhe a primeira da lista`() {
        // |200-300| == |400-300|; `minByOrNull` mantém o primeiro encontrado.
        val item =
            album(
                AlbumImage(url = "menor", width = 200),
                AlbumImage(url = "maior", width = 400),
            )
        assertEquals("menor", item.bestImageUrl(300))
    }

    // ── Fallbacks ───────────────────────────────────────────────────────────

    @Test
    fun `cai na primeira imagem quando nenhuma largura foi informada`() {
        val item =
            album(
                AlbumImage(url = "primeira", width = null),
                AlbumImage(url = "segunda", width = null),
            )
        assertEquals("primeira", item.bestImageUrl(300))
    }

    @Test
    fun `ignora imagens sem largura quando ha ao menos uma com largura`() {
        val item =
            album(
                AlbumImage(url = "sem-largura", width = null),
                AlbumImage(url = "com-largura", width = 300),
            )
        assertEquals("com-largura", item.bestImageUrl(300))
    }

    @Test
    fun `devolve nulo quando a lista de imagens esta vazia`() {
        assertNull(AlbumItem().bestImageUrl(300))
    }

    @Test
    fun `album sem imagens usa a lista vazia como default`() {
        // Faixas de podcast/local files chegam sem `images` no JSON
        assertEquals(emptyList<AlbumImage>(), AlbumItem().images)
    }
}

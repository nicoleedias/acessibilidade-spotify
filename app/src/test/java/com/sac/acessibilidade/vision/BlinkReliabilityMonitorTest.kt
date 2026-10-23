package com.sac.acessibilidade.vision

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Confiabilidade da piscada sob oclusão (óculos escuros).
 * Janela pequena nos testes para manter os casos legíveis.
 */
class BlinkReliabilityMonitorTest {
    private fun monitor(
        janela: Int = 10,
        variacaoMinima: Float = 0.08f,
    ) = BlinkReliabilityMonitor(windowSize = janela, minVariation = variacaoMinima)

    private fun BlinkReliabilityMonitor.alimentar(vararg scores: Float) = scores.map { observe(it) }.last()

    @Test
    fun `comeca confiavel antes de reunir amostras suficientes`() {
        val m = monitor()
        assertTrue(m.isReliable)
    }

    @Test
    fun `janela incompleta nao conclui nada mesmo com sinal plano`() {
        val m = monitor(janela = 10)
        repeat(9) { m.observe(0.02f) }
        assertTrue("não deve acusar antes da janela encher", m.isReliable)
    }

    @Test
    fun `olho visivel e parado ainda varia o suficiente para ser confiavel`() {
        val m = monitor(janela = 10)
        // ruído natural do modelo com o olho aberto
        val resultado = m.alimentar(0.02f, 0.05f, 0.03f, 0.11f, 0.04f, 0.02f, 0.13f, 0.06f, 0.03f, 0.09f)
        assertTrue(resultado)
    }

    @Test
    fun `piscada real produz variacao ampla e mantem confiavel`() {
        val m = monitor(janela = 10)
        val resultado = m.alimentar(0.02f, 0.03f, 0.40f, 0.85f, 0.60f, 0.10f, 0.02f, 0.03f, 0.02f, 0.04f)
        assertTrue(resultado)
    }

    /** Óculos escuros: o modelo não vê a pálpebra e repete o mesmo valor. */
    @Test
    fun `sinal praticamente constante indica olho encoberto`() {
        val m = monitor(janela = 10)
        val resultado = m.alimentar(0.03f, 0.03f, 0.031f, 0.03f, 0.029f, 0.03f, 0.03f, 0.031f, 0.03f, 0.03f)
        assertFalse(resultado)
    }

    @Test
    fun `sinal exatamente constante indica olho encoberto`() {
        val m = monitor(janela = 10)
        assertFalse(m.alimentar(*FloatArray(10) { 0.05f }))
    }

    @Test
    fun `variacao exatamente no limiar ainda e aceita`() {
        val m = monitor(janela = 4, variacaoMinima = 0.08f)
        assertTrue(m.alimentar(0.00f, 0.04f, 0.08f, 0.02f))
    }

    @Test
    fun `variacao logo abaixo do limiar e recusada`() {
        val m = monitor(janela = 4, variacaoMinima = 0.08f)
        assertFalse(m.alimentar(0.00f, 0.03f, 0.079f, 0.02f))
    }

    /** O usuário tira os óculos: o estado precisa voltar sozinho, sem reiniciar o app. */
    @Test
    fun `volta a ser confiavel quando o olho reaparece`() {
        val m = monitor(janela = 4)
        m.alimentar(0.03f, 0.03f, 0.03f, 0.03f)
        assertFalse(m.isReliable)

        m.alimentar(0.50f, 0.02f, 0.40f, 0.01f)
        assertTrue(m.isReliable)
    }

    @Test
    fun `janela deslizante descarta amostras antigas`() {
        val m = monitor(janela = 3)
        m.alimentar(0.90f, 0.01f, 0.90f)
        assertTrue(m.isReliable)
        // as três últimas já são planas, mesmo tendo havido pico antes
        m.alimentar(0.03f, 0.03f, 0.03f)
        assertFalse(m.isReliable)
    }

    @Test
    fun `reset volta ao estado inicial`() {
        val m = monitor(janela = 4)
        m.alimentar(0.03f, 0.03f, 0.03f, 0.03f)
        assertFalse(m.isReliable)

        m.reset()

        assertTrue(m.isReliable)
    }
}

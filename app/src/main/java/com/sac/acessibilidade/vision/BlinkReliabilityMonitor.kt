package com.sac.acessibilidade.vision

/**
 * Observa se a piscada ainda é um gesto utilizável para o usuário atual.
 *
 * O detector facial estima a abertura da pálpebra a partir da imagem. Com óculos
 * escuros — e, em menor grau, lentes muito reflexivas — ele não enxerga o olho e
 * passa a devolver sempre o mesmo valor. O efeito prático é cruel: a piscada
 * simplesmente nunca dispara, e nada na tela explica o motivo.
 *
 * Em vez de olhar um quadro isolado, acompanha a VARIAÇÃO do sinal ao longo de
 * uma janela. Olho visível produz variação natural, mesmo sem piscar de propósito;
 * olho encoberto produz uma linha quase reta.
 *
 * Kotlin puro, sem dependência de Android — testável sem câmera.
 */
class BlinkReliabilityMonitor(
    private val windowSize: Int = DEFAULT_WINDOW,
    private val minVariation: Float = DEFAULT_MIN_VARIATION,
) {
    private val amostras = ArrayDeque<Float>(windowSize)

    /** Até reunir uma janela inteira, não acusa nada: evita alarme no primeiro segundo. */
    var isReliable: Boolean = true
        private set

    /**
     * @param blinkScore maior score de piscada do quadro (olho direito ou esquerdo).
     * @return o estado de confiabilidade após incluir esta amostra.
     */
    fun observe(blinkScore: Float): Boolean {
        amostras.addLast(blinkScore)
        if (amostras.size > windowSize) amostras.removeFirst()
        if (amostras.size < windowSize) return isReliable
        val variacao = (amostras.max() - amostras.min())
        isReliable = variacao >= minVariation
        return isReliable
    }

    fun reset() {
        amostras.clear()
        isReliable = true
    }

    companion object {
        /** ~3 a 6 segundos de vídeo, conforme a taxa de quadros. */
        const val DEFAULT_WINDOW = 90

        /**
         * Abaixo disto a curva é plana demais para ter vindo de um olho visível.
         * Calibrado para ficar bem abaixo da amplitude de uma piscada real (~0,6)
         * e acima do ruído do modelo com o olho aberto e parado.
         */
        const val DEFAULT_MIN_VARIATION = 0.08f
    }
}

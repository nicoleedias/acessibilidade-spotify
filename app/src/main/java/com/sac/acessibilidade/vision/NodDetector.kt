package com.sac.acessibilidade.vision

/**
 * Detecta o gesto NOD (aceno de cabeça) a partir de uma série temporal de pitch.
 *
 * Algoritmo: observa o sinal de pitch em uma janela deslizante de [WINDOW_MS] ms.
 * Um NOD é confirmado quando o pitch:
 *  1. Ultrapassa +[amplitudeDeg] (queixo desce) e depois
 *  2. Volta abaixo de -[amplitudeDeg] (queixo sobe), ou vice-versa,
 * dentro da janela de tempo — caracterizando um ciclo completo.
 *
 * Sem dependências Android — testável com dados sintéticos.
 */
class NodDetector {
    private data class PitchSample(val pitchDeg: Float, val timestampMs: Long)

    private val buffer = ArrayDeque<PitchSample>(MAX_SAMPLES)
    private var lastNodMs = -COOLDOWN_MS

    /**
     * Alimenta um novo valor de pitch e retorna true se um NOD foi detectado.
     * [amplitudeDeg] deve vir dos thresholds de calibração do usuário.
     */
    fun feed(
        pitchDeg: Float,
        nowMs: Long,
        amplitudeDeg: Float,
    ): Boolean {
        buffer.addLast(PitchSample(pitchDeg, nowMs))
        evictOld(nowMs)

        if (nowMs - lastNodMs < COOLDOWN_MS) return false
        if (detectCycle(amplitudeDeg)) {
            lastNodMs = nowMs
            buffer.clear()
            return true
        }
        return false
    }

    private fun evictOld(nowMs: Long) {
        while (buffer.isNotEmpty() && nowMs - buffer.first().timestampMs > WINDOW_MS) {
            buffer.removeFirst()
        }
    }

    /**
     * Verifica se o buffer contém um ciclo pitch-positivo → pitch-negativo
     * (ou o inverso) com amplitude maior que [amplitudeDeg].
     */
    private fun detectCycle(amplitudeDeg: Float): Boolean {
        if (buffer.size < MIN_SAMPLES) return false
        var sawPositive = false
        var sawNegative = false
        for (sample in buffer) {
            if (sample.pitchDeg > amplitudeDeg) sawPositive = true
            if (sample.pitchDeg < -amplitudeDeg) sawNegative = true
        }
        return sawPositive && sawNegative
    }

    companion object {
        private const val WINDOW_MS = 900L
        private const val COOLDOWN_MS = 1_200L
        private const val MAX_SAMPLES = 60
        private const val MIN_SAMPLES = 6
    }
}

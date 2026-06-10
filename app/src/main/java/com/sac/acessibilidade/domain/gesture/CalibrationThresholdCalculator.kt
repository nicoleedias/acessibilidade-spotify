package com.sac.acessibilidade.domain.gesture

import kotlin.math.min

/**
 * Converte os picos de amplitude medidos na calibração (UC02) em [CalibrationThresholds]
 * utilizáveis pelo classificador de gestos.
 *
 * Sem dependências Android — testável com dados sintéticos.
 *
 * Regras de projeto (justificáveis na defesa do TCC):
 * - Usa [THRESHOLD_RATIO] do pico medido: o usuário não precisa repetir a amplitude
 *   máxima a cada gesto, o que melhora a usabilidade — essencial para mobilidade reduzida.
 * - Aplica mínimos de segurança por eixo para evitar falso-positivo em repouso/jitter.
 * - Deriva a amplitude do NOD da menor amplitude de pitch medida (cima/baixo), garantindo
 *   que o aceno exija movimento real dentro do alcance individual do usuário.
 *
 * Todos os picos recebidos já devem ser **relativos à pose neutra** do usuário.
 */
object CalibrationThresholdCalculator {
    data class Peaks(
        val rollRightDeg: Float,
        val rollLeftDeg: Float,
        val pitchUpDeg: Float,
        val pitchDownDeg: Float,
        val yawRightDeg: Float,
        val yawLeftDeg: Float,
    )

    fun build(peaks: Peaks): CalibrationThresholds {
        val pitchRange = min(peaks.pitchUpDeg, peaks.pitchDownDeg)
        val nodAmplitude = (pitchRange * NOD_RATIO).coerceIn(MIN_NOD_DEG, MAX_NOD_DEG)
        return CalibrationThresholds(
            rollRightDeg = threshold(peaks.rollRightDeg, MIN_ROLL_DEG),
            rollLeftDeg = threshold(peaks.rollLeftDeg, MIN_ROLL_DEG),
            pitchUpDeg = threshold(peaks.pitchUpDeg, MIN_PITCH_DEG),
            pitchDownDeg = threshold(peaks.pitchDownDeg, MIN_PITCH_DEG),
            yawRightDeg = threshold(peaks.yawRightDeg, MIN_YAW_DEG),
            yawLeftDeg = threshold(peaks.yawLeftDeg, MIN_YAW_DEG),
            nodPitchAmplitudeDeg = nodAmplitude,
        )
    }

    private fun threshold(
        peak: Float,
        minimum: Float,
    ): Float = (peak * THRESHOLD_RATIO).coerceAtLeast(minimum)

    /** Amplitude mínima (relativa ao neutro) que conta como "usuário chegou ao limite". */
    fun entryMinDegFor(axis: Axis): Float =
        when (axis) {
            Axis.ROLL -> MIN_ROLL_DEG
            Axis.PITCH -> MIN_PITCH_DEG
            Axis.YAW -> MIN_YAW_DEG
        }

    enum class Axis { ROLL, PITCH, YAW }

    const val THRESHOLD_RATIO = 0.75f
    const val NOD_RATIO = 0.6f
    const val MIN_ROLL_DEG = 6f
    const val MIN_PITCH_DEG = 5f
    const val MIN_YAW_DEG = 8f
    const val MIN_NOD_DEG = 8f
    const val MAX_NOD_DEG = 22f
}

package com.sac.acessibilidade.domain.gesture

import kotlin.math.abs
import kotlin.math.min

/**
 * Converte os picos de amplitude medidos na calibração (UC02) em [CalibrationThresholds]
 * utilizáveis pelo classificador de gestos.
 *
 * Sem dependências Android — testável com dados sintéticos.
 *
 * Regras de projeto (justificáveis na defesa do TCC):
 * - **Polaridade aprendida**: os picos chegam COM SINAL, exatamente como o estimador os
 *   produziu enquanto o usuário se movia na direção pedida. O sinal do pico do passo
 *   "direita"/"baixo" define a polaridade do eixo ([CalibrationThresholds.rollSign],
 *   [CalibrationThresholds.pitchSign], [CalibrationThresholds.yawSign]). Assim a
 *   classificação fica agnóstica à convenção da câmera (espelhamento, rotação do
 *   sensor) — nenhuma direção é assumida em código.
 * - Usa [THRESHOLD_RATIO] do pico medido: o usuário não precisa repetir a amplitude
 *   máxima a cada gesto, o que melhora a usabilidade — essencial para mobilidade reduzida.
 * - Aplica mínimos de segurança por eixo para evitar falso-positivo em repouso/jitter.
 * - Deriva a amplitude do NOD da menor amplitude de pitch medida (cima/baixo), garantindo
 *   que o aceno exija movimento real dentro do alcance individual do usuário.
 *
 * Todos os picos recebidos já devem ser **relativos à pose neutra** do usuário.
 */
object CalibrationThresholdCalculator {
    /** Picos COM SINAL medidos em cada passo da calibração. */
    data class Peaks(
        val tiltRight: Float,
        val tiltLeft: Float,
        val tiltUp: Float,
        val tiltDown: Float,
        val turnRight: Float,
        val turnLeft: Float,
    )

    fun build(peaks: Peaks): CalibrationThresholds {
        val pitchRange = min(abs(peaks.tiltUp), abs(peaks.tiltDown))
        val nodAmplitude = (pitchRange * NOD_RATIO).coerceIn(MIN_NOD_DEG, MAX_NOD_DEG)
        return CalibrationThresholds(
            rollRightDeg = threshold(peaks.tiltRight, MIN_ROLL_DEG),
            rollLeftDeg = threshold(peaks.tiltLeft, MIN_ROLL_DEG),
            pitchUpDeg = threshold(peaks.tiltUp, MIN_PITCH_DEG),
            pitchDownDeg = threshold(peaks.tiltDown, MIN_PITCH_DEG),
            yawRightDeg = threshold(peaks.turnRight, MIN_YAW_DEG),
            yawLeftDeg = threshold(peaks.turnLeft, MIN_YAW_DEG),
            nodPitchAmplitudeDeg = nodAmplitude,
            // Polaridade: sinal observado no passo da direção positiva de cada eixo
            // (direita para roll/yaw, baixo para pitch — convenção do classificador).
            rollSign = signOf(peaks.tiltRight),
            pitchSign = signOf(peaks.tiltDown),
            yawSign = signOf(peaks.turnRight),
        )
    }

    private fun threshold(
        signedPeak: Float,
        minimum: Float,
    ): Float = (abs(signedPeak) * THRESHOLD_RATIO).coerceAtLeast(minimum)

    private fun signOf(value: Float): Float = if (value < 0f) -1f else 1f

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

    // Pisos por eixo. Roll usa graus reais (atan2) e atinge amplitude com folga.
    // Yaw/pitch vêm de geometria 2D que encolhe na projeção (virar/levantar o queixo
    // aponta o nariz para a câmera), então recebem pisos menores para que mesmo um
    // movimento pequeno seja calibrável e detectável — essencial para mobilidade reduzida.
    const val MIN_ROLL_DEG = 6f
    const val MIN_PITCH_DEG = 4f
    const val MIN_YAW_DEG = 5f
    const val MIN_NOD_DEG = 8f
    const val MAX_NOD_DEG = 22f
}

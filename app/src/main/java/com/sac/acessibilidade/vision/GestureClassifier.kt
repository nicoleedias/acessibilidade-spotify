package com.sac.acessibilidade.vision

import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.sac.acessibilidade.domain.gesture.CalibrationThresholds
import com.sac.acessibilidade.domain.gesture.Gesture

/**
 * Classifica gestos cefálicos a partir dos landmarks e blendshapes do MediaPipe.
 *
 * Sem dependências Android (Context/Activity) — testável com dados sintéticos.
 * Os thresholds vêm da calibração salva pelo usuário, respeitando a amplitude
 * individual de movimento (essencial para usuários com mobilidade reduzida).
 */
object GestureClassifier {
    private const val BLENDSHAPE_BLINK_RIGHT = "eyeBlinkRight"
    private const val BLENDSHAPE_BLINK_LEFT = "eyeBlinkLeft"

    /**
     * O eixo vencedor precisa superar o segundo colocado por este fator. Abaixo
     * disso o movimento é diagonal demais para ser um gesto deliberado.
     */
    private const val DOMINANCE_MARGIN = 1.3f

    fun classify(
        landmarks: List<NormalizedLandmark>,
        blendshapes: List<Category>?,
        thresholds: CalibrationThresholds,
    ): Gesture? {
        // Piscar tem prioridade — é intencional e não ambíguo com gestos de cabeça
        val blinkGesture = classifyBlink(blendshapes, thresholds.blinkThreshold)
        if (blinkGesture != null) return blinkGesture

        val pose = HeadPoseEstimator.estimate(landmarks) ?: return null
        return classifyHeadPose(pose, thresholds)
    }

    /**
     * Variante que recebe uma pose já corrigida pelo baseline neutro.
     * Usada pelo [com.sac.acessibilidade.vision.GestureProcessor] em tempo real.
     */
    fun classifyWithPose(
        pose: HeadPoseEstimator.HeadPose,
        blendshapes: List<Category>?,
        thresholds: CalibrationThresholds,
    ): Gesture? {
        val blinkGesture = classifyBlink(blendshapes, thresholds.blinkThreshold)
        if (blinkGesture != null) return blinkGesture
        return classifyHeadPose(pose, thresholds)
    }

    /**
     * Maior score de piscada do quadro. Alimenta o [BlinkReliabilityMonitor],
     * que acompanha a variação do sinal para saber se o olho está visível.
     */
    fun maxBlinkScore(blendshapes: List<Category>?): Float {
        val (right, left) = blinkScores(blendshapes)
        return maxOf(right, left)
    }

    /** Scores de piscada do quadro, na ordem (direito, esquerdo). */
    private fun blinkScores(blendshapes: List<Category>?): Pair<Float, Float> {
        blendshapes ?: return 0f to 0f
        val right = blendshapes.firstOrNull { it.categoryName() == BLENDSHAPE_BLINK_RIGHT }?.score() ?: 0f
        val left = blendshapes.firstOrNull { it.categoryName() == BLENDSHAPE_BLINK_LEFT }?.score() ?: 0f
        return right to left
    }

    /** Detecta piscadas isoladamente — instantâneo, sem baseline (usado pelo processador). */
    fun classifyBlink(
        blendshapes: List<Category>?,
        threshold: Float,
    ): Gesture? {
        blendshapes ?: return null
        val (right, left) = blinkScores(blendshapes)
        return when {
            right >= threshold -> Gesture.BLINK_RIGHT_EYE
            left >= threshold -> Gesture.BLINK_LEFT_EYE
            else -> null
        }
    }

    private fun classifyHeadPose(
        pose: HeadPoseEstimator.HeadPose,
        t: CalibrationThresholds,
    ): Gesture? {
        // Normaliza cada eixo pela polaridade APRENDIDA na calibração — a direção
        // não é assumida pelo código, é a que o usuário de fato produziu quando
        // a calibração pediu "direita"/"baixo". Imune a espelhamento de câmera.
        val roll = pose.roll * t.rollSign
        val pitchDown = pose.pitch * t.pitchSign
        val pitchUp = pose.pitch * t.effectivePitchUpSign
        val yaw = pose.yaw * t.yawSign
        // Cada direção vira uma razão "quanto do limiar foi atingido". Em vez de uma
        // ordem fixa de prioridade (roll antes de pitch antes de yaw), vence o eixo
        // que mais se destaca — e movimento diagonal ambíguo não dispara nada.
        // Movimentos naturais (olhar ao redor, ajeitar a postura) quase sempre
        // misturam eixos; um gesto intencional é dominado por um só.
        val ranked =
            listOf(
                Gesture.TILT_HEAD_RIGHT to roll / t.rollRightDeg,
                Gesture.TILT_HEAD_LEFT to -roll / t.rollLeftDeg,
                Gesture.TILT_HEAD_UP to pitchUp / t.pitchUpDeg,
                Gesture.TILT_HEAD_DOWN to pitchDown / t.pitchDownDeg,
                Gesture.TURN_FACE_RIGHT to yaw / t.yawRightDeg,
                Gesture.TURN_FACE_LEFT to -yaw / t.yawLeftDeg,
            ).sortedByDescending { it.second }
        val (best, bestRatio) = ranked[0]
        val runnerUpRatio = ranked[1].second
        return when {
            bestRatio <= 1f -> null
            runnerUpRatio * DOMINANCE_MARGIN > bestRatio -> null
            else -> best
        }
    }
}

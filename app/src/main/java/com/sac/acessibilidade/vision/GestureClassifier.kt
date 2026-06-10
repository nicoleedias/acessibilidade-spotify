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

    private fun classifyBlink(
        blendshapes: List<Category>?,
        threshold: Float,
    ): Gesture? {
        blendshapes ?: return null
        val right = blendshapes.firstOrNull { it.categoryName() == BLENDSHAPE_BLINK_RIGHT }?.score() ?: 0f
        val left = blendshapes.firstOrNull { it.categoryName() == BLENDSHAPE_BLINK_LEFT }?.score() ?: 0f
        return when {
            right >= threshold -> Gesture.BLINK_RIGHT_EYE
            left >= threshold -> Gesture.BLINK_LEFT_EYE
            else -> null
        }
    }

    private fun classifyHeadPose(
        pose: HeadPoseEstimator.HeadPose,
        t: CalibrationThresholds,
    ): Gesture? =
        when {
            pose.roll > t.rollRightDeg -> Gesture.TILT_HEAD_RIGHT
            pose.roll < -t.rollLeftDeg -> Gesture.TILT_HEAD_LEFT
            pose.pitch < -t.pitchUpDeg -> Gesture.TILT_HEAD_UP
            pose.pitch > t.pitchDownDeg -> Gesture.TILT_HEAD_DOWN
            pose.yaw > t.yawRightDeg -> Gesture.TURN_FACE_RIGHT
            pose.yaw < -t.yawLeftDeg -> Gesture.TURN_FACE_LEFT
            else -> null
        }
}

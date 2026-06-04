package com.sac.acessibilidade.vision

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.atan2

/**
 * Estima a pose da cabeça a partir dos landmarks do MediaPipe Face Mesh (478 pts).
 *
 * Retorna ângulos em graus em escala geométrica normalizada:
 *  - roll  : inclinação lateral (+ = lado direito desce, câmera frontal espelhada)
 *  - pitch : inclinação frente/trás (+ = queixo desce, - = queixo sobe)
 *  - yaw   : rotação horizontal (+ = nariz para direita da imagem)
 *
 * Valores aproximados ±45° representam ~máximo confortável para um usuário adulto.
 * Não usa Context — testável com dados sintéticos.
 */
object HeadPoseEstimator {
    private const val IDX_NOSE_TIP = 4
    private const val IDX_LEFT_EYE = 33
    private const val IDX_RIGHT_EYE = 263
    private const val IDX_LEFT_FACE = 234
    private const val IDX_RIGHT_FACE = 454
    private const val IDX_FOREHEAD = 10
    private const val IDX_CHIN = 152
    private const val MIN_LANDMARKS = 478

    data class HeadPose(
        val roll: Float,
        val pitch: Float,
        val yaw: Float,
    )

    fun estimate(landmarks: List<NormalizedLandmark>): HeadPose? {
        if (landmarks.size < MIN_LANDMARKS) return null

        val noseTip = landmarks[IDX_NOSE_TIP]
        val leftEye = landmarks[IDX_LEFT_EYE]
        val rightEye = landmarks[IDX_RIGHT_EYE]
        val leftFace = landmarks[IDX_LEFT_FACE]
        val rightFace = landmarks[IDX_RIGHT_FACE]
        val forehead = landmarks[IDX_FOREHEAD]
        val chin = landmarks[IDX_CHIN]

        val roll =
            Math.toDegrees(
                atan2(
                    (rightEye.y() - leftEye.y()).toDouble(),
                    (rightEye.x() - leftEye.x()).toDouble(),
                ),
            ).toFloat()

        val faceWidth = rightFace.x() - leftFace.x()
        val yaw =
            if (faceWidth > 0f) {
                val faceCenterX = (leftFace.x() + rightFace.x()) / 2f
                ((noseTip.x() - faceCenterX) / (faceWidth * 0.5f)) * 45f
            } else {
                0f
            }

        val faceHeight = chin.y() - forehead.y()
        val pitch =
            if (faceHeight > 0f) {
                val faceCenterY = (forehead.y() + chin.y()) / 2f
                ((noseTip.y() - faceCenterY) / (faceHeight * 0.5f)) * 45f
            } else {
                0f
            }

        return HeadPose(roll = roll, pitch = pitch, yaw = yaw)
    }
}

package com.sac.acessibilidade.vision

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.asin
import kotlin.math.atan2

/**
 * Estima a pose da cabeça (roll/pitch/yaw, em graus) a partir do MediaPipe Face Mesh.
 *
 * Duas fontes, em ordem de preferência:
 * 1. **Matriz de transformação facial** ([fromTransformationMatrix]) — pose 3D real do
 *    solvePnP interno do MediaPipe contra o modelo facial canônico. Eixos desacoplados:
 *    virar o rosto (yaw) e levantar o queixo (pitch) produzem rotação real mesmo quando
 *    o deslocamento 2D do nariz é pequeno (movimento em direção à câmera).
 * 2. **Geometria 2D dos landmarks** ([estimate]) — fallback quando a matriz não está
 *    disponível.
 *
 * A convenção de sinais NÃO precisa ser consistente entre dispositivos: a polaridade
 * de cada eixo é aprendida na calibração (UC02) e aplicada pelo classificador.
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
    ) {
        /** Subtrai o baseline neutro para obter ângulos relativos à posição de repouso. */
        operator fun minus(other: HeadPose) = HeadPose(roll - other.roll, pitch - other.pitch, yaw - other.yaw)
    }

    /**
     * Extrai a pose do resultado do FaceLandmarker usando a **geometria 2D dos
     * landmarks** — a abordagem validada no dispositivo.
     *
     * A matriz de transformação 3D ([fromTransformationMatrix]) está implementada e
     * testada (eixos desacoplados, graus reais), mas a convenção de sinais/ordem do
     * MediaPipe precisa ser confirmada no aparelho antes de virar padrão; até lá,
     * mantemos a geometria, que já funciona de forma estável.
     */
    fun fromResult(result: FaceLandmarkerResult): HeadPose? {
        val landmarks = result.faceLandmarks().firstOrNull() ?: return null
        return estimate(landmarks)
    }

    /**
     * Decompõe a rotação de uma matriz 4x4 column-major (canônico → câmera) em
     * ângulos de Euler (R = Rz·Ry·Rx). Os valores são graus REAIS de rotação da
     * cabeça — virar 30° produz ~30°, independentemente da projeção 2D.
     * Disponível para validação futura no device (ver [fromResult]).
     */
    fun fromTransformationMatrix(matrix: FloatArray): HeadPose {
        val yaw = Math.toDegrees(asin((-matrix[2]).coerceIn(-1f, 1f).toDouble())).toFloat()
        val pitch = Math.toDegrees(atan2(matrix[6].toDouble(), matrix[10].toDouble())).toFloat()
        val roll = Math.toDegrees(atan2(matrix[1].toDouble(), matrix[0].toDouble())).toFloat()
        return HeadPose(roll = roll, pitch = pitch, yaw = yaw)
    }

    fun estimate(landmarks: List<NormalizedLandmark>): HeadPose? {
        if (landmarks.size < MIN_LANDMARKS) return null

        val noseTip = landmarks[IDX_NOSE_TIP]
        val leftEye = landmarks[IDX_LEFT_EYE]
        val rightEye = landmarks[IDX_RIGHT_EYE]
        val leftFace = landmarks[IDX_LEFT_FACE]
        val rightFace = landmarks[IDX_RIGHT_FACE]
        val forehead = landmarks[IDX_FOREHEAD]
        val chin = landmarks[IDX_CHIN]

        // Câmera frontal espelhada: assim como no yaw, a inclinação lateral aparece
        // invertida na imagem que o MediaPipe processa. Negamos o ângulo para que
        // inclinar a cabeça para a DIREITA do usuário produza roll positivo,
        // alinhando com TILT_HEAD_RIGHT.
        val roll =
            -Math.toDegrees(
                atan2(
                    (rightEye.y() - leftEye.y()).toDouble(),
                    (rightEye.x() - leftEye.x()).toDouble(),
                ),
            ).toFloat()

        val faceWidth = rightFace.x() - leftFace.x()
        val yaw =
            if (faceWidth > 0f) {
                val faceCenterX = (leftFace.x() + rightFace.x()) / 2f
                // Câmera frontal espelhada: virar o rosto para a direita desloca o nariz
                // para a ESQUERDA da imagem. Compensamos o espelho para que "virar à
                // direita" produza yaw positivo, alinhando com TURN_FACE_RIGHT.
                ((faceCenterX - noseTip.x()) / (faceWidth * 0.5f)) * 45f
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

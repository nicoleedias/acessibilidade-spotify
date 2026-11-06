package com.sac.acessibilidade.vision

import com.sac.acessibilidade.domain.gesture.CalibrationThresholdCalculator
import com.sac.acessibilidade.domain.gesture.CalibrationThresholds
import com.sac.acessibilidade.domain.gesture.Gesture
import com.sac.acessibilidade.vision.HeadPoseEstimator.HeadPose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Diferenciar movimento natural de gesto intencional: um gesto deliberado é
 * dominado por um eixo e é sustentado; olhar ao redor mistura eixos e é breve.
 */
class NaturalMovementRejectionTest {
    private val t = CalibrationThresholds()

    private fun classify(pose: HeadPose) = GestureClassifier.classifyWithPose(pose, blendshapes = null, t)

    @Test
    fun `gesto de um eixo so e reconhecido`() {
        assertEquals(Gesture.TURN_FACE_RIGHT, classify(HeadPose(roll = 0f, pitch = 0f, yaw = 25f)))
    }

    @Test
    fun `movimento diagonal ambiguo nao dispara nada`() {
        // Passa do limiar de roll (15) e de yaw (20) quase na mesma proporção
        assertNull(classify(HeadPose(roll = 18f, pitch = 0f, yaw = 23f)))
    }

    @Test
    fun `eixo claramente dominante vence mesmo com ruido no outro`() {
        assertEquals(Gesture.TILT_HEAD_RIGHT, classify(HeadPose(roll = 30f, pitch = 0f, yaw = 8f)))
    }

    @Test
    fun `cima e detectado quando calibracao mediu cima e baixo com o mesmo sinal`() {
        // Estimador 2D assimétrico: cima (10) e baixo (14) saem com o mesmo sinal
        val calibrated =
            CalibrationThresholdCalculator.build(
                CalibrationThresholdCalculator.Peaks(
                    tiltRight = 20f,
                    tiltLeft = -20f,
                    tiltUp = 10f,
                    tiltDown = 14f,
                    turnRight = 20f,
                    turnLeft = -20f,
                ),
            )
        val up = GestureClassifier.classifyWithPose(HeadPose(0f, 10f, 0f), null, calibrated)
        assertEquals(Gesture.TILT_HEAD_UP, up)
    }

    @Test
    fun `pico de cima abaixo do piso nao inverte a polaridade`() {
        val calibrated =
            CalibrationThresholdCalculator.build(
                CalibrationThresholdCalculator.Peaks(
                    tiltRight = 20f,
                    tiltLeft = -20f,
                    tiltUp = -1f,
                    tiltDown = 14f,
                    turnRight = 20f,
                    turnLeft = -20f,
                ),
            )
        assertNull(calibrated.pitchUpSign)
    }

    @Test
    fun `gesto breve nao dispara mas sustentado dispara`() {
        val stabilizer = HeadGestureStabilizer(baselineStableFrames = 2, maxWarmupFrames = 2)
        val neutral = HeadPose(0f, 0f, 0f)
        repeat(2) { stabilizer.update(neutral, t) }
        val turned = HeadPose(0f, 0f, 30f)

        val brief = (1..3).map { stabilizer.update(turned, t).gesture }
        assertEquals(listOf<Gesture?>(null, null, null), brief)

        stabilizer.update(neutral, t)
        repeat(5) { stabilizer.update(neutral, t) }
        val held = (1..6).mapNotNull { stabilizer.update(turned, t).gesture }
        assertEquals(listOf(Gesture.TURN_FACE_RIGHT), held)
    }
}

package com.sac.acessibilidade.vision

import com.sac.acessibilidade.domain.gesture.CalibrationThresholds
import com.sac.acessibilidade.domain.gesture.Gesture
import com.sac.acessibilidade.vision.HeadPoseEstimator.HeadPose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class HeadGestureStabilizerTest {
    private lateinit var stabilizer: HeadGestureStabilizer

    private val thresholds =
        CalibrationThresholds(
            rollRightDeg = 10f,
            rollLeftDeg = 10f,
            pitchUpDeg = 10f,
            pitchDownDeg = 10f,
            yawRightDeg = 10f,
            yawLeftDeg = 10f,
            blinkThreshold = 0.5f,
            nodPitchAmplitudeDeg = 12f,
        )

    @Before
    fun setUp() {
        stabilizer =
            HeadGestureStabilizer(
                sustainFrames = 2,
                baselineStableFrames = 3,
                stableRangeDeg = 3f,
                releaseRatio = 0.5f,
                driftAlpha = 0.03f,
                maxWarmupFrames = 8,
            )
    }

    private fun pose(
        roll: Float = 0f,
        pitch: Float = 0f,
        yaw: Float = 0f,
    ) = HeadPose(roll, pitch, yaw)

    private fun feed(p: HeadPose) = stabilizer.update(p, thresholds)

    /** Estabelece o baseline com 3 frames parados e consome o 1º frame relativo. */
    private fun establishBaseline() {
        repeat(3) { feed(pose()) }
    }

    @Test
    fun `baseline nao fica pronto antes do numero minimo de frames estaveis`() {
        feed(pose())
        val result = feed(pose())
        assertNull("relativePose deve ser nulo enquanto o baseline aquece", result.relativePose)
    }

    @Test
    fun `baseline fica pronto apos frames parados`() {
        establishBaseline()
        val result = feed(pose())
        assertNotNull("relativePose deve existir após baseline estável", result.relativePose)
    }

    @Test
    fun `poses instaveis nao fixam o baseline de imediato`() {
        feed(pose(roll = 0f))
        feed(pose(roll = 20f))
        feed(pose(roll = 0f))
        val result = feed(pose(roll = 0f))
        assertNull("variação alta não deve fixar baseline cedo", result.relativePose)
    }

    @Test
    fun `fallback fixa o baseline mesmo sem cabeca parada`() {
        // Alterna valores para nunca satisfazer a estabilidade (range > 3)
        repeat(8) { i -> feed(pose(roll = if (i % 2 == 0) 0f else 20f)) }
        // Após maxWarmupFrames (8) o baseline trava à força
        val result = feed(pose())
        assertNotNull("fallback deve garantir baseline operacional", result.relativePose)
    }

    @Test
    fun `dispara gesto direcional apos sustain`() {
        establishBaseline()
        assertNull(feed(pose(roll = 20f)).gesture)
        assertEquals(Gesture.TILT_HEAD_RIGHT, feed(pose(roll = 20f)).gesture)
    }

    @Test
    fun `gesto mantido nao dispara repetidamente`() {
        establishBaseline()
        feed(pose(roll = 20f))
        feed(pose(roll = 20f)) // dispara aqui
        assertNull("manter a cabeça inclinada não deve re-disparar", feed(pose(roll = 20f)).gesture)
        assertNull(feed(pose(roll = 20f)).gesture)
    }

    @Test
    fun `re-arma apos voltar ao neutro e dispara de novo`() {
        establishBaseline()
        feed(pose(roll = 20f))
        assertEquals(Gesture.TILT_HEAD_RIGHT, feed(pose(roll = 20f)).gesture)
        // volta ao neutro → re-arma
        feed(pose(roll = 0f))
        feed(pose(roll = 0f))
        // novo gesto válido
        assertNull(feed(pose(roll = 20f)).gesture)
        assertEquals(Gesture.TILT_HEAD_RIGHT, feed(pose(roll = 20f)).gesture)
    }

    @Test
    fun `gesto oposto so dispara apos sustain proprio`() {
        establishBaseline()
        // vira para a esquerda (yaw negativo)
        assertNull(feed(pose(yaw = -20f)).gesture)
        assertEquals(Gesture.TURN_FACE_LEFT, feed(pose(yaw = -20f)).gesture)
    }

    @Test
    fun `reset limpa o baseline`() {
        establishBaseline()
        assertNotNull(feed(pose()).relativePose)
        stabilizer.reset()
        assertNull("após reset o baseline deve reaquecer", feed(pose()).relativePose)
    }
}

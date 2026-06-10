package com.sac.acessibilidade.domain.gesture

import com.sac.acessibilidade.domain.gesture.CalibrationThresholdCalculator.Axis
import com.sac.acessibilidade.domain.gesture.CalibrationThresholdCalculator.Peaks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationThresholdCalculatorTest {
    private val delta = 0.001f

    private fun peaks(
        tiltRight: Float = 30f,
        tiltLeft: Float = -30f,
        tiltUp: Float = -25f,
        tiltDown: Float = 25f,
        turnRight: Float = 40f,
        turnLeft: Float = -40f,
    ) = Peaks(tiltRight, tiltLeft, tiltUp, tiltDown, turnRight, turnLeft)

    @Test
    fun `aplica 75 por cento da magnitude do pico medido`() {
        val result = CalibrationThresholdCalculator.build(peaks(tiltRight = 40f))
        // |40| * 0.75 = 30, bem acima do mínimo de roll
        assertEquals(30f, result.rollRightDeg, delta)
    }

    @Test
    fun `magnitude ignora o sinal do pico`() {
        // Pico negativo (câmera espelhada): magnitude continua válida
        val result = CalibrationThresholdCalculator.build(peaks(tiltRight = -40f))
        assertEquals(30f, result.rollRightDeg, delta)
    }

    @Test
    fun `respeita o minimo de seguranca quando o pico e pequeno`() {
        // Pico minúsculo: |4| * 0.75 = 3, abaixo do mínimo de roll (6)
        val result = CalibrationThresholdCalculator.build(peaks(tiltLeft = 4f))
        assertEquals(CalibrationThresholdCalculator.MIN_ROLL_DEG, result.rollLeftDeg, delta)
    }

    @Test
    fun `cada eixo usa seu proprio minimo`() {
        val result = CalibrationThresholdCalculator.build(peaks(tiltRight = 0f, tiltUp = 0f, turnRight = 0f))
        assertEquals(CalibrationThresholdCalculator.MIN_ROLL_DEG, result.rollRightDeg, delta)
        assertEquals(CalibrationThresholdCalculator.MIN_PITCH_DEG, result.pitchUpDeg, delta)
        assertEquals(CalibrationThresholdCalculator.MIN_YAW_DEG, result.yawRightDeg, delta)
    }

    // ── Polaridade aprendida ────────────────────────────────────────────────

    @Test
    fun `polaridade positiva quando o pico da direita e positivo`() {
        val result = CalibrationThresholdCalculator.build(peaks(tiltRight = 30f, turnRight = 40f, tiltDown = 25f))
        assertEquals(1f, result.rollSign, delta)
        assertEquals(1f, result.yawSign, delta)
        assertEquals(1f, result.pitchSign, delta)
    }

    @Test
    fun `polaridade negativa quando o estimador inverte o eixo`() {
        // Câmera com convenção invertida: inclinar/virar à direita produz valor negativo
        val result =
            CalibrationThresholdCalculator.build(
                peaks(
                    tiltRight = -30f,
                    tiltLeft = 28f,
                    turnRight = -40f,
                    turnLeft = 38f,
                    tiltDown = -25f,
                    tiltUp = 22f,
                ),
            )
        assertEquals(-1f, result.rollSign, delta)
        assertEquals(-1f, result.yawSign, delta)
        assertEquals(-1f, result.pitchSign, delta)
        // Magnitudes continuam corretas
        assertEquals(22.5f, result.rollRightDeg, delta)
        assertEquals(30f, result.yawRightDeg, delta)
    }

    // ── NOD ─────────────────────────────────────────────────────────────────

    @Test
    fun `nod usa a menor amplitude de pitch medida`() {
        // |tiltUp| menor (20) domina: 20 * 0.6 = 12
        val result = CalibrationThresholdCalculator.build(peaks(tiltUp = -20f, tiltDown = 40f))
        assertEquals(12f, result.nodPitchAmplitudeDeg, delta)
    }

    @Test
    fun `nod respeita o teto maximo`() {
        val result = CalibrationThresholdCalculator.build(peaks(tiltUp = -90f, tiltDown = 90f))
        assertEquals(CalibrationThresholdCalculator.MAX_NOD_DEG, result.nodPitchAmplitudeDeg, delta)
    }

    @Test
    fun `nod respeita o piso minimo`() {
        val result = CalibrationThresholdCalculator.build(peaks(tiltUp = -2f, tiltDown = 2f))
        assertEquals(CalibrationThresholdCalculator.MIN_NOD_DEG, result.nodPitchAmplitudeDeg, delta)
    }

    @Test
    fun `thresholds gerados sao sempre positivos`() {
        val result = CalibrationThresholdCalculator.build(peaks())
        assertTrue(result.rollRightDeg > 0f)
        assertTrue(result.rollLeftDeg > 0f)
        assertTrue(result.pitchUpDeg > 0f)
        assertTrue(result.pitchDownDeg > 0f)
        assertTrue(result.yawRightDeg > 0f)
        assertTrue(result.yawLeftDeg > 0f)
        assertTrue(result.nodPitchAmplitudeDeg > 0f)
    }

    @Test
    fun `entry minimo por eixo corresponde aos minimos de seguranca`() {
        assertEquals(
            CalibrationThresholdCalculator.MIN_ROLL_DEG,
            CalibrationThresholdCalculator.entryMinDegFor(Axis.ROLL),
            delta,
        )
        assertEquals(
            CalibrationThresholdCalculator.MIN_PITCH_DEG,
            CalibrationThresholdCalculator.entryMinDegFor(Axis.PITCH),
            delta,
        )
        assertEquals(
            CalibrationThresholdCalculator.MIN_YAW_DEG,
            CalibrationThresholdCalculator.entryMinDegFor(Axis.YAW),
            delta,
        )
    }
}

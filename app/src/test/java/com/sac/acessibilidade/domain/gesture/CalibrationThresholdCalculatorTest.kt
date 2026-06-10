package com.sac.acessibilidade.domain.gesture

import com.sac.acessibilidade.domain.gesture.CalibrationThresholdCalculator.Axis
import com.sac.acessibilidade.domain.gesture.CalibrationThresholdCalculator.Peaks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationThresholdCalculatorTest {
    private val delta = 0.001f

    private fun peaks(
        rollRight: Float = 30f,
        rollLeft: Float = 30f,
        pitchUp: Float = 25f,
        pitchDown: Float = 25f,
        yawRight: Float = 40f,
        yawLeft: Float = 40f,
    ) = Peaks(rollRight, rollLeft, pitchUp, pitchDown, yawRight, yawLeft)

    @Test
    fun `aplica 75 por cento do pico medido`() {
        val result = CalibrationThresholdCalculator.build(peaks(rollRight = 40f))
        // 40 * 0.75 = 30, bem acima do mínimo de roll
        assertEquals(30f, result.rollRightDeg, delta)
    }

    @Test
    fun `respeita o minimo de seguranca quando o pico e pequeno`() {
        // Pico minúsculo: 4 * 0.75 = 3, abaixo do mínimo de roll (6)
        val result = CalibrationThresholdCalculator.build(peaks(rollLeft = 4f))
        assertEquals(CalibrationThresholdCalculator.MIN_ROLL_DEG, result.rollLeftDeg, delta)
    }

    @Test
    fun `cada eixo usa seu proprio minimo`() {
        val result = CalibrationThresholdCalculator.build(peaks(rollRight = 0f, pitchUp = 0f, yawRight = 0f))
        assertEquals(CalibrationThresholdCalculator.MIN_ROLL_DEG, result.rollRightDeg, delta)
        assertEquals(CalibrationThresholdCalculator.MIN_PITCH_DEG, result.pitchUpDeg, delta)
        assertEquals(CalibrationThresholdCalculator.MIN_YAW_DEG, result.yawRightDeg, delta)
    }

    @Test
    fun `nod usa a menor amplitude de pitch medida`() {
        // pitchUp menor (20) domina: 20 * 0.6 = 12
        val result = CalibrationThresholdCalculator.build(peaks(pitchUp = 20f, pitchDown = 40f))
        assertEquals(12f, result.nodPitchAmplitudeDeg, delta)
    }

    @Test
    fun `nod respeita o teto maximo`() {
        // pitch enorme não deve gerar amplitude de nod impraticável
        val result = CalibrationThresholdCalculator.build(peaks(pitchUp = 90f, pitchDown = 90f))
        assertEquals(CalibrationThresholdCalculator.MAX_NOD_DEG, result.nodPitchAmplitudeDeg, delta)
    }

    @Test
    fun `nod respeita o piso minimo`() {
        // pitch pequeno não deve gerar nod fácil demais
        val result = CalibrationThresholdCalculator.build(peaks(pitchUp = 2f, pitchDown = 2f))
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

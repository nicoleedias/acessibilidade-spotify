package com.sac.acessibilidade.domain.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Trava os valores padrão de [CalibrationThresholds].
 *
 * Esses números estão duplicados nas constantes privadas de
 * `CalibrationRepositoryImpl` (DEFAULT_ROLL_DEG, DEFAULT_PITCH_DEG, …), que são
 * usadas quando o usuário ainda não calibrou. Se um dos lados mudar sem o outro,
 * o app passa a se comportar de um jeito antes da calibração e de outro depois.
 * Este teste fixa o lado do domínio; `CalibrationRepositoryImplTest`
 * (androidTest) fecha o outro lado comparando com `CalibrationThresholds()`.
 */
class CalibrationThresholdsTest {
    private val delta = 0.001f

    @Test
    fun `defaults de roll sao simetricos em 15 graus`() {
        val defaults = CalibrationThresholds()
        assertEquals(15f, defaults.rollRightDeg, delta)
        assertEquals(15f, defaults.rollLeftDeg, delta)
    }

    @Test
    fun `defaults de pitch sao simetricos em 12 graus`() {
        val defaults = CalibrationThresholds()
        assertEquals(12f, defaults.pitchUpDeg, delta)
        assertEquals(12f, defaults.pitchDownDeg, delta)
    }

    @Test
    fun `defaults de yaw sao simetricos em 20 graus`() {
        val defaults = CalibrationThresholds()
        assertEquals(20f, defaults.yawRightDeg, delta)
        assertEquals(20f, defaults.yawLeftDeg, delta)
    }

    @Test
    fun `default de piscada e meio`() {
        assertEquals(0.5f, CalibrationThresholds().blinkThreshold, delta)
    }

    @Test
    fun `default de amplitude do nod e 12 graus`() {
        assertEquals(12f, CalibrationThresholds().nodPitchAmplitudeDeg, delta)
    }

    @Test
    fun `polaridade default e positiva nos tres eixos`() {
        // Antes de calibrar assume-se a convenção direta do estimador;
        // a calibração (UC02) sobrescreve com o sinal realmente observado.
        val defaults = CalibrationThresholds()
        assertEquals(1f, defaults.rollSign, delta)
        assertEquals(1f, defaults.pitchSign, delta)
        assertEquals(1f, defaults.yawSign, delta)
    }

    @Test
    fun `todos os limiares default sao positivos`() {
        val defaults = CalibrationThresholds()
        assertTrue(defaults.rollRightDeg > 0f)
        assertTrue(defaults.rollLeftDeg > 0f)
        assertTrue(defaults.pitchUpDeg > 0f)
        assertTrue(defaults.pitchDownDeg > 0f)
        assertTrue(defaults.yawRightDeg > 0f)
        assertTrue(defaults.yawLeftDeg > 0f)
        assertTrue(defaults.blinkThreshold > 0f)
        assertTrue(defaults.nodPitchAmplitudeDeg > 0f)
    }

    @Test
    fun `defaults respeitam os minimos de seguranca do calculador`() {
        // Os defaults nunca podem ser mais permissivos que o piso aplicado à
        // calibração real — seria um falso positivo garantido antes de calibrar.
        val defaults = CalibrationThresholds()
        assertTrue(defaults.rollRightDeg >= CalibrationThresholdCalculator.MIN_ROLL_DEG)
        assertTrue(defaults.pitchUpDeg >= CalibrationThresholdCalculator.MIN_PITCH_DEG)
        assertTrue(defaults.yawRightDeg >= CalibrationThresholdCalculator.MIN_YAW_DEG)
        assertTrue(defaults.nodPitchAmplitudeDeg >= CalibrationThresholdCalculator.MIN_NOD_DEG)
    }
}

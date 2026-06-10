package com.sac.acessibilidade.vision

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NodDetectorTest {
    private lateinit var detector: NodDetector
    private val amplitude = 8f

    @Before
    fun setUp() {
        detector = NodDetector()
    }

    private fun feedAll(
        values: List<Float>,
        startMs: Long = 0L,
        stepMs: Long = 30L,
        amp: Float = amplitude,
    ): Boolean {
        var now = startMs
        var detected = false
        for (v in values) {
            now += stepMs
            detected = detected || detector.feed(v, now, amp)
        }
        return detected
    }

    @Test
    fun `nao detecta nod com pitch estatico`() {
        val detected = feedAll(List(20) { 0f })
        assertFalse(detected)
    }

    @Test
    fun `detecta nod com ciclo positivo para negativo`() {
        val positive = List(5) { 12f }
        val negative = List(5) { -10f }
        val detected = feedAll(positive + negative)
        assertTrue("Deveria detectar NOD no ciclo positivo→negativo", detected)
    }

    @Test
    fun `detecta nod com ciclo negativo para positivo`() {
        val negative = List(5) { -11f }
        val positive = List(5) { 9f }
        val detected = feedAll(negative + positive)
        assertTrue("Deveria detectar NOD no ciclo negativo→positivo", detected)
    }

    @Test
    fun `nao detecta nod abaixo da amplitude minima`() {
        // Pitches oscilam mas ficam abaixo do limiar de 8°
        val values = List(5) { 5f } + List(5) { -5f }
        val detected = feedAll(values)
        assertFalse("Amplitude abaixo do limiar não deve disparar NOD", detected)
    }

    @Test
    fun `cooldown impede nod consecutivo imediato`() {
        // Primeiro ciclo → deve detectar
        val firstCycle = List(5) { 12f } + List(5) { -10f }
        var now = 0L
        var firstDetected = false
        for (v in firstCycle) {
            now += 30
            firstDetected = firstDetected || detector.feed(v, now, amplitude)
        }
        assertTrue("Primeiro NOD deve ser detectado", firstDetected)

        // Segundo ciclo começa logo após (total < 1200ms de cooldown)
        val secondCycle = List(5) { 12f } + List(5) { -10f }
        var secondDetected = false
        for (v in secondCycle) {
            now += 30
            secondDetected = secondDetected || detector.feed(v, now, amplitude)
        }
        assertFalse("Cooldown deveria impedir NOD consecutivo dentro de 1200ms", secondDetected)
    }
}

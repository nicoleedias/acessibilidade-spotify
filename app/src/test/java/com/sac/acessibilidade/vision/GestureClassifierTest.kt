package com.sac.acessibilidade.vision

import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.sac.acessibilidade.domain.gesture.CalibrationThresholds
import com.sac.acessibilidade.domain.gesture.Gesture
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GestureClassifierTest {
    private val thresholds =
        CalibrationThresholds(
            rollRightDeg = 15f,
            rollLeftDeg = 15f,
            pitchUpDeg = 12f,
            pitchDownDeg = 12f,
            yawRightDeg = 20f,
            yawLeftDeg = 20f,
            blinkThreshold = 0.5f,
        )

    private lateinit var neutralLandmarks: MutableList<NormalizedLandmark>

    @Before
    fun setUp() {
        neutralLandmarks = MutableList(478) { mockLandmark(0.5f, 0.5f) }
        setNeutralFaceKeypoints(neutralLandmarks)
    }

    private fun mockLandmark(
        x: Float,
        y: Float,
        z: Float = 0f,
    ): NormalizedLandmark =
        mockk<NormalizedLandmark>().also {
            every { it.x() } returns x
            every { it.y() } returns y
            every { it.z() } returns z
        }

    private fun setNeutralFaceKeypoints(list: MutableList<NormalizedLandmark>) {
        list[4] = mockLandmark(0.5f, 0.5f)
        list[33] = mockLandmark(0.42f, 0.45f)
        list[263] = mockLandmark(0.58f, 0.45f)
        list[234] = mockLandmark(0.3f, 0.5f)
        list[454] = mockLandmark(0.7f, 0.5f)
        list[10] = mockLandmark(0.5f, 0.3f)
        list[152] = mockLandmark(0.5f, 0.7f)
    }

    private fun blendshape(
        name: String,
        score: Float,
    ): Category =
        mockk<Category>().also {
            every { it.categoryName() } returns name
            every { it.score() } returns score
        }

    // ── Sem gesto ────────────────────────────────────────────────────────────

    @Test
    fun `retorna null quando rosto esta neutro`() {
        val result = GestureClassifier.classify(neutralLandmarks, emptyList(), thresholds)
        assertNull(result)
    }

    @Test
    fun `retorna null com landmarks insuficientes`() {
        val result = GestureClassifier.classify(List(10) { mockLandmark(0.5f, 0.5f) }, null, thresholds)
        assertNull(result)
    }

    // ── Gestos de cabeça ─────────────────────────────────────────────────────

    @Test
    fun `detecta TILT_HEAD_RIGHT quando o usuario inclina para a direita`() {
        // Câmera espelhada: inclinar à direita abaixa o olho à ESQUERDA da imagem (33)
        neutralLandmarks[33] = mockLandmark(0.42f, 0.65f)
        neutralLandmarks[263] = mockLandmark(0.58f, 0.40f)

        val result = GestureClassifier.classify(neutralLandmarks, null, thresholds)
        assertEquals(Gesture.TILT_HEAD_RIGHT, result)
    }

    @Test
    fun `detecta TILT_HEAD_LEFT quando o usuario inclina para a esquerda`() {
        // Câmera espelhada: inclinar à esquerda abaixa o olho à DIREITA da imagem (263)
        neutralLandmarks[33] = mockLandmark(0.42f, 0.40f)
        neutralLandmarks[263] = mockLandmark(0.58f, 0.65f)

        val result = GestureClassifier.classify(neutralLandmarks, null, thresholds)
        assertEquals(Gesture.TILT_HEAD_LEFT, result)
    }

    @Test
    fun `detecta TURN_FACE_RIGHT quando o rosto vira para a direita`() {
        // Câmera espelhada: virar à direita desloca o nariz para a ESQUERDA da imagem
        neutralLandmarks[4] = mockLandmark(0.25f, 0.5f)

        val result = GestureClassifier.classify(neutralLandmarks, null, thresholds)
        assertEquals(Gesture.TURN_FACE_RIGHT, result)
    }

    @Test
    fun `detecta TURN_FACE_LEFT quando o rosto vira para a esquerda`() {
        // Câmera espelhada: virar à esquerda desloca o nariz para a DIREITA da imagem
        neutralLandmarks[4] = mockLandmark(0.75f, 0.5f)

        val result = GestureClassifier.classify(neutralLandmarks, null, thresholds)
        assertEquals(Gesture.TURN_FACE_LEFT, result)
    }

    @Test
    fun `detecta TILT_HEAD_UP quando nariz esta acima do centro vertical`() {
        // Nariz acima do centro: pitch negativo abaixo de -pitchUpDeg
        neutralLandmarks[4] = mockLandmark(0.5f, 0.3f)

        val result = GestureClassifier.classify(neutralLandmarks, null, thresholds)
        assertEquals(Gesture.TILT_HEAD_UP, result)
    }

    @Test
    fun `detecta TILT_HEAD_DOWN quando nariz esta abaixo do centro vertical`() {
        neutralLandmarks[4] = mockLandmark(0.5f, 0.7f)

        val result = GestureClassifier.classify(neutralLandmarks, null, thresholds)
        assertEquals(Gesture.TILT_HEAD_DOWN, result)
    }

    // ── Piscadas ─────────────────────────────────────────────────────────────

    @Test
    fun `detecta BLINK_RIGHT_EYE quando score excede threshold`() {
        val blendshapes =
            listOf(
                blendshape("eyeBlinkRight", 0.9f),
                blendshape("eyeBlinkLeft", 0.1f),
            )
        val result = GestureClassifier.classify(neutralLandmarks, blendshapes, thresholds)
        assertEquals(Gesture.BLINK_RIGHT_EYE, result)
    }

    @Test
    fun `detecta BLINK_LEFT_EYE quando score excede threshold`() {
        val blendshapes =
            listOf(
                blendshape("eyeBlinkRight", 0.1f),
                blendshape("eyeBlinkLeft", 0.8f),
            )
        val result = GestureClassifier.classify(neutralLandmarks, blendshapes, thresholds)
        assertEquals(Gesture.BLINK_LEFT_EYE, result)
    }

    @Test
    fun `piscada tem prioridade sobre gesto de cabeca`() {
        // Cabeça inclinada E piscada ativa — piscada deve vencer
        neutralLandmarks[33] = mockLandmark(0.42f, 0.40f)
        neutralLandmarks[263] = mockLandmark(0.58f, 0.65f)
        val blendshapes = listOf(blendshape("eyeBlinkRight", 0.9f))

        val result = GestureClassifier.classify(neutralLandmarks, blendshapes, thresholds)
        assertEquals(Gesture.BLINK_RIGHT_EYE, result)
    }

    @Test
    fun `nao detecta piscada quando score esta abaixo do threshold`() {
        val blendshapes =
            listOf(
                blendshape("eyeBlinkRight", 0.3f),
                blendshape("eyeBlinkLeft", 0.2f),
            )
        val result = GestureClassifier.classify(neutralLandmarks, blendshapes, thresholds)
        // Rosto neutro com piscadas fracas — deve retornar null
        assertNull(result)
    }
}

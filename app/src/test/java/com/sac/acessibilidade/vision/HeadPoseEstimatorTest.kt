package com.sac.acessibilidade.vision

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadPoseEstimatorTest {
    private fun landmark(
        x: Float,
        y: Float,
        z: Float = 0f,
    ): NormalizedLandmark =
        mockk<NormalizedLandmark>().also {
            every { it.x() } returns x
            every { it.y() } returns y
            every { it.z() } returns z
        }

    private fun neutralFace(): MutableList<NormalizedLandmark> {
        // Cria 478 landmarks neutros (centro do frame)
        val list = MutableList(478) { landmark(0.5f, 0.5f) }

        // Landmarks relevantes para pose centralizada
        // IDX_NOSE_TIP = 4: centro
        list[4] = landmark(0.5f, 0.5f)
        // IDX_LEFT_EYE = 33, IDX_RIGHT_EYE = 263: mesma altura, simétricos
        list[33] = landmark(0.42f, 0.45f)
        list[263] = landmark(0.58f, 0.45f)
        // IDX_LEFT_FACE = 234, IDX_RIGHT_FACE = 454
        list[234] = landmark(0.3f, 0.5f)
        list[454] = landmark(0.7f, 0.5f)
        // IDX_FOREHEAD = 10, IDX_CHIN = 152
        list[10] = landmark(0.5f, 0.3f)
        list[152] = landmark(0.5f, 0.7f)

        return list
    }

    @Test
    fun `estimate retorna null com lista de landmarks insuficiente`() {
        val result = HeadPoseEstimator.estimate(List(100) { landmark(0.5f, 0.5f) })
        assertNull(result)
    }

    @Test
    fun `estimate retorna pose nao nula com 478 landmarks`() {
        val result = HeadPoseEstimator.estimate(neutralFace())
        assertNotNull(result)
    }

    @Test
    fun `roll positivo quando o usuario inclina a cabeca para a direita`() {
        val face = neutralFace()
        // Câmera espelhada: inclinar à direita abaixa o olho que está à ESQUERDA
        // da imagem (landmark 33). O estimador compensa o espelho → roll positivo.
        face[33] = landmark(0.42f, 0.55f)
        face[263] = landmark(0.58f, 0.45f)

        val pose = HeadPoseEstimator.estimate(face)
        assertNotNull(pose)
        assertTrue("Roll deveria ser positivo, foi ${pose!!.roll}", pose.roll > 0f)
    }

    @Test
    fun `roll negativo quando o usuario inclina a cabeca para a esquerda`() {
        val face = neutralFace()
        // Câmera espelhada: inclinar à esquerda abaixa o olho que está à DIREITA
        // da imagem (landmark 263) → roll negativo após a compensação.
        face[33] = landmark(0.42f, 0.45f)
        face[263] = landmark(0.58f, 0.55f)

        val pose = HeadPoseEstimator.estimate(face)
        assertNotNull(pose)
        assertTrue("Roll deveria ser negativo, foi ${pose!!.roll}", pose.roll < 0f)
    }

    @Test
    fun `yaw positivo quando o rosto vira para a direita do usuario`() {
        val face = neutralFace()
        // Câmera frontal espelhada: virar o rosto para a direita desloca o nariz
        // para a ESQUERDA da imagem. O estimador compensa o espelho → yaw positivo.
        face[4] = landmark(0.4f, 0.5f)

        val pose = HeadPoseEstimator.estimate(face)
        assertNotNull(pose)
        assertTrue("Yaw deveria ser positivo, foi ${pose!!.yaw}", pose.yaw > 0f)
    }

    @Test
    fun `pitch positivo quando nariz esta abaixo do centro vertical do rosto`() {
        val face = neutralFace()
        // Nariz abaixo do centro (queixo descendo)
        face[4] = landmark(0.5f, 0.6f)

        val pose = HeadPoseEstimator.estimate(face)
        assertNotNull(pose)
        assertTrue("Pitch deveria ser positivo, foi ${pose!!.pitch}", pose.pitch > 0f)
    }
}

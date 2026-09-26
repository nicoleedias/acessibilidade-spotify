package com.sac.acessibilidade.vision

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureQualityEvaluatorTest {
    /** Luminância e enquadramento saudáveis — o cenário "tudo certo". */
    private fun good(
        faceCount: Int = 1,
        meanLuma: Float = 140f,
        faceHeightRatio: Float = 0.5f,
    ) = CaptureQualityEvaluator.evaluate(faceCount, meanLuma, faceHeightRatio)

    @Test
    fun `quadro bem iluminado com um rosto centralizado nao acusa problema`() {
        assertEquals(CaptureIssue.NONE, good())
    }

    @Test
    fun `ambiente escuro e sinalizado`() {
        assertEquals(CaptureIssue.TOO_DARK, good(meanLuma = 30f))
    }

    @Test
    fun `contraluz ou superexposicao e sinalizada`() {
        assertEquals(CaptureIssue.TOO_BRIGHT, good(meanLuma = 245f))
    }

    @Test
    fun `ausencia de rosto e sinalizada quando a luz esta boa`() {
        assertEquals(CaptureIssue.NO_FACE, good(faceCount = 0))
    }

    @Test
    fun `mais de um rosto no quadro e sinalizado`() {
        assertEquals(CaptureIssue.MULTIPLE_FACES, good(faceCount = 2))
    }

    @Test
    fun `rosto distante demais e sinalizado`() {
        assertEquals(CaptureIssue.FACE_TOO_FAR, good(faceHeightRatio = 0.10f))
    }

    @Test
    fun `rosto colado na camera e sinalizado`() {
        assertEquals(CaptureIssue.FACE_TOO_CLOSE, good(faceHeightRatio = 0.95f))
    }

    // ── Prioridade: iluminação antes de rosto ────────────────────────────────
    // No escuro o MediaPipe perde o rosto. Dizer "rosto não detectado" manda o
    // usuário mexer na posição quando o que resolve é acender a luz.

    @Test
    fun `no escuro sem rosto a mensagem e sobre a luz e nao sobre o rosto`() {
        assertEquals(CaptureIssue.TOO_DARK, good(faceCount = 0, meanLuma = 20f))
    }

    @Test
    fun `no escuro com dois rostos a luz ainda tem prioridade`() {
        assertEquals(CaptureIssue.TOO_DARK, good(faceCount = 2, meanLuma = 20f))
    }

    @Test
    fun `superexposicao tem prioridade sobre ausencia de rosto`() {
        assertEquals(CaptureIssue.TOO_BRIGHT, good(faceCount = 0, meanLuma = 250f))
    }

    @Test
    fun `dois rostos tem prioridade sobre enquadramento`() {
        assertEquals(CaptureIssue.MULTIPLE_FACES, good(faceCount = 2, faceHeightRatio = 0.05f))
    }

    @Test
    fun `enquadramento so e avaliado quando ha exatamente um rosto`() {
        assertEquals(CaptureIssue.NO_FACE, good(faceCount = 0, faceHeightRatio = 0.95f))
    }

    // ── Fronteiras exatas dos limiares ───────────────────────────────────────

    @Test
    fun `luminancia exatamente no limiar escuro ainda e aceita`() {
        assertEquals(CaptureIssue.NONE, good(meanLuma = CaptureQualityEvaluator.DARK_LUMA))
    }

    @Test
    fun `luminancia exatamente no limiar claro ainda e aceita`() {
        assertEquals(CaptureIssue.NONE, good(meanLuma = CaptureQualityEvaluator.BRIGHT_LUMA))
    }

    @Test
    fun `rosto exatamente no tamanho minimo ainda e aceito`() {
        assertEquals(
            CaptureIssue.NONE,
            good(faceHeightRatio = CaptureQualityEvaluator.MIN_FACE_HEIGHT_RATIO),
        )
    }

    @Test
    fun `rosto exatamente no tamanho maximo ainda e aceito`() {
        assertEquals(
            CaptureIssue.NONE,
            good(faceHeightRatio = CaptureQualityEvaluator.MAX_FACE_HEIGHT_RATIO),
        )
    }

    // ── Classificação bloqueante ────────────────────────────────────────────

    @Test
    fun `apenas ausencia de rosto e multiplos rostos bloqueiam a calibracao`() {
        val bloqueantes = CaptureIssue.entries.filter { it.isBlocking }.toSet()
        assertEquals(setOf(CaptureIssue.NO_FACE, CaptureIssue.MULTIPLE_FACES), bloqueantes)
    }

    @Test
    fun `problemas de iluminacao avisam mas nao travam o usuario`() {
        assertEquals(false, CaptureIssue.TOO_DARK.isBlocking)
        assertEquals(false, CaptureIssue.TOO_BRIGHT.isBlocking)
    }
}

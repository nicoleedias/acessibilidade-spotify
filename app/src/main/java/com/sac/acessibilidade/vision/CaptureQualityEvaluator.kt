package com.sac.acessibilidade.vision

/**
 * Traduz medidas agregadas do frame no [CaptureIssue] mais acionável para o usuário.
 *
 * Recebe apenas números — nenhuma dependência de Android, Bitmap ou MediaPipe — para
 * que toda a política de qualidade seja testável na JVM. Quem mede é o
 * [CalibrationPoseAnalyzer]; quem decide é esta classe.
 *
 * **Privacidade (LGPD):** trabalha só com métricas agregadas (luminância média,
 * contagem de rostos, proporção do rosto no quadro). Nenhum pixel ou landmark
 * trafega por aqui nem é armazenado.
 */
object CaptureQualityEvaluator {
    /** Abaixo disto o MediaPipe começa a perder landmarks de forma perceptível. */
    const val DARK_LUMA = 55f

    /** Acima disto o rosto satura e o contraste dos olhos/boca se perde. */
    const val BRIGHT_LUMA = 225f

    /** Rosto ocupando menos que isto da altura do quadro: longe demais para medir ângulo. */
    const val MIN_FACE_HEIGHT_RATIO = 0.22f

    /** Rosto maior que isto costuma estar cortado pelas bordas do quadro. */
    const val MAX_FACE_HEIGHT_RATIO = 0.85f

    /**
     * @param faceCount rostos detectados no frame.
     * @param meanLuma luminância média do frame, 0..255.
     * @param faceHeightRatio altura do rosto dividida pela altura do quadro, 0..1.
     *   Ignorado quando [faceCount] != 1.
     *
     * A ordem de avaliação é intencional: iluminação vem primeiro porque é a **causa
     * raiz** mais comum de "rosto não detectado". Avisar "está escuro" resolve o
     * problema do usuário; avisar "rosto não detectado" num quarto sem luz não.
     */
    fun evaluate(
        faceCount: Int,
        meanLuma: Float,
        faceHeightRatio: Float,
    ): CaptureIssue =
        when {
            meanLuma < DARK_LUMA -> CaptureIssue.TOO_DARK
            meanLuma > BRIGHT_LUMA -> CaptureIssue.TOO_BRIGHT
            faceCount <= 0 -> CaptureIssue.NO_FACE
            faceCount > 1 -> CaptureIssue.MULTIPLE_FACES
            faceHeightRatio < MIN_FACE_HEIGHT_RATIO -> CaptureIssue.FACE_TOO_FAR
            faceHeightRatio > MAX_FACE_HEIGHT_RATIO -> CaptureIssue.FACE_TOO_CLOSE
            else -> CaptureIssue.NONE
        }
}

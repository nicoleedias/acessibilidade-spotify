package com.sac.acessibilidade.domain.gesture

/**
 * Thresholds de calibração por usuário, em graus (pseudo-escala geométrica).
 * Valores padrão funcionam para a maioria das pessoas; o fluxo de calibração
 * os substitui pelos valores reais medidos com MediaPipe Face Mesh (UC02).
 */
data class CalibrationThresholds(
    val rollRightDeg: Float = 15f,
    val rollLeftDeg: Float = 15f,
    val pitchUpDeg: Float = 12f,
    val pitchDownDeg: Float = 12f,
    val yawRightDeg: Float = 20f,
    val yawLeftDeg: Float = 20f,
    val blinkThreshold: Float = 0.5f,
    /** Amplitude mínima de pitch (relativa ao baseline) para confirmar um NOD. */
    val nodPitchAmplitudeDeg: Float = 12f,
    /**
     * Polaridade de cada eixo APRENDIDA na calibração (+1 ou -1).
     * Em vez de assumir a convenção de sinais da câmera/estimador (que varia com
     * espelhamento e rotação do sensor), a calibração observa o sinal real produzido
     * quando o usuário se move na direção pedida. Elimina por construção qualquer
     * inversão direita/esquerda, em qualquer dispositivo.
     */
    val rollSign: Float = 1f,
    val pitchSign: Float = 1f,
    val yawSign: Float = 1f,
)

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
)

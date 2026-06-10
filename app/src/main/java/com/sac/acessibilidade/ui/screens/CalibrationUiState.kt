package com.sac.acessibilidade.ui.screens

enum class CalibrationStep {
    NEUTRAL,
    TILT_RIGHT,
    TILT_LEFT,
    TILT_UP,
    TILT_DOWN,
    TURN_RIGHT,
    TURN_LEFT,
    DONE,
}

data class CalibrationUiState(
    val step: CalibrationStep = CalibrationStep.NEUTRAL,
    val holdProgress: Float = 0f,
    val isHolding: Boolean = false,
    val isSaving: Boolean = false,
    /** Ângulo atual do eixo relevante para o passo, relativo à pose neutra (graus, abs). */
    val currentAngleDeg: Float = 0f,
    /** Um rosto utilizável está sendo detectado neste instante. */
    val faceDetected: Boolean = false,
    /** Captura da pose neutra em andamento (passo NEUTRAL). */
    val isCapturingNeutral: Boolean = false,
    /** Progresso 0..1 da captura da pose neutra. */
    val neutralProgress: Float = 0f,
    /** Usuário atingiu a amplitude mínima exigida para confirmar o passo atual. */
    val isAtLimit: Boolean = false,
    /** Mensagem de orientação após uma tentativa inválida (ex.: movimento pequeno). */
    val retryMessage: String? = null,
)

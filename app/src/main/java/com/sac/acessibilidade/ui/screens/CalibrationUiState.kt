package com.sac.acessibilidade.ui.screens

enum class CalibrationStep {
    NEUTRAL,
    TILT_RIGHT,
    TILT_LEFT,
    TILT_UP,
    TILT_DOWN,
    DONE,
}

data class CalibrationUiState(
    val step: CalibrationStep = CalibrationStep.NEUTRAL,
    val holdProgress: Float = 0f,
    val isSaving: Boolean = false,
)

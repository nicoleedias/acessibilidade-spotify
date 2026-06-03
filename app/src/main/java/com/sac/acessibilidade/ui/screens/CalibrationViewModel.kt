package com.sac.acessibilidade.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sac.acessibilidade.data.calibration.CalibrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalibrationViewModel
    @Inject
    constructor(
        private val calibrationRepository: CalibrationRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CalibrationUiState())
        val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()

        private var holdJob: Job? = null

        /** Chamado pelo botão "Começar" (step NEUTRAL) e pelo timer automático nos outros steps. */
        fun advance() {
            holdJob?.cancel()
            val next =
                when (_uiState.value.step) {
                    CalibrationStep.NEUTRAL -> CalibrationStep.TILT_RIGHT
                    CalibrationStep.TILT_RIGHT -> CalibrationStep.TILT_LEFT
                    CalibrationStep.TILT_LEFT -> CalibrationStep.TILT_UP
                    CalibrationStep.TILT_UP -> CalibrationStep.TILT_DOWN
                    CalibrationStep.TILT_DOWN -> CalibrationStep.DONE
                    CalibrationStep.DONE -> CalibrationStep.DONE
                }
            _uiState.update { it.copy(step = next, holdProgress = 0f) }
            when {
                next == CalibrationStep.DONE -> saveThresholds()
                next != CalibrationStep.NEUTRAL -> startHoldTimer()
            }
        }

        private fun startHoldTimer() {
            holdJob =
                viewModelScope.launch {
                    val stepDelayMs = HOLD_DURATION_MS / HOLD_STEPS
                    for (i in 0 until HOLD_STEPS) {
                        delay(stepDelayMs)
                        _uiState.update { it.copy(holdProgress = (i + 1f) / HOLD_STEPS) }
                    }
                    advance()
                }
        }

        private fun saveThresholds() {
            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true) }
                calibrationRepository.saveDefaultThresholds()
                _uiState.update { it.copy(isSaving = false) }
            }
        }

        override fun onCleared() {
            super.onCleared()
            holdJob?.cancel()
        }

        companion object {
            private const val HOLD_DURATION_MS = 2_500L
            private const val HOLD_STEPS = 50
        }
    }

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

        /**
         * Chamado pelo botão "Começar" (NEUTRAL) e pelo botão "Concluído" (DONE).
         * NÃO avança nos passos de direção — para esses, o usuário usa [confirmPosition].
         */
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
            _uiState.update { it.copy(step = next, holdProgress = 0f, isHolding = false) }
            if (next == CalibrationStep.DONE) saveThresholds()
        }

        /**
         * Chamado quando o usuário confirma que está na posição correta.
         * Inicia um hold de [HOLD_DURATION_MS] ms com feedback de progresso.
         * Quando completo, avança automaticamente para o próximo passo.
         * Pode ser chamado novamente para reiniciar (usuário perdeu a posição).
         *
         * TODO (UC02): substituir pelo hold timer do MediaPipe — avançar quando
         * os landmarks confirmarem que o ângulo está acima do threshold detectado.
         */
        fun confirmPosition() {
            holdJob?.cancel()
            _uiState.update { it.copy(isHolding = true, holdProgress = 0f) }
            holdJob =
                viewModelScope.launch {
                    val stepDelayMs = HOLD_DURATION_MS / HOLD_STEPS
                    for (i in 0 until HOLD_STEPS) {
                        delay(stepDelayMs)
                        _uiState.update { it.copy(holdProgress = (i + 1f) / HOLD_STEPS) }
                    }
                    _uiState.update { it.copy(isHolding = false) }
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
            private const val HOLD_DURATION_MS = 2_000L
            private const val HOLD_STEPS = 40
        }
    }

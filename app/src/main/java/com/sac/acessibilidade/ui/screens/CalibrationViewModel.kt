package com.sac.acessibilidade.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sac.acessibilidade.data.calibration.CalibrationRepository
import com.sac.acessibilidade.domain.gesture.CalibrationThresholdCalculator
import com.sac.acessibilidade.domain.gesture.CalibrationThresholdCalculator.Axis
import com.sac.acessibilidade.vision.CalibrationPoseAnalyzer
import com.sac.acessibilidade.vision.HeadPoseEstimator.HeadPose
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max

@HiltViewModel
class CalibrationViewModel
    @Inject
    constructor(
        private val calibrationRepository: CalibrationRepository,
        @ApplicationContext context: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CalibrationUiState())
        val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()

        /** Exposto à tela para ser ligado ao ImageAnalysis do CameraX. */
        val poseAnalyzer = CalibrationPoseAnalyzer(context)

        private var holdJob: Job? = null

        // ── Pose neutra (medida no passo NEUTRAL; tudo é relativo a ela) ───────
        private val neutralSamples = ArrayDeque<HeadPose>(NEUTRAL_STEPS)
        private var neutral: HeadPose? = null

        // ── Picos medidos por eixo, relativos ao neutro ────────────────────────
        private var peakRollRight = 0f
        private var peakRollLeft = 0f
        private var peakPitchUp = 0f
        private var peakPitchDown = 0f
        private var peakYawRight = 0f
        private var peakYawLeft = 0f

        init {
            poseAnalyzer.initialize()
            collectPose()
        }

        // ── Coleta contínua da pose ────────────────────────────────────────────

        private fun collectPose() {
            viewModelScope.launch {
                poseAnalyzer.poseFlow.collect { rawPose ->
                    if (rawPose == null) {
                        onFaceLost()
                        return@collect
                    }
                    onPose(rawPose)
                }
            }
        }

        private fun onFaceLost() {
            val wasHolding = _uiState.value.isHolding
            _uiState.update { it.copy(faceDetected = false, currentAngleDeg = 0f, isAtLimit = false) }
            if (wasHolding) abortHold(MSG_FACE_LOST)
            if (_uiState.value.isCapturingNeutral) abortNeutral()
        }

        private fun onPose(rawPose: HeadPose) {
            val state = _uiState.value
            if (state.isCapturingNeutral) neutralSamples.addLast(rawPose)

            val relative = neutral?.let { rawPose - it } ?: ZERO_POSE
            val angle = angleForStep(state.step, relative)
            val atLimit = angle >= entryMinFor(state.step)
            _uiState.update {
                it.copy(faceDetected = true, currentAngleDeg = angle, isAtLimit = atLimit)
            }

            if (state.isHolding) {
                recordPeak(state.step, relative)
                if (angle < entryMinFor(state.step) * HOLD_KEEP_RATIO) abortHold(MSG_LEFT_POSITION)
            }
        }

        // ── Passo NEUTRAL: captura da pose de repouso ──────────────────────────

        fun startNeutralCapture() {
            if (!_uiState.value.faceDetected) {
                setRetry(MSG_NO_FACE)
                return
            }
            holdJob?.cancel()
            neutralSamples.clear()
            neutral = null
            _uiState.update {
                it.copy(isCapturingNeutral = true, neutralProgress = 0f, retryMessage = null)
            }
            holdJob =
                viewModelScope.launch {
                    val stepDelayMs = NEUTRAL_DURATION_MS / NEUTRAL_STEPS
                    for (i in 0 until NEUTRAL_STEPS) {
                        delay(stepDelayMs)
                        _uiState.update { it.copy(neutralProgress = (i + 1f) / NEUTRAL_STEPS) }
                    }
                    finishNeutralCapture()
                }
        }

        private fun finishNeutralCapture() {
            if (neutralSamples.size < MIN_NEUTRAL_SAMPLES) {
                abortNeutral()
                return
            }
            neutral = neutralSamples.average()
            _uiState.update { it.copy(isCapturingNeutral = false, neutralProgress = 1f) }
            advance()
        }

        private fun abortNeutral() {
            holdJob?.cancel()
            neutralSamples.clear()
            _uiState.update {
                it.copy(isCapturingNeutral = false, neutralProgress = 0f, retryMessage = MSG_HOLD_STILL)
            }
        }

        // ── Confirmação de um passo direcional ─────────────────────────────────

        fun confirmPosition() {
            val state = _uiState.value
            if (!state.faceDetected) {
                setRetry(MSG_NO_FACE)
                return
            }
            if (!state.isAtLimit) {
                setRetry(MSG_GO_FURTHER)
                return
            }
            holdJob?.cancel()
            resetPeak(state.step)
            _uiState.update { it.copy(isHolding = true, holdProgress = 0f, retryMessage = null) }
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

        private fun abortHold(message: String) {
            holdJob?.cancel()
            _uiState.update {
                it.copy(isHolding = false, holdProgress = 0f, retryMessage = message)
            }
        }

        // ── Avanço de etapas ───────────────────────────────────────────────────

        fun advance() {
            holdJob?.cancel()
            val next =
                when (_uiState.value.step) {
                    CalibrationStep.NEUTRAL -> CalibrationStep.TILT_RIGHT
                    CalibrationStep.TILT_RIGHT -> CalibrationStep.TILT_LEFT
                    CalibrationStep.TILT_LEFT -> CalibrationStep.TILT_UP
                    CalibrationStep.TILT_UP -> CalibrationStep.TILT_DOWN
                    CalibrationStep.TILT_DOWN -> CalibrationStep.TURN_RIGHT
                    CalibrationStep.TURN_RIGHT -> CalibrationStep.TURN_LEFT
                    CalibrationStep.TURN_LEFT -> CalibrationStep.DONE
                    CalibrationStep.DONE -> CalibrationStep.DONE
                }
            _uiState.update {
                it.copy(
                    step = next,
                    holdProgress = 0f,
                    isHolding = false,
                    currentAngleDeg = 0f,
                    isAtLimit = false,
                    retryMessage = null,
                )
            }
            if (next == CalibrationStep.DONE) saveThresholds()
        }

        private fun saveThresholds() {
            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true) }
                calibrationRepository.saveThresholds(CalibrationThresholdCalculator.build(measuredPeaks()))
                _uiState.update { it.copy(isSaving = false) }
            }
        }

        private fun measuredPeaks() =
            CalibrationThresholdCalculator.Peaks(
                rollRightDeg = peakRollRight,
                rollLeftDeg = peakRollLeft,
                pitchUpDeg = peakPitchUp,
                pitchDownDeg = peakPitchDown,
                yawRightDeg = peakYawRight,
                yawLeftDeg = peakYawLeft,
            )

        // ── Helpers de medição ─────────────────────────────────────────────────

        private fun angleForStep(
            step: CalibrationStep,
            pose: HeadPose,
        ): Float =
            when (step) {
                CalibrationStep.TILT_RIGHT -> max(0f, pose.roll)
                CalibrationStep.TILT_LEFT -> max(0f, -pose.roll)
                CalibrationStep.TILT_UP -> max(0f, -pose.pitch)
                CalibrationStep.TILT_DOWN -> max(0f, pose.pitch)
                CalibrationStep.TURN_RIGHT -> max(0f, pose.yaw)
                CalibrationStep.TURN_LEFT -> max(0f, -pose.yaw)
                else -> 0f
            }

        private fun recordPeak(
            step: CalibrationStep,
            pose: HeadPose,
        ) {
            when (step) {
                CalibrationStep.TILT_RIGHT -> peakRollRight = max(peakRollRight, pose.roll)
                CalibrationStep.TILT_LEFT -> peakRollLeft = max(peakRollLeft, abs(pose.roll))
                CalibrationStep.TILT_UP -> peakPitchUp = max(peakPitchUp, abs(pose.pitch))
                CalibrationStep.TILT_DOWN -> peakPitchDown = max(peakPitchDown, pose.pitch)
                CalibrationStep.TURN_RIGHT -> peakYawRight = max(peakYawRight, pose.yaw)
                CalibrationStep.TURN_LEFT -> peakYawLeft = max(peakYawLeft, abs(pose.yaw))
                else -> Unit
            }
        }

        private fun resetPeak(step: CalibrationStep) {
            when (step) {
                CalibrationStep.TILT_RIGHT -> peakRollRight = 0f
                CalibrationStep.TILT_LEFT -> peakRollLeft = 0f
                CalibrationStep.TILT_UP -> peakPitchUp = 0f
                CalibrationStep.TILT_DOWN -> peakPitchDown = 0f
                CalibrationStep.TURN_RIGHT -> peakYawRight = 0f
                CalibrationStep.TURN_LEFT -> peakYawLeft = 0f
                else -> Unit
            }
        }

        private fun entryMinFor(step: CalibrationStep): Float =
            when (step) {
                CalibrationStep.TILT_RIGHT, CalibrationStep.TILT_LEFT ->
                    CalibrationThresholdCalculator.entryMinDegFor(Axis.ROLL)
                CalibrationStep.TILT_UP, CalibrationStep.TILT_DOWN ->
                    CalibrationThresholdCalculator.entryMinDegFor(Axis.PITCH)
                CalibrationStep.TURN_RIGHT, CalibrationStep.TURN_LEFT ->
                    CalibrationThresholdCalculator.entryMinDegFor(Axis.YAW)
                else -> Float.MAX_VALUE
            }

        private fun setRetry(message: String) {
            _uiState.update { it.copy(retryMessage = message) }
        }

        private fun ArrayDeque<HeadPose>.average(): HeadPose =
            HeadPose(
                roll = map { it.roll }.average().toFloat(),
                pitch = map { it.pitch }.average().toFloat(),
                yaw = map { it.yaw }.average().toFloat(),
            )

        override fun onCleared() {
            super.onCleared()
            holdJob?.cancel()
            poseAnalyzer.release()
        }

        companion object {
            private val ZERO_POSE = HeadPose(0f, 0f, 0f)

            private const val HOLD_DURATION_MS = 1_500L
            private const val HOLD_STEPS = 30

            private const val NEUTRAL_DURATION_MS = 1_500L
            private const val NEUTRAL_STEPS = 30
            private const val MIN_NEUTRAL_SAMPLES = 8

            /** Durante o hold, permite cair até 70% do limiar antes de abortar (tolera tremor). */
            private const val HOLD_KEEP_RATIO = 0.7f

            private const val MSG_NO_FACE = "Posicione o rosto dentro do oval"
            private const val MSG_FACE_LOST = "Rosto não detectado — vamos repetir este passo"
            private const val MSG_LEFT_POSITION = "Você saiu da posição — tente manter até o fim"
            private const val MSG_GO_FURTHER = "Vá até o limite confortável antes de confirmar"
            private const val MSG_HOLD_STILL = "Fique parado olhando para a frente e tente de novo"
        }
    }

package com.sac.acessibilidade.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sac.acessibilidade.data.calibration.CalibrationRepository
import com.sac.acessibilidade.domain.gesture.CalibrationThresholdCalculator
import com.sac.acessibilidade.domain.gesture.CalibrationThresholdCalculator.Axis
import com.sac.acessibilidade.vision.CalibrationPoseAnalyzer
import com.sac.acessibilidade.vision.CaptureIssue
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

        // ── Picos COM SINAL por passo, relativos ao neutro ─────────────────────
        // O sinal é preservado para que o CalibrationThresholdCalculator aprenda a
        // polaridade real de cada eixo neste dispositivo (imune a espelhamento).
        private var peakTiltRight = 0f
        private var peakTiltLeft = 0f
        private var peakTiltUp = 0f
        private var peakTiltDown = 0f
        private var peakTurnRight = 0f
        private var peakTurnLeft = 0f

        init {
            poseAnalyzer.initialize()
            collectPose()
            collectQuality()
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

        /**
         * Acompanha a qualidade do quadro. Um segundo rosto entrando em cena no meio
         * de uma medição invalida a captura: os picos passariam a misturar duas
         * pessoas, então abortamos o passo em vez de salvar um limiar corrompido.
         */
        private fun collectQuality() {
            viewModelScope.launch {
                poseAnalyzer.qualityFlow.collect { issue ->
                    val previous = _uiState.value
                    _uiState.update { it.copy(captureIssue = issue) }
                    if (issue == CaptureIssue.MULTIPLE_FACES) {
                        if (previous.isHolding) abortHold(MSG_MULTIPLE_FACES)
                        if (previous.isCapturingNeutral) abortNeutral(MSG_MULTIPLE_FACES)
                    }
                }
            }
        }

        private fun onFaceLost() {
            val wasHolding = _uiState.value.isHolding
            _uiState.update { it.copy(faceDetected = false, currentAngleDeg = 0f, isAtLimit = false) }
            if (wasHolding) abortHold(MSG_FACE_LOST)
            if (_uiState.value.isCapturingNeutral) abortNeutral(MSG_HOLD_STILL)
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
            blockingMessage()?.let {
                setRetry(it)
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
                abortNeutral(MSG_HOLD_STILL)
                return
            }
            neutral = neutralSamples.average()
            _uiState.update { it.copy(isCapturingNeutral = false, neutralProgress = 1f) }
            advance()
        }

        private fun abortNeutral(message: String) {
            holdJob?.cancel()
            neutralSamples.clear()
            _uiState.update {
                it.copy(isCapturingNeutral = false, neutralProgress = 0f, retryMessage = message)
            }
        }

        // ── Confirmação de um passo direcional ─────────────────────────────────

        fun confirmPosition() {
            val state = _uiState.value
            blockingMessage()?.let {
                setRetry(it)
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
                tiltRight = peakTiltRight,
                tiltLeft = peakTiltLeft,
                tiltUp = peakTiltUp,
                tiltDown = peakTiltDown,
                turnRight = peakTurnRight,
                turnLeft = peakTurnLeft,
            )

        // ── Helpers de medição ─────────────────────────────────────────────────

        /**
         * Ângulo exibido/medido: valor ABSOLUTO do eixo relevante ao passo.
         * A direção não importa aqui — a polaridade é aprendida pelo sinal do pico,
         * então qualquer convenção de câmera (espelhada ou não) funciona.
         */
        private fun angleForStep(
            step: CalibrationStep,
            pose: HeadPose,
        ): Float =
            when (step) {
                CalibrationStep.TILT_RIGHT, CalibrationStep.TILT_LEFT -> abs(pose.roll)
                CalibrationStep.TILT_UP, CalibrationStep.TILT_DOWN -> abs(pose.pitch)
                CalibrationStep.TURN_RIGHT, CalibrationStep.TURN_LEFT -> abs(pose.yaw)
                else -> 0f
            }

        private fun recordPeak(
            step: CalibrationStep,
            pose: HeadPose,
        ) {
            when (step) {
                CalibrationStep.TILT_RIGHT -> peakTiltRight = maxAbs(peakTiltRight, pose.roll)
                CalibrationStep.TILT_LEFT -> peakTiltLeft = maxAbs(peakTiltLeft, pose.roll)
                CalibrationStep.TILT_UP -> peakTiltUp = maxAbs(peakTiltUp, pose.pitch)
                CalibrationStep.TILT_DOWN -> peakTiltDown = maxAbs(peakTiltDown, pose.pitch)
                CalibrationStep.TURN_RIGHT -> peakTurnRight = maxAbs(peakTurnRight, pose.yaw)
                CalibrationStep.TURN_LEFT -> peakTurnLeft = maxAbs(peakTurnLeft, pose.yaw)
                else -> Unit
            }
        }

        /** Mantém o valor de maior magnitude, preservando o sinal. */
        private fun maxAbs(
            current: Float,
            candidate: Float,
        ): Float = if (abs(candidate) > abs(current)) candidate else current

        private fun resetPeak(step: CalibrationStep) {
            when (step) {
                CalibrationStep.TILT_RIGHT -> peakTiltRight = 0f
                CalibrationStep.TILT_LEFT -> peakTiltLeft = 0f
                CalibrationStep.TILT_UP -> peakTiltUp = 0f
                CalibrationStep.TILT_DOWN -> peakTiltDown = 0f
                CalibrationStep.TURN_RIGHT -> peakTurnRight = 0f
                CalibrationStep.TURN_LEFT -> peakTurnLeft = 0f
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

        /**
         * Motivo que impede iniciar/confirmar uma medição agora, ou null se o quadro
         * está utilizável. Dois rostos vêm antes de "sem rosto" porque é o caso em que
         * o usuário precisa agir sobre o ambiente, não sobre a própria posição.
         */
        private fun blockingMessage(): String? {
            val state = _uiState.value
            return when {
                state.captureIssue == CaptureIssue.MULTIPLE_FACES -> MSG_MULTIPLE_FACES
                !state.faceDetected -> MSG_NO_FACE
                else -> null
            }
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
            private const val MSG_MULTIPLE_FACES =
                "Mais de um rosto na câmera — só o seu deve aparecer durante a calibração"
        }
    }

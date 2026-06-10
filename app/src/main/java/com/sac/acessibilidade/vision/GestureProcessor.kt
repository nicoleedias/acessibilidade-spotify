package com.sac.acessibilidade.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.sac.acessibilidade.data.calibration.CalibrationRepository
import com.sac.acessibilidade.domain.gesture.CalibrationThresholds
import com.sac.acessibilidade.domain.gesture.Gesture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Processador de gestos cefálicos via MediaPipe Face Mesh.
 *
 * Implementa [ImageAnalysis.Analyzer] para receber frames diretamente do CameraX.
 * Roda inferência em LIVE_STREAM (assíncrono), emite gestos detectados via [gestureFlow].
 *
 * Proteções contra falsos positivos:
 * - Baseline neutro: primeiros [BASELINE_COUNT] frames constroem a posição de repouso;
 *   todos os ângulos são relativos a ela — câmera levemente inclinada não causa disparos.
 * - Sustain: gesto de cabeça deve persistir em [SUSTAIN_FRAMES] frames consecutivos.
 * - Cooldown: [COOLDOWN_MS] entre comandos; [NodDetector] tem cooldown próprio para NOD.
 */
@Singleton
class GestureProcessor
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val calibrationRepository: CalibrationRepository,
    ) : ImageAnalysis.Analyzer {
        private val _gestureFlow = MutableSharedFlow<Gesture>(extraBufferCapacity = 1)
        val gestureFlow: SharedFlow<Gesture> = _gestureFlow.asSharedFlow()

        private var faceLandmarker: FaceLandmarker? = null
        private var lastGestureMs = 0L
        private val nodDetector = NodDetector()

        // Baseline neutro: média dos primeiros BASELINE_COUNT frames válidos
        private val baselineSamples = ArrayDeque<HeadPoseEstimator.HeadPose>(BASELINE_COUNT)
        private var neutralBaseline: HeadPoseEstimator.HeadPose? = null

        // Sustain: exige SUSTAIN_FRAMES frames consecutivos com o mesmo gesto
        private var sustainedGesture: Gesture? = null
        private var sustainCount = 0

        fun initialize() {
            if (faceLandmarker != null) return
            runCatching {
                val options =
                    FaceLandmarker.FaceLandmarkerOptions
                        .builder()
                        .setBaseOptions(
                            BaseOptions.builder().setModelAssetPath(MODEL_ASSET).build(),
                        )
                        .setRunningMode(RunningMode.LIVE_STREAM)
                        .setNumFaces(1)
                        .setMinFaceDetectionConfidence(0.5f)
                        .setMinFacePresenceConfidence(0.5f)
                        .setMinTrackingConfidence(0.5f)
                        .setOutputFaceBlendshapes(true)
                        .setResultListener { result, _ -> onResult(result) }
                        .setErrorListener { _ -> }
                        .build()
                faceLandmarker = FaceLandmarker.createFromOptions(context, options)
            }
        }

        /** Reseta baseline e contadores — chamar sempre que a câmera de gestos é ativada. */
        fun resetBaseline() {
            baselineSamples.clear()
            neutralBaseline = null
            sustainedGesture = null
            sustainCount = 0
            lastGestureMs = 0L
        }

        override fun analyze(imageProxy: ImageProxy) {
            val landmarker =
                faceLandmarker ?: run {
                    imageProxy.close()
                    return
                }

            val timestamp = imageProxy.imageInfo.timestamp
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val bitmap = imageProxy.toBitmap()
            imageProxy.close()

            val rotatedBitmap = bitmap.applyRotation(rotationDegrees)
            val mpImage = BitmapImageBuilder(rotatedBitmap).build()
            runCatching { landmarker.detectAsync(mpImage, timestamp) }
        }

        private fun onResult(result: FaceLandmarkerResult) {
            if (result.faceLandmarks().isEmpty()) return
            val now = SystemClock.uptimeMillis()
            val landmarks = result.faceLandmarks()[0]
            val thresholds = calibrationRepository.getThresholds()
            val detected = detectGesture(result, landmarks, thresholds, now)
            if (detected != null) {
                lastGestureMs = now
                _gestureFlow.tryEmit(detected)
            }
        }

        private fun detectGesture(
            result: FaceLandmarkerResult,
            landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
            thresholds: CalibrationThresholds,
            now: Long,
        ): Gesture? {
            val rawPose = HeadPoseEstimator.estimate(landmarks) ?: return null
            collectBaseline(rawPose)
            val baseline = neutralBaseline ?: return null // bloqueia até baseline pronto
            val pose = rawPose - baseline
            return classifyAdjustedPose(result, pose, thresholds, now)
        }

        private fun classifyAdjustedPose(
            result: FaceLandmarkerResult,
            pose: HeadPoseEstimator.HeadPose,
            thresholds: CalibrationThresholds,
            now: Long,
        ): Gesture? {
            // NOD: NodDetector gerencia cooldown próprio; checamos também o cooldown global
            if (nodDetector.feed(pose.pitch, now, thresholds.nodPitchAmplitudeDeg) &&
                now - lastGestureMs >= COOLDOWN_MS
            ) {
                sustainedGesture = null
                sustainCount = 0
                return Gesture.NOD
            }
            if (now - lastGestureMs < COOLDOWN_MS) return null
            val blendshapes =
                if (result.faceBlendshapes().isPresent) {
                    result.faceBlendshapes().get().getOrNull(0)
                } else {
                    null
                }
            val candidate = GestureClassifier.classifyWithPose(pose, blendshapes, thresholds)
            return withSustain(candidate)
        }

        /**
         * Exige que o mesmo gesto apareça em [SUSTAIN_FRAMES] frames consecutivos
         * antes de confirmar — elimina disparos por cruzamentos momentâneos do threshold.
         */
        private fun withSustain(candidate: Gesture?): Gesture? =
            if (candidate != null && candidate == sustainedGesture) {
                sustainCount++
                if (sustainCount >= SUSTAIN_FRAMES) {
                    sustainCount = 0
                    sustainedGesture = null
                    candidate
                } else {
                    null
                }
            } else {
                sustainedGesture = candidate
                sustainCount = if (candidate != null) 1 else 0
                null
            }

        private fun collectBaseline(pose: HeadPoseEstimator.HeadPose) {
            if (neutralBaseline != null) return
            baselineSamples.addLast(pose)
            if (baselineSamples.size >= BASELINE_COUNT) {
                neutralBaseline =
                    HeadPoseEstimator.HeadPose(
                        roll = baselineSamples.map { it.roll }.average().toFloat(),
                        pitch = baselineSamples.map { it.pitch }.average().toFloat(),
                        yaw = baselineSamples.map { it.yaw }.average().toFloat(),
                    )
            }
        }

        fun release() {
            faceLandmarker?.close()
            faceLandmarker = null
        }

        private fun Bitmap.applyRotation(degrees: Int): Bitmap {
            if (degrees == 0) return this
            val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
            return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
                .also { if (it !== this) recycle() }
        }

        companion object {
            private const val MODEL_ASSET = "face_landmarker.task"
            private const val COOLDOWN_MS = 1_000L
            private const val BASELINE_COUNT = 30 // ~2s a 15fps para capturar pose neutra
            private const val SUSTAIN_FRAMES = 3 // frames consecutivos para confirmar gesto
        }
    }

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
        private val stabilizer = HeadGestureStabilizer()

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
                        .setOutputFacialTransformationMatrixes(true)
                        .setResultListener { result, _ -> onResult(result) }
                        .setErrorListener { _ -> }
                        .build()
                faceLandmarker = FaceLandmarker.createFromOptions(context, options)
            }
        }

        /** Reseta baseline e contadores — chamar sempre que a câmera de gestos é ativada. */
        fun resetBaseline() {
            stabilizer.reset()
            nodDetector.reset()
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
            val thresholds = calibrationRepository.getThresholds()
            val detected = detectGesture(result, thresholds, now)
            if (detected != null) {
                lastGestureMs = now
                _gestureFlow.tryEmit(detected)
            }
        }

        private fun detectGesture(
            result: FaceLandmarkerResult,
            thresholds: CalibrationThresholds,
            now: Long,
        ): Gesture? {
            // Matriz de transformação 3D quando disponível (yaw/pitch reais), senão geometria 2D
            val rawPose = HeadPoseEstimator.fromResult(result) ?: return null
            val state = stabilizer.update(rawPose, thresholds)
            val relative = state.relativePose ?: return null // baseline ainda aquecendo
            // NOD alimenta sempre (mantém buffer contínuo), mesmo durante o cooldown
            val nod = nodDetector.feed(relative.pitch, now, thresholds.nodPitchAmplitudeDeg)
            return if (now - lastGestureMs < COOLDOWN_MS) {
                null
            } else {
                firstGesture(nod, state.gesture, result, thresholds)
            }
        }

        /** Prioridade: NOD (temporal) → gesto direcional estabilizado → piscada. */
        private fun firstGesture(
            nod: Boolean,
            directional: Gesture?,
            result: FaceLandmarkerResult,
            thresholds: CalibrationThresholds,
        ): Gesture? {
            if (nod) return Gesture.NOD
            if (directional != null) return directional
            val blendshapes =
                if (result.faceBlendshapes().isPresent) {
                    result.faceBlendshapes().get().getOrNull(0)
                } else {
                    null
                }
            return GestureClassifier.classifyBlink(blendshapes, thresholds.blinkThreshold)
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
        }
    }

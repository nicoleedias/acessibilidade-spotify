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
 * Dependência do modelo: o arquivo `face_landmarker.task` deve estar em
 * `app/src/main/assets/`. A task Gradle `downloadFaceLandmarkerModel` baixa
 * automaticamente na primeira build se o arquivo não existir.
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

        /** Inicializa o FaceLandmarker. Seguro chamar múltiplas vezes (guarda-se com idem­potência). */
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

        /** Chamado pelo CameraX para cada frame. Converte para Bitmap, envia ao MediaPipe. */
        override fun analyze(imageProxy: ImageProxy) {
            val landmarker =
                faceLandmarker ?: run {
                    imageProxy.close()
                    return
                }

            val timestamp = imageProxy.imageInfo.timestamp
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val bitmap = imageProxy.toBitmap()
            imageProxy.close() // fecha cedo — dados já copiados no Bitmap

            val rotatedBitmap = bitmap.applyRotation(rotationDegrees)
            val mpImage = BitmapImageBuilder(rotatedBitmap).build()
            runCatching { landmarker.detectAsync(mpImage, timestamp) }
        }

        private fun onResult(result: FaceLandmarkerResult) {
            if (result.faceLandmarks().isEmpty()) return

            val now = SystemClock.uptimeMillis()
            if (now - lastGestureMs < COOLDOWN_MS) return

            val landmarks = result.faceLandmarks()[0]
            val blendshapes =
                if (result.faceBlendshapes().isPresent) {
                    result.faceBlendshapes().get().getOrNull(0)
                } else {
                    null
                }
            val thresholds = calibrationRepository.getThresholds()

            val gesture = GestureClassifier.classify(landmarks, blendshapes, thresholds) ?: return
            lastGestureMs = now
            _gestureFlow.tryEmit(gesture)
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
            private const val COOLDOWN_MS = 800L
        }
    }

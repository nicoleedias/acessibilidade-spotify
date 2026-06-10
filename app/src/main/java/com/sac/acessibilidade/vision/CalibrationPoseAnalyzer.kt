package com.sac.acessibilidade.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Analisador leve para a tela de calibração.
 *
 * Emite [HeadPoseEstimator.HeadPose] continuamente via [poseFlow].
 * Não classifica gestos — apenas mede os ângulos brutos para que o
 * ViewModel registre os picos por direção durante o hold do usuário.
 *
 * Não é Singleton: cada sessão de calibração cria e libera sua própria instância.
 */
class CalibrationPoseAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {
    private val _poseFlow = MutableStateFlow<HeadPoseEstimator.HeadPose?>(null)
    val poseFlow: StateFlow<HeadPoseEstimator.HeadPose?> = _poseFlow.asStateFlow()

    private var faceLandmarker: FaceLandmarker? = null

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
                    .setOutputFaceBlendshapes(false)
                    .setResultListener { result, _ ->
                        val landmarks =
                            result.faceLandmarks().firstOrNull() ?: run {
                                _poseFlow.value = null
                                return@setResultListener
                            }
                        _poseFlow.value = HeadPoseEstimator.estimate(landmarks)
                    }
                    .setErrorListener { _ -> _poseFlow.value = null }
                    .build()
            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
        }
    }

    override fun analyze(imageProxy: ImageProxy) {
        val landmarker =
            faceLandmarker ?: run {
                imageProxy.close()
                return
            }
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val timestamp = imageProxy.imageInfo.timestamp
        val bitmap = imageProxy.toBitmap()
        imageProxy.close()
        val rotated = bitmap.applyRotation(rotationDegrees)
        val mpImage = BitmapImageBuilder(rotated).build()
        runCatching { landmarker.detectAsync(mpImage, timestamp) }
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
    }
}

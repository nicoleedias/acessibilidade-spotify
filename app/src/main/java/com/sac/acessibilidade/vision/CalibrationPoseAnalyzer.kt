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
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Analisador leve para a tela de calibração.
 *
 * Emite [HeadPoseEstimator.HeadPose] continuamente via [poseFlow] e a qualidade da
 * captura via [qualityFlow]. Não classifica gestos — apenas mede os ângulos brutos
 * para que o ViewModel registre os picos por direção durante o hold do usuário.
 *
 * Não é Singleton: cada sessão de calibração cria e libera sua própria instância.
 *
 * **Privacidade (LGPD):** o frame é usado em memória e descartado. A única coisa
 * derivada dele é a luminância MÉDIA (um número agregado) — nenhum pixel, landmark
 * ou recorte de rosto é gravado, logado ou enviado para qualquer lugar.
 */
class CalibrationPoseAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {
    private val _poseFlow = MutableStateFlow<HeadPoseEstimator.HeadPose?>(null)
    val poseFlow: StateFlow<HeadPoseEstimator.HeadPose?> = _poseFlow.asStateFlow()

    private val _qualityFlow = MutableStateFlow(CaptureIssue.NO_FACE)
    val qualityFlow: StateFlow<CaptureIssue> = _qualityFlow.asStateFlow()

    private var faceLandmarker: FaceLandmarker? = null

    /**
     * Medida no thread de análise, lida no callback do MediaPipe (outro thread).
     * Volatile garante que o callback enxergue o valor do frame recém-enviado.
     */
    @Volatile
    private var lastMeanLuma = 0f

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
                    // 2 e não 1: precisamos SABER que há mais de um rosto no quadro para
                    // bloquear a calibração (regra de um rosto por conta, UC02). Com
                    // numFaces=1 o segundo rosto seria silenciosamente ignorado.
                    .setNumFaces(MAX_TRACKED_FACES)
                    .setMinFaceDetectionConfidence(0.5f)
                    .setMinFacePresenceConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .setOutputFaceBlendshapes(false)
                    .setResultListener { result, _ ->
                        // Mesma fonte de pose do GestureProcessor — calibração e runtime
                        // DEVEM medir com o mesmo estimador (matriz 3D com fallback 2D)
                        _poseFlow.value = HeadPoseEstimator.fromResult(result)
                        _qualityFlow.value = evaluateQuality(result)
                    }
                    .setErrorListener { _ ->
                        _poseFlow.value = null
                        _qualityFlow.value = CaptureIssue.NO_FACE
                    }
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
        // Antes de rotacionar: applyRotation pode reciclar o bitmap original.
        // A rotação não altera a média, então medir aqui é equivalente e mais seguro.
        lastMeanLuma = bitmap.meanLuma()
        val rotated = bitmap.applyRotation(rotationDegrees)
        val mpImage = BitmapImageBuilder(rotated).build()
        runCatching { landmarker.detectAsync(mpImage, timestamp) }
    }

    fun release() {
        faceLandmarker?.close()
        faceLandmarker = null
    }

    private fun evaluateQuality(result: FaceLandmarkerResult): CaptureIssue {
        val faces = result.faceLandmarks()
        return CaptureQualityEvaluator.evaluate(
            faceCount = faces.size,
            meanLuma = lastMeanLuma,
            faceHeightRatio = faces.firstOrNull()?.heightRatio() ?: 0f,
        )
    }

    /**
     * Altura do rosto como fração da altura do quadro. Os landmarks já são
     * normalizados em 0..1, então basta a amplitude vertical.
     */
    private fun List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>.heightRatio(): Float {
        if (isEmpty()) return 0f
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (landmark in this) {
            val y = landmark.y()
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        return (maxY - minY).coerceIn(0f, 1f)
    }

    /**
     * Luminância média do quadro (0..255), por amostragem esparsa.
     *
     * Lê [LUMA_ROWS] linhas inteiras em bloco (getPixels) e percorre as colunas com
     * passo — ~1.000 amostras por frame em vez de milhões. A conversão usa pesos
     * inteiros da ITU-R BT.601 para evitar aritmética de ponto flutuante por pixel.
     */
    private fun Bitmap.meanLuma(): Float {
        if (width <= 0 || height <= 0) return 0f
        val rowCount = LUMA_ROWS.coerceAtMost(height)
        val colStep = (width / LUMA_COLS).coerceAtLeast(1)
        val row = IntArray(width)
        var sum = 0L
        var count = 0
        for (i in 0 until rowCount) {
            val y = i * height / rowCount
            getPixels(row, 0, width, 0, y, width, 1)
            var x = 0
            while (x < width) {
                val pixel = row[x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                sum += ((R_WEIGHT * r) + (G_WEIGHT * g) + (B_WEIGHT * b)) shr LUMA_SHIFT
                count++
                x += colStep
            }
        }
        return if (count == 0) 0f else sum.toFloat() / count
    }

    private fun Bitmap.applyRotation(degrees: Int): Bitmap {
        if (degrees == 0) return this
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
            .also { if (it !== this) recycle() }
    }

    companion object {
        private const val MODEL_ASSET = "face_landmarker.task"
        private const val MAX_TRACKED_FACES = 2

        private const val LUMA_ROWS = 24
        private const val LUMA_COLS = 48

        // Pesos BT.601 (0.299 / 0.587 / 0.114) escalados por 256.
        private const val R_WEIGHT = 77
        private const val G_WEIGHT = 150
        private const val B_WEIGHT = 29
        private const val LUMA_SHIFT = 8
    }
}

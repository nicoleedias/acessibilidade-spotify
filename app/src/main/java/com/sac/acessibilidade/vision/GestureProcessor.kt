package com.sac.acessibilidade.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.sac.acessibilidade.data.calibration.CalibrationRepository
import com.sac.acessibilidade.domain.gesture.CalibrationThresholds
import com.sac.acessibilidade.domain.gesture.Gesture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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

        /**
         * Limiares em cache. Antes eram relidos do disco a cada frame — onze buscas
         * em SharedPreferences e um objeto novo por quadro, trinta vezes por segundo.
         * São recarregados em [resetBaseline], chamado sempre que o rastreamento começa,
         * então uma recalibração continua sendo refletida.
         */
        @Volatile
        private var cachedThresholds: CalibrationThresholds? = null
        private val nodDetector = NodDetector()
        private val stabilizer = HeadGestureStabilizer()
        private val blinkMonitor = BlinkReliabilityMonitor()

        /**
         * Falso quando o olho está encoberto — tipicamente óculos escuros — e a
         * piscada deixou de ser utilizável. A tela avisa, em vez de deixar o
         * usuário repetindo um gesto que nunca vai disparar.
         */
        private val _blinkReliable = MutableStateFlow(true)
        val blinkReliable: StateFlow<Boolean> = _blinkReliable.asStateFlow()

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
                        // 0.4 em vez do padrão 0.5: aceita o rosto parcialmente encoberto
                        // por óculos, touca, franja ou barba. O que impede falso positivo
                        // aqui não é a confiança do detector, e sim o estabilizador —
                        // baseline, sustain de 2 quadros e histerese de re-arme.
                        .setMinFaceDetectionConfidence(0.4f)
                        .setMinFacePresenceConfidence(0.4f)
                        .setMinTrackingConfidence(0.4f)
                        .setOutputFaceBlendshapes(true)
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
            blinkMonitor.reset()
            _blinkReliable.value = true
            lastGestureMs = 0L
            cachedThresholds = calibrationRepository.getThresholds()
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
            val thresholds =
                cachedThresholds
                    ?: calibrationRepository.getThresholds().also { cachedThresholds = it }
            val blendshapes = blendshapesOf(result)
            _blinkReliable.value = blinkMonitor.observe(GestureClassifier.maxBlinkScore(blendshapes))
            val detected = detectGesture(result, blendshapes, thresholds, now)
            if (detected != null) {
                lastGestureMs = now
                _gestureFlow.tryEmit(detected)
            }
        }

        private fun blendshapesOf(result: FaceLandmarkerResult): List<Category>? =
            if (result.faceBlendshapes().isPresent) {
                result.faceBlendshapes().get().getOrNull(0)
            } else {
                null
            }

        private fun detectGesture(
            result: FaceLandmarkerResult,
            blendshapes: List<Category>?,
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
                firstGesture(nod, state.gesture, blendshapes, thresholds)
            }
        }

        /** Prioridade: NOD (temporal) → gesto direcional estabilizado → piscada. */
        private fun firstGesture(
            nod: Boolean,
            directional: Gesture?,
            blendshapes: List<Category>?,
            thresholds: CalibrationThresholds,
        ): Gesture? {
            if (nod) return Gesture.NOD
            if (directional != null) return directional
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

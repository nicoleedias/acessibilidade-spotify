package com.sac.acessibilidade.vision

import com.sac.acessibilidade.domain.gesture.CalibrationThresholds
import com.sac.acessibilidade.domain.gesture.Gesture
import com.sac.acessibilidade.vision.HeadPoseEstimator.HeadPose
import kotlin.math.abs
import kotlin.math.min

/**
 * Estabiliza a detecção de gestos cefálicos de cabeça (roll/pitch/yaw), separando a
 * decisão da captura de frames do MediaPipe. **Sem dependências Android** — testável
 * com [HeadPose] sintéticos.
 *
 * Três mecanismos reduzem falsos positivos e gestos perdidos:
 *
 * 1. **Baseline estável** — a pose neutra é fixada quando a cabeça permanece parada
 *    por [baselineStableFrames] frames (faixa de variação < [stableRangeDeg]), evitando
 *    "congelar" uma pose em movimento. Há um fallback: após [maxWarmupFrames] frames o
 *    baseline trava de qualquer forma (média da janela), garantindo que o sistema sempre
 *    fique operacional mesmo que a cabeça nunca fique perfeitamente imóvel.
 * 2. **Baseline adaptativo** — enquanto o usuário está próximo do neutro, a referência é
 *    puxada lentamente (EMA com [driftAlpha]) para a pose atual, corrigindo mudanças de
 *    postura ao longo da sessão.
 * 3. **Histerese / re-arme** — após disparar um gesto, um novo disparo só é permitido
 *    depois que a cabeça retorna para perto do neutro ([releaseRatio] do limiar),
 *    impedindo disparos repetidos ao manter a cabeça inclinada.
 *
 * O cooldown global entre comandos distintos é responsabilidade do chamador
 * ([GestureProcessor]); aqui tratamos apenas estabilização e sustain.
 */
class HeadGestureStabilizer(
    private val sustainFrames: Int = DEFAULT_SUSTAIN_FRAMES,
    private val baselineStableFrames: Int = DEFAULT_BASELINE_FRAMES,
    private val stableRangeDeg: Float = DEFAULT_STABLE_RANGE_DEG,
    private val releaseRatio: Float = DEFAULT_RELEASE_RATIO,
    private val driftAlpha: Float = DEFAULT_DRIFT_ALPHA,
    private val maxWarmupFrames: Int = DEFAULT_MAX_WARMUP_FRAMES,
) {
    /** Resultado de um frame: pose relativa ao neutro (null até o baseline ficar pronto). */
    data class Result(
        val relativePose: HeadPose?,
        val gesture: Gesture?,
    )

    private val baselineWindow = ArrayDeque<HeadPose>(baselineStableFrames)
    private var baseline: HeadPose? = null
    private var warmupFrames = 0

    private var sustained: Gesture? = null
    private var sustainCount = 0
    private var armed = true

    fun reset() {
        baselineWindow.clear()
        baseline = null
        warmupFrames = 0
        sustained = null
        sustainCount = 0
        armed = true
    }

    fun update(
        raw: HeadPose,
        thresholds: CalibrationThresholds,
    ): Result {
        val current = baseline
        if (current == null) {
            captureBaseline(raw)
            return Result(relativePose = null, gesture = null)
        }
        val relative = raw - current
        val gesture = evaluate(relative, raw, current, thresholds)
        return Result(relativePose = relative, gesture = gesture)
    }

    private fun evaluate(
        relative: HeadPose,
        raw: HeadPose,
        current: HeadPose,
        thresholds: CalibrationThresholds,
    ): Gesture? {
        val candidate = GestureClassifier.classifyWithPose(relative, blendshapes = null, thresholds)
        if (candidate == null) {
            sustained = null
            sustainCount = 0
            if (nearNeutral(relative, thresholds)) {
                armed = true
                baseline = current.drift(raw, driftAlpha)
            }
            return null
        }
        if (!armed) return null
        return confirmSustain(candidate)
    }

    private fun confirmSustain(candidate: Gesture): Gesture? {
        if (candidate == sustained) {
            sustainCount++
        } else {
            sustained = candidate
            sustainCount = 1
        }
        if (sustainCount < sustainFrames) return null
        armed = false
        sustained = null
        sustainCount = 0
        return candidate
    }

    private fun captureBaseline(raw: HeadPose) {
        warmupFrames++
        baselineWindow.addLast(raw)
        while (baselineWindow.size > baselineStableFrames) baselineWindow.removeFirst()

        val full = baselineWindow.size == baselineStableFrames
        val stableLock = full && baselineWindow.isStable(stableRangeDeg)
        // Fallback: garante que o baseline trave mesmo se a cabeça nunca ficar
        // perfeitamente parada (o estimador geométrico tem ruído). Sem isso, nenhum
        // gesto dispararia. Após maxWarmupFrames, trava com a média da janela.
        val forcedLock = warmupFrames >= maxWarmupFrames && baselineWindow.isNotEmpty()
        if (stableLock || forcedLock) {
            baseline = baselineWindow.average()
            baselineWindow.clear()
            warmupFrames = 0
            armed = true
        }
    }

    private fun nearNeutral(
        relative: HeadPose,
        t: CalibrationThresholds,
    ): Boolean {
        val rollLimit = min(t.rollRightDeg, t.rollLeftDeg) * releaseRatio
        val pitchLimit = min(t.pitchUpDeg, t.pitchDownDeg) * releaseRatio
        val yawLimit = min(t.yawRightDeg, t.yawLeftDeg) * releaseRatio
        return abs(relative.roll) < rollLimit &&
            abs(relative.pitch) < pitchLimit &&
            abs(relative.yaw) < yawLimit
    }

    private fun HeadPose.drift(
        target: HeadPose,
        alpha: Float,
    ): HeadPose =
        HeadPose(
            roll = roll * (1 - alpha) + target.roll * alpha,
            pitch = pitch * (1 - alpha) + target.pitch * alpha,
            yaw = yaw * (1 - alpha) + target.yaw * alpha,
        )

    private fun ArrayDeque<HeadPose>.isStable(rangeDeg: Float): Boolean {
        val rolls = map { it.roll }
        val pitches = map { it.pitch }
        val yaws = map { it.yaw }
        return rolls.range() < rangeDeg && pitches.range() < rangeDeg && yaws.range() < rangeDeg
    }

    private fun List<Float>.range(): Float = (maxOrNull() ?: 0f) - (minOrNull() ?: 0f)

    private fun ArrayDeque<HeadPose>.average(): HeadPose =
        HeadPose(
            roll = map { it.roll }.average().toFloat(),
            pitch = map { it.pitch }.average().toFloat(),
            yaw = map { it.yaw }.average().toFloat(),
        )

    companion object {
        private const val DEFAULT_SUSTAIN_FRAMES = 2
        private const val DEFAULT_BASELINE_FRAMES = 10
        private const val DEFAULT_STABLE_RANGE_DEG = 6f
        private const val DEFAULT_RELEASE_RATIO = 0.5f
        private const val DEFAULT_DRIFT_ALPHA = 0.03f
        private const val DEFAULT_MAX_WARMUP_FRAMES = 30
    }
}

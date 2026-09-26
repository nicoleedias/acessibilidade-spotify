package com.sac.acessibilidade.data.calibration

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sac.acessibilidade.domain.gesture.CalibrationThresholds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integração com o `SharedPreferences` real (UC02).
 *
 * São 11 chaves de string escritas e lidas à mão em
 * `CalibrationRepositoryImpl`. Trocar `roll_right_deg` por `roll_left_deg` num
 * dos lados inverteria os limiares do usuário sem quebrar a compilação — só um
 * round-trip com valores distintos detecta isso.
 */
@RunWith(AndroidJUnit4::class)
class CalibrationRepositoryImplTest {
    private lateinit var context: Context
    private lateinit var repository: CalibrationRepositoryImpl

    @Before
    fun limparPreferencias() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context
            .getSharedPreferences("sac_calibration", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        repository = CalibrationRepositoryImpl(context)
    }

    private val delta = 0.001f

    /** Valores todos distintos entre si, para flagrar troca de chaves. */
    private fun limiaresDistintos() =
        CalibrationThresholds(
            rollRightDeg = 11f,
            rollLeftDeg = 12f,
            pitchUpDeg = 13f,
            pitchDownDeg = 14f,
            yawRightDeg = 15f,
            yawLeftDeg = 16f,
            blinkThreshold = 0.17f,
            nodPitchAmplitudeDeg = 18f,
            rollSign = 1f,
            pitchSign = -1f,
            yawSign = -1f,
        )

    // ── Estado inicial ──────────────────────────────────────────────────────

    @Test
    fun usuarioNovoNaoEstaCalibrado() {
        assertFalse(repository.isCalibrated())
    }

    @Test
    fun semCalibracaoDevolveOsPadroesDoDominio() {
        // Fecha a duplicação entre as constantes DEFAULT_* do repositório e os
        // valores default de CalibrationThresholds.
        assertEquals(CalibrationThresholds(), repository.getThresholds())
    }

    // ── Round-trip ──────────────────────────────────────────────────────────

    @Test
    fun gravaELeTodosOsOnzeLimiaresSemTrocarChaves() {
        val esperado = limiaresDistintos()

        repository.saveThresholds(esperado)
        val lido = repository.getThresholds()

        assertEquals(esperado.rollRightDeg, lido.rollRightDeg, delta)
        assertEquals(esperado.rollLeftDeg, lido.rollLeftDeg, delta)
        assertEquals(esperado.pitchUpDeg, lido.pitchUpDeg, delta)
        assertEquals(esperado.pitchDownDeg, lido.pitchDownDeg, delta)
        assertEquals(esperado.yawRightDeg, lido.yawRightDeg, delta)
        assertEquals(esperado.yawLeftDeg, lido.yawLeftDeg, delta)
        assertEquals(esperado.blinkThreshold, lido.blinkThreshold, delta)
        assertEquals(esperado.nodPitchAmplitudeDeg, lido.nodPitchAmplitudeDeg, delta)
        assertEquals(esperado, lido)
    }

    @Test
    fun gravarLimiaresMarcaComoCalibrado() {
        repository.saveThresholds(limiaresDistintos())

        assertTrue(repository.isCalibrated())
    }

    @Test
    fun polaridadeNegativaSobreviveAoRoundTrip() {
        // A polaridade aprendida (+1/-1) é o que impede a inversão
        // direita/esquerda entre dispositivos. Perder o sinal quebra UC02.
        val comSinaisInvertidos =
            CalibrationThresholds(rollSign = -1f, pitchSign = -1f, yawSign = -1f)

        repository.saveThresholds(comSinaisInvertidos)
        val lido = repository.getThresholds()

        assertEquals(-1f, lido.rollSign, delta)
        assertEquals(-1f, lido.pitchSign, delta)
        assertEquals(-1f, lido.yawSign, delta)
    }

    @Test
    fun recalibrarSobrescreveOsValoresAnteriores() {
        repository.saveThresholds(limiaresDistintos())

        val novos = CalibrationThresholds(rollRightDeg = 42f, yawLeftDeg = 43f)
        repository.saveThresholds(novos)

        assertEquals(novos, repository.getThresholds())
    }

    @Test
    fun gravarPadroesMarcaComoCalibradoComOsValoresDefault() {
        // Caminho "pular calibração" da tela inicial.
        repository.saveDefaultThresholds()

        assertTrue(repository.isCalibrated())
        assertEquals(CalibrationThresholds(), repository.getThresholds())
    }

    @Test
    fun novaInstanciaLeOQueFoiGravado() {
        // Simula reabrir o app.
        repository.saveThresholds(limiaresDistintos())

        val outraInstancia = CalibrationRepositoryImpl(context)

        assertTrue(outraInstancia.isCalibrated())
        assertEquals(limiaresDistintos(), outraInstancia.getThresholds())
    }
}

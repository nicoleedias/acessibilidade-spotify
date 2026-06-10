package com.sac.acessibilidade.data.calibration

import com.sac.acessibilidade.domain.gesture.CalibrationThresholds

interface CalibrationRepository {
    /** Persiste os thresholds medidos na sessão de calibração do usuário. */
    fun saveThresholds(thresholds: CalibrationThresholds)

    /** Atalho que salva os valores padrão (sem calibração real). */
    fun saveDefaultThresholds()

    fun isCalibrated(): Boolean

    fun getThresholds(): CalibrationThresholds
}

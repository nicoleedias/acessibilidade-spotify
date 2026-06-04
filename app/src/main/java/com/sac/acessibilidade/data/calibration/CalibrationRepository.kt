package com.sac.acessibilidade.data.calibration

import com.sac.acessibilidade.domain.gesture.CalibrationThresholds

interface CalibrationRepository {
    fun saveDefaultThresholds()

    fun isCalibrated(): Boolean

    /** Retorna os thresholds calibrados pelo usuário (ou padrões se não calibrado). */
    fun getThresholds(): CalibrationThresholds
}

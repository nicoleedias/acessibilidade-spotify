package com.sac.acessibilidade.data.calibration

interface CalibrationRepository {
    fun saveDefaultThresholds()

    fun isCalibrated(): Boolean
}

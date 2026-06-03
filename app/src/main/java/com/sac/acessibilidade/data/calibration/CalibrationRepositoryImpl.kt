package com.sac.acessibilidade.data.calibration

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalibrationRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : CalibrationRepository {
        private val prefs = context.getSharedPreferences("sac_calibration", Context.MODE_PRIVATE)

        override fun saveDefaultThresholds() {
            prefs
                .edit()
                // Thresholds em graus — serão sobrescritos pelos valores reais do MediaPipe (UC02)
                .putFloat(KEY_TILT_RIGHT_DEG, DEFAULT_TILT_DEG)
                .putFloat(KEY_TILT_LEFT_DEG, DEFAULT_TILT_DEG)
                .putFloat(KEY_TILT_UP_DEG, DEFAULT_VERTICAL_DEG)
                .putFloat(KEY_TILT_DOWN_DEG, DEFAULT_VERTICAL_DEG)
                .putFloat(KEY_BLINK_EAR, DEFAULT_BLINK_EAR)
                .putBoolean(KEY_IS_CALIBRATED, true)
                .apply()
        }

        override fun isCalibrated(): Boolean = prefs.getBoolean(KEY_IS_CALIBRATED, false)

        private companion object {
            const val KEY_TILT_RIGHT_DEG = "tilt_right_deg"
            const val KEY_TILT_LEFT_DEG = "tilt_left_deg"
            const val KEY_TILT_UP_DEG = "tilt_up_deg"
            const val KEY_TILT_DOWN_DEG = "tilt_down_deg"
            const val KEY_BLINK_EAR = "blink_ear"
            const val KEY_IS_CALIBRATED = "is_calibrated"
            const val DEFAULT_TILT_DEG = 20f
            const val DEFAULT_VERTICAL_DEG = 15f
            const val DEFAULT_BLINK_EAR = 0.25f
        }
    }

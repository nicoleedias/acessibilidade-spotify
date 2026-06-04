package com.sac.acessibilidade.data.calibration

import android.content.Context
import com.sac.acessibilidade.domain.gesture.CalibrationThresholds
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
                .putFloat(KEY_ROLL_RIGHT, DEFAULT_ROLL_DEG)
                .putFloat(KEY_ROLL_LEFT, DEFAULT_ROLL_DEG)
                .putFloat(KEY_PITCH_UP, DEFAULT_PITCH_DEG)
                .putFloat(KEY_PITCH_DOWN, DEFAULT_PITCH_DEG)
                .putFloat(KEY_YAW_RIGHT, DEFAULT_YAW_DEG)
                .putFloat(KEY_YAW_LEFT, DEFAULT_YAW_DEG)
                .putFloat(KEY_BLINK, DEFAULT_BLINK)
                .putBoolean(KEY_IS_CALIBRATED, true)
                .apply()
        }

        override fun isCalibrated(): Boolean = prefs.getBoolean(KEY_IS_CALIBRATED, false)

        override fun getThresholds(): CalibrationThresholds =
            CalibrationThresholds(
                rollRightDeg = prefs.getFloat(KEY_ROLL_RIGHT, DEFAULT_ROLL_DEG),
                rollLeftDeg = prefs.getFloat(KEY_ROLL_LEFT, DEFAULT_ROLL_DEG),
                pitchUpDeg = prefs.getFloat(KEY_PITCH_UP, DEFAULT_PITCH_DEG),
                pitchDownDeg = prefs.getFloat(KEY_PITCH_DOWN, DEFAULT_PITCH_DEG),
                yawRightDeg = prefs.getFloat(KEY_YAW_RIGHT, DEFAULT_YAW_DEG),
                yawLeftDeg = prefs.getFloat(KEY_YAW_LEFT, DEFAULT_YAW_DEG),
                blinkThreshold = prefs.getFloat(KEY_BLINK, DEFAULT_BLINK),
            )

        private companion object {
            const val KEY_ROLL_RIGHT = "roll_right_deg"
            const val KEY_ROLL_LEFT = "roll_left_deg"
            const val KEY_PITCH_UP = "pitch_up_deg"
            const val KEY_PITCH_DOWN = "pitch_down_deg"
            const val KEY_YAW_RIGHT = "yaw_right_deg"
            const val KEY_YAW_LEFT = "yaw_left_deg"
            const val KEY_BLINK = "blink_threshold"
            const val KEY_IS_CALIBRATED = "is_calibrated"
            const val DEFAULT_ROLL_DEG = 15f
            const val DEFAULT_PITCH_DEG = 12f
            const val DEFAULT_YAW_DEG = 20f
            const val DEFAULT_BLINK = 0.5f
        }
    }

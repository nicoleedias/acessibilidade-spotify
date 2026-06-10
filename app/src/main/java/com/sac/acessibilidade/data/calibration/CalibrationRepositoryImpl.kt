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

        override fun saveThresholds(thresholds: CalibrationThresholds) {
            prefs
                .edit()
                .putFloat(KEY_ROLL_RIGHT, thresholds.rollRightDeg)
                .putFloat(KEY_ROLL_LEFT, thresholds.rollLeftDeg)
                .putFloat(KEY_PITCH_UP, thresholds.pitchUpDeg)
                .putFloat(KEY_PITCH_DOWN, thresholds.pitchDownDeg)
                .putFloat(KEY_YAW_RIGHT, thresholds.yawRightDeg)
                .putFloat(KEY_YAW_LEFT, thresholds.yawLeftDeg)
                .putFloat(KEY_BLINK, thresholds.blinkThreshold)
                .putFloat(KEY_NOD_AMPLITUDE, thresholds.nodPitchAmplitudeDeg)
                .putFloat(KEY_ROLL_SIGN, thresholds.rollSign)
                .putFloat(KEY_PITCH_SIGN, thresholds.pitchSign)
                .putFloat(KEY_YAW_SIGN, thresholds.yawSign)
                .putBoolean(KEY_IS_CALIBRATED, true)
                .apply()
        }

        override fun saveDefaultThresholds() = saveThresholds(CalibrationThresholds())

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
                nodPitchAmplitudeDeg = prefs.getFloat(KEY_NOD_AMPLITUDE, DEFAULT_NOD_AMPLITUDE),
                rollSign = prefs.getFloat(KEY_ROLL_SIGN, DEFAULT_SIGN),
                pitchSign = prefs.getFloat(KEY_PITCH_SIGN, DEFAULT_SIGN),
                yawSign = prefs.getFloat(KEY_YAW_SIGN, DEFAULT_SIGN),
            )

        private companion object {
            const val KEY_ROLL_RIGHT = "roll_right_deg"
            const val KEY_ROLL_LEFT = "roll_left_deg"
            const val KEY_PITCH_UP = "pitch_up_deg"
            const val KEY_PITCH_DOWN = "pitch_down_deg"
            const val KEY_YAW_RIGHT = "yaw_right_deg"
            const val KEY_YAW_LEFT = "yaw_left_deg"
            const val KEY_BLINK = "blink_threshold"
            const val KEY_NOD_AMPLITUDE = "nod_amplitude_deg"
            const val KEY_ROLL_SIGN = "roll_sign"
            const val KEY_PITCH_SIGN = "pitch_sign"
            const val KEY_YAW_SIGN = "yaw_sign"
            const val KEY_IS_CALIBRATED = "is_calibrated"
            const val DEFAULT_ROLL_DEG = 15f
            const val DEFAULT_PITCH_DEG = 12f
            const val DEFAULT_YAW_DEG = 20f
            const val DEFAULT_BLINK = 0.5f
            const val DEFAULT_NOD_AMPLITUDE = 12f
            const val DEFAULT_SIGN = 1f
        }
    }

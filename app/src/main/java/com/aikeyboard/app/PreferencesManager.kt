package com.aikeyboard.app

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var learningEnabled: Boolean
        get() = prefs.getBoolean(KEY_LEARNING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_LEARNING_ENABLED, value).apply()

    var totalWordsTyped: Int
        get() = prefs.getInt(KEY_TOTAL_WORDS_TYPED, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_WORDS_TYPED, value).apply()

    var totalPredictions: Int
        get() = prefs.getInt(KEY_TOTAL_PREDICTIONS, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_PREDICTIONS, value).apply()

    var keyboardScalePercent: Int
        get() = prefs.getInt(KEY_KEYBOARD_SCALE_PERCENT, 100)
        set(value) = prefs.edit().putInt(KEY_KEYBOARD_SCALE_PERCENT, value.coerceIn(80, 140)).apply()

    var hapticEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var numberRowEnabled: Boolean
        get() = prefs.getBoolean(KEY_NUMBER_ROW_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_NUMBER_ROW_ENABLED, value).apply()

    fun incrementWordsTyped() {
        totalWordsTyped++
    }

    fun incrementPredictions() {
        totalPredictions++
    }

    fun getPredictionAccuracy(): Float {
        return if (totalWordsTyped > 0) {
            (totalPredictions.toFloat() / totalWordsTyped) * 100
        } else {
            0f
        }
    }

    fun clearAll() {
        val scale = keyboardScalePercent
        val haptic = hapticEnabled
        val sound = soundEnabled
        val numberRow = numberRowEnabled
        prefs.edit().clear().apply()
        keyboardScalePercent = scale
        hapticEnabled = haptic
        soundEnabled = sound
        numberRowEnabled = numberRow
    }

    companion object {
        private const val PREFS_NAME = "ai_keyboard_prefs"
        private const val KEY_LEARNING_ENABLED = "learning_enabled"
        private const val KEY_TOTAL_WORDS_TYPED = "total_words_typed"
        private const val KEY_TOTAL_PREDICTIONS = "total_predictions"
        private const val KEY_KEYBOARD_SCALE_PERCENT = "keyboard_scale_percent"
        private const val KEY_HAPTIC_ENABLED = "haptic_enabled"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_NUMBER_ROW_ENABLED = "number_row_enabled"
    }
}

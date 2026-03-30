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
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "ai_keyboard_prefs"
        private const val KEY_LEARNING_ENABLED = "learning_enabled"
        private const val KEY_TOTAL_WORDS_TYPED = "total_words_typed"
        private const val KEY_TOTAL_PREDICTIONS = "total_predictions"
    }
}

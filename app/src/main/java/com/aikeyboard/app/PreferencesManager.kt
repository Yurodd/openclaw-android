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
        get() = prefs.getInt(KEY_KEYBOARD_SCALE_PERCENT, 140)
        set(value) = prefs.edit().putInt(KEY_KEYBOARD_SCALE_PERCENT, value.coerceIn(80, 140)).apply()

    var hapticEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var numberRowEnabled: Boolean
        get() = prefs.getBoolean(KEY_NUMBER_ROW_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NUMBER_ROW_ENABLED, value).apply()

    var emojiPanelEnabled: Boolean
        get() = prefs.getBoolean(KEY_EMOJI_PANEL_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_EMOJI_PANEL_ENABLED, value).apply()

    var clipboardPanelEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLIPBOARD_PANEL_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_CLIPBOARD_PANEL_ENABLED, value).apply()

    var punctuationShortcutsEnabled: Boolean
        get() = prefs.getBoolean(KEY_PUNCTUATION_SHORTCUTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_PUNCTUATION_SHORTCUTS_ENABLED, value).apply()

    var selectedTheme: KeyboardThemeOption
        get() = KeyboardThemeOption.fromStorage(prefs.getString(KEY_SELECTED_THEME, KeyboardThemeOption.DARK.storageValue))
        set(value) = prefs.edit().putString(KEY_SELECTED_THEME, value.storageValue).apply()

    var oneHandedMode: OneHandedMode
        get() = OneHandedMode.fromStorage(prefs.getString(KEY_ONE_HANDED_MODE, OneHandedMode.OFF.storageValue))
        set(value) = prefs.edit().putString(KEY_ONE_HANDED_MODE, value.storageValue).apply()

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

    fun addClipboardEntry(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val updated = getClipboardHistory().toMutableList().apply {
            removeAll { it == trimmed }
            add(0, trimmed)
            while (size > MAX_CLIPBOARD_ITEMS) {
                removeAt(lastIndex)
            }
        }
        prefs.edit().putString(KEY_CLIPBOARD_HISTORY, updated.joinToString(CLIPBOARD_SEPARATOR)).apply()
    }

    fun getClipboardHistory(): List<String> {
        val raw = prefs.getString(KEY_CLIPBOARD_HISTORY, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(CLIPBOARD_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun clearClipboardHistory() {
        prefs.edit().remove(KEY_CLIPBOARD_HISTORY).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
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
        private const val KEY_EMOJI_PANEL_ENABLED = "emoji_panel_enabled"
        private const val KEY_CLIPBOARD_PANEL_ENABLED = "clipboard_panel_enabled"
        private const val KEY_PUNCTUATION_SHORTCUTS_ENABLED = "punctuation_shortcuts_enabled"
        private const val KEY_SELECTED_THEME = "selected_theme"
        private const val KEY_ONE_HANDED_MODE = "one_handed_mode"
        private const val KEY_CLIPBOARD_HISTORY = "clipboard_history"
        private const val CLIPBOARD_SEPARATOR = "\u001F"
        private const val MAX_CLIPBOARD_ITEMS = 12
    }
}

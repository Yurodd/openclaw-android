package com.aikeyboard.app

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView

class AIMethodService : InputMethodService() {

    private lateinit var keyboardView: KeyboardView
    private lateinit var predictionEngine: PredictionEngine
    private lateinit var userHistoryManager: UserHistoryManager
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var autoCorrectEngine: AutoCorrectEngine
    private val handler = Handler(Looper.getMainLooper())

    private var currentWord = StringBuilder()
    private var currentSentence = ""
    private var lastCommittedSeparator: String = ""

    private lateinit var suggestionBar: LinearLayout
    private var suggestionChips = mutableListOf<TextView>()

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
        userHistoryManager = UserHistoryManager(this)
        predictionEngine = PredictionEngine(userHistoryManager)
        autoCorrectEngine = AutoCorrectEngine(userHistoryManager)
    }

    override fun onCreateInputView(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#1A1A1A"))
        }

        suggestionBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(android.graphics.Color.parseColor("#2D2D2D"))
            setPadding(16, 12, 16, 12)
        }

        suggestionChips.clear()
        repeat(3) {
            val chip = TextView(this@AIMethodService).apply {
                text = ""
                setTextColor(android.graphics.Color.WHITE)
                textSize = 16f
                setPadding(32, 8, 32, 8)
                isClickable = true
                setOnClickListener { view ->
                    val suggestion = (view as TextView).text.toString()
                    if (suggestion.isNotEmpty()) {
                        commitSuggestion(suggestion)
                    }
                }
            }
            suggestionChips.add(chip)
            suggestionBar.addView(chip)
        }

        container.addView(suggestionBar)

        keyboardView = KeyboardView(this, preferencesManager)
        container.addView(keyboardView)

        return container
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        currentWord.clear()
        currentSentence = ""
        lastCommittedSeparator = ""
        updateSuggestions(listOf("", "", ""))
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (::keyboardView.isInitialized) {
            keyboardView.reloadLayout()
        }
    }

    fun onKeyPressed() {
        if (preferencesManager.hapticEnabled) {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(12)
                }
            }
        }

        if (preferencesManager.soundEnabled) {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.3f)
        }
    }

    fun handleCharacter(char: String) {
        if (isWordCharacter(char)) {
            currentInputConnection?.commitText(char, 1)
            currentWord.append(char)
            lastCommittedSeparator = ""
            updateSuggestions(predictionsForCurrentInput())
            return
        }

        handleSeparator(char)
    }

    fun handleBackspace() {
        if (currentWord.isNotEmpty()) {
            currentWord.deleteCharAt(currentWord.length - 1)
        } else {
            lastCommittedSeparator = ""
        }
        currentInputConnection?.deleteSurroundingText(1, 0)
        updateSuggestions(predictionsForCurrentInput())
    }

    fun handleSpace() {
        if (currentWord.isNotEmpty()) {
            commitCurrentWord(separator = " ")
        } else {
            if (lastCommittedSeparator == " ") {
                currentInputConnection?.deleteSurroundingText(1, 0)
                currentInputConnection?.commitText(". ", 1)
                lastCommittedSeparator = ". "
            } else {
                currentInputConnection?.commitText(" ", 1)
                lastCommittedSeparator = " "
            }
        }

        updateSuggestions(safePredictions(""))
    }

    fun handleReturn() {
        commitCurrentWord(separator = "")
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))

        currentSentence = ""
        currentWord.clear()
        lastCommittedSeparator = "\n"
        updateSuggestions(listOf("", "", ""))
    }

    fun commitSuggestion(suggestion: String) {
        currentInputConnection?.deleteSurroundingText(currentWord.length, 0)
        currentInputConnection?.commitText("$suggestion ", 1)

        recordCommittedWord(suggestion)
        preferencesManager.incrementPredictions()

        currentWord.clear()
        lastCommittedSeparator = " "
        updateSuggestions(safePredictions(""))
    }

    private fun safePredictions(prefix: String): List<String> {
        return try {
            val predictions = predictionEngine.predictNextWord(currentSentence.trim(), prefix)
            if (prefix.isNotEmpty()) {
                predictions.filter { it.startsWith(prefix, ignoreCase = true) || it.isEmpty() }
                    .take(3)
                    .padEnd(3, "")
            } else {
                predictions.take(3).padEnd(3, "")
            }
        } catch (_: Exception) {
            listOf("", "", "")
        }
    }

    private fun predictionsForCurrentInput(): List<String> {
        val prefix = currentWord.toString()
        val autoCorrection = if (prefix.length >= 3) autoCorrectEngine.previewCorrection(prefix) else null
        val predictions = safePredictions(prefix).toMutableList()

        if (!autoCorrection.isNullOrBlank() && !autoCorrection.equals(prefix, ignoreCase = true)) {
            predictions.removeAll { it.equals(autoCorrection, ignoreCase = true) }
            predictions.add(0, autoCorrection)
        }

        return predictions.take(3).padEnd(3, "")
    }

    private fun handleSeparator(separator: String) {
        val autoSpace = separator in setOf("!", "?", ".")
        commitCurrentWord(separator = separator, autoAppendSpace = autoSpace)
        updateSuggestions(safePredictions(""))
    }

    private fun commitCurrentWord(
        separator: String,
        autoAppendSpace: Boolean = false
    ) {
        val originalWord = currentWord.toString().trim()
        val correction = autoCorrectEngine.getCorrection(originalWord)
        val committedWord = correction ?: originalWord

        if (originalWord.isNotEmpty()) {
            if (correction != null && correction != originalWord) {
                currentInputConnection?.deleteSurroundingText(originalWord.length, 0)
                currentInputConnection?.commitText(committedWord, 1)
            }
            recordCommittedWord(committedWord)
            currentWord.clear()
        }

        var separatorText = separator
        if (autoAppendSpace && separator.isNotEmpty()) {
            separatorText += " "
        }

        if (separatorText.isNotEmpty()) {
            currentInputConnection?.commitText(separatorText, 1)
            lastCommittedSeparator = separatorText
        }
    }

    private fun recordCommittedWord(word: String) {
        val cleanWord = word.trim()
        if (cleanWord.isEmpty()) return

        userHistoryManager.addTypedWord(cleanWord)
        preferencesManager.incrementWordsTyped()
        currentSentence = if (currentSentence.isBlank()) cleanWord else "$currentSentence $cleanWord"
        userHistoryManager.addPhrase(currentSentence)
        lastCommittedSeparator = ""
    }

    private fun isWordCharacter(char: String): Boolean {
        return char.length == 1 && (char[0].isLetterOrDigit() || char == "'")
    }

    fun updateSuggestions(suggestions: List<String>) {
        handler.post {
            suggestions.forEachIndexed { index, suggestion ->
                if (index < suggestionChips.size) {
                    suggestionChips[index].text = suggestion
                    suggestionChips[index].alpha = if (suggestion.isEmpty()) 0.3f else 1.0f
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        userHistoryManager.close()
    }

    private fun List<String>.padEnd(count: Int, pad: String): List<String> {
        val result = this.toMutableList()
        while (result.size < count) {
            result.add(pad)
        }
        return result.take(count)
    }
}

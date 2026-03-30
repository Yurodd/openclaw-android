package com.aikeyboard.app

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.HorizontalScrollView
import com.google.android.material.chip.Chip

class AIMethodService : InputMethodService() {

    private lateinit var keyboardView: KeyboardView
    private lateinit var predictionEngine: PredictionEngine
    private lateinit var userHistoryManager: UserHistoryManager
    private lateinit var preferencesManager: PreferencesManager

    private val handler = Handler(Looper.getMainLooper())
    private var currentWord = StringBuilder()
    private var currentSentence = ""

    private lateinit var suggestionBar: LinearLayout
    private var suggestionChips = mutableListOf<TextView>()

    override fun onCreate() {
        super.onCreate()
        
        preferencesManager = PreferencesManager(this)
        userHistoryManager = UserHistoryManager(this)
        predictionEngine = PredictionEngine(userHistoryManager)
    }

    override fun onCreateInputView(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#1A1A1A"))
        }

        // Suggestion bar
        suggestionBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(android.graphics.Color.parseColor("#2D2D2D"))
            setPadding(16, 12, 16, 12)
        }

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

        // Keyboard view
        keyboardView = KeyboardView(this)
        container.addView(keyboardView)

        return container
    }

    override fun onStartInput(attribute: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        currentWord.clear()
        currentSentence = ""
        updateSuggestions(listOf("", "", ""))
    }

    fun handleCharacter(char: String) {
        currentWord.append(char)
        
        // Get prefix predictions with current partial word
        val predictions = predictionEngine.predictNextWord(
            currentSentence.takeLast(50),
            currentWord.toString()
        )
        
        // Filter suggestions to start with current prefix when typing mid-word
        if (currentWord.length >= 1) {
            val prefix = currentWord.toString()
            val filtered = predictions.filter { 
                it.startsWith(prefix, ignoreCase = true) || it.isEmpty() 
            }
            updateSuggestions(filtered.take(3).padEnd(3, ""))
        } else {
            updateSuggestions(predictions.take(3).padEnd(3, ""))
        }
        
        currentInputConnection?.commitText(char, 1)
    }

    fun handleBackspace() {
        if (currentWord.isNotEmpty()) {
            currentWord.deleteCharAt(currentWord.length - 1)
        }
        
        currentInputConnection?.deleteSurroundingText(1, 0)
        
        val predictions = predictionEngine.predictNextWord(
            currentSentence.takeLast(50),
            currentWord.toString()
        )
        updateSuggestions(predictions.take(3).padEnd(3, ""))
    }

    fun handleSpace() {
        val word = currentWord.toString()
        
        if (word.isNotEmpty()) {
            // Learn the word
            userHistoryManager.addTypedWord(word)
            preferencesManager.incrementWordsTyped()
            
            // Learn the phrase
            if (currentSentence.isNotEmpty()) {
                userHistoryManager.addPhrase(currentSentence.trim() + " " + word)
            }
            
            currentSentence += " $word"
            currentWord.clear()
        }
        
        currentInputConnection?.commitText(" ", 1)
        
        // Get next word predictions (no prefix after space)
        val predictions = predictionEngine.predictNextWord(currentSentence.takeLast(50), "")
        updateSuggestions(predictions.take(3).padEnd(3, ""))
    }

    fun handleReturn() {
        currentInputConnection?.sendKeyEvent(
            android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER)
        )
        currentInputConnection?.sendKeyEvent(
            android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER)
        )
        
        if (currentSentence.isNotEmpty() && currentWord.isNotEmpty()) {
            userHistoryManager.addPhrase(currentSentence + " " + currentWord)
            currentWord.clear()
        }
        currentSentence = ""
        updateSuggestions(listOf("", "", ""))
    }

    fun commitSuggestion(suggestion: String) {
        // Delete current partial word
        currentInputConnection?.deleteSurroundingText(currentWord.length, 0)
        
        // Commit the suggestion + space
        currentInputConnection?.commitText("$suggestion ", 1)
        
        // Learn
        userHistoryManager.addTypedWord(suggestion)
        preferencesManager.incrementWordsTyped()
        
        currentSentence += " $suggestion"
        currentWord.clear()
        
        // Get next predictions (no prefix after committing a suggestion)
        val predictions = predictionEngine.predictNextWord(currentSentence.takeLast(50), "")
        updateSuggestions(predictions.take(3).padEnd(3, ""))
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

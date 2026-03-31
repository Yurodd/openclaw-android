package com.aikeyboard.app

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

class AIMethodService : InputMethodService() {

    private lateinit var keyboardView: KeyboardView
    private lateinit var predictionEngine: PredictionEngine
    private lateinit var userHistoryManager: UserHistoryManager
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var autoCorrectEngine: AutoCorrectEngine
    private val handler = Handler(Looper.getMainLooper())

    private var currentWord = StringBuilder()
    private var currentSentence = ""

    private lateinit var rootContainer: LinearLayout
    private lateinit var suggestionBar: LinearLayout
    private lateinit var accessoryScroller: HorizontalScrollView
    private lateinit var accessoryBar: LinearLayout
    private var suggestionChips = mutableListOf<TextView>()

    // Stability first: keep live keypress path minimal.
    private val livePredictionsEnabled = false

    private enum class PanelMode {
        NONE,
        EMOJI,
        CLIPBOARD
    }

    private var activePanel = PanelMode.NONE

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
        userHistoryManager = UserHistoryManager(this)
        predictionEngine = PredictionEngine(userHistoryManager)
        autoCorrectEngine = AutoCorrectEngine(userHistoryManager)
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    override fun onCreateInputView(): View {
        rootContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        suggestionBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        suggestionChips.clear()
        repeat(3) {
            val chip = buildActionChip().apply {
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

        accessoryBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        accessoryScroller = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(accessoryBar)
        }

        keyboardView = KeyboardView(this, preferencesManager)

        rootContainer.addView(suggestionBar)
        rootContainer.addView(accessoryScroller)
        rootContainer.addView(keyboardView)

        applyTheme()
        populatePanel(PanelMode.NONE)
        return rootContainer
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        currentWord.clear()
        currentSentence = ""
        updateSuggestions(listOf("", "", ""))
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        syncClipboardFromSystem()
        if (::keyboardView.isInitialized) {
            applyTheme()
            keyboardView.reloadLayout()
            populatePanel(activePanel)
        }
        if (shouldAutoCapitalize(info)) {
            currentWord.clear()
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
        currentInputConnection?.commitText(char, 1)

        if (char.length == 1 && (char[0].isLetterOrDigit() || char[0] == '\'') ) {
            currentWord.append(char)
        } else {
            currentWord.clear()
        }

        if (livePredictionsEnabled) {
            updateSuggestions(safePredictions(currentWord.toString()))
        } else {
            val correctionHint = if (currentWord.isNotEmpty()) autoCorrectEngine.previewCorrection(currentWord.toString()) else null
            if (correctionHint != null) {
                updateSuggestions(listOf(correctionHint, "", ""))
            }
        }
    }

    fun handlePunctuation(punctuation: String) {
        val suffix = when (punctuation) {
            ".", "!", "?" -> "$punctuation "
            else -> punctuation
        }
        finalizeCurrentWordAndCommit(suffix)
    }

    fun commitText(text: String) {
        currentInputConnection?.commitText(text, 1)
        syncClipboardFromSystem()
        if (text.any { !it.isLetterOrDigit() }) {
            currentWord.clear()
            updateSuggestions(safePredictions(""))
        }
    }

    fun handleBackspace() {
        if (currentWord.isNotEmpty()) {
            currentWord.deleteCharAt(currentWord.length - 1)
        }
        currentInputConnection?.deleteSurroundingText(1, 0)

        if (livePredictionsEnabled) {
            updateSuggestions(safePredictions(currentWord.toString()))
        } else {
            if (currentWord.isEmpty()) {
                updateSuggestions(listOf("", "", ""))
            }
        }
    }

    fun handleSpace() {
        if (currentWord.isEmpty()) {
            currentInputConnection?.commitText(" ", 1)
            return
        }
        finalizeCurrentWordAndCommit(" ")
    }

    fun handleReturn() {
        finalizeCurrentWordAndCommit("")
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        currentSentence = ""
        updateSuggestions(listOf("", "", ""))
    }

    fun commitSuggestion(suggestion: String) {
        currentInputConnection?.deleteSurroundingText(currentWord.length, 0)
        currentInputConnection?.commitText("$suggestion ", 1)

        userHistoryManager.addTypedWord(suggestion)
        preferencesManager.incrementWordsTyped()
        preferencesManager.incrementPredictions()

        currentSentence = if (currentSentence.isBlank()) suggestion else "$currentSentence $suggestion"
        currentWord.clear()
        updateSuggestions(safePredictions(""))
    }

    private fun finalizeCurrentWordAndCommit(suffix: String) {
        val typedWord = currentWord.toString().trim()
        if (typedWord.isNotEmpty()) {
            val corrected = autoCorrectEngine.getCorrection(typedWord) ?: typedWord
            if (corrected != typedWord) {
                currentInputConnection?.deleteSurroundingText(typedWord.length, 0)
                currentInputConnection?.commitText(corrected, 1)
                preferencesManager.incrementPredictions()
            }

            userHistoryManager.addTypedWord(corrected)
            preferencesManager.incrementWordsTyped()
            currentSentence = if (currentSentence.isBlank()) corrected else "$currentSentence $corrected"
            userHistoryManager.addPhrase(currentSentence)
        }

        currentWord.clear()
        if (suffix.isNotEmpty()) {
            currentInputConnection?.commitText(suffix, 1)
            if (suffix.endsWith(". ") || suffix.endsWith("! ") || suffix.endsWith("? ")) {
                currentSentence = ""
            }
        }

        updateSuggestions(safePredictions(""))
    }

    fun toggleEmojiPanel() {
        activePanel = if (activePanel == PanelMode.EMOJI) PanelMode.NONE else PanelMode.EMOJI
        populatePanel(activePanel)
    }

    fun toggleClipboardPanel() {
        syncClipboardFromSystem()
        activePanel = if (activePanel == PanelMode.CLIPBOARD) PanelMode.NONE else PanelMode.CLIPBOARD
        populatePanel(activePanel)
    }

    fun hidePanels() {
        activePanel = PanelMode.NONE
        populatePanel(activePanel)
    }

    private fun populatePanel(mode: PanelMode) {
        if (!::accessoryBar.isInitialized) return

        accessoryBar.removeAllViews()
        val palette = KeyboardThemes.palette(preferencesManager.selectedTheme)
        accessoryScroller.setBackgroundColor(palette.panelBackground)
        accessoryBar.setBackgroundColor(palette.panelBackground)

        val entries = when (mode) {
            PanelMode.EMOJI -> commonEmoji
            PanelMode.CLIPBOARD -> getClipboardEntriesForPanel()
            PanelMode.NONE -> defaultQuickActions()
        }

        entries.forEach { entry ->
            accessoryBar.addView(buildActionChip(entry.label).apply {
                setOnClickListener {
                    when (entry.type) {
                        PanelEntryType.TEXT -> commitText(entry.value)
                        PanelEntryType.CLIPBOARD -> commitClipboardEntry(entry.value)
                        PanelEntryType.ACTION -> handlePanelAction(entry.value)
                    }
                }
            })
        }

        accessoryScroller.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun defaultQuickActions(): List<PanelEntry> {
        val items = mutableListOf(
            PanelEntry("😊 Emoji", "emoji_panel", PanelEntryType.ACTION),
            PanelEntry("📋 Clipboard", "clipboard_panel", PanelEntryType.ACTION),
            PanelEntry("—", "—"),
            PanelEntry("…", "…"),
            PanelEntry("@", "@"),
            PanelEntry("#", "#")
        )
        if (preferencesManager.punctuationShortcutsEnabled) {
            items.addAll(listOf(
                PanelEntry("!", "!"),
                PanelEntry("?", "?"),
                PanelEntry(".com", ".com")
            ))
        }
        return items
    }

    private fun getClipboardEntriesForPanel(): List<PanelEntry> {
        val history = preferencesManager.getClipboardHistory()
        if (history.isEmpty()) {
            return listOf(PanelEntry("Clipboard empty", "", PanelEntryType.ACTION))
        }
        return history.map {
            PanelEntry(it.take(30).ifBlank { "Clipboard" }, it, PanelEntryType.CLIPBOARD)
        }
    }

    private fun commitClipboardEntry(text: String) {
        currentInputConnection?.commitText(text, 1)
        preferencesManager.addClipboardEntry(text)
        currentWord.clear()
        updateSuggestions(safePredictions(""))
    }

    private fun handlePanelAction(action: String) {
        when (action) {
            "emoji_panel" -> toggleEmojiPanel()
            "clipboard_panel" -> toggleClipboardPanel()
        }
    }

    private fun syncClipboardFromSystem() {
        if (!preferencesManager.clipboardPanelEnabled) return
        try {
            val clip = clipboardManager.primaryClip ?: return
            if (clip.itemCount == 0) return
            val desc = clipboardManager.primaryClipDescription
            if (desc != null && !desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) && !desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
                return
            }
            val text = clip.getItemAt(0).coerceToText(this)?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) {
                preferencesManager.addClipboardEntry(text)
            }
        } catch (_: Exception) {
        }
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

    fun updateSuggestions(suggestions: List<String>) {
        handler.post {
            val palette = KeyboardThemes.palette(preferencesManager.selectedTheme)
            suggestions.forEachIndexed { index, suggestion ->
                if (index < suggestionChips.size) {
                    suggestionChips[index].text = suggestion
                    suggestionChips[index].alpha = if (suggestion.isEmpty()) 0.35f else 1.0f
                    suggestionChips[index].setTextColor(palette.keyTextColor)
                    suggestionChips[index].setBackgroundColor(palette.chipBackground)
                }
            }
        }
    }

    private fun applyTheme() {
        if (!::rootContainer.isInitialized) return
        val palette = KeyboardThemes.palette(preferencesManager.selectedTheme)
        rootContainer.setBackgroundColor(palette.keyboardBackground)
        suggestionBar.setBackgroundColor(palette.panelBackground)
        suggestionChips.forEach {
            it.setTextColor(palette.keyTextColor)
            it.setBackgroundColor(palette.chipBackground)
        }
        accessoryScroller.setBackgroundColor(palette.panelBackground)
        accessoryBar.setBackgroundColor(palette.panelBackground)
    }

    private fun buildActionChip(text: String = ""): TextView {
        val palette = KeyboardThemes.palette(preferencesManager.selectedTheme)
        return TextView(this).apply {
            this.text = text
            setTextColor(palette.keyTextColor)
            textSize = 15f
            setPadding(dp(14), dp(8), dp(14), dp(8))
            setBackgroundColor(palette.chipBackground)
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(8)
            }
        }
    }

    private fun shouldAutoCapitalize(info: EditorInfo?): Boolean {
        val inputType = info?.inputType ?: return false
        val capitalizationMask = inputType and InputType.TYPE_MASK_FLAGS
        return capitalizationMask and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES != 0
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

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private data class PanelEntry(
        val label: String,
        val value: String,
        val type: PanelEntryType = PanelEntryType.TEXT
    )

    private enum class PanelEntryType {
        TEXT,
        CLIPBOARD,
        ACTION
    }

    companion object {
        private val commonEmoji = listOf(
            PanelEntry("😀", "😀"), PanelEntry("😂", "😂"), PanelEntry("🥹", "🥹"),
            PanelEntry("😍", "😍"), PanelEntry("🙏", "🙏"), PanelEntry("🔥", "🔥"),
            PanelEntry("🎉", "🎉"), PanelEntry("❤️", "❤️"), PanelEntry("👍", "👍"),
            PanelEntry("🤝", "🤝"), PanelEntry("🤔", "🤔"), PanelEntry("😅", "😅"),
            PanelEntry("😭", "😭"), PanelEntry("💀", "💀"), PanelEntry("✨", "✨"),
            PanelEntry("👏", "👏"), PanelEntry("🙌", "🙌"), PanelEntry("🤷", "🤷")
        )
    }
}

package com.aikeyboard.app

import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class KeyboardView(
    private val service: AIMethodService,
    private val preferencesManager: PreferencesManager
) : LinearLayout(service) {

    private var isShifted = false
    private var isSymbols = false
    private val repeatHandler = Handler(Looper.getMainLooper())
    private var backspaceRepeating = false

    private val scale: Float
        get() = preferencesManager.keyboardScalePercent / 100f

    private val palette: KeyboardPalette
        get() = KeyboardThemes.palette(preferencesManager.selectedTheme)

    private val keyWidth: Int
        get() = (48 * scale).toInt()
    private val keyHeight: Int
        get() = (56 * scale).toInt()

    private val backspaceRepeater = object : Runnable {
        override fun run() {
            if (!backspaceRepeating) return
            service.handleBackspace()
            repeatHandler.postDelayed(this, 60)
        }
    }

    init {
        orientation = VERTICAL
        setPadding(dp(6), dp(6), dp(6), dp(6))
        rebuildKeys()
    }

    fun reloadLayout() {
        rebuildKeys()
    }

    private fun rebuildKeys() {
        removeAllViews()
        val currentPalette = palette
        setBackgroundColor(currentPalette.keyboardBackground)

        val keyboardHost = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            gravity = when (preferencesManager.oneHandedMode) {
                OneHandedMode.LEFT -> Gravity.START
                OneHandedMode.RIGHT -> Gravity.END
                OneHandedMode.OFF -> Gravity.CENTER_HORIZONTAL
            }
        }

        buildRows().forEach { row ->
            val rowLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
            row.forEach { key -> rowLayout.addView(createKeyView(key)) }
            keyboardHost.addView(rowLayout)
        }
        addView(keyboardHost)
    }

    private fun buildRows(): List<List<String>> {
        val rows = mutableListOf<List<String>>()

        if (preferencesManager.punctuationShortcutsEnabled) {
            rows.add(listOf("!", "?", ",", ".", "'", ":", ";", "@", "#", "/"))
        }

        if (isSymbols) {
            rows.add(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"))
            rows.add(listOf("@", "#", "$", "%", "&", "-", "+", "(", ")"))
            rows.add(listOf("*", "\"", "'", ":", ";", "!", "?", "/", "backspace"))
            rows.add(listOf(symbolToggleLabel(), emojiToggleKey(), clipboardToggleKey(), ",", "space", ".", "return"))
            return rows
        }

        if (preferencesManager.numberRowEnabled) {
            rows.add(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"))
        }
        rows.add(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"))
        rows.add(listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"))
        rows.add(listOf("shift", "z", "x", "c", "v", "b", "n", "m", "backspace"))
        rows.add(listOf(symbolToggleLabel(), emojiToggleKey(), clipboardToggleKey(), ",", "space", ".", "return"))
        return rows
    }

    private fun emojiToggleKey(): String = if (preferencesManager.emojiPanelEnabled) "emoji" else "globe"

    private fun clipboardToggleKey(): String = if (preferencesManager.clipboardPanelEnabled) "clip" else "prefs"

    private fun symbolToggleLabel(): String = if (isSymbols) "ABC" else "123"

    private fun createKeyView(key: String): View {
        val currentPalette = palette
        val textView = TextView(context).apply {
            text = getKeyLabel(key)
            setTextColor(currentPalette.keyTextColor)
            textSize = when (key) {
                "space" -> 15f
                "emoji", "clip", "globe" -> 18f
                else -> 18f
            }
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setBackgroundColor(getKeyBackground(key))
            isClickable = true
            isFocusable = false
        }

        val size = getKeySize(key)
        textView.layoutParams = LayoutParams(size.first, size.second).apply {
            setMargins(dp(2), dp(2), dp(2), dp(2))
        }

        textView.setOnClickListener {
            handleKeyPress(key)
        }

        textView.setOnLongClickListener {
            when (key) {
                "backspace" -> {
                    backspaceRepeating = true
                    repeatHandler.post(backspaceRepeater)
                    true
                }
                "." -> {
                    service.commitText(".com")
                    true
                }
                "," -> {
                    service.commitText("?! ")
                    true
                }
                else -> false
            }
        }

        textView.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.setBackgroundColor(currentPalette.pressedKeyBackground)
                    false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.setBackgroundColor(getKeyBackground(key))
                    if (key == "backspace") stopBackspaceRepeat()
                    false
                }
                else -> false
            }
        }

        return textView
    }

    private fun getKeyLabel(key: String): String {
        return when (key) {
            "shift" -> "⇧"
            "backspace" -> "⌫"
            "space" -> "space"
            "return" -> "↵"
            "123" -> "123"
            "ABC" -> "ABC"
            "emoji" -> "😊"
            "clip" -> "📋"
            "globe" -> "⚙"
            "prefs" -> "⋯"
            else -> if (!isSymbols && isShifted && key.length == 1 && key[0].isLetter()) key.uppercase() else key
        }
    }

    private fun getKeyBackground(key: String): Int {
        val currentPalette = palette
        return when (key) {
            "shift", "backspace", "123", "ABC", "return", "emoji", "clip", "globe", "prefs" -> currentPalette.specialKeyBackground
            else -> currentPalette.keyBackground
        }
    }

    private fun getKeySize(key: String): Pair<Int, Int> {
        val widthMultiplier = when (preferencesManager.oneHandedMode) {
            OneHandedMode.OFF -> 1f
            OneHandedMode.LEFT, OneHandedMode.RIGHT -> 0.86f
        }
        val baseWidth = (keyWidth * widthMultiplier).toInt().coerceAtLeast(dp(28))
        return when (key) {
            "space" -> Pair((baseWidth * 3.6f).toInt(), keyHeight)
            "backspace", "return" -> Pair((baseWidth * 1.45f).toInt(), keyHeight)
            "shift", "123", "ABC", "emoji", "clip", "globe", "prefs" -> Pair((baseWidth * 1.15f).toInt(), keyHeight)
            else -> Pair(baseWidth, keyHeight)
        }
    }

    private fun handleKeyPress(key: String) {
        service.onKeyPressed()
        when (key) {
            "shift" -> {
                isShifted = !isShifted
                rebuildKeys()
            }
            "backspace" -> service.handleBackspace()
            "space" -> service.handleSpace()
            "return" -> service.handleReturn()
            "123" -> {
                isSymbols = true
                rebuildKeys()
            }
            "ABC" -> {
                isSymbols = false
                rebuildKeys()
            }
            "emoji" -> service.toggleEmojiPanel()
            "clip" -> service.toggleClipboardPanel()
            "globe", "prefs" -> service.hidePanels()
            else -> {
                val output = if (!isSymbols && isShifted) key.uppercase() else key
                if (output.length == 1 && !output[0].isLetterOrDigit() && output[0] != '\'') {
                    service.handlePunctuation(output)
                } else {
                    service.handleCharacter(output)
                }
            }
        }

        if (!isSymbols && key != "shift" && isShifted && key.length == 1 && key[0].isLetter()) {
            isShifted = false
            rebuildKeys()
        }
    }

    private fun stopBackspaceRepeat() {
        backspaceRepeating = false
        repeatHandler.removeCallbacks(backspaceRepeater)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}

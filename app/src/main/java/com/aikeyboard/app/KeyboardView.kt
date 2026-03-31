package com.aikeyboard.app

import android.graphics.Color
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
    private val repeatHandler = Handler(Looper.getMainLooper())
    private var backspaceRepeating = false

    private val scale: Float
        get() = preferencesManager.keyboardScalePercent / 100f

    private val normalBgColor = Color.parseColor("#4A4A4A")
    private val specialBgColor = Color.parseColor("#2D2D2D")
    private val pressedBgColor = Color.parseColor("#707070")
    private val keyTextColor = Color.WHITE

    private val keyWidth: Int
        get() = (72 * scale).toInt()
    private val keyHeight: Int
        get() = (80 * scale).toInt()

    private val backspaceRepeater = object : Runnable {
        override fun run() {
            if (!backspaceRepeating) return
            service.handleBackspace()
            repeatHandler.postDelayed(this, 60)
        }
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#1A1A1A"))
        setPadding(dp(6), dp(6), dp(6), dp(6))
        rebuildKeys()
    }

    fun reloadLayout() {
        rebuildKeys()
    }

    private fun rebuildKeys() {
        removeAllViews()
        buildRows().forEach { row ->
            val rowLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL
            }

            row.forEach { key -> rowLayout.addView(createKeyView(key)) }
            addView(rowLayout)
        }
    }

    private fun buildRows(): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        if (preferencesManager.numberRowEnabled) {
            rows.add(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"))
        }
        rows.add(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"))
        rows.add(listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"))
        rows.add(listOf("shift", "z", "x", "c", "v", "b", "n", "m", "backspace"))
        rows.add(listOf("123", ",", "space", ".", "return"))
        return rows
    }

    private fun createKeyView(key: String): View {
        val textView = TextView(context).apply {
            text = getKeyLabel(key)
            setTextColor(keyTextColor)
            textSize = if (key == "space") 16f else 20f
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
            if (key == "backspace") {
                backspaceRepeating = true
                repeatHandler.post(backspaceRepeater)
                true
            } else {
                false
            }
        }

        textView.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.setBackgroundColor(pressedBgColor)
                    false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.setBackgroundColor(getKeyBackground(key))
                    if (key == "backspace") {
                        stopBackspaceRepeat()
                    }
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
            else -> if (isShifted && key.length == 1 && key[0].isLetter()) key.uppercase() else key
        }
    }

    private fun getKeyBackground(key: String): Int {
        return when (key) {
            "shift", "backspace", "123", "return" -> specialBgColor
            else -> normalBgColor
        }
    }

    private fun getKeySize(key: String): Pair<Int, Int> {
        return when (key) {
            "space" -> Pair((keyWidth * 5.4f).toInt(), keyHeight)
            "backspace", "return" -> Pair((keyWidth * 1.7f).toInt(), keyHeight)
            "shift", "123" -> Pair((keyWidth * 1.3f).toInt(), keyHeight)
            else -> Pair(keyWidth, keyHeight)
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
            "123" -> { /* placeholder for symbols layout */ }
            else -> service.handleCharacter(if (isShifted) key.uppercase() else key)
        }

        if (key != "shift" && isShifted && key.length == 1 && key[0].isLetter()) {
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

package com.aikeyboard.app

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView

class KeyboardView(private val service: AIMethodService) : LinearLayout(service) {

    private val keyRows = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("shift", "z", "x", "c", "v", "b", "n", "m", "backspace"),
        listOf("123", ",", "space", ".", "return")
    )

    private var isShifted = false
    private var currentPopup: PopupWindow? = null
    private var lastTouchedKey: String? = null

    private val keyWidth = 72
    private val keyHeight = 80
    private val popupWidth = 120
    private val popupHeight = 80

    private val normalBgColor = Color.parseColor("#4A4A4A")
    private val specialBgColor = Color.parseColor("#2D2D2D")
    private val keyTextColor = Color.WHITE
    private val shiftedBgColor = Color.parseColor("#6B6B6B")

    private var currentSuggestions = listOf("", "", "")

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#1A1A1A"))
        setPadding(8, 8, 8, 8)

        setupKeyRows()
    }

    private fun setupKeyRows() {
        for (row in keyRows) {
            val rowLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL
            }

            for (key in row) {
                val keyView = createKeyView(key)
                rowLayout.addView(keyView)
            }

            addView(rowLayout)
        }
    }

    private fun createKeyView(key: String): View {
        val textView = TextView(context).apply {
            text = getKeyLabel(key)
            setTextColor(keyTextColor)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setBackgroundColor(getKeyBackground(key))
        }

        val size = getKeySize(key)
        textView.layoutParams = LayoutParams(size.first, size.second).apply {
            setMargins(4, 4, 4, 4)
        }

        textView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchedKey = key
                    showPopup(v as TextView, key)
                    v.alpha = 0.7f
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    dismissPopup()
                    v.alpha = 1.0f
                    if (event.action == MotionEvent.ACTION_UP) {
                        handleKeyPress(key)
                    }
                }
            }
            true
        }

        return textView
    }

    private fun getKeyLabel(key: String): String {
        return when (key) {
            "shift" -> if (isShifted) "⇧" else "⇧"
            "backspace" -> "⌫"
            "space" -> "space"
            "return" -> "↵"
            "123" -> "123"
            else -> if (isShifted) key.uppercase() else key
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
            "space" -> Pair(400, keyHeight)
            "backspace", "return" -> Pair(140, keyHeight)
            "shift" -> Pair(100, keyHeight)
            "123" -> Pair(100, keyHeight)
            else -> Pair(keyWidth, keyHeight)
        }
    }

    private fun showPopup(anchor: TextView, key: String) {
        dismissPopup()

        val popupView = TextView(context).apply {
            text = getKeyLabel(key)
            setTextColor(Color.WHITE)
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setBackgroundColor(if (key == "shift") shiftedBgColor else normalBgColor)
        }

        currentPopup = PopupWindow(popupView, popupWidth, popupHeight).apply {
            showAtLocation(anchor, Gravity.CENTER, 0, 0)
        }
    }

    private fun dismissPopup() {
        currentPopup?.dismiss()
        currentPopup = null
    }

    private fun handleKeyPress(key: String) {
        when (key) {
            "shift" -> {
                isShifted = !isShifted
                refreshKeys()
            }
            "backspace" -> service.handleBackspace()
            "space" -> service.handleSpace()
            "return" -> service.handleReturn()
            "123" -> { /* TODO: Switch to number layout */ }
            else -> service.handleCharacter(if (isShifted) key.uppercase() else key)
        }

        if (key != "shift" && isShifted) {
            isShifted = false
            refreshKeys()
        }
    }

    private fun refreshKeys() {
        removeAllViews()
        setupKeyRows()
    }

    fun updateSuggestions(suggestions: List<String>) {
        currentSuggestions = suggestions
        service.updateSuggestions(suggestions)
    }
}

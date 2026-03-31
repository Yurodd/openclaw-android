package com.aikeyboard.app

import android.graphics.Typeface
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefsManager: PreferencesManager
    private lateinit var userHistoryManager: UserHistoryManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefsManager = PreferencesManager(this)
        userHistoryManager = UserHistoryManager(this)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val scrollView = ScrollView(this).apply {
            addView(content)
        }

        val title = TextView(this).apply {
            text = "AI Keyboard Settings"
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        }
        content.addView(title)

        val intro = TextView(this).apply {
            text = "Helpful defaults are on by default: max keyboard size, number row, emoji, clipboard, and punctuation shortcuts."
            textSize = 14f
            setPadding(0, 0, 0, 24)
        }
        content.addView(intro)

        content.addView(sectionTitle("Typing"))
        content.addView(makeSwitch(
            label = "Enable learning",
            checked = prefsManager.learningEnabled
        ) { prefsManager.learningEnabled = it })
        content.addView(makeSwitch(
            label = "Show number row",
            checked = prefsManager.numberRowEnabled
        ) { prefsManager.numberRowEnabled = it })
        content.addView(makeSwitch(
            label = "Punctuation shortcuts row",
            checked = prefsManager.punctuationShortcutsEnabled
        ) { prefsManager.punctuationShortcutsEnabled = it })
        content.addView(makeSwitch(
            label = "Emoji panel key",
            checked = prefsManager.emojiPanelEnabled
        ) { prefsManager.emojiPanelEnabled = it })
        content.addView(makeSwitch(
            label = "Clipboard panel key",
            checked = prefsManager.clipboardPanelEnabled
        ) { prefsManager.clipboardPanelEnabled = it })
        content.addView(makeSwitch(
            label = "Haptic feedback",
            checked = prefsManager.hapticEnabled
        ) { prefsManager.hapticEnabled = it })
        content.addView(makeSwitch(
            label = "Key sound",
            checked = prefsManager.soundEnabled
        ) { prefsManager.soundEnabled = it })

        content.addView(sectionTitle("Layout"))

        val sizeTitle = TextView(this).apply {
            text = "Keyboard size"
            textSize = 18f
            setPadding(0, 12, 0, 8)
        }
        content.addView(sizeTitle)

        val sizeValue = TextView(this).apply {
            text = "${prefsManager.keyboardScalePercent}%"
            textSize = 14f
            setPadding(0, 0, 0, 8)
        }
        content.addView(sizeValue)

        val sizeSeek = SeekBar(this).apply {
            max = 60
            progress = prefsManager.keyboardScalePercent - 80
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = progress + 80
                    prefsManager.keyboardScalePercent = value
                    sizeValue.text = "$value%"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        content.addView(sizeSeek)

        content.addView(makeSpinnerRow(
            title = "Theme",
            options = KeyboardThemeOption.entries.map { it.label },
            selectedIndex = KeyboardThemeOption.entries.indexOf(prefsManager.selectedTheme)
        ) { selected ->
            prefsManager.selectedTheme = KeyboardThemeOption.entries[selected]
        })

        content.addView(makeSpinnerRow(
            title = "One-handed mode",
            options = OneHandedMode.entries.map { it.label },
            selectedIndex = OneHandedMode.entries.indexOf(prefsManager.oneHandedMode)
        ) { selected ->
            prefsManager.oneHandedMode = OneHandedMode.entries[selected]
        })

        content.addView(sectionTitle("Data"))

        val statsTitle = TextView(this).apply {
            text = "Statistics"
            textSize = 18f
            setPadding(0, 12, 0, 16)
        }
        content.addView(statsTitle)

        val wordsTyped = TextView(this).apply {
            text = "Words typed: ${prefsManager.totalWordsTyped}"
            textSize = 16f
            setPadding(0, 8, 0, 8)
        }
        content.addView(wordsTyped)

        val accuracy = TextView(this).apply {
            text = "Prediction accuracy: ${String.format("%.1f", prefsManager.getPredictionAccuracy())}%"
            textSize = 16f
            setPadding(0, 8, 0, 16)
        }
        content.addView(accuracy)

        val clearClipboardButton = Button(this).apply {
            text = "Clear clipboard history"
            setOnClickListener { prefsManager.clearClipboardHistory() }
        }
        content.addView(clearClipboardButton)

        val clearButton = Button(this).apply {
            text = "Clear learned history"
            setOnClickListener {
                userHistoryManager.clearHistory()
                prefsManager.totalWordsTyped = 0
                prefsManager.totalPredictions = 0
                wordsTyped.text = "Words typed: 0"
                accuracy.text = "Prediction accuracy: 0.0%"
            }
        }
        content.addView(clearButton)

        content.addView(sectionTitle("What shipped"))
        val featureText = TextView(this).apply {
            text = "• Theme support: Dark, Light, AMOLED\n" +
                "• One-handed layout alignment (left / normal / right)\n" +
                "• Emoji quick-insert panel\n" +
                "• Clipboard history quick-insert panel\n" +
                "• Punctuation shortcuts row + long-press .com / ?!\n" +
                "• Number row, haptics, and max size default on"
            textSize = 14f
        }
        content.addView(featureText)

        setContentView(scrollView)
    }

    private fun makeSwitch(label: String, checked: Boolean, onToggle: (Boolean) -> Unit): Switch {
        return Switch(this).apply {
            text = label
            isChecked = checked
            textSize = 18f
            setPadding(0, 8, 0, 8)
            setOnCheckedChangeListener { _, isChecked -> onToggle(isChecked) }
        }
    }

    private fun makeSpinnerRow(
        title: String,
        options: List<String>,
        selectedIndex: Int,
        onSelected: (Int) -> Unit
    ): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 8)
        }
        row.addView(TextView(this).apply {
            text = title
            textSize = 18f
            setPadding(0, 0, 0, 8)
        })
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        spinner.setSelection(selectedIndex.coerceAtLeast(0))
        spinner.post { onSelected(spinner.selectedItemPosition) }
        spinner.onItemSelectedListener = SimpleItemSelectedListener { position -> onSelected(position) }
        row.addView(spinner)
        return row
    }

    private fun sectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 24, 0, 12)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        userHistoryManager.close()
    }
}

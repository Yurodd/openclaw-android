package com.aikeyboard.app

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
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
            setPadding(0, 0, 0, 48)
        }
        content.addView(title)

        content.addView(makeSwitch(
            label = "Enable Learning",
            checked = prefsManager.learningEnabled
        ) { prefsManager.learningEnabled = it })

        content.addView(makeSwitch(
            label = "Enable Number Row",
            checked = prefsManager.numberRowEnabled
        ) { prefsManager.numberRowEnabled = it })

        content.addView(makeSwitch(
            label = "Haptic Feedback",
            checked = prefsManager.hapticEnabled
        ) { prefsManager.hapticEnabled = it })

        content.addView(makeSwitch(
            label = "Key Sound",
            checked = prefsManager.soundEnabled
        ) { prefsManager.soundEnabled = it })

        val sizeTitle = TextView(this).apply {
            text = "Keyboard Size"
            textSize = 18f
            setPadding(0, 36, 0, 8)
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

        val statsTitle = TextView(this).apply {
            text = "Statistics"
            textSize = 18f
            setPadding(0, 32, 0, 16)
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
            setPadding(0, 8, 0, 8)
        }
        content.addView(accuracy)

        val clearButton = Button(this).apply {
            text = "Clear History"
            setOnClickListener {
                userHistoryManager.clearHistory()
                prefsManager.totalWordsTyped = 0
                prefsManager.totalPredictions = 0
                wordsTyped.text = "Words typed: 0"
                accuracy.text = "Prediction accuracy: 0.0%"
            }
        }
        content.addView(clearButton)

        val featureTitle = TextView(this).apply {
            text = "Current Features"
            textSize = 18f
            setPadding(0, 32, 0, 16)
        }
        content.addView(featureTitle)

        val featureText = TextView(this).apply {
            text = "• Next-word suggestions\n• Learned word history\n• Number row toggle\n• Adjustable keyboard size\n• Haptic and sound toggles\n• Long-press backspace repeat"
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

    override fun onDestroy() {
        super.onDestroy()
        userHistoryManager.close()
    }
}

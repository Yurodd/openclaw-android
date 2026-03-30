package com.aikeyboard.app

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
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

        val layout = LinearLayout(this).apply {
            orientation = VERTICAL
            setPadding(48, 48, 48, 48)
        }

        // Title
        val title = TextView(this).apply {
            text = "AI Keyboard Settings"
            textSize = 24f
            setPadding(0, 0, 0, 48)
        }
        layout.addView(title)

        // Learning toggle
        val learningSwitch = Switch(this).apply {
            text = "Enable Learning"
            isChecked = prefsManager.learningEnabled
            textSize = 18f
            setOnCheckedChangeListener { _, isChecked ->
                prefsManager.learningEnabled = isChecked
            }
        }
        layout.addView(learningSwitch)

        // Stats section
        val statsTitle = TextView(this).apply {
            text = "Statistics"
            textSize = 18f
            setPadding(0, 32, 0, 16)
        }
        layout.addView(statsTitle)

        val wordsTyped = TextView(this).apply {
            text = "Words typed: ${prefsManager.totalWordsTyped}"
            textSize = 16f
            setPadding(0, 8, 0, 8)
        }
        layout.addView(wordsTyped)

        val accuracy = TextView(this).apply {
            text = "Prediction accuracy: ${String.format("%.1f", prefsManager.getPredictionAccuracy())}%"
            textSize = 16f
            setPadding(0, 8, 0, 8)
        }
        layout.addView(accuracy)

        // Clear history button
        val clearButton = Button(this).apply {
            text = "Clear History"
            setOnClickListener {
                userHistoryManager.clearAll()
                prefsManager.clearAll()
                wordsTyped.text = "Words typed: 0"
                accuracy.text = "Prediction accuracy: 0.0%"
            }
        }
        layout.addView(clearButton)

        // About section
        val aboutTitle = TextView(this).apply {
            text = "About"
            textSize = 18f
            setPadding(0, 32, 0, 16)
        }
        layout.addView(aboutTitle)

        val aboutText = TextView(this).apply {
            text = "AI Keyboard v1.0\n\nLearns your typing patterns to provide smarter predictions. All data stays on your device."
            textSize = 14f
        }
        layout.addView(aboutText)

        setContentView(layout)
    }

    override fun onDestroy() {
        super.onDestroy()
        userHistoryManager.close()
    }
}

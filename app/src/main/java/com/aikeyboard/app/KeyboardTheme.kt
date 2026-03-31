package com.aikeyboard.app

import android.graphics.Color

enum class KeyboardThemeOption(val storageValue: String, val label: String) {
    DARK("dark", "Dark"),
    LIGHT("light", "Light"),
    AMOLED("amoled", "AMOLED");

    companion object {
        fun fromStorage(value: String?): KeyboardThemeOption {
            return entries.firstOrNull { it.storageValue == value } ?: DARK
        }
    }
}

enum class OneHandedMode(val storageValue: String, val label: String) {
    OFF("off", "Normal"),
    LEFT("left", "Left-handed"),
    RIGHT("right", "Right-handed");

    companion object {
        fun fromStorage(value: String?): OneHandedMode {
            return entries.firstOrNull { it.storageValue == value } ?: OFF
        }
    }
}

data class KeyboardPalette(
    val keyboardBackground: Int,
    val panelBackground: Int,
    val keyBackground: Int,
    val specialKeyBackground: Int,
    val pressedKeyBackground: Int,
    val keyTextColor: Int,
    val subtleTextColor: Int,
    val chipBackground: Int
)

object KeyboardThemes {
    fun palette(option: KeyboardThemeOption): KeyboardPalette {
        return when (option) {
            KeyboardThemeOption.LIGHT -> KeyboardPalette(
                keyboardBackground = Color.parseColor("#F4F5F7"),
                panelBackground = Color.parseColor("#FFFFFF"),
                keyBackground = Color.parseColor("#FFFFFF"),
                specialKeyBackground = Color.parseColor("#D9DEE5"),
                pressedKeyBackground = Color.parseColor("#BFC7D2"),
                keyTextColor = Color.parseColor("#111827"),
                subtleTextColor = Color.parseColor("#4B5563"),
                chipBackground = Color.parseColor("#E5E7EB")
            )
            KeyboardThemeOption.AMOLED -> KeyboardPalette(
                keyboardBackground = Color.BLACK,
                panelBackground = Color.parseColor("#050505"),
                keyBackground = Color.parseColor("#121212"),
                specialKeyBackground = Color.parseColor("#1E1E1E"),
                pressedKeyBackground = Color.parseColor("#2A2A2A"),
                keyTextColor = Color.WHITE,
                subtleTextColor = Color.parseColor("#BDBDBD"),
                chipBackground = Color.parseColor("#171717")
            )
            KeyboardThemeOption.DARK -> KeyboardPalette(
                keyboardBackground = Color.parseColor("#1A1A1A"),
                panelBackground = Color.parseColor("#252525"),
                keyBackground = Color.parseColor("#4A4A4A"),
                specialKeyBackground = Color.parseColor("#2D2D2D"),
                pressedKeyBackground = Color.parseColor("#707070"),
                keyTextColor = Color.WHITE,
                subtleTextColor = Color.parseColor("#C7C7C7"),
                chipBackground = Color.parseColor("#333333")
            )
        }
    }
}

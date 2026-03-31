package com.aikeyboard.app

/**
 * Lightweight, conservative autocorrect for common typos.
 * Designed to run on word commit (space / punctuation), not every keystroke.
 */
class AutoCorrectEngine(
    private val userHistoryManager: UserHistoryManager
) {

    private val commonCorrections = mapOf(
        "teh" to "the",
        "adn" to "and",
        "thsi" to "this",
        "taht" to "that",
        "wiht" to "with",
        "wih" to "with",
        "recieve" to "receive",
        "seperate" to "separate",
        "definately" to "definitely",
        "occured" to "occurred",
        "untill" to "until",
        "tommorow" to "tomorrow",
        "becuase" to "because",
        "beggining" to "beginning",
        "goverment" to "government",
        "enviroment" to "environment",
        "agian" to "again",
        "wierd" to "weird",
        "dont" to "don't",
        "cant" to "can't",
        "wont" to "won't",
        "didnt" to "didn't",
        "doesnt" to "doesn't",
        "isnt" to "isn't",
        "arent" to "aren't",
        "shouldnt" to "shouldn't",
        "wouldnt" to "wouldn't",
        "couldnt" to "couldn't",
        "ive" to "i've",
        "ill" to "i'll",
        "im" to "i'm",
        "id" to "i'd"
    )

    fun getCorrection(word: String): String? {
        val trimmed = word.trim()
        if (trimmed.length < 3) return null
        if (!trimmed.any { it.isLetter() }) return null

        val normalized = trimmed.lowercase()
        val candidate = commonCorrections[normalized] ?: return null

        // Be conservative: don't replace words the user already types often.
        if (userHistoryManager.getWordFrequency(normalized) > 1) return null

        return applyCaseStyle(trimmed, candidate)
    }

    fun previewCorrection(word: String): String? {
        return getCorrection(word)
    }

    private fun applyCaseStyle(original: String, corrected: String): String {
        return when {
            original.all { !it.isLetter() || it.isUpperCase() } -> corrected.uppercase()
            original.firstOrNull()?.isUpperCase() == true -> corrected.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase() else ch.toString()
            }
            else -> corrected
        }
    }
}

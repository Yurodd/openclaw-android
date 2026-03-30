package com.aikeyboard.app

/**
 * Hybrid prediction engine - simplified for initial build.
 * Full RRF + Vector implementation coming soon.
 */
class PredictionEngine(private val userHistoryManager: UserHistoryManager) {

    companion object {
        private val COMMON_WORDS = listOf(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "I",
            "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
            "this", "but", "his", "by", "from", "they", "we", "say", "her", "she"
        )
    }

    fun predictNextWord(context: String, currentPrefix: String = ""): List<String> {
        val words = context.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        
        val results = mutableListOf<String>()
        
        // Get user's recent words
        if (words.isNotEmpty()) {
            val predictions = userHistoryManager.getWordPredictions(words.last())
            results.addAll(predictions)
        }
        
        // Add common words as fallback
        results.addAll(COMMON_WORDS.take(3))
        
        // Filter by prefix if needed
        val filtered = if (currentPrefix.isNotEmpty()) {
            results.filter { it.startsWith(currentPrefix.lowercase()) }
        } else {
            results
        }
        
        return filtered.distinct().take(3).ifEmpty { COMMON_WORDS.take(3) }
    }

    fun predictPhrase(partial: String): List<String> {
        return userHistoryManager.getPhrasePredictions(partial).take(3)
    }

    fun getLearningSuggestions(context: String): List<String> {
        return emptyList()
    }
}

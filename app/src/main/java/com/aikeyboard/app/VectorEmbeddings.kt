package com.aikeyboard.app

/**
 * Simple semantic similarity using word co-occurrence.
 * This is a minimal implementation - the full version with embeddings is coming soon.
 */
class VectorEmbeddings {
    
    companion object {
        const val RRF_K = 60
    }

    // Simple word similarity map based on common word associations
    private val similarWords = mapOf(
        "i" to listOf("we", "you", "they"),
        "you" to listOf("I", "we", "they"),
        "the" to listOf("a", "this", "that"),
        "is" to listOf("are", "was", "be"),
        "it" to listOf("this", "that", "he", "she"),
        "to" to listOf("for", "from", "of"),
        "and" to listOf("or", "but", "with"),
        "good" to listOf("great", "best", "nice"),
        "great" to listOf("good", "amazing", "best"),
        "bad" to listOf("good", "worst", "terrible"),
        "love" to listOf("like", "hate", "enjoy"),
        "want" to listOf("need", "wish", "desire"),
        "need" to listOf("want", "must", "should"),
        "time" to listOf("moment", "hour", "day"),
        "day" to listOf("time", "night", "today"),
        "see" to listOf("watch", "look", "view"),
        "go" to listOf("come", "move", "walk"),
        "come" to listOf("go", "arrive", "visit"),
        "think" to listOf("believe", "know", "feel"),
        "know" to listOf("think", "believe", "understand")
    )

    fun vocabularySize(): Int = similarWords.size

    fun getSimilarFromContext(contextWords: List<String>, limit: Int = 10): List<Pair<String, Float>> {
        val results = mutableListOf<Pair<String, Float>>()
        for (word in contextWords) {
            similarWords[word.lowercase()]?.forEach { similar ->
                results.add(similar to 0.5f)
            }
        }
        return results.distinct().take(limit)
    }

    fun getVector(word: String): FloatArray? = null
    fun findSimilarWords(word: String, limit: Int = 10): List<Pair<String, Float>> = emptyList()
}

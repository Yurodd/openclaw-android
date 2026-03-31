package com.aikeyboard.app

/**
 * Hybrid prediction engine using RRF (Reciprocal Rank Fusion).
 * Combines:
 * 1. FTS5 - exact prefix matching from user history
 * 2. Bigram/Trigram - learned word sequences
 * 3. Vector embeddings - semantic similarity
 * 
 * All computation is on-device - no internet required.
 */
class PredictionEngine(private val userHistoryManager: UserHistoryManager) {

    companion object {
        private const val RRF_K = 60
        
        // Common English bigrams for fallback
        private val COMMON_BIGRAMS = mapOf(
            "i" to listOf("am", "was", "have", "will", "can", "would", "should", "could"),
            "i am" to listOf("going", "not", "a", "the", "so", "very", "really", "glad"),
            "i will" to listOf("be", "have", "do", "get", "see", "make", "go", "come"),
            "i have" to listOf("been", "a", "not", "some", "the", "no", "more", "had"),
            "i want" to listOf("to", "a", "the", "you", "this", "that", "some", "it"),
            "i need" to listOf("to", "a", "the", "you", "some", "this", "that", "it"),
            "i think" to listOf("that", "you", "this", "it", "so", "the", "we", "they"),
            "you are" to listOf("a", "the", "so", "very", "not", "going", "right", "good"),
            "you have" to listOf("a", "to", "been", "the", "some", "no", "not", "one"),
            "you can" to listOf("do", "be", "get", "have", "make", "see", "go", "come"),
            "the best" to listOf("way", "thing", "part", "time", "place", "choice", "decision"),
            "the first" to listOf("time", "step", "thing", "place", "person", "time"),
            "the most" to listOf("important", "common", "likely", "frequent", "popular"),
            "how are" to listOf("you", "they", "we", "things", "going"),
            "how do" to listOf("you", "we", "they", "I", "that", "this"),
            "what is" to listOf("the", "a", "that", "this", "it", "going on"),
            "what are" to listOf("you", "they", "we", "the", "those", "these"),
            "thank you" to listOf("very much", "so much", "for", "!"),
            "nice to" to listOf("meet", "see", "talk", "hear", "know"),
            "see you" to listOf("later", "soon", "tomorrow", "next", "!"),
            "good morning" to listOf("!", "how are", "hope you"),
            "good night" to listOf("!", "sleep well", "see you"),
            "a lot" to listOf("of", "to", "that", "more", "people"),
            "a lot of" to listOf("people", "things", "time", "work", "fun"),
            "i'm going" to listOf("to", "to the", "to be", "out", "home"),
            "i'm not" to listOf("sure", "going", "able", "available", "there"),
            "let me" to listOf("know", "see", "think", "check", "ask"),
            "do you" to listOf("want", "have", "know", "think", "like", "need"),
            "can you" to listOf("help", "do", "send", "give", "tell", "show"),
            "would you" to listOf("like", "mind", "be", "help", "please"),
            "it was" to listOf("a", "the", "not", "very", "really", "great"),
            "it is" to listOf("a", "the", "not", "very", "really", "important")
        )

        // Common trigrams
        private val COMMON_TRIGRAMS = mapOf(
            "nice to meet" to listOf("you"),
            "how are you" to listOf("doing", "today", "?"),
            "thank you very" to listOf("much", "so much"),
            "i don't know" to listOf("how", "what", "if", "that"),
            "what do you" to listOf("think", "want", "mean", "have", "need"),
            "how about" to listOf("you", "that", "we", "this"),
            "i would like" to listOf("to", "a", "the", "you", "to know"),
            "can i help" to listOf("you", "with that", "you with"),
            "let me know" to listOf("if", "when", "what", "that", "you"),
            "it was nice" to listOf("meeting", "seeing", "talking", "to"),
            "what's up" to listOf("?", "with you", "today"),
            "long time no" to listOf("see", "talk", "hear")
        )

        // Fallback common words
        private val COMMON_WORDS = listOf(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "I",
            "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
            "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
            "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
            "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
            "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
            "people", "into", "year", "your", "good", "some", "could", "them", "see", "other"
        )

    }

    /**
     * Predicts next words using hybrid RRF fusion.
     * @param context The sentence so far
     * @param currentPrefix The partial word being typed (if any)
     * @return Top 3 predicted words
     */
    fun predictNextWord(context: String, currentPrefix: String = ""): List<String> {
        val words = context.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val rrfScores = mutableMapOf<String, Float>()

        // Source 1: FTS5 exact prefix matches from user history
        if (currentPrefix.isNotEmpty()) {
            val ftsMatches = userHistoryManager.searchFTS(currentPrefix)
            addRRFScores(rrfScores, ftsMatches, sourceWeight = 3)
        }

        // Source 2: User's learned bigrams/trigrams
        if (words.isNotEmpty()) {
            val ngramPredictions = getNgramPredictions(words)
            addRRFScores(rrfScores, ngramPredictions, sourceWeight = 2)
        }

        // Source 3: Vector semantic similarity is disabled in the live IME path for stability.
        // It can be re-enabled later once the typing path is proven solid on-device.

        // Sort and filter
        val sorted = rrfScores.entries
            .sortedByDescending { it.value }
            .map { it.key }

        // Filter: if typing prefix, only show words starting with it
        val filtered = if (currentPrefix.isNotEmpty()) {
            sorted.filter { it.startsWith(currentPrefix.lowercase()) }
        } else {
            sorted
        }

        // Ensure we have 3 results
        val results = filtered.take(3).toMutableList()
        if (results.size < 3) {
            for (word in COMMON_WORDS) {
                if (word !in results) {
                    results.add(word)
                    if (results.size >= 3) break
                }
            }
        }

        return results.take(3)
    }

    /**
     * Gets n-gram predictions from user's history + common fallbacks.
     */
    private fun getNgramPredictions(words: List<String>): List<String> {
        val results = mutableListOf<String>()

        // Try trigram (last 2 words)
        if (words.size >= 2) {
            val trigramKey = "${words[words.size - 2]} ${words[words.size - 1]}"
            COMMON_TRIGRAMS[trigramKey]?.let { results.addAll(it.take(2)) }
            
            // Check user's learned trigrams
            val userTrigrams = userHistoryManager.getUserTrigramPredictions(
                words[words.size - 2], words[words.size - 1]
            )
            results.addAll(userTrigrams.take(2))
        }

        // Try bigram (last word)
        val lastWord = words.last()
        COMMON_BIGRAMS[lastWord]?.let { results.addAll(it.take(2)) }
        
        val userBigrams = userHistoryManager.getUserBigramPredictions(lastWord)
        results.addAll(userBigrams.take(2))

        return results.distinct()
    }

    /**
     * Adds RRF scores for a ranked list of predictions.
     * RRF formula: score = Σ 1/(k + rank)
     */
    private fun addRRFScores(
        scores: MutableMap<String, Float>,
        predictions: List<String>,
        sourceWeight: Int
    ) {
        predictions.forEachIndexed { index, word ->
            val rank = index + 1
            val rrfScore = 1.0f / (RRF_K + rank)
            val weightedScore = rrfScore * sourceWeight
            
            val existing = scores[word] ?: 0f
            scores[word] = existing + weightedScore
        }
    }

    /**
     * Predicts phrase completions.
     */
    fun predictPhrase(partial: String): List<String> {
        val userPhrases = userHistoryManager.getPhrasePredictions(partial)
        if (userPhrases.isNotEmpty()) {
            return userPhrases.take(3)
        }

        val words = partial.trim().lowercase().split(Regex("\\s+"))
        if (words.size >= 2) {
            val trigramKey = "${words[words.size - 2]} ${words[words.size - 1]}"
            COMMON_TRIGRAMS[trigramKey]?.let { completions ->
                val lastWord = words.last()
                return completions.map { "$lastWord $it" }.take(3)
            }
        }

        return emptyList()
    }

    /**
     * Placeholder for ML-based suggestions.
     */
    fun getLearningSuggestions(context: String): List<String> {
        return emptyList()
    }
}

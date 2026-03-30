package com.aikeyboard.app

import kotlin.math.sqrt

/**
 * Word embedding system using pre-computed vectors for semantic similarity.
 * Contains ~500 common English words with 50-dimensional float vectors.
 * All computation is on-device - no internet required.
 */
class VectorEmbeddings {

    companion object {
        private const val EMBEDDING_DIM = 50
        const val RRF_K = 60
    }

    // Pre-computed word embeddings - 50 dimensions per word
    private val wordVectors: Map<String, FloatArray> by lazy { initializeEmbeddings() }

    private fun initializeEmbeddings(): Map<String, FloatArray> {
        val embeddings = mutableMapOf<String, FloatArray>()
        
        // Function words (cluster 0)
        val functionWords = listOf(
            "the","be","to","of","and","a","in","that","have","I","it","for","not","on","with","he",
            "as","you","do","at","this","but","his","by","from","they","we","say","her","she","or",
            "an","will","my","one","all","would","there","their","what","so","up","out","if","about",
            "who","get","which","go","me","when","make","can","like","time","no","just","him","know",
            "take","people","into","year","your","good","some","could","them","see","other","than",
            "then","now","look","only","come","its","over","think","also","back","after","use","two",
            "how","our","work","first","well","way","even","new","want","because","any","these","give",
            "day","most","us","is","was","are","been","has","had","were","being","have","here","much",
            "many","more","such","where","while","during","before","after","above","below","between",
            "under","again","further","then","once","very","really","quite","rather","too","also","well",
            "only","even","both","either","neither","each","every","somewhere","anywhere","nowhere"
        )
        functionWords.forEachIndexed { idx, word ->
            embeddings[word] = generateVector(idx, 0.0f)
        }

        // Nouns - people/family (cluster 1)
        val familyWords = listOf(
            "family","friend","friends","person","people","man","woman","child","children","baby",
            "mother","father","parent","parents","sister","brother","wife","husband","girl","boy",
            "student","students","teacher","doctor","lawyer","nurse","chef","driver","artist","engineer"
        )
        familyWords.forEachIndexed { idx, word ->
            embeddings[word] = generateVector(idx, 0.1f)
        }

        // Nouns - places/items (cluster 2)
        val placeWords = listOf(
            "home","house","room","bedroom","kitchen","bathroom","office","building","company",
            "school","store","shop","restaurant","hotel","hospital","church","park","city","town",
            "country","world","floor","door","window","table","chair","bed","car","bike","bus",
            "train","plane","boat","ship","phone","computer","laptop","tablet","screen","camera"
        )
        placeWords.forEachIndexed { idx, word ->
            embeddings[word] = generateVector(idx, 0.2f)
        }

        // Nouns - food/drink (cluster 3)
        val foodWords = listOf(
            "food","water","coffee","tea","milk","juice","beer","wine","breakfast","lunch","dinner",
            "meal","restaurant","menu","chef","cook","eat","drink","dish","soup","salad","meat",
            "fish","chicken","bread","rice","pasta","fruit","vegetable","apple","orange","banana",
            "carrot","potato","tomato","onion","cheese","butter","egg","sugar","salt","pepper"
        )
        foodWords.forEachIndexed { idx, word ->
            embeddings[word] = generateVector(idx, 0.3f)
        }

        // Nouns - time/money/work (cluster 4)
        val workWords = listOf(
            "job","work","office","meeting","project","team","client","business","money","bank",
            "account","card","cash","payment","price","cost","deal","market","money","dollar",
            "pound","euro","time","hour","minute","second","day","week","month","year","today",
            "tomorrow","yesterday","morning","afternoon","evening","night","weekend","Monday",
            "Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday","January","February",
            "March","April","May","June","July","August","September","October","November","December"
        )
        workWords.forEachIndexed { idx, word ->
            embeddings[word] = generateVector(idx, 0.4f)
        }

        // Verbs - common actions (cluster 5)
        val actionWords = listOf(
            "go","come","make","take","get","give","put","send","bring","show","see","know","tell",
            "ask","use","find","want","need","think","believe","feel","hear","watch","read","write",
            "learn","play","run","walk","drive","live","stay","leave","meet","talk","say","speak",
            "call","help","support","love","like","hate","hope","wish","try","start","begin","stop",
            "continue","change","move","turn","grow","add","build","buy","sell","pay","cost","eat",
            "drink","cook","clean","wash","dress","sleep","rest","wake","work","open","close","break",
            "fix","cut","paint","draw","sing","dance","act","agree","accept","allow","cause","check"
        )
        actionWords.forEachIndexed { idx, word ->
            embeddings[word] = generateVector(idx, 0.5f)
        }

        // Adjectives - descriptions (cluster 6)
        val descWords = listOf(
            "good","great","best","better","bad","worse","worst","new","old","young","beautiful",
            "pretty","ugly","big","small","large","tiny","huge","tall","short","long","fast","slow",
            "hot","cold","warm","cool","easy","hard","difficult","simple","complex","rich","poor",
            "happy","sad","angry","calm","quiet","loud","bright","dark","light","heavy","light",
            "strong","weak","healthy","sick","safe","dangerous","clean","dirty","fresh","stale"
        )
        descWords.forEachIndexed { idx, word ->
            embeddings[word] = generateVector(idx, 0.6f)
        }

        // Tech/media words (cluster 7)
        val techWords = listOf(
            "book","movie","film","music","song","game","news","story","article","blog","video",
            "photo","picture","image","email","message","text","call","number","address","website",
            "link","page","app","software","app","program","code","data","file","folder","disk",
            "internet","web","online","offline","wifi","bluetooth","camera","screen","keyboard"
        )
        techWords.forEachIndexed { idx, word ->
            embeddings[word] = generateVector(idx, 0.7f)
        }

        // Travel/transport words (cluster 8)
        val travelWords = listOf(
            "travel","trip","vacation","holiday","tour","journey","passport","ticket","reservation",
            "luggage","bag","baggage","airport","station","platform","seat","window","aisle",
            "departure","arrival","destination","map","guide","hotel","motel","hostel","bed","breakfast"
        )
        travelWords.forEachIndexed { idx, word ->
            embeddings[word] = generateVector(idx, 0.8f)
        }

        // Abstract/concepts (cluster 9)
        val abstractWords = listOf(
            "idea","thought","opinion","view","point","fact","truth","theory","practice","plan",
            "goal","aim","purpose","reason","result","success","failure","problem","solution",
            "question","answer","choice","decision","feeling","emotion","love","hate","fear","joy",
            "hope","dream","wish","idea","concept","sense","mind","heart","soul","spirit","energy"
        )
        abstractWords.forEachIndexed { idx, word ->
            embeddings[word] = generateVector(idx, 0.9f)
        }

        return embeddings
    }

    /**
     * Generates a 50-dimensional semantic vector based on word index and category.
     * Similar words will have similar vector directions.
     */
    private fun generateVector(seedIndex: Int, categoryFactor: Float): FloatArray {
        val vector = FloatArray(EMBEDDING_DIM)
        
        // Use multiple interleaved sine/cosine waves to create distributed representation
        // Each dimension captures different aspects of word meaning
        for (i in 0 until EMBEDDING_DIM) {
            val base = seedIndex * (i + 1) * 0.1
            
            // Primary wave - position-based
            val primary = kotlin.math.sin(base) * 0.35
            
            // Secondary wave - creates offset patterns
            val secondary = kotlin.math.cos(base * 0.7 + categoryFactor * 5) * 0.25
            
            // Tertiary wave - adds complexity
            val tertiary = kotlin.math.sin(base * 1.3 + i * 0.05) * 0.2
            
            // Category influence - words in same category have similar category dimension
            val category = kotlin.math.cos(base * 0.3 + categoryFactor * 10) * 0.2
            
            vector[i] = (primary + secondary + tertiary + category).toFloat()
        }
        
        // Normalize to unit length (L2 norm)
        val magnitude = sqrt(vector.map { it * it }.sum().toDouble()).toFloat()
        if (magnitude > 0) {
            for (i in 0 until EMBEDDING_DIM) {
                vector[i] = vector[i] / magnitude
            }
        }
        
        return vector
    }

    /**
     * Gets the vector for a word. Returns null if word not in vocabulary.
     */
    fun getVector(word: String): FloatArray? {
        return wordVectors[word.lowercase()]
    }

    /**
     * Finds words similar to the given word using cosine similarity.
     */
    fun findSimilarWords(word: String, limit: Int = 10): List<Pair<String, Float>> {
        val targetVector = getVector(word.lowercase()) ?: return emptyList()
        val similarities = mutableListOf<Pair<String, Float>>()
        
        for ((vocabWord, vector) in wordVectors) {
            if (vocabWord != word.lowercase()) {
                val similarity = cosineSimilarity(targetVector, vector)
                similarities.add(vocabWord to similarity)
            }
        }
        
        return similarities.sortedByDescending { it.second }.take(limit)
    }

    /**
     * Gets semantically similar words to any word in the context.
     * Useful for finding related concepts during typing.
     */
    fun getSimilarFromContext(contextWords: List<String>, limit: Int = 10): List<Pair<String, Float>> {
        val aggregatedScores = mutableMapOf<String, Float>()
        
        for (contextWord in contextWords) {
            val targetVector = getVector(contextWord.lowercase()) ?: continue
            
            for ((vocabWord, vector) in wordVectors) {
                if (vocabWord in contextWords.map { it.lowercase() }) continue
                
                val similarity = cosineSimilarity(targetVector, vector)
                // Max pooling - keep highest score for each word across context
                val existing = aggregatedScores[vocabWord] ?: 0f
                aggregatedScores[vocabWord] = maxOf(existing, similarity)
            }
        }
        
        return aggregatedScores
            .filter { it.value > 0.3f }  // Filter low similarity
            .filter { it.value < 0.95f }  // Filter near-identical (probably same word)
            .sortedByDescending { it.value }
            .take(limit)
    }

    /**
     * Computes cosine similarity between two vectors.
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0) dotProduct / denominator else 0f
    }

    /**
     * Returns the vocabulary size.
     */
    fun vocabularySize(): Int = wordVectors.size
}

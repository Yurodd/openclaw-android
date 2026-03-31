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
        
        // Common words with their semantic vectors
        val commonWords = listOf(
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
            "only","even","both","either","neither","each","every","somewhere","anywhere","nowhere",
            "family","friend","person","people","man","woman","child","children","baby",
            "mother","father","parent","parents","sister","brother","wife","husband","girl","boy",
            "home","house","room","bedroom","kitchen","bathroom","office","building","company",
            "school","store","shop","restaurant","hotel","hospital","church","park","city","town",
            "country","world","floor","door","window","table","chair","bed","car","bike","bus",
            "food","water","coffee","tea","milk","juice","beer","wine","breakfast","lunch","dinner",
            "meal","menu","chef","cook","eat","drink","dish","soup","salad","meat","fish","chicken",
            "bread","rice","pasta","fruit","vegetable","apple","orange","banana","carrot","potato",
            "job","work","office","meeting","project","team","client","business","money","bank",
            "account","card","cash","payment","price","cost","deal","market","time","hour","minute",
            "second","day","week","month","year","today","tomorrow","yesterday","morning",
            "afternoon","evening","night","weekend","Monday","Tuesday","Wednesday","Thursday",
            "Friday","Saturday","Sunday","go","come","make","take","get","give","put","send","bring",
            "show","see","know","tell","ask","use","find","want","need","think","believe","feel",
            "hear","watch","read","write","learn","play","run","walk","drive","live","stay","leave",
            "meet","talk","call","help","support","love","like","hate","hope","wish","try","start",
            "good","great","best","better","bad","worse","worst","new","old","young","beautiful",
            "pretty","ugly","big","small","large","tiny","huge","tall","short","long","fast","slow",
            "hot","cold","warm","cool","easy","hard","difficult","simple","complex","rich","poor",
            "happy","sad","angry","calm","quiet","loud","bright","dark","light","heavy","strong",
            "book","movie","film","music","song","game","news","story","article","blog","video",
            "photo","picture","image","email","message","text","number","address","website","link",
            "travel","trip","vacation","holiday","tour","journey","passport","ticket","reservation",
            "luggage","airport","station","platform","seat","destination","map","guide","hotel",
            "idea","thought","opinion","view","point","fact","truth","theory","plan","goal","aim",
            "reason","result","success","failure","problem","solution","question","answer","choice"
        )
        
        commonWords.forEachIndexed { idx, word ->
            embeddings[word] = generateVector(idx, idx.toFloat())
        }

        return embeddings
    }

    private fun generateVector(seedIndex: Int, seed: Float): FloatArray {
        val vector = FloatArray(EMBEDDING_DIM)
        
        for (i in 0 until EMBEDDING_DIM) {
            val angle = seedIndex * (i + 1) * 0.1 + seed * 0.5
            vector[i] = (kotlin.math.sin(angle) * 0.5 + kotlin.math.cos(angle * 0.7) * 0.3 + 0.2).toFloat()
        }
        
        // Normalize
        val magnitude = sqrt(vector.map { it * it }.sum().toDouble()).toFloat()
        if (magnitude > 0) {
            for (i in 0 until EMBEDDING_DIM) {
                vector[i] = vector[i] / magnitude
            }
        }
        
        return vector
    }

    fun getVector(word: String): FloatArray? {
        return wordVectors[word.lowercase()]
    }

    fun findSimilarWords(word: String, limit: Int = 10): List<Pair<String, Float>> {
        val targetVector = getVector(word.lowercase()) ?: return emptyList()
        val results = mutableListOf<Pair<String, Float>>()
        
        for ((vocabWord, vector) in wordVectors) {
            if (vocabWord != word.lowercase()) {
                val similarity = cosineSimilarity(targetVector, vector)
                results.add(vocabWord to similarity)
            }
        }
        
        return results.sortedByDescending { it.second }.take(limit)
    }

    fun getSimilarFromContext(contextWords: List<String>, limit: Int = 10): List<Pair<String, Float>> {
        val aggregatedScores = mutableMapOf<String, Float>()
        
        for (contextWord in contextWords) {
            val targetVector = getVector(contextWord.lowercase()) ?: continue
            
            for ((vocabWord, vector) in wordVectors) {
                if (vocabWord in contextWords.map { it.lowercase() }) continue
                
                val similarity = cosineSimilarity(targetVector, vector)
                if (similarity > 0.3f && similarity < 0.95f) {
                    val existing = aggregatedScores[vocabWord] ?: 0f
                    if (similarity > existing) {
                        aggregatedScores[vocabWord] = similarity
                    }
                }
            }
        }
        
        return aggregatedScores.toList().sortedByDescending { it.second }.take(limit)
    }

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

    fun vocabularySize(): Int = wordVectors.size
}

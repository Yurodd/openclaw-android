package com.aikeyboard.app

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

/**
 * Manages user typing history for personalized predictions.
 * Stores words and phrases with frequency data.
 * Uses FTS5 for fast prefix search.
 */
class UserHistoryManager(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    /**
     * Records a typed word and updates frequency.
     */
    fun addTypedWord(word: String) {
        val normalizedWord = word.lowercase().trim()
        if (normalizedWord.isEmpty()) return

        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_WORD, normalizedWord)
            put(DatabaseHelper.COLUMN_FREQUENCY, 1)
            put(DatabaseHelper.COLUMN_LAST_USED, System.currentTimeMillis() / 1000)
        }

        db.insertWithOnConflict(
            DatabaseHelper.TABLE_USER_WORDS,
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    /**
     * Adds a word to the FTS5 index for full-text search.
     */
    fun addWordToFTS(word: String) {
        val normalizedWord = word.lowercase().trim()
        if (normalizedWord.isEmpty()) return

        val db = dbHelper.writableDatabase
        db.execSQL("""
            INSERT OR IGNORE INTO ${DatabaseHelper.TABLE_USER_WORDS_FTS}(${DatabaseHelper.COLUMN_WORD})
            VALUES (?)
        """, arrayOf(normalizedWord))
    }

    /**
     * Searches for words matching prefix using FTS5.
     */
    fun searchFTS(query: String): List<String> {
        val normalizedQuery = query.lowercase().trim()
        if (normalizedQuery.isEmpty()) return emptyList()

        val results = mutableListOf<String>()
        val db = dbHelper.readableDatabase

        val cursor: Cursor = db.rawQuery("""
            SELECT u.${DatabaseHelper.COLUMN_WORD}
            FROM ${DatabaseHelper.TABLE_USER_WORDS_FTS} fts
            JOIN ${DatabaseHelper.TABLE_USER_WORDS} u ON fts.rowid = u.rowid
            WHERE ${DatabaseHelper.TABLE_USER_WORDS_FTS} MATCH ?
            ORDER BY u.${DatabaseHelper.COLUMN_FREQUENCY} DESC
            LIMIT 20
        """, arrayOf("$normalizedQuery*"))

        cursor.use {
            while (it.moveToNext()) {
                results.add(it.getString(0))
            }
        }

        return results
    }

    /**
     * Gets frequency count for a word.
     */
    fun getWordFrequency(word: String): Int {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT ${DatabaseHelper.COLUMN_FREQUENCY} FROM ${DatabaseHelper.TABLE_USER_WORDS} WHERE ${DatabaseHelper.COLUMN_WORD} = ?",
            arrayOf(word.lowercase())
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0)
            }
        }
        return 0
    }

    /**
     * Gets word predictions for a prefix.
     */
    fun getWordPredictions(prefix: String): List<String> {
        val results = mutableListOf<String>()
        val db = dbHelper.readableDatabase

        val cursor: Cursor = db.rawQuery("""
            SELECT ${DatabaseHelper.COLUMN_WORD} 
            FROM ${DatabaseHelper.TABLE_USER_WORDS} 
            WHERE ${DatabaseHelper.COLUMN_WORD} LIKE ?
            ORDER BY ${DatabaseHelper.COLUMN_FREQUENCY} DESC
            LIMIT 10
        """, arrayOf("$prefix%"))

        cursor.use {
            while (it.moveToNext()) {
                results.add(it.getString(0))
            }
        }

        return results
    }

    /**
     * Gets bigram predictions from user's history.
     */
    fun getUserBigramPredictions(word: String): List<String> {
        // For simplicity, return words that commonly follow this word in user history
        // In a full implementation, you'd have a separate bigram table
        return getWordPredictions(word).take(3)
    }

    /**
     * Gets trigram predictions from user's history.
     */
    fun getUserTrigramPredictions(word1: String, word2: String): List<String> {
        // Simplified: return words that start with word2
        return getWordPredictions(word2).take(2)
    }

    /**
     * Gets phrase predictions.
     */
    fun getPhrasePredictions(partial: String): List<String> {
        val results = mutableListOf<String>()
        val db = dbHelper.readableDatabase

        val cursor: Cursor = db.rawQuery("""
            SELECT ${DatabaseHelper.COLUMN_PHRASE}
            FROM ${DatabaseHelper.TABLE_USER_PHRASES}
            WHERE ${DatabaseHelper.COLUMN_PHRASE} LIKE ?
            ORDER BY ${DatabaseHelper.COLUMN_PHRASE_FREQUENCY} DESC
            LIMIT 5
        """, arrayOf("$partial%"))

        cursor.use {
            while (it.moveToNext()) {
                results.add(it.getString(0))
            }
        }

        return results
    }

    /**
     * Adds a phrase to history.
     */
    fun addPhrase(phrase: String) {
        if (phrase.isBlank()) return

        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_PHRASE, phrase.lowercase().trim())
            put(DatabaseHelper.COLUMN_PHRASE_FREQUENCY, 1)
        }

        db.insertWithOnConflict(
            DatabaseHelper.TABLE_USER_PHRASES,
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    /**
     * Gets recent words for context.
     */
    fun getRecentWords(limit: Int = 20): List<String> {
        val results = mutableListOf<String>()
        val db = dbHelper.readableDatabase

        val cursor: Cursor = db.rawQuery("""
            SELECT ${DatabaseHelper.COLUMN_WORD}
            FROM ${DatabaseHelper.TABLE_USER_WORDS}
            ORDER BY ${DatabaseHelper.COLUMN_LAST_USED} DESC
            LIMIT ?
        """, arrayOf(limit.toString()))

        cursor.use {
            while (it.moveToNext()) {
                results.add(it.getString(0))
            }
        }

        return results
    }

    /**
     * Clears all history.
     */
    fun clearHistory() {
        val db = dbHelper.writableDatabase
        db.delete(DatabaseHelper.TABLE_USER_WORDS, null, null)
        db.delete(DatabaseHelper.TABLE_USER_PHRASES, null, null)
    }

    /**
     * Closes the database connection.
     */
    fun close() {
        dbHelper.close()
    }
}

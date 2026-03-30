package com.aikeyboard.app

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

class UserHistoryManager(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    fun addTypedWord(word: String) {
        val normalized = word.lowercase().trim()
        if (normalized.isEmpty()) return

        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_WORD, normalized)
            put(DatabaseHelper.COLUMN_FREQ, 1)
        }
        db.insertWithOnConflict(DatabaseHelper.TABLE_WORDS, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getWordPredictions(prefix: String): List<String> {
        val results = mutableListOf<String>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT ${DatabaseHelper.COLUMN_WORD} FROM ${DatabaseHelper.TABLE_WORDS} WHERE ${DatabaseHelper.COLUMN_WORD} LIKE ? ORDER BY ${DatabaseHelper.COLUMN_FREQ} DESC LIMIT 10",
            arrayOf("$prefix%")
        )
        cursor.use {
            while (it.moveToNext()) {
                results.add(it.getString(0))
            }
        }
        return results
    }

    fun getPhrasePredictions(partial: String): List<String> {
        return emptyList()
    }

    fun clearHistory() {
        dbHelper.writableDatabase.delete(DatabaseHelper.TABLE_WORDS, null, null)
    }

    fun close() {
        dbHelper.close()
    }
}

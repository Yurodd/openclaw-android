package com.aikeyboard.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "ai_keyboard.db"
        const val DATABASE_VERSION = 1

        const val TABLE_WORDS = "words"
        const val COLUMN_WORD = "word"
        const val COLUMN_FREQ = "frequency"

        private const val CREATE_TABLE = """
            CREATE TABLE $TABLE_WORDS (
                $COLUMN_WORD TEXT PRIMARY KEY,
                $COLUMN_FREQ INTEGER DEFAULT 1
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        // No-op for now
    }
}

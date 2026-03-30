package com.aikeyboard.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLite database helper for AI Keyboard predictions.
 * Manages user_words, user_phrases, and FTS5 virtual tables.
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        const val DATABASE_NAME = "ai_keyboard_predictions.db"
        const val DATABASE_VERSION = 2

        // User words table
        const val TABLE_USER_WORDS = "user_words"
        const val COLUMN_WORD = "word"
        const val COLUMN_FREQUENCY = "frequency"
        const val COLUMN_LAST_USED = "last_used"

        // User phrases table
        const val TABLE_USER_PHRASES = "user_phrases"
        const val COLUMN_PHRASE = "phrase"
        const val COLUMN_PHRASE_FREQUENCY = "frequency"

        // FTS5 virtual table for full-text search
        const val TABLE_USER_WORDS_FTS = "user_words_fts"

        private const val CREATE_USER_WORDS_TABLE = """
            CREATE TABLE IF NOT EXISTS $TABLE_USER_WORDS (
                $COLUMN_WORD TEXT PRIMARY KEY,
                $COLUMN_FREQUENCY INTEGER DEFAULT 1,
                $COLUMN_LAST_USED INTEGER DEFAULT 0
            )
        """

        private const val CREATE_USER_PHRASES_TABLE = """
            CREATE TABLE IF NOT EXISTS $TABLE_USER_PHRASES (
                $COLUMN_PHRASE TEXT PRIMARY KEY,
                $COLUMN_PHRASE_FREQUENCY INTEGER DEFAULT 1
            )
        """

        private const val CREATE_FTS_TABLE = """
            CREATE VIRTUAL TABLE IF NOT EXISTS $TABLE_USER_WORDS_FTS USING fts5(
                $COLUMN_WORD,
                content='$TABLE_USER_WORDS',
                content_rowid='rowid'
            )
        """

        private const val CREATE_INDEX_LAST_USED = """
            CREATE INDEX IF NOT EXISTS idx_last_used ON $TABLE_USER_WORDS($COLUMN_LAST_USED DESC)
        """

        private const val CREATE_INDEX_FREQUENCY = """
            CREATE INDEX IF NOT EXISTS idx_frequency ON $TABLE_USER_WORDS($COLUMN_FREQUENCY DESC)
        """

        // Triggers to keep FTS5 in sync
        private const val TRIGGER_INSERT = """
            CREATE TRIGGER IF NOT EXISTS ${TABLE_USER_WORDS}_ai AFTER INSERT ON $TABLE_USER_WORDS BEGIN
                INSERT INTO $TABLE_USER_WORDS_FTS(rowid, $COLUMN_WORD) VALUES (NEW.rowid, NEW.$COLUMN_WORD);
            END
        """

        private const val TRIGGER_DELETE = """
            CREATE TRIGGER IF NOT EXISTS ${TABLE_USER_WORDS}_ad AFTER DELETE ON $TABLE_USER_WORDS BEGIN
                INSERT INTO $TABLE_USER_WORDS_FTS($TABLE_USER_WORDS_FTS, rowid, $COLUMN_WORD) 
                VALUES('delete', OLD.rowid, OLD.$COLUMN_WORD);
            END
        """

        private const val TRIGGER_UPDATE = """
            CREATE TRIGGER IF NOT EXISTS ${TABLE_USER_WORDS}_au AFTER UPDATE ON $TABLE_USER_WORDS BEGIN
                INSERT INTO $TABLE_USER_WORDS_FTS($TABLE_USER_WORDS_FTS, rowid, $COLUMN_WORD) 
                VALUES('delete', OLD.rowid, OLD.$COLUMN_WORD);
                INSERT INTO $TABLE_USER_WORDS_FTS(rowid, $COLUMN_WORD) VALUES (NEW.rowid, NEW.$COLUMN_WORD);
            END
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_USER_WORDS_TABLE)
        db.execSQL(CREATE_USER_PHRASES_TABLE)
        db.execSQL(CREATE_FTS_TABLE)
        db.execSQL(CREATE_INDEX_LAST_USED)
        db.execSQL(CREATE_INDEX_FREQUENCY)
        db.execSQL(TRIGGER_INSERT)
        db.execSQL(TRIGGER_DELETE)
        db.execSQL(TRIGGER_UPDATE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(CREATE_FTS_TABLE)
            db.execSQL(TRIGGER_INSERT)
            db.execSQL(TRIGGER_DELETE)
            db.execSQL(TRIGGER_UPDATE)
            db.execSQL("""
                INSERT INTO $TABLE_USER_WORDS_FTS(rowid, $COLUMN_WORD)
                SELECT rowid, $COLUMN_WORD FROM $TABLE_USER_WORDS
            """)
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }
}

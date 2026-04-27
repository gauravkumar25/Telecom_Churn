package com.studybuddy.app.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds multi-year retention metadata columns to knowledge_chunks.
 *
 * grade        — student's class at ingestion time (e.g. "Class 7")
 * academic_year — school year at ingestion time (e.g. "2025-26")
 *
 * Both default to '' so existing rows are valid without re-scanning.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE knowledge_chunks ADD COLUMN grade TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE knowledge_chunks ADD COLUMN academicYear TEXT NOT NULL DEFAULT ''")
    }
}

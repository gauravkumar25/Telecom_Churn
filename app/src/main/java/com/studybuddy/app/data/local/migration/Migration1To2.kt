package com.studybuddy.app.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `knowledge_chunks` (
                `id`             INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `sourceImageUri` TEXT    NOT NULL,
                `subjectId`      INTEGER,
                `chapterId`      INTEGER,
                `subjectName`    TEXT    NOT NULL DEFAULT '',
                `chapterName`    TEXT    NOT NULL DEFAULT '',
                `contentType`    TEXT    NOT NULL DEFAULT 'TEXT',
                `exerciseId`     TEXT,
                `itemNumber`     INTEGER,
                `chapterRef`     TEXT,
                `chunkText`      TEXT    NOT NULL,
                `chunkIndex`     INTEGER NOT NULL,
                `embedding`      BLOB    NOT NULL,
                `createdAt`      INTEGER NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_chunks_subjectId`      ON `knowledge_chunks` (`subjectId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_chunks_chapterId`      ON `knowledge_chunks` (`chapterId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_chunks_sourceImageUri` ON `knowledge_chunks` (`sourceImageUri`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_chunks_exerciseId`     ON `knowledge_chunks` (`exerciseId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_chunks_contentType`    ON `knowledge_chunks` (`contentType`)")
    }
}

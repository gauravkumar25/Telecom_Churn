package com.studybuddy.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One semantic chunk of text extracted from a chapter screenshot.
 * A single uploaded image produces N chunks, all sharing the same sourceImageUri.
 *
 * NOTE: FloatArray breaks data class structural equality — never compare
 * KnowledgeChunk instances with == for embedding values; use id instead.
 */
@Entity(
    tableName = "knowledge_chunks",
    indices = [
        Index("subjectId"),
        Index("chapterId"),
        Index("sourceImageUri")
    ]
)
data class KnowledgeChunk(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** file:// URI of the source image — acts as the document identifier. */
    val sourceImageUri: String,

    val subjectId: Long? = null,
    val chapterId: Long? = null,

    /** Denormalized names so the Knowledge screen needs no JOINs. */
    val subjectName: String = "",
    val chapterName: String = "",

    val chunkText: String,

    /** Position of this chunk within the document (0-based). */
    val chunkIndex: Int,

    /**
     * 256-dim L2-normalised hash embedding stored as ByteArray (256 * 4 = 1 KB).
     * Converted by Converters.floatArrayToBytes / bytesToFloatArray.
     */
    val embedding: FloatArray,

    val createdAt: Long = System.currentTimeMillis()
)

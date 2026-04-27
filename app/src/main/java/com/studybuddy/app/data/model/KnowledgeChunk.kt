package com.studybuddy.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One semantic unit extracted from a chapter screenshot.
 * A single image produces N chunks, all sharing the same sourceImageUri (document ID).
 *
 * Structural fields (exerciseId, itemNumber, chapterRef, contentType) enable
 * direct SQL lookup for queries like "exercise 3.4 question 4".
 * The embedding enables cosine-similarity search for open-ended semantic queries.
 *
 * NOTE: FloatArray breaks data class structural equality — never compare instances
 * with == for embedding values; use id instead.
 */
@Entity(
    tableName = "knowledge_chunks",
    indices = [
        Index("subjectId"),
        Index("chapterId"),
        Index("sourceImageUri"),
        Index("exerciseId"),
        Index("contentType")
    ]
)
data class KnowledgeChunk(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** file:// URI of the source image — acts as document identifier. */
    val sourceImageUri: String,

    /** Optional FK to Subject / Chapter rows (nullable — user may not tag). */
    val subjectId: Long? = null,
    val chapterId: Long? = null,

    /** Denormalised for display without JOINs. */
    val subjectName: String = "",
    val chapterName: String = "",

    // ── Structural metadata extracted by Claude Vision ───────────────────────
    val contentType: ContentType = ContentType.TEXT,

    /** Exercise identifier as printed in the book, e.g. "3.4", "2B". */
    val exerciseId: String? = null,

    /** Item number within an exercise or list, e.g. 4 for "Question 4". */
    val itemNumber: Int? = null,

    /** Chapter / section reference as printed, e.g. "Chapter 3", "Section 3.4". */
    val chapterRef: String? = null,

    // ── Content ──────────────────────────────────────────────────────────────
    val chunkText: String,

    /** 0-based position within the document (for ordering within a source). */
    val chunkIndex: Int,

    /**
     * 512-dim L2-normalised embedding stored as ByteArray (512 × 4 = 2 KB/chunk).
     * Converted by Converters.floatArrayToBytes / bytesToFloatArray.
     */
    val embedding: FloatArray,

    // ── Multi-year retention metadata ────────────────────────────────────────
    /**
     * Student's grade/class at the time this chunk was ingested, e.g. "Class 7".
     * Copied from the student profile so data remains attributable across years.
     * The AI uses grade for explanation depth (not as a DB search filter).
     */
    val grade: String = "",

    /**
     * Academic year when ingested, e.g. "2025-26".
     * Derived automatically from the system clock at ingestion time.
     */
    val academicYear: String = "",

    val createdAt: Long = System.currentTimeMillis()
)

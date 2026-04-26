package com.studybuddy.app.data.local.dao

import androidx.room.*
import com.studybuddy.app.data.model.ContentType
import com.studybuddy.app.data.model.DocumentSummaryTuple
import com.studybuddy.app.data.model.KnowledgeChunk
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chunks: List<KnowledgeChunk>)

    // ── Semantic search (loads all embeddings for brute-force cosine scoring) ─

    @Query("SELECT * FROM knowledge_chunks")
    suspend fun getAllChunksSync(): List<KnowledgeChunk>

    @Query("SELECT COUNT(*) FROM knowledge_chunks")
    fun getChunkCount(): Flow<Int>

    // ── Structured lookups (SQL — fast, exact) ────────────────────────────────

    /** Exact match: specific question in a specific exercise. */
    @Query("""
        SELECT * FROM knowledge_chunks
        WHERE exerciseId = :exerciseId
          AND itemNumber = :itemNumber
          AND (:contentType IS NULL OR contentType = :contentType)
        ORDER BY chunkIndex ASC
    """)
    suspend fun findByExerciseAndItem(
        exerciseId: String,
        itemNumber: Int,
        contentType: String? = ContentType.QUESTION.name
    ): List<KnowledgeChunk>

    /** All chunks from one exercise (e.g. quiz mode: "quiz me on exercise 3.4"). */
    @Query("""
        SELECT * FROM knowledge_chunks
        WHERE exerciseId = :exerciseId
        ORDER BY itemNumber ASC, chunkIndex ASC
    """)
    suspend fun findByExercise(exerciseId: String): List<KnowledgeChunk>

    /** Filter by content type across the whole KB. */
    @Query("""
        SELECT * FROM knowledge_chunks
        WHERE contentType = :contentType
          AND (:subjectId IS NULL OR subjectId = :subjectId)
        ORDER BY createdAt DESC
        LIMIT :limit
    """)
    suspend fun findByContentType(
        contentType: String,
        subjectId: Long? = null,
        limit: Int = 20
    ): List<KnowledgeChunk>

    /** Chapter-level filter. */
    @Query("""
        SELECT * FROM knowledge_chunks
        WHERE chapterRef LIKE '%' || :chapterRef || '%'
          AND (:contentType IS NULL OR contentType = :contentType)
        ORDER BY chunkIndex ASC
        LIMIT :limit
    """)
    suspend fun findByChapter(
        chapterRef: String,
        contentType: String? = null,
        limit: Int = 20
    ): List<KnowledgeChunk>

    // ── Document management ───────────────────────────────────────────────────

    @Query("""
        SELECT sourceImageUri,
               subjectId,
               subjectName,
               chapterId,
               chapterName,
               MIN(chunkText) AS textPreview,
               COUNT(*)       AS chunkCount,
               MIN(createdAt) AS createdAt
        FROM knowledge_chunks
        GROUP BY sourceImageUri
        ORDER BY createdAt DESC
    """)
    fun getDocumentSummaries(): Flow<List<DocumentSummaryTuple>>

    @Query("DELETE FROM knowledge_chunks WHERE sourceImageUri = :uri")
    suspend fun deleteByDocument(uri: String)

    @Query("DELETE FROM knowledge_chunks WHERE subjectId = :subjectId")
    suspend fun deleteBySubject(subjectId: Long)
}

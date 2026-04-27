package com.studybuddy.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.studybuddy.app.data.embedding.QueryRouter
import com.studybuddy.app.data.embedding.TextChunker
import com.studybuddy.app.data.embedding.TextEmbedder
import com.studybuddy.app.data.local.dao.KnowledgeChunkDao
import com.studybuddy.app.data.model.ContentType
import com.studybuddy.app.data.model.DocumentSummaryTuple
import com.studybuddy.app.data.model.KnowledgeChunk
import com.studybuddy.app.network.GeminiApiService
import com.studybuddy.app.network.models.GeminiContent
import com.studybuddy.app.network.models.GeminiGenerationConfig
import com.studybuddy.app.network.models.GeminiPart
import com.studybuddy.app.network.models.GeminiRequest
import com.studybuddy.app.network.models.GeminiSystemInstruction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

// ── Gemini Vision structured response models ──────────────────────────────────

private data class ExtractedDocument(
    @SerializedName("chapterRef")  val chapterRef:  String?            = null,
    @SerializedName("sectionRef")  val sectionRef:  String?            = null,
    @SerializedName("title")       val title:       String?            = null,
    @SerializedName("items")       val items:       List<ExtractedItem> = emptyList()
)

private data class ExtractedItem(
    @SerializedName("type")        val type:       String  = "TEXT",
    @SerializedName("exerciseId")  val exerciseId: String? = null,
    @SerializedName("number")      val number:     Int?    = null,
    @SerializedName("text")        val text:       String  = ""
)

// ─────────────────────────────────────────────────────────────────────────────

/** Model for fast OCR/extraction — Gemini Flash is free-tier eligible. */
private const val EXTRACTION_MODEL = "gemini-2.0-flash"

@Singleton
class KnowledgeRepository @Inject constructor(
    private val dao: KnowledgeChunkDao,
    private val geminiApi: GeminiApiService,
    private val prefsRepo: PreferencesRepository,
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    val documentSummaries: Flow<List<DocumentSummaryTuple>> = dao.getDocumentSummaries()

    /** Emits true once at least one chunk exists — used to skip search on empty KB. */
    val hasChunks: Flow<Boolean> = dao.getChunkCount().map { it > 0 }

    // ── Ingestion ─────────────────────────────────────────────────────────────

    /**
     * Full pipeline: image → Gemini Vision (structured JSON) → chunks → embed → Room.
     * Returns the number of chunks stored on success.
     */
    suspend fun processImage(
        imageUri:    Uri,
        subjectId:   Long?   = null,
        chapterId:   Long?   = null,
        subjectName: String  = "",
        chapterName: String  = "",
        onProgress:  (String) -> Unit = {}
    ): Result<Int> {
        val apiKey = prefsRepo.geminiApiKey.first()
        if (apiKey.isBlank()) return Result.failure(Exception("Gemini API key not set — go to Settings."))

        // Snapshot the student's grade and current academic year for multi-year tagging
        val grade        = prefsRepo.studentClass.first()
        val academicYear = deriveAcademicYear()

        return try {
            onProgress("Reading image…")
            val imageBase64 = withContext(Dispatchers.IO) { encodeImage(imageUri) }
                ?: return Result.failure(Exception("Could not read image file."))

            onProgress("Extracting content with AI…")
            val response = geminiApi.generateContent(
                model   = EXTRACTION_MODEL,
                apiKey  = apiKey,
                request = GeminiRequest(
                    systemInstruction = GeminiSystemInstruction(
                        parts = listOf(GeminiPart.text(STRUCTURED_EXTRACTION_PROMPT))
                    ),
                    contents = listOf(
                        GeminiContent(
                            role  = "user",
                            parts = listOf(
                                GeminiPart.image("image/jpeg", imageBase64),
                                GeminiPart.text("Extract all content from this image as structured JSON.")
                            )
                        )
                    ),
                    generationConfig = GeminiGenerationConfig(
                        maxOutputTokens = 4096,
                        temperature     = 0.1f  // low temperature for deterministic extraction
                    )
                )
            )

            if (!response.isSuccessful || response.body() == null)
                return Result.failure(Exception("Extraction failed (HTTP ${response.code()})."))

            val body = response.body()!!
            if (body.error != null)
                return Result.failure(Exception("AI error: ${body.error.message}"))

            val rawJson = body.text.trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

            val doc = runCatching { gson.fromJson(rawJson, ExtractedDocument::class.java) }
                .getOrNull()
                ?: return Result.failure(Exception("Could not parse AI response. Try again."))

            if (doc.items.isEmpty())
                return Result.failure(Exception("No content found in image."))

            onProgress("Generating embeddings…")
            val chunks = withContext(Dispatchers.Default) {
                buildChunks(doc, imageUri, subjectId, chapterId, subjectName, chapterName, grade, academicYear)
            }

            onProgress("Saving to Knowledge Base…")
            withContext(Dispatchers.IO) { dao.insertAll(chunks) }

            Result.success(chunks.size)
        } catch (e: Exception) {
            Result.failure(Exception("Processing failed: ${e.message}"))
        }
    }

    private fun buildChunks(
        doc:          ExtractedDocument,
        imageUri:     Uri,
        subjectId:    Long?,
        chapterId:    Long?,
        subjectName:  String,
        chapterName:  String,
        grade:        String = "",
        academicYear: String = ""
    ): List<KnowledgeChunk> {
        val chunks = mutableListOf<KnowledgeChunk>()
        var index = 0

        for (item in doc.items) {
            if (item.text.isBlank()) continue

            val contentType = runCatching { ContentType.valueOf(item.type) }
                .getOrDefault(ContentType.TEXT)

            val subChunks = if (item.text.length > 400)
                TextChunker.chunk(item.text)
            else
                listOf(item.text)

            for (sub in subChunks) {
                chunks.add(
                    KnowledgeChunk(
                        sourceImageUri = imageUri.toString(),
                        subjectId      = subjectId,
                        chapterId      = chapterId,
                        subjectName    = subjectName,
                        chapterName    = chapterName,
                        contentType    = contentType,
                        exerciseId     = item.exerciseId ?: doc.sectionRef,
                        itemNumber     = item.number,
                        chapterRef     = doc.chapterRef,
                        chunkText      = sub,
                        chunkIndex     = index++,
                        embedding      = TextEmbedder.embed(sub),
                        grade          = grade,
                        academicYear   = academicYear
                    )
                )
            }
        }
        return chunks
    }

    // ── Retrieval — hybrid search ─────────────────────────────────────────────

    suspend fun search(query: String, topK: Int = 6): List<KnowledgeChunk> {
        if (!hasChunks.first()) return emptyList()

        val parsed  = QueryRouter.parse(query)
        val results = mutableMapOf<Long, KnowledgeChunk>()

        // Structural path
        if (parsed.hasStructuralHint) {
            val structural = withContext(Dispatchers.IO) {
                when {
                    parsed.exerciseId != null && parsed.itemNumber != null ->
                        dao.findByExerciseAndItem(parsed.exerciseId, parsed.itemNumber, parsed.contentType?.name)

                    parsed.fetchAllInExercise && parsed.exerciseId != null ->
                        dao.findByExercise(parsed.exerciseId)

                    parsed.exerciseId != null ->
                        dao.findByExercise(parsed.exerciseId)

                    parsed.chapterRef != null ->
                        dao.findByChapter(parsed.chapterRef, parsed.contentType?.name)

                    parsed.contentType != null ->
                        dao.findByContentType(parsed.contentType.name)

                    else -> emptyList()
                }
            }
            structural.forEach { results[it.id] = it }
        }

        // Semantic path (always runs)
        val semantic = withContext(Dispatchers.Default) {
            val queryVec  = TextEmbedder.embed(parsed.semanticQuery)
            val allChunks = withContext(Dispatchers.IO) { dao.getAllChunksSync() }
            allChunks
                .map { it to TextEmbedder.cosineSimilarity(queryVec, it.embedding) }
                .filter  { (_, score) -> score > 0.15f }
                .sortedByDescending { (_, score) -> score }
                .take(topK)
                .map { (chunk, _) -> chunk }
        }
        semantic.forEach { if (it.id !in results) results[it.id] = it }

        return results.values.take(topK)
    }

    suspend fun deleteDocument(sourceImageUri: String, deleteImageFile: Boolean = false) {
        dao.deleteByDocument(sourceImageUri)
        if (deleteImageFile) {
            val uri = Uri.parse(sourceImageUri)
            if (uri.scheme == "file") {
                val file = java.io.File(uri.path ?: return)
                if (file.exists()) file.delete()
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the Indian academic year string for the current date, e.g. "2025-26".
     * Academic year starts in April (month index 3); before April the year wraps back.
     */
    private fun deriveAcademicYear(): String {
        val cal   = java.util.Calendar.getInstance()
        val year  = cal.get(java.util.Calendar.YEAR)
        val month = cal.get(java.util.Calendar.MONTH) // 0-based
        val startYear = if (month >= 3) year else year - 1
        return "$startYear-${(startYear + 1).toString().takeLast(2)}"
    }

    private fun encodeImage(uri: Uri): String? = try {
        val stream = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(stream)
        stream.close()
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) { null }
}

// ── System prompt for structured extraction ───────────────────────────────────

private const val STRUCTURED_EXTRACTION_PROMPT = """
You are an educational content extractor specialised in textbooks and worksheets.

Analyse the image and return ONLY valid JSON — no explanation, no markdown fences:

{
  "chapterRef": "Chapter 3" or null,
  "sectionRef": "3.4" or null,
  "title": "section title" or null,
  "items": [
    {
      "type": "QUESTION|DEFINITION|FORMULA|EXAMPLE|THEOREM|DIAGRAM_DESC|TEXT",
      "exerciseId": "3.4" or null,
      "number": 4 or null,
      "text": "full verbatim text"
    }
  ]
}

Rules:
- Each distinct question, definition, formula, or example → separate item.
- exerciseId: the exercise label exactly as printed (e.g. "3.4", "2B"). null if none.
- number: the item's own number within its exercise/list. null if unnumbered.
- Preserve mathematical notation, symbols and equations exactly as written.
- Regular paragraph text → type TEXT.
- If you cannot read part of the image, include what is visible and note [unclear].
"""

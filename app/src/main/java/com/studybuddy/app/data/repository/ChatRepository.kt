package com.studybuddy.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.studybuddy.app.data.local.dao.MessageDao
import com.studybuddy.app.data.local.dao.StudySessionDao
import com.studybuddy.app.data.model.Exam
import com.studybuddy.app.data.model.Message
import com.studybuddy.app.data.model.MessageRole
import com.studybuddy.app.data.model.MessageType
import com.studybuddy.app.data.model.StudySession
import com.studybuddy.app.data.model.Subject
import com.studybuddy.app.network.GeminiApiService
import com.studybuddy.app.network.models.GeminiContent
import com.studybuddy.app.network.models.GeminiPart
import com.studybuddy.app.network.models.GeminiRequest
import com.studybuddy.app.network.models.GeminiSystemInstruction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val CHAT_MODEL = "gemini-2.0-flash"

@Singleton
class ChatRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val studySessionDao: StudySessionDao,
    private val geminiApi: GeminiApiService,
    private val prefsRepo: PreferencesRepository,
    private val knowledgeRepository: KnowledgeRepository,
    @ApplicationContext private val context: Context
) {
    // Permanent image storage: data/data/<package>/files/images/
    // This directory survives cache clears and app restarts.
    private val imagesDir: File = File(context.filesDir, "images").also { it.mkdirs() }

    val allSessions: Flow<List<StudySession>> = studySessionDao.getAllSessions()

    /**
     * Copies an image (camera capture or gallery pick) into permanent internal storage.
     * Returns a file:// Uri pointing to the copy, or null if the copy fails.
     */
    suspend fun copyToInternalStorage(source: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            val ext = when (context.contentResolver.getType(source)) {
                "image/png"  -> "png"
                "image/webp" -> "webp"
                else         -> "jpg"
            }
            val dest = File(
                imagesDir,
                "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$ext"
            )
            context.contentResolver.openInputStream(source)?.use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            Uri.fromFile(dest)
        } catch (e: Exception) {
            null
        }
    }

    fun getMessagesForSession(sessionId: String): Flow<List<Message>> =
        messageDao.getMessagesForSession(sessionId)

    suspend fun createSession(title: String): StudySession {
        val session = StudySession(id = UUID.randomUUID().toString(), title = title)
        studySessionDao.insert(session)
        return session
    }

    suspend fun deleteSession(sessionId: String) {
        messageDao.deleteSession(sessionId)
        studySessionDao.delete(sessionId)
    }

    suspend fun sendMessage(
        sessionId: String,
        userText: String,
        imageUri: Uri? = null,
        subjects: List<Subject> = emptyList(),
        upcomingExams: List<Exam> = emptyList()
    ): Result<Message> {
        val apiKey = prefsRepo.geminiApiKey.first()
        if (apiKey.isBlank()) return Result.failure(Exception("Please set your Gemini API key in Settings."))

        val studentName  = prefsRepo.studentName.first().ifBlank { "there" }
        val studentClass = prefsRepo.studentClass.first()
        val buddyName    = prefsRepo.buddyName.first().ifBlank { "Buddy" }
        val extraContext = prefsRepo.extraContext.first()
        val model        = prefsRepo.chatModel.first()

        val userMessage = Message(
            sessionId = sessionId,
            role      = MessageRole.USER,
            content   = userText,
            imageUri  = imageUri?.toString(),
            type      = if (imageUri != null) MessageType.IMAGE else MessageType.TEXT
        )
        messageDao.insert(userMessage)
        updateSessionLastMessage(sessionId, userText)

        val recentMessages = messageDao.getRecentMessages(sessionId)

        // Hybrid search: structured SQL + cosine similarity over extracted chapter knowledge
        val ragContext   = buildRagContext(knowledgeRepository.search(userText))
        val systemPrompt = buildSystemPrompt(
            studentName, studentClass, buddyName, subjects, upcomingExams, extraContext, ragContext
        )

        val geminiContents = buildGeminiContents(recentMessages, imageUri)

        return try {
            val response = geminiApi.generateContent(
                model   = model,
                apiKey  = apiKey,
                request = GeminiRequest(
                    systemInstruction = GeminiSystemInstruction(
                        parts = listOf(GeminiPart.text(systemPrompt))
                    ),
                    contents = geminiContents
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.error != null) {
                    return Result.failure(Exception("Gemini error: ${body.error.message}"))
                }
                val replyText = body.text.ifBlank { "Hmm, I couldn't generate a response. Please try again!" }
                val assistantMessage = Message(
                    sessionId = sessionId,
                    role      = MessageRole.ASSISTANT,
                    content   = replyText
                )
                messageDao.insert(assistantMessage)
                updateSessionLastMessage(sessionId, replyText)
                Result.success(assistantMessage)
            } else {
                val code = response.code()
                val hint = when (code) {
                    400 -> "Bad request — check your API key format."
                    403 -> "API key invalid or quota exceeded."
                    429 -> "Rate limit hit — wait a moment and try again."
                    else -> "HTTP $code"
                }
                Result.failure(Exception("Oops! Something went wrong ($hint). Check Settings."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Couldn't reach StudyBuddy. Check your internet connection!"))
        }
    }

    private suspend fun updateSessionLastMessage(sessionId: String, lastMsg: String) {
        val sessions = allSessions.first()
        val session  = sessions.find { it.id == sessionId } ?: return
        studySessionDao.update(
            session.copy(
                lastMessage  = lastMsg.take(80),
                messageCount = session.messageCount + 1,
                updatedAt    = System.currentTimeMillis()
            )
        )
    }

    private fun buildRagContext(chunks: List<com.studybuddy.app.data.model.KnowledgeChunk>): String {
        if (chunks.isEmpty()) return ""
        return chunks.mapIndexed { i, c ->
            val label = buildString {
                if (c.subjectName.isNotBlank()) append(c.subjectName)
                if (c.exerciseId != null)       append(" / Exercise ${c.exerciseId}")
                if (c.itemNumber != null)       append(" Q${c.itemNumber}")
                if (c.chapterRef != null)       append(" (${c.chapterRef})")
            }.trim().let { if (it.isNotBlank()) "[$it] " else "" }
            "[${i + 1}] $label${c.chunkText}"
        }.joinToString("\n\n")
    }

    private fun buildSystemPrompt(
        studentName:  String,
        studentClass: String,
        buddyName:    String,
        subjects:     List<Subject>,
        upcomingExams: List<Exam>,
        extraContext: String,
        ragContext:   String = ""
    ): String {
        val subjectsInfo = if (subjects.isNotEmpty()) {
            "Subjects & Syllabus:\n" + subjects.joinToString("\n") { s ->
                "- ${s.name}: ${s.syllabus.take(300).ifBlank { "No syllabus added yet" }}"
            }
        } else "No subjects added yet."

        val examsInfo = if (upcomingExams.isNotEmpty()) {
            "Upcoming Exams:\n" + upcomingExams.joinToString("\n") { e ->
                "- ${e.subjectName} in ${e.daysUntilExam} days (${e.startTime})"
            }
        } else "No upcoming exams."

        return """
You are $buddyName, the coolest and most helpful AI study buddy for $studentName${if (studentClass.isNotEmpty()) " who is in $studentClass" else ""}! 🌟

Your personality:
- Warm, encouraging, and friendly — like their best friend who's great at studying
- Make learning FUN and exciting, never boring
- Celebrate every achievement, no matter how small
- Be patient and find creative ways to explain concepts
- Use emojis occasionally to keep things lively 😊
- Ask engaging questions to test understanding
- Create mnemonics, stories, or tricks to remember tough topics
- Always be age-appropriate and positive

Your capabilities:
1. CHAT & EXPLAIN: Answer questions about any subject in simple, fun ways
2. QUIZ MODE: When asked, quiz the student on topics — give hints if they're stuck
3. STUDY SCHEDULE: Create personalised study plans based on exams and topics
4. REVISION: Help revise chapters systematically, covering all key points
5. IMAGE ANALYSIS: When a photo of a chapter/textbook is shared, read and explain it
6. MOTIVATION: Keep spirits high when studying feels hard

Student's current context:
$subjectsInfo

$examsInfo

${if (extraContext.isNotEmpty()) "Additional notes from parent:\n$extraContext" else ""}

${if (ragContext.isNotEmpty()) """
--- KNOWLEDGE BASE (extracted from student's own textbook photos) ---
Prioritise this content when answering. It is the student's actual syllabus material.

$ragContext

--- END KNOWLEDGE BASE ---
""" else ""}

Rules:
- Never discourage or make the student feel bad
- If they're stressed about exams, first acknowledge their feelings, then help
- Keep responses focused and not too long (unless explaining complex topics)
- When creating study schedules, be realistic and include breaks
- If asked something unrelated to studies, gently guide back to studying
        """.trimIndent()
    }

    private fun buildGeminiContents(messages: List<Message>, newImageUri: Uri?): List<GeminiContent> =
        messages.map { msg ->
            val parts = mutableListOf<GeminiPart>()

            if (msg.imageUri != null && msg.role == MessageRole.USER) {
                val base64 = encodeImageToBase64(Uri.parse(msg.imageUri))
                if (base64 != null) parts.add(GeminiPart.image("image/jpeg", base64))
            }
            parts.add(GeminiPart.text(msg.content))

            GeminiContent(
                role  = if (msg.role == MessageRole.USER) "user" else "model",
                parts = parts
            )
        }

    private fun encodeImageToBase64(uri: Uri): String? = try {
        val stream = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(stream)
        stream.close()
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) { null }
}

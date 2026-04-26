package com.studybuddy.app.data.embedding

import com.studybuddy.app.data.model.ContentType

/**
 * Classifies a student's natural-language query into a structured retrieval intent.
 *
 * Two retrieval modes:
 *   STRUCTURED — explicit reference to a numbered exercise / question / example.
 *                Resolved via SQL metadata lookup (exact match on exerciseId + itemNumber).
 *   SEMANTIC   — open-ended question. Resolved via cosine similarity search.
 *
 * Most queries that contain a reference also carry semantic content, so both
 * modes can fire simultaneously — the results are merged before injection.
 *
 * Example parses:
 *   "explain question 4 in exercise 3.4"
 *       → exerciseId="3.4", itemNumber=4, contentType=QUESTION
 *   "I don't understand Q2 of 3.4"
 *       → exerciseId="3.4", itemNumber=2, contentType=QUESTION
 *   "show me example 3 from chapter 5"
 *       → chapterRef="5", itemNumber=3, contentType=EXAMPLE
 *   "what is the formula for speed"
 *       → contentType=FORMULA (semantic search)
 *   "quiz me on exercise 3.4"
 *       → exerciseId="3.4", fetchAll=true
 *   "explain photosynthesis"
 *       → pure semantic
 */
object QueryRouter {

    data class ParsedQuery(
        val exerciseId: String? = null,
        val itemNumber: Int? = null,
        val chapterRef: String? = null,
        val contentType: ContentType? = null,
        /** When true, retrieve ALL items in the exercise (e.g. "quiz me on 3.4"). */
        val fetchAllInExercise: Boolean = false,
        /** The raw query, always used for semantic search as well. */
        val semanticQuery: String
    ) {
        val hasStructuralHint: Boolean
            get() = exerciseId != null || itemNumber != null || chapterRef != null || contentType != null
    }

    // ── Pattern library ──────────────────────────────────────────────────────

    // "exercise 3.4 question 4" / "ex 3.4 Q4"
    private val EX_Q = Regex(
        """(?:exercise|ex\.?)\s*([\d.]+[a-zA-Z]?)\s*[,;]?\s*(?:question|q\.?|no\.?|#)\s*(\d+)""",
        RegexOption.IGNORE_CASE
    )

    // "question 4 in exercise 3.4" / "Q4 of 3.4"
    private val Q_EX = Regex(
        """(?:question|q\.?|no\.?|#)\s*(\d+)\s*(?:in|of|from)?\s*(?:exercise|ex\.?)?\s*([\d.]+[a-zA-Z]?)""",
        RegexOption.IGNORE_CASE
    )

    // "exercise 3.4" alone — fetch all questions in it
    private val EX_ONLY = Regex(
        """(?:exercise|ex\.?)\s*([\d.]+[a-zA-Z]?)""",
        RegexOption.IGNORE_CASE
    )

    // "example 2" / "example number 2"
    private val EXAMPLE = Regex(
        """example\s*(?:number\s*)?(\d+)""",
        RegexOption.IGNORE_CASE
    )

    // "chapter 3" / "chapter three"
    private val CHAPTER = Regex(
        """chapter\s+(\d+|one|two|three|four|five|six|seven|eight|nine|ten)""",
        RegexOption.IGNORE_CASE
    )

    // Content type hints in the query
    private val FORMULA_HINT = Regex("""formula|equation|rule""", RegexOption.IGNORE_CASE)
    private val DEFINITION_HINT = Regex("""definition|meaning|what is|what are|define""", RegexOption.IGNORE_CASE)
    private val QUIZ_HINT = Regex("""quiz|test me|ask me|all questions""", RegexOption.IGNORE_CASE)

    // ── Public API ───────────────────────────────────────────────────────────

    fun parse(query: String): ParsedQuery {
        // "exercise 3.4 question 4"
        EX_Q.find(query)?.let { m ->
            return ParsedQuery(
                exerciseId = m.groupValues[1],
                itemNumber = m.groupValues[2].toIntOrNull(),
                contentType = ContentType.QUESTION,
                semanticQuery = query
            )
        }

        // "question 4 in exercise 3.4" / "Q4 of 3.4"
        Q_EX.find(query)?.let { m ->
            return ParsedQuery(
                exerciseId = m.groupValues[2].takeIf { it.isNotBlank() },
                itemNumber = m.groupValues[1].toIntOrNull(),
                contentType = ContentType.QUESTION,
                semanticQuery = query
            )
        }

        // "example 3"
        EXAMPLE.find(query)?.let { m ->
            val chapterRef = CHAPTER.find(query)?.groupValues?.get(1)
            return ParsedQuery(
                itemNumber = m.groupValues[1].toIntOrNull(),
                chapterRef = chapterRef,
                contentType = ContentType.EXAMPLE,
                semanticQuery = query
            )
        }

        // "exercise 3.4" alone — quiz / all questions mode
        EX_ONLY.find(query)?.let { m ->
            val isQuiz = QUIZ_HINT.containsMatchIn(query)
            return ParsedQuery(
                exerciseId = m.groupValues[1],
                fetchAllInExercise = isQuiz,
                contentType = if (isQuiz) null else ContentType.QUESTION,
                semanticQuery = query
            )
        }

        // Chapter-level reference
        CHAPTER.find(query)?.let { m ->
            val contentType = when {
                FORMULA_HINT.containsMatchIn(query) -> ContentType.FORMULA
                DEFINITION_HINT.containsMatchIn(query) -> ContentType.DEFINITION
                else -> null
            }
            return ParsedQuery(
                chapterRef = m.groupValues[1],
                contentType = contentType,
                semanticQuery = query
            )
        }

        // Content-type hints without explicit location
        val contentType = when {
            FORMULA_HINT.containsMatchIn(query) -> ContentType.FORMULA
            DEFINITION_HINT.containsMatchIn(query) -> ContentType.DEFINITION
            else -> null
        }

        return ParsedQuery(contentType = contentType, semanticQuery = query)
    }
}

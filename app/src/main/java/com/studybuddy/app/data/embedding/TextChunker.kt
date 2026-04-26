package com.studybuddy.app.data.embedding

/**
 * Splits extracted OCR text into overlapping chunks suitable for embedding.
 *
 * Strategy:
 *   1. Split on paragraph boundaries (2+ newlines).
 *   2. Short paragraphs (≤ maxChars) are emitted as one chunk.
 *   3. Long paragraphs are sliced with a sliding window [maxChars] wide,
 *      advancing by (maxChars - overlap) each step so adjacent chunks share
 *      ~50 chars of context at their boundary.
 *   4. Tiny trailing fragments (< 20 chars) are discarded.
 */
object TextChunker {

    fun chunk(
        text: String,
        maxChars: Int = 400,
        overlap: Int = 50
    ): List<String> {
        val paragraphs = text
            .split(Regex("\\n{2,}"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val chunks = mutableListOf<String>()

        for (para in paragraphs) {
            if (para.length <= maxChars) {
                chunks.add(para)
                continue
            }
            var start = 0
            while (start < para.length) {
                val end = minOf(start + maxChars, para.length)
                val slice = para.substring(start, end).trim()
                if (slice.length >= 20) chunks.add(slice)
                if (end == para.length) break
                start = end - overlap
            }
        }

        return chunks
    }
}

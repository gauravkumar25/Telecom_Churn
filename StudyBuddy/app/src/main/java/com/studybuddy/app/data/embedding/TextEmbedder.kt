package com.studybuddy.app.data.embedding

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Offline 256-dim text embedding using the hashing trick (feature hashing).
 *
 * Algorithm:
 *   1. Tokenize (lowercase, split on non-alphanumeric, drop stopwords)
 *   2. For each token: hash → bucket index (abs(hash) % DIMS), accumulate ±1
 *      using the hash sign to reduce collision cancellation
 *   3. L2-normalise the resulting vector
 *
 * String.hashCode() is guaranteed stable per the Java Language Spec (s[0]*31^(n-1)+…)
 * and ART implements it faithfully, so embeddings survive app restarts.
 *
 * Storage cost: 256 floats × 4 bytes = 1 024 bytes per chunk.
 */
object TextEmbedder {

    const val DIMS = 256

    private val STOPWORDS = setOf(
        "the", "a", "an", "is", "in", "on", "at", "to", "of", "and", "or",
        "it", "be", "as", "by", "for", "was", "are", "this", "that", "with",
        "from", "not", "but", "its", "their", "they", "we", "he", "she", "do",
        "did", "has", "had", "have", "will", "would", "could", "should", "may",
        "can", "also", "so", "if", "then", "than", "when", "which", "who"
    )

    fun tokenize(text: String): List<String> =
        text.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 2 && it !in STOPWORDS }

    /**
     * Returns an L2-normalised 256-dim float array for [text].
     * The zero vector is returned unchanged when no meaningful tokens exist.
     */
    fun embed(text: String): FloatArray {
        val vec = FloatArray(DIMS)
        for (token in tokenize(text)) {
            val h = token.hashCode()
            val dim = abs(h) % DIMS          // abs prevents negative index
            val sign = if (h >= 0) +1f else -1f  // sign trick reduces cancellation
            vec[dim] += sign
        }
        return l2normalize(vec)
    }

    fun l2normalize(vec: FloatArray): FloatArray {
        val norm = sqrt(vec.sumOf { (it * it).toDouble() }).toFloat()
        if (norm < 1e-9f) return vec
        return FloatArray(DIMS) { vec[it] / norm }
    }

    /**
     * Cosine similarity of two L2-normalised vectors (dot product = cosine when unit).
     * Range: [-1, 1]. Typical relevant threshold: > 0.15.
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        for (i in a.indices) dot += a[i] * b[i]
        return dot
    }
}

package com.studybuddy.app.data.embedding

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Offline 512-dim text embedding using the feature hashing trick with bigrams.
 *
 * Why 512-dim + bigrams over the original 256-dim unigram approach:
 *   - Doubles the embedding space → ~50% fewer hash collisions
 *   - Bigrams capture multi-word concepts: "exercise 3", "question 4",
 *     "speed distance", "quadratic equation" as single features
 *   - Still zero dependencies, zero model files, works fully offline
 *   - Can be hot-swapped for a TFLite model by changing only this file
 *
 * Algorithm:
 *   1. Tokenize (lowercase, alphanumeric only, drop stopwords & len < 2)
 *   2. Emit unigrams → hash → accumulate ±1 into first 256 dims
 *   3. Emit adjacent bigrams → hash → accumulate ±1 into dims 256-511
 *   4. L2-normalise the 512-dim vector
 *
 * String.hashCode() is specified by the Java Language Spec (s[0]*31^(n-1)+…)
 * and is faithfully implemented by Android ART — stable across app restarts.
 */
object TextEmbedder {

    const val DIMS = 512
    private const val HALF = DIMS / 2   // 256 — unigrams live here
                                         //       bigrams live in [256, 511]

    private val STOPWORDS = setOf(
        "the", "a", "an", "is", "in", "on", "at", "to", "of", "and", "or",
        "it", "be", "as", "by", "for", "was", "are", "this", "that", "with",
        "from", "not", "but", "its", "their", "they", "we", "he", "she", "do",
        "did", "has", "had", "have", "will", "would", "could", "should", "may",
        "can", "also", "so", "if", "then", "than", "when", "which", "who", "very"
    )

    fun tokenize(text: String): List<String> =
        text.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 2 && it !in STOPWORDS }

    /**
     * Returns an L2-normalised 512-dim float array for [text].
     * Dims 0–255 encode unigrams; dims 256–511 encode adjacent bigrams.
     */
    fun embed(text: String): FloatArray {
        val vec = FloatArray(DIMS)
        val tokens = tokenize(text)

        // Unigrams → dims 0..255
        for (token in tokens) {
            val h = token.hashCode()
            val dim = abs(h) % HALF
            vec[dim] += if (h >= 0) +1f else -1f
        }

        // Bigrams → dims 256..511
        for (i in 0 until tokens.size - 1) {
            val bigram = "${tokens[i]}_${tokens[i + 1]}"
            val h = bigram.hashCode()
            val dim = HALF + (abs(h) % HALF)
            vec[dim] += if (h >= 0) +1f else -1f
        }

        return l2normalize(vec)
    }

    fun l2normalize(vec: FloatArray): FloatArray {
        val norm = sqrt(vec.sumOf { (it * it).toDouble() }).toFloat()
        if (norm < 1e-9f) return vec
        return FloatArray(DIMS) { vec[it] / norm }
    }

    /**
     * Cosine similarity of two L2-normalised vectors (= dot product when unit).
     * Range: [-1, 1]. Chunks scoring below ~0.15 are typically noise.
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        for (i in a.indices) dot += a[i] * b[i]
        return dot
    }
}

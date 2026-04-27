package com.studybuddy.app.network.models

import com.google.gson.annotations.SerializedName

data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    @SerializedName("usageMetadata") val usageMetadata: GeminiUsage? = null,
    val error: GeminiError? = null
) {
    /** Convenience: extract the text from the first candidate's first text part. */
    val text: String
        get() = candidates.firstOrNull()
            ?.content?.parts
            ?.firstOrNull { it.text != null }
            ?.text ?: ""
}

data class GeminiCandidate(
    val content: GeminiContent = GeminiContent("model", emptyList()),
    @SerializedName("finishReason") val finishReason: String? = null,
    val index: Int = 0
)

data class GeminiUsage(
    @SerializedName("promptTokenCount") val promptTokenCount: Int = 0,
    @SerializedName("candidatesTokenCount") val candidatesTokenCount: Int = 0,
    @SerializedName("totalTokenCount") val totalTokenCount: Int = 0
)

data class GeminiError(
    val code: Int,
    val message: String,
    val status: String
)

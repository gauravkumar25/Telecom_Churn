package com.studybuddy.app.network.models

import com.google.gson.annotations.SerializedName

data class GeminiRequest(
    @SerializedName("system_instruction") val systemInstruction: GeminiSystemInstruction? = null,
    val contents: List<GeminiContent>,
    @SerializedName("generationConfig") val generationConfig: GeminiGenerationConfig = GeminiGenerationConfig()
)

data class GeminiSystemInstruction(
    val parts: List<GeminiPart>
)

data class GeminiContent(
    val role: String,   // "user" or "model"
    val parts: List<GeminiPart>
)

/**
 * Unified part: set exactly one field per instance.
 * Gson skips null fields, so only the populated field is serialised.
 * Use GeminiPart.text("…") or GeminiPart.image(…) factory functions.
 */
data class GeminiPart(
    val text: String? = null,
    @SerializedName("inline_data") val inlineData: GeminiInlineData? = null
) {
    companion object {
        fun text(value: String) = GeminiPart(text = value)
        fun image(mimeType: String, base64Data: String) =
            GeminiPart(inlineData = GeminiInlineData(mimeType = mimeType, data = base64Data))
    }
}

data class GeminiInlineData(
    @SerializedName("mime_type") val mimeType: String,
    val data: String
)

data class GeminiGenerationConfig(
    @SerializedName("maxOutputTokens") val maxOutputTokens: Int = 4096,
    val temperature: Float = 0.9f,
    @SerializedName("topP") val topP: Float = 0.95f
)

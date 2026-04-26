package com.studybuddy.app.network.models

import com.google.gson.annotations.SerializedName

data class ClaudeResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ResponseContent>,
    val model: String,
    @SerializedName("stop_reason") val stopReason: String?,
    val usage: Usage
) {
    val text: String get() = content.filterIsInstance<ResponseContent>().joinToString("") { it.text }
}

data class ResponseContent(
    val type: String,
    val text: String = ""
)

data class Usage(
    @SerializedName("input_tokens") val inputTokens: Int,
    @SerializedName("output_tokens") val outputTokens: Int
)

data class ClaudeErrorResponse(
    val type: String,
    val error: ErrorDetail
)

data class ErrorDetail(
    val type: String,
    val message: String
)

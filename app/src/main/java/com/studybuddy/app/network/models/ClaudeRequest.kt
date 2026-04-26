package com.studybuddy.app.network.models

import com.google.gson.annotations.SerializedName

data class ClaudeRequest(
    val model: String = "claude-sonnet-4-6",
    @SerializedName("max_tokens") val maxTokens: Int = 4096,
    val system: String,
    val messages: List<ClaudeMessage>,
    val stream: Boolean = false
)

data class ClaudeMessage(
    val role: String,
    val content: List<ContentBlock>
)

sealed class ContentBlock {
    data class Text(val type: String = "text", val text: String) : ContentBlock()
    data class Image(
        val type: String = "image",
        val source: ImageSource
    ) : ContentBlock()
}

data class ImageSource(
    val type: String = "base64",
    @SerializedName("media_type") val mediaType: String,
    val data: String
)

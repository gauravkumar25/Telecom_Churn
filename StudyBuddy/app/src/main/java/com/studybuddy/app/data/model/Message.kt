package com.studybuddy.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageRole { USER, ASSISTANT }
enum class MessageType { TEXT, IMAGE, VOICE }

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val imageUri: String? = null,
    val type: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

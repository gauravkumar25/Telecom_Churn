package com.studybuddy.app.data.local

import androidx.room.TypeConverter
import com.studybuddy.app.data.model.ChapterStatus
import com.studybuddy.app.data.model.ContentType
import com.studybuddy.app.data.model.MessageRole
import com.studybuddy.app.data.model.MessageType
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Converters {
    @TypeConverter fun fromMessageRole(role: MessageRole) = role.name
    @TypeConverter fun toMessageRole(name: String) = MessageRole.valueOf(name)

    @TypeConverter fun fromMessageType(type: MessageType) = type.name
    @TypeConverter fun toMessageType(name: String) = MessageType.valueOf(name)

    @TypeConverter fun fromChapterStatus(status: ChapterStatus) = status.name
    @TypeConverter fun toChapterStatus(name: String) = ChapterStatus.valueOf(name)

    @TypeConverter fun fromContentType(type: ContentType) = type.name
    @TypeConverter fun toContentType(name: String) =
        runCatching { ContentType.valueOf(name) }.getOrDefault(ContentType.TEXT)

    /**
     * FloatArray ↔ ByteArray using LITTLE_ENDIAN ByteBuffer.
     * Each float = 4 bytes, so a 512-dim vector = 2 048 bytes per chunk.
     * ByteOrder is fixed at write time so reading on any device is consistent.
     */
    @TypeConverter
    fun floatArrayToBytes(arr: FloatArray): ByteArray {
        val buf = ByteBuffer.allocate(arr.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        buf.asFloatBuffer().put(arr)
        return buf.array()
    }

    @TypeConverter
    fun bytesToFloatArray(bytes: ByteArray): FloatArray {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val arr = FloatArray(bytes.size / 4)
        buf.asFloatBuffer().get(arr)
        return arr
    }
}

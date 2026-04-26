package com.studybuddy.app.data.local

import androidx.room.TypeConverter
import com.studybuddy.app.data.model.ChapterStatus
import com.studybuddy.app.data.model.MessageRole
import com.studybuddy.app.data.model.MessageType

class Converters {
    @TypeConverter fun fromMessageRole(role: MessageRole) = role.name
    @TypeConverter fun toMessageRole(name: String) = MessageRole.valueOf(name)

    @TypeConverter fun fromMessageType(type: MessageType) = type.name
    @TypeConverter fun toMessageType(name: String) = MessageType.valueOf(name)

    @TypeConverter fun fromChapterStatus(status: ChapterStatus) = status.name
    @TypeConverter fun toChapterStatus(name: String) = ChapterStatus.valueOf(name)
}

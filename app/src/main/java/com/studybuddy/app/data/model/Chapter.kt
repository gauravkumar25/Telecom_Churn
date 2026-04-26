package com.studybuddy.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ChapterStatus { NOT_STARTED, IN_PROGRESS, COMPLETED, NEEDS_REVISION }

@Entity(
    tableName = "chapters",
    foreignKeys = [ForeignKey(
        entity = Subject::class,
        parentColumns = ["id"],
        childColumns = ["subjectId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("subjectId")]
)
data class Chapter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val name: String,
    val description: String = "",
    val status: ChapterStatus = ChapterStatus.NOT_STARTED,
    val screenshotUris: String = "",   // JSON array of local URIs
    val notes: String = "",
    val estimatedHours: Float = 1f,
    val createdAt: Long = System.currentTimeMillis()
)

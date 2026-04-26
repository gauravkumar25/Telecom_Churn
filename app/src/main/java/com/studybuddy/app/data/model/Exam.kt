package com.studybuddy.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exams")
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectName: String,
    val examDate: Long,
    val startTime: String = "10:00 AM",
    val venue: String = "",
    val notes: String = "",
    val colorHex: String = "#6650A4",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val daysUntilExam: Long
        get() = ((examDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
}

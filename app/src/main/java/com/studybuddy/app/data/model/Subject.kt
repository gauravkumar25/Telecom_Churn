package com.studybuddy.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#6650A4",
    val syllabus: String = "",
    val totalChapters: Int = 0,
    val completedChapters: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

val defaultSubjectColors = listOf(
    "#6650A4", "#00897B", "#E91E63", "#1976D2",
    "#F57C00", "#7B1FA2", "#388E3C", "#C62828"
)

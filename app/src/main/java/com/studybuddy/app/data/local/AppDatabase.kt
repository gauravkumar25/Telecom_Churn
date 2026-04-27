package com.studybuddy.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.studybuddy.app.data.local.dao.ChapterDao
import com.studybuddy.app.data.local.dao.ExamDao
import com.studybuddy.app.data.local.dao.KnowledgeChunkDao
import com.studybuddy.app.data.local.dao.MessageDao
import com.studybuddy.app.data.local.dao.StudySessionDao
import com.studybuddy.app.data.local.dao.SubjectDao
import com.studybuddy.app.data.model.Chapter
import com.studybuddy.app.data.model.Exam
import com.studybuddy.app.data.model.KnowledgeChunk
import com.studybuddy.app.data.model.Message
import com.studybuddy.app.data.model.StudySession
import com.studybuddy.app.data.model.Subject

@Database(
    entities = [
        Message::class,
        Subject::class,
        Chapter::class,
        Exam::class,
        StudySession::class,
        KnowledgeChunk::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun subjectDao(): SubjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun examDao(): ExamDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun knowledgeChunkDao(): KnowledgeChunkDao
}

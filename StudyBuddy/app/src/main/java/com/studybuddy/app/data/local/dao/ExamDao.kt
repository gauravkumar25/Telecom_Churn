package com.studybuddy.app.data.local.dao

import androidx.room.*
import com.studybuddy.app.data.model.Exam
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY examDate ASC")
    fun getAllExams(): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE examDate >= :now AND isCompleted = 0 ORDER BY examDate ASC LIMIT 3")
    fun getUpcomingExams(now: Long = System.currentTimeMillis()): Flow<List<Exam>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exam: Exam): Long

    @Update
    suspend fun update(exam: Exam)

    @Delete
    suspend fun delete(exam: Exam)
}

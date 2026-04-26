package com.studybuddy.app.data.local.dao

import androidx.room.*
import com.studybuddy.app.data.model.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: StudySession)

    @Update
    suspend fun update(session: StudySession)

    @Query("DELETE FROM study_sessions WHERE id = :sessionId")
    suspend fun delete(sessionId: String)
}

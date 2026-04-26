package com.studybuddy.app.data.local.dao

import androidx.room.*
import com.studybuddy.app.data.model.Chapter
import com.studybuddy.app.data.model.ChapterStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY id ASC")
    fun getChaptersForSubject(subjectId: Long): Flow<List<Chapter>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chapter: Chapter): Long

    @Update
    suspend fun update(chapter: Chapter)

    @Delete
    suspend fun delete(chapter: Chapter)

    @Query("UPDATE chapters SET status = :status WHERE id = :chapterId")
    suspend fun updateStatus(chapterId: Long, status: ChapterStatus)
}

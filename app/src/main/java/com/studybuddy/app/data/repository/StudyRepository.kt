package com.studybuddy.app.data.repository

import com.studybuddy.app.data.local.dao.ChapterDao
import com.studybuddy.app.data.local.dao.ExamDao
import com.studybuddy.app.data.local.dao.SubjectDao
import com.studybuddy.app.data.model.Chapter
import com.studybuddy.app.data.model.ChapterStatus
import com.studybuddy.app.data.model.Exam
import com.studybuddy.app.data.model.Subject
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudyRepository @Inject constructor(
    private val subjectDao: SubjectDao,
    private val chapterDao: ChapterDao,
    private val examDao: ExamDao
) {
    val allSubjects: Flow<List<Subject>> = subjectDao.getAllSubjects()
    val allExams: Flow<List<Exam>> = examDao.getAllExams()
    val upcomingExams: Flow<List<Exam>> = examDao.getUpcomingExams()

    suspend fun addSubject(subject: Subject) = subjectDao.insert(subject)
    suspend fun updateSubject(subject: Subject) = subjectDao.update(subject)
    suspend fun deleteSubject(subject: Subject) = subjectDao.delete(subject)

    fun getChaptersForSubject(subjectId: Long): Flow<List<Chapter>> =
        chapterDao.getChaptersForSubject(subjectId)

    suspend fun addChapter(chapter: Chapter) = chapterDao.insert(chapter)
    suspend fun updateChapter(chapter: Chapter) = chapterDao.update(chapter)
    suspend fun deleteChapter(chapter: Chapter) = chapterDao.delete(chapter)
    suspend fun updateChapterStatus(chapterId: Long, status: ChapterStatus) =
        chapterDao.updateStatus(chapterId, status)

    suspend fun addExam(exam: Exam) = examDao.insert(exam)
    suspend fun updateExam(exam: Exam) = examDao.update(exam)
    suspend fun deleteExam(exam: Exam) = examDao.delete(exam)
}

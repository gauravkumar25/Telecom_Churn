package com.studybuddy.app.ui.screens.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.app.data.model.Chapter
import com.studybuddy.app.data.model.ChapterStatus
import com.studybuddy.app.data.model.Subject
import com.studybuddy.app.data.repository.StudyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubjectsViewModel @Inject constructor(
    private val studyRepository: StudyRepository
) : ViewModel() {

    val subjects: StateFlow<List<Subject>> = studyRepository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSubject(name: String, syllabus: String, colorHex: String) {
        viewModelScope.launch {
            studyRepository.addSubject(Subject(name = name, syllabus = syllabus, colorHex = colorHex))
        }
    }

    fun updateSubject(subject: Subject) {
        viewModelScope.launch { studyRepository.updateSubject(subject) }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch { studyRepository.deleteSubject(subject) }
    }

    fun getChaptersForSubject(subjectId: Long): Flow<List<Chapter>> =
        studyRepository.getChaptersForSubject(subjectId)

    fun addChapter(subjectId: Long, name: String, description: String, estimatedHours: Float) {
        viewModelScope.launch {
            studyRepository.addChapter(
                Chapter(subjectId = subjectId, name = name, description = description, estimatedHours = estimatedHours)
            )
        }
    }

    fun updateChapterStatus(chapterId: Long, status: ChapterStatus) {
        viewModelScope.launch { studyRepository.updateChapterStatus(chapterId, status) }
    }

    fun deleteChapter(chapter: Chapter) {
        viewModelScope.launch { studyRepository.deleteChapter(chapter) }
    }
}

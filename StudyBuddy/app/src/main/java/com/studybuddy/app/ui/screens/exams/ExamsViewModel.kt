package com.studybuddy.app.ui.screens.exams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.app.data.model.Exam
import com.studybuddy.app.data.repository.StudyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExamsViewModel @Inject constructor(
    private val studyRepository: StudyRepository
) : ViewModel() {

    val exams: StateFlow<List<Exam>> = studyRepository.allExams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addExam(subjectName: String, examDate: Long, startTime: String, venue: String, notes: String, colorHex: String) {
        viewModelScope.launch {
            studyRepository.addExam(
                Exam(subjectName = subjectName, examDate = examDate, startTime = startTime,
                    venue = venue, notes = notes, colorHex = colorHex)
            )
        }
    }

    fun markCompleted(exam: Exam) {
        viewModelScope.launch { studyRepository.updateExam(exam.copy(isCompleted = true)) }
    }

    fun deleteExam(exam: Exam) {
        viewModelScope.launch { studyRepository.deleteExam(exam) }
    }
}

package com.studybuddy.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.app.data.model.Exam
import com.studybuddy.app.data.model.StudySession
import com.studybuddy.app.data.repository.ChatRepository
import com.studybuddy.app.data.repository.PreferencesRepository
import com.studybuddy.app.data.repository.StudyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val studyRepository: StudyRepository,
    prefsRepository: PreferencesRepository
) : ViewModel() {

    val studentName: StateFlow<String> = prefsRepository.studentName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val buddyName: StateFlow<String> = prefsRepository.buddyName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Buddy")

    val sessions: StateFlow<List<StudySession>> = chatRepository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingExams: StateFlow<List<Exam>> = studyRepository.upcomingExams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _newSessionId = MutableStateFlow<String?>(null)
    val newSessionId: StateFlow<String?> = _newSessionId

    fun createNewSession(title: String = "Study Chat") {
        viewModelScope.launch {
            val session = chatRepository.createSession(title)
            _newSessionId.value = session.id
        }
    }

    fun clearNewSession() { _newSessionId.value = null }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch { chatRepository.deleteSession(sessionId) }
    }
}

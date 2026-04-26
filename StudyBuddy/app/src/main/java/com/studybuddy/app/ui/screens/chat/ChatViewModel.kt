package com.studybuddy.app.ui.screens.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.app.data.model.Exam
import com.studybuddy.app.data.model.Message
import com.studybuddy.app.data.model.Subject
import com.studybuddy.app.data.repository.ChatRepository
import com.studybuddy.app.data.repository.StudyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingImageUri: Uri? = null,
    val isVoiceMode: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    private val _upcomingExams = MutableStateFlow<List<Exam>>(emptyList())

    init {
        viewModelScope.launch {
            studyRepository.allSubjects.collect { _subjects.value = it }
        }
        viewModelScope.launch {
            studyRepository.upcomingExams.collect { _upcomingExams.value = it }
        }
    }

    fun loadMessages(sessionId: String) {
        viewModelScope.launch {
            chatRepository.getMessagesForSession(sessionId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun setImage(uri: Uri?) {
        _uiState.update { it.copy(pendingImageUri = uri) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun sendMessage(sessionId: String, textOverride: String? = null) {
        val text = textOverride ?: _uiState.value.inputText.trim()
        val imageUri = _uiState.value.pendingImageUri
        if (text.isBlank() && imageUri == null) return

        _uiState.update { it.copy(inputText = "", pendingImageUri = null, isLoading = true, error = null) }

        viewModelScope.launch {
            val result = chatRepository.sendMessage(
                sessionId = sessionId,
                userText = text.ifBlank { "I've shared a photo — please help me understand this." },
                imageUri = imageUri,
                subjects = _subjects.value,
                upcomingExams = _upcomingExams.value
            )
            result.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}

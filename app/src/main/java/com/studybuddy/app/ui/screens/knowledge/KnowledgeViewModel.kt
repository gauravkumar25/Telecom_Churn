package com.studybuddy.app.ui.screens.knowledge

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.app.data.model.DocumentSummaryTuple
import com.studybuddy.app.data.repository.KnowledgeRepository
import com.studybuddy.app.data.repository.StudyRepository
import com.studybuddy.app.data.model.Subject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KnowledgeUiState(
    val documents: List<DocumentSummaryTuple> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val totalChunks: Int = 0,
    val isProcessing: Boolean = false,
    val processingMessage: String = "",
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class KnowledgeViewModel @Inject constructor(
    private val knowledgeRepository: KnowledgeRepository,
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KnowledgeUiState())
    val uiState: StateFlow<KnowledgeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                knowledgeRepository.documentSummaries,
                knowledgeRepository.hasChunks,
                studyRepository.allSubjects
            ) { docs, _, subjects ->
                Triple(docs, docs.sumOf { it.chunkCount }, subjects)
            }.collect { (docs, total, subjects) ->
                _uiState.update { it.copy(documents = docs, totalChunks = total, subjects = subjects) }
            }
        }
    }

    fun processImage(
        imageUri: Uri,
        subjectId: Long? = null,
        subjectName: String = ""
    ) {
        _uiState.update { it.copy(isProcessing = true, processingMessage = "Starting…", error = null, successMessage = null) }
        viewModelScope.launch {
            val result = knowledgeRepository.processImage(
                imageUri = imageUri,
                subjectId = subjectId,
                subjectName = subjectName,
                onProgress = { msg -> _uiState.update { it.copy(processingMessage = msg) } }
            )
            result.fold(
                onSuccess = { count ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            processingMessage = "",
                            successMessage = "✅ Added $count chunks to Knowledge Base!"
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isProcessing = false, processingMessage = "", error = e.message) }
                }
            )
        }
    }

    fun deleteDocument(sourceImageUri: String, deleteImageFile: Boolean) {
        viewModelScope.launch {
            knowledgeRepository.deleteDocument(sourceImageUri, deleteImageFile)
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearSuccess() = _uiState.update { it.copy(successMessage = null) }
}

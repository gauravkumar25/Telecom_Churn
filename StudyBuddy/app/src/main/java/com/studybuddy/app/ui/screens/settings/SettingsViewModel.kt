package com.studybuddy.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.app.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepo: PreferencesRepository
) : ViewModel() {

    val studentName: StateFlow<String> = prefsRepo.studentName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val studentClass: StateFlow<String> = prefsRepo.studentClass
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val apiKey: StateFlow<String> = prefsRepo.claudeApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val buddyName: StateFlow<String> = prefsRepo.buddyName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Buddy")
    val extraContext: StateFlow<String> = prefsRepo.extraContext
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun saveStudentName(name: String) = viewModelScope.launch { prefsRepo.saveStudentName(name) }
    fun saveStudentClass(cls: String) = viewModelScope.launch { prefsRepo.saveStudentClass(cls) }
    fun saveApiKey(key: String) = viewModelScope.launch { prefsRepo.saveApiKey(key) }
    fun saveBuddyName(name: String) = viewModelScope.launch { prefsRepo.saveBuddyName(name) }
    fun saveExtraContext(ctx: String) = viewModelScope.launch { prefsRepo.saveExtraContext(ctx) }
}

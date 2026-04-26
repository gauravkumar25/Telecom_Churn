package com.studybuddy.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "studybuddy_prefs")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val STUDENT_NAME = stringPreferencesKey("student_name")
        val STUDENT_CLASS = stringPreferencesKey("student_class")
        val CLAUDE_API_KEY = stringPreferencesKey("claude_api_key")
        val BUDDY_NAME = stringPreferencesKey("buddy_name")
        val EXTRA_CONTEXT = stringPreferencesKey("extra_context")
    }

    val studentName: Flow<String> = context.dataStore.data.map { it[STUDENT_NAME] ?: "" }
    val studentClass: Flow<String> = context.dataStore.data.map { it[STUDENT_CLASS] ?: "" }
    val claudeApiKey: Flow<String> = context.dataStore.data.map { it[CLAUDE_API_KEY] ?: "" }
    val buddyName: Flow<String> = context.dataStore.data.map { it[BUDDY_NAME] ?: "Buddy" }
    val extraContext: Flow<String> = context.dataStore.data.map { it[EXTRA_CONTEXT] ?: "" }

    suspend fun saveStudentName(name: String) = context.dataStore.edit { it[STUDENT_NAME] = name }
    suspend fun saveStudentClass(cls: String) = context.dataStore.edit { it[STUDENT_CLASS] = cls }
    suspend fun saveApiKey(key: String) = context.dataStore.edit { it[CLAUDE_API_KEY] = key }
    suspend fun saveBuddyName(name: String) = context.dataStore.edit { it[BUDDY_NAME] = name }
    suspend fun saveExtraContext(ctx: String) = context.dataStore.edit { it[EXTRA_CONTEXT] = ctx }
}

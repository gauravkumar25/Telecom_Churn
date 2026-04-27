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
        val STUDENT_NAME    = stringPreferencesKey("student_name")
        val STUDENT_CLASS   = stringPreferencesKey("student_class")
        val GEMINI_API_KEY  = stringPreferencesKey("gemini_api_key")
        val BUDDY_NAME      = stringPreferencesKey("buddy_name")
        val EXTRA_CONTEXT   = stringPreferencesKey("extra_context")
        val CHAT_MODEL      = stringPreferencesKey("chat_model")
    }

    val studentName:  Flow<String> = context.dataStore.data.map { it[STUDENT_NAME]   ?: "" }
    val studentClass: Flow<String> = context.dataStore.data.map { it[STUDENT_CLASS]  ?: "" }
    val geminiApiKey: Flow<String> = context.dataStore.data.map { it[GEMINI_API_KEY] ?: "" }
    val buddyName:    Flow<String> = context.dataStore.data.map { it[BUDDY_NAME]     ?: "Buddy" }
    val extraContext: Flow<String> = context.dataStore.data.map { it[EXTRA_CONTEXT]  ?: "" }
    /** Model used for chat. Defaults to Gemini Flash (fast + free-tier eligible). */
    val chatModel:    Flow<String> = context.dataStore.data.map { it[CHAT_MODEL] ?: "gemini-2.0-flash" }

    suspend fun saveStudentName(name: String)  = context.dataStore.edit { it[STUDENT_NAME]   = name }
    suspend fun saveStudentClass(cls: String)  = context.dataStore.edit { it[STUDENT_CLASS]  = cls  }
    suspend fun saveGeminiApiKey(key: String)  = context.dataStore.edit { it[GEMINI_API_KEY] = key  }
    suspend fun saveBuddyName(name: String)    = context.dataStore.edit { it[BUDDY_NAME]     = name }
    suspend fun saveExtraContext(ctx: String)  = context.dataStore.edit { it[EXTRA_CONTEXT]  = ctx  }
    suspend fun saveChatModel(model: String)   = context.dataStore.edit { it[CHAT_MODEL]     = model }
}

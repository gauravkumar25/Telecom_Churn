package com.studybuddy.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val studentName by viewModel.studentName.collectAsState()
    val studentClass by viewModel.studentClass.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val buddyName by viewModel.buddyName.collectAsState()
    val extraContext by viewModel.extraContext.collectAsState()

    var nameInput by remember(studentName) { mutableStateOf(studentName) }
    var classInput by remember(studentClass) { mutableStateOf(studentClass) }
    var apiKeyInput by remember(apiKey) { mutableStateOf(apiKey) }
    var buddyInput by remember(buddyName) { mutableStateOf(buddyName) }
    var contextInput by remember(extraContext) { mutableStateOf(extraContext) }
    var showApiKey by remember { mutableStateOf(false) }
    var savedSnack by remember { mutableStateOf(false) }

    LaunchedEffect(savedSnack) {
        if (savedSnack) {
            kotlinx.coroutines.delay(2000)
            savedSnack = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("⚙️ Settings", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Student Profile
            SectionHeader("Student Profile 👧")
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Student Name") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = classInput,
                onValueChange = { classInput = it },
                label = { Text("Class / Grade") },
                leadingIcon = { Icon(Icons.Default.School, null) },
                placeholder = { Text("e.g. Class 10, Grade 5") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Buddy Customization
            SectionHeader("Your Study Buddy 🤓")
            OutlinedTextField(
                value = buddyInput,
                onValueChange = { buddyInput = it },
                label = { Text("Buddy's Name") },
                leadingIcon = { Icon(Icons.Default.Face, null) },
                placeholder = { Text("e.g. Einstein, Buddy, Maya") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = contextInput,
                onValueChange = { contextInput = it },
                label = { Text("Extra Notes for Buddy") },
                leadingIcon = { Icon(Icons.Default.Notes, null) },
                placeholder = { Text("e.g. She loves science fiction. She struggles with algebra.") },
                minLines = 3, maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            // API Key
            SectionHeader("API Configuration 🔑")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Get your free API key at aistudio.google.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text("Gemini API Key") },
                leadingIcon = { Icon(Icons.Default.Key, null) },
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Save button
            Button(
                onClick = {
                    viewModel.saveStudentName(nameInput)
                    viewModel.saveStudentClass(classInput)
                    viewModel.saveApiKey(apiKeyInput)
                    viewModel.saveBuddyName(buddyInput.ifBlank { "Buddy" })
                    viewModel.saveExtraContext(contextInput)
                    savedSnack = true
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Save Settings", fontWeight = FontWeight.Bold)
            }

            if (savedSnack) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text("Settings saved! ✨", color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }

            // App info
            SectionHeader("About 📱")
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    InfoRow("App", "StudyBuddy v1.0")
                    InfoRow("AI Model", "Gemini 2.0 Flash")
                    InfoRow("Features", "Chat • Voice • Image • Exams • Schedule")
                    InfoRow("Made with", "❤️ for students everywhere")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("$label:", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(90.dp))
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

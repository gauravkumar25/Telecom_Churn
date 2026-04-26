package com.studybuddy.app.ui.screens.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studybuddy.app.data.model.Chapter
import com.studybuddy.app.data.model.ChapterStatus
import com.studybuddy.app.data.model.Subject
import com.studybuddy.app.data.model.defaultSubjectColors

@Composable
fun SubjectsScreen(viewModel: SubjectsViewModel = hiltViewModel()) {
    val subjects by viewModel.subjects.collectAsState()
    var showAddSubject by remember { mutableStateOf(false) }
    var expandedSubjectId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(title = { Text("📚 Subjects & Syllabus", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSubject = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add Subject") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (subjects.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📖", style = MaterialTheme.typography.displayMedium)
                            Text("No subjects yet! Add your first subject.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
            }
            items(subjects) { subject ->
                SubjectCard(
                    subject = subject,
                    isExpanded = expandedSubjectId == subject.id,
                    onToggle = {
                        expandedSubjectId = if (expandedSubjectId == subject.id) null else subject.id
                    },
                    onDelete = { viewModel.deleteSubject(subject) },
                    onUpdate = viewModel::updateSubject,
                    viewModel = viewModel
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddSubject) {
        AddSubjectDialog(
            onDismiss = { showAddSubject = false },
            onAdd = { name, syllabus, color ->
                viewModel.addSubject(name, syllabus, color)
                showAddSubject = false
            }
        )
    }
}

@Composable
private fun SubjectCard(
    subject: Subject,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (Subject) -> Unit,
    viewModel: SubjectsViewModel
) {
    val chapters by viewModel.getChaptersForSubject(subject.id).collectAsState(initial = emptyList())
    var showAddChapter by remember { mutableStateOf(false) }
    var showEditSyllabus by remember { mutableStateOf(false) }
    val subjectColor = remember(subject.colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(subject.colorHex)) }.getOrDefault(Color(0xFF6650A4))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(subjectColor)
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(subject.name, color = Color.White, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("${chapters.count { it.status == ChapterStatus.COMPLETED }}/${chapters.size} done",
                    color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = Color.White
                )
            }

            if (isExpanded) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Syllabus
                    if (subject.syllabus.isNotBlank()) {
                        Text("Syllabus:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                        Text(subject.syllabus, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), maxLines = 3)
                        Spacer(Modifier.height(8.dp))
                    }

                    // Chapters
                    chapters.forEach { chapter ->
                        ChapterRow(chapter = chapter,
                            onStatusChange = { viewModel.updateChapterStatus(chapter.id, it) },
                            onDelete = { viewModel.deleteChapter(chapter) }
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showAddChapter = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                            Text(" Chapter")
                        }
                        OutlinedButton(onClick = { showEditSyllabus = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                            Text(" Syllabus")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Delete subject", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showAddChapter) {
        AddChapterDialog(
            onDismiss = { showAddChapter = false },
            onAdd = { name, desc, hours ->
                viewModel.addChapter(subject.id, name, desc, hours)
                showAddChapter = false
            }
        )
    }

    if (showEditSyllabus) {
        EditSyllabusDialog(
            currentSyllabus = subject.syllabus,
            onDismiss = { showEditSyllabus = false },
            onSave = { newSyllabus ->
                onUpdate(subject.copy(syllabus = newSyllabus))
                showEditSyllabus = false
            }
        )
    }
}

@Composable
private fun ChapterRow(chapter: Chapter, onStatusChange: (ChapterStatus) -> Unit, onDelete: () -> Unit) {
    val statusIcon = when (chapter.status) {
        ChapterStatus.NOT_STARTED -> "⬜"
        ChapterStatus.IN_PROGRESS -> "🔄"
        ChapterStatus.COMPLETED -> "✅"
        ChapterStatus.NEEDS_REVISION -> "🔁"
    }
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(statusIcon, modifier = Modifier.clickable { showMenu = true })
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(chapter.name, style = MaterialTheme.typography.bodyMedium)
            if (chapter.description.isNotBlank())
                Text(chapter.description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Text("~${chapter.estimatedHours}h", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            ChapterStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text("${statusIcon(status)} ${status.name.replace('_', ' ')}") },
                    onClick = { onStatusChange(status); showMenu = false }
                )
            }
        }
    }
}

private fun statusIcon(status: ChapterStatus) = when (status) {
    ChapterStatus.NOT_STARTED -> "⬜"
    ChapterStatus.IN_PROGRESS -> "🔄"
    ChapterStatus.COMPLETED -> "✅"
    ChapterStatus.NEEDS_REVISION -> "🔁"
}

@Composable
private fun AddSubjectDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var syllabus by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(defaultSubjectColors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subject") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Subject Name") }, singleLine = true)
                OutlinedTextField(value = syllabus, onValueChange = { syllabus = it },
                    label = { Text("Syllabus / Topics") }, minLines = 3, maxLines = 5)
                Text("Color:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    defaultSubjectColors.take(4).forEach { hex ->
                        val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)
                        Box(modifier = Modifier
                            .size(if (selectedColor == hex) 34.dp else 28.dp)
                            .background(c, RoundedCornerShape(50))
                            .clickable { selectedColor = hex })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onAdd(name, syllabus, selectedColor) }) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddChapterDialog(onDismiss: () -> Unit, onAdd: (String, String, Float) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("1") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Chapter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Chapter Name") }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description (optional)") })
                OutlinedTextField(value = hours, onValueChange = { hours = it }, label = { Text("Estimated Hours") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onAdd(name, desc, hours.toFloatOrNull() ?: 1f)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EditSyllabusDialog(currentSyllabus: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var syllabus by remember { mutableStateOf(currentSyllabus) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Syllabus") },
        text = {
            OutlinedTextField(
                value = syllabus,
                onValueChange = { syllabus = it },
                label = { Text("Syllabus / Topics") },
                minLines = 5, maxLines = 10,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { onSave(syllabus) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

package com.studybuddy.app.ui.screens.exams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studybuddy.app.data.model.Exam
import com.studybuddy.app.data.model.defaultSubjectColors
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamsScreen(viewModel: ExamsViewModel = hiltViewModel()) {
    val exams by viewModel.exams.collectAsState()
    var showAddExam by remember { mutableStateOf(false) }

    val upcoming = exams.filter { !it.isCompleted && it.examDate >= System.currentTimeMillis() }
    val past = exams.filter { it.isCompleted || it.examDate < System.currentTimeMillis() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("📅 Exam Timetable", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddExam = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add Exam") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (upcoming.isEmpty() && past.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📅", style = MaterialTheme.typography.displayMedium)
                            Text("No exams added yet!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text("Add your exam dates to stay prepared!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            if (upcoming.isNotEmpty()) {
                item {
                    Text("Upcoming Exams", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 4.dp))
                }
                items(upcoming) { exam ->
                    ExamCard(exam = exam,
                        onMarkDone = { viewModel.markCompleted(exam) },
                        onDelete = { viewModel.deleteExam(exam) })
                }
            }

            if (past.isNotEmpty()) {
                item {
                    Text("Completed / Past", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                items(past) { exam ->
                    ExamCard(exam = exam, isPast = true,
                        onMarkDone = {},
                        onDelete = { viewModel.deleteExam(exam) })
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddExam) {
        AddExamDialog(
            onDismiss = { showAddExam = false },
            onAdd = { subject, date, time, venue, notes, color ->
                viewModel.addExam(subject, date, time, venue, notes, color)
                showAddExam = false
            }
        )
    }
}

@Composable
private fun ExamCard(exam: Exam, isPast: Boolean = false, onMarkDone: () -> Unit, onDelete: () -> Unit) {
    val examColor = runCatching { Color(android.graphics.Color.parseColor(exam.colorHex)) }.getOrDefault(Color(0xFF6650A4))
    val dateFormat = remember { SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(if (isPast) 1.dp else 4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(8.dp).fillMaxHeight().background(if (isPast) Color.Gray else examColor))
            Column(modifier = Modifier.weight(1f).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(exam.subjectName, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isPast) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f))
                    if (!isPast && exam.daysUntilExam <= 7) {
                        Surface(shape = RoundedCornerShape(50),
                            color = if (exam.daysUntilExam <= 2) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.tertiaryContainer) {
                            Text(
                                if (exam.daysUntilExam == 0L) "TODAY!" else "${exam.daysUntilExam}d left",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text(dateFormat.format(Date(exam.examDate)), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text(exam.startTime, style = MaterialTheme.typography.bodySmall)
                }
                if (exam.venue.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text(exam.venue, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (exam.notes.isNotBlank()) {
                    Text(exam.notes, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                if (!isPast) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onMarkDone, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp))
                            Text(" Done")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null, Modifier.size(14.dp))
                        Text(" Remove")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExamDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Long, String, String, String, String) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var startTime by remember { mutableStateOf("10:00 AM") }
    var venue by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(defaultSubjectColors[0]) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    date = datePickerState.selectedDateMillis ?: date
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    val dateFormat = remember { SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Exam") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = subject, onValueChange = { subject = it },
                    label = { Text("Subject Name") }, singleLine = true)
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(dateFormat.format(Date(date)))
                }
                OutlinedTextField(value = startTime, onValueChange = { startTime = it },
                    label = { Text("Start Time (e.g. 10:00 AM)") }, singleLine = true)
                OutlinedTextField(value = venue, onValueChange = { venue = it },
                    label = { Text("Venue (optional)") }, singleLine = true)
                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text("Notes (optional)") }, maxLines = 2)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (subject.isNotBlank()) onAdd(subject, date, startTime, venue, notes, selectedColor)
            }) { Text("Add Exam") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

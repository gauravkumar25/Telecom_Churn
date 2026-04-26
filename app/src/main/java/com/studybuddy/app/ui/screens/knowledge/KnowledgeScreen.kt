package com.studybuddy.app.ui.screens.knowledge

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import com.studybuddy.app.data.model.DocumentSummaryTuple
import com.studybuddy.app.data.model.Subject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen(viewModel: KnowledgeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showScanSheet by remember { mutableStateOf(false) }
    var selectedSubjectId by remember { mutableStateOf<Long?>(null) }
    var selectedSubjectName by remember { mutableStateOf("") }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            viewModel.processImage(it, selectedSubjectId, selectedSubjectName)
            showScanSheet = false
        }
    }

    // Auto-clear success after 3 s
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🧠 Knowledge Base", fontWeight = FontWeight.Bold)
                        Text(
                            "${uiState.documents.size} docs · ${uiState.totalChunks} chunks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isProcessing) {
                ExtendedFloatingActionButton(
                    onClick = { showScanSheet = true },
                    icon = { Icon(Icons.Default.DocumentScanner, null) },
                    text = { Text("Scan Chapter") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Processing banner
            AnimatedVisibility(visible = uiState.isProcessing) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(uiState.processingMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // Success banner
            uiState.successMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(msg, modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }

            // Error banner
            uiState.error?.let { err ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(err, modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer)
                        IconButton(onClick = viewModel::clearError) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // How it works explanation (shown only when empty)
            if (uiState.documents.isEmpty() && !uiState.isProcessing) {
                EmptyState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.documents, key = { it.sourceImageUri }) { doc ->
                        DocumentCard(
                            doc = doc,
                            onDelete = { deleteImage ->
                                viewModel.deleteDocument(doc.sourceImageUri, deleteImage)
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Scan sheet: pick subject then launch gallery
    if (showScanSheet) {
        ModalBottomSheet(onDismissRequest = { showScanSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Scan a Chapter Photo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Pick which subject this photo belongs to (optional):",
                    style = MaterialTheme.typography.bodySmall)

                // Subject chips
                if (uiState.subjects.isNotEmpty()) {
                    val untagged = Subject(id = -1L, name = "No tag", colorHex = "#9E9E9E")
                    (listOf(untagged) + uiState.subjects).forEach { subject ->
                        val isSelected = if (subject.id == -1L) selectedSubjectId == null
                        else selectedSubjectId == subject.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (subject.id == -1L) { selectedSubjectId = null; selectedSubjectName = "" }
                                else { selectedSubjectId = subject.id; selectedSubjectName = subject.name }
                            },
                            label = { Text(subject.name) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Pick from Gallery", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DocumentCard(
    doc: DocumentSummaryTuple,
    onDelete: (deleteImageFile: Boolean) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    if (showDeleteDialog) {
        var deleteFile by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove from Knowledge Base?") },
            text = {
                Column {
                    Text("This removes all extracted text chunks for this document.")
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = deleteFile, onCheckedChange = { deleteFile = it })
                        Spacer(Modifier.width(6.dp))
                        Text("Also delete the original image file", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onDelete(deleteFile); showDeleteDialog = false }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Subject colour strip
            Box(
                modifier = Modifier.width(6.dp).fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
            Column(modifier = Modifier.weight(1f).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            doc.chapterName.ifBlank { "Untitled Document" },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (doc.subjectName.isNotBlank()) {
                            Text(doc.subjectName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "${doc.chunkCount} chunks",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    doc.textPreview.take(120) + if (doc.textPreview.length > 120) "…" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Spacer(Modifier.width(4.dp))
                    Text(dateFormat.format(Date(doc.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Remove", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🧠", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(12.dp))
        Text("Knowledge Base is empty", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Scan any chapter photo and the AI will extract all questions, definitions, formulas and examples — then remember them forever.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HowItWorksRow("📸", "Scan a chapter photo")
                HowItWorksRow("🤖", "AI reads every question, formula & definition")
                HowItWorksRow("💾", "Saved forever — image can be deleted")
                HowItWorksRow("💬", "Ask anything — AI finds the exact answer from your notes")
            }
        }
    }
}

@Composable
private fun HowItWorksRow(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

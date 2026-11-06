package com.attendanceradar.app.feature.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendanceradar.app.core.design.AppDimensions
import com.attendanceradar.app.core.design.ConfirmDestructiveDialog
import com.attendanceradar.app.data.backup.CsvExportScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) {
            viewModel.reportPickerCancelled("JSON backup export")
        } else {
            val fileName = resolveDisplayName(context, uri) ?: "75_backup.json"
            viewModel.exportBackupJson(fileName) { content ->
                writeTextToUri(context, uri, content)
            }
        }
    }

    val openRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            viewModel.reportPickerCancelled("Backup restore selection")
        } else {
            val fileName = resolveDisplayName(context, uri) ?: "backup.json"
            val readResult = readTextFromUri(context, uri)
            viewModel.validateSelectedBackupFile(fileName, readResult)
        }
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri == null) {
            viewModel.reportPickerCancelled("CSV export")
        } else {
            val fileName = resolveDisplayName(context, uri) ?: "75_attendance.csv"
            viewModel.exportCsvFile(fileName) { content ->
                writeTextToUri(context, uri, content)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup, Restore & CSV") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AppDimensions.SpaceMD)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceMD)
        ) {
            state.statusBannerMessage?.let { msg ->
                Surface(
                    color = if (state.isStatusError) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.isStatusError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // 1. JSON Full Backup & Restore Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(AppDimensions.SpaceMD),
                    verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceSM)
                ) {
                    Text(
                        text = "Restorable JSON Backup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Creates a complete, versioned UTF-8 JSON file containing all active and archived semesters, subjects, attendance records, and preferences. Only JSON backups can be restored into 75%.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceSM)
                    ) {
                        Button(
                            onClick = {
                                val suggestedName = "75_backup_${LocalDate.now()}.json"
                                createBackupLauncher.launch(suggestedName)
                            },
                            enabled = !state.isBusy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Text("Create backup", modifier = Modifier.padding(start = 6.dp))
                        }

                        OutlinedButton(
                            onClick = {
                                openRestoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            enabled = !state.isBusy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                            Text("Restore backup", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }

            // 2. CSV Spreadsheet Export Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(AppDimensions.SpaceMD),
                    verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceSM)
                ) {
                    Text(
                        text = "CSV Export (For Spreadsheet Analysis)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Exports one row per attendance record (including Present, Absent, and Cancelled sessions) for inspection in spreadsheet tools. Note: CSV files are for analysis only and cannot be used to restore your database.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Export scope",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.selectedCsvScope == CsvExportScope.ACTIVE_SEMESTER,
                            onClick = { viewModel.selectCsvScope(CsvExportScope.ACTIVE_SEMESTER) },
                            label = { Text("Active semester") }
                        )
                        FilterChip(
                            selected = state.selectedCsvScope == CsvExportScope.SELECTED_SEMESTER,
                            onClick = { viewModel.selectCsvScope(CsvExportScope.SELECTED_SEMESTER) },
                            label = { Text("Selected semester") }
                        )
                        FilterChip(
                            selected = state.selectedCsvScope == CsvExportScope.ALL_SEMESTERS,
                            onClick = { viewModel.selectCsvScope(CsvExportScope.ALL_SEMESTERS) },
                            label = { Text("All semesters") }
                        )
                    }

                    if (state.selectedCsvScope == CsvExportScope.SELECTED_SEMESTER && state.semesters.isNotEmpty()) {
                        Text(
                            text = "Choose semester",
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.semesters.forEach { sem ->
                                FilterChip(
                                    selected = state.selectedCsvSemesterId == sem.semesterId,
                                    onClick = { viewModel.selectCsvSemester(sem.semesterId) },
                                    label = { Text(sem.name) }
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val suggestedCsv = "75_attendance_${LocalDate.now()}.csv"
                            createCsvLauncher.launch(suggestedCsv)
                        },
                        enabled = !state.isBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.TableView, contentDescription = null)
                        Text("Export CSV", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimensions.SpaceXL))
        }
    }

    // Validated Restore Preview & Explicit Replacement Confirmation Dialog
    state.pendingRestorePreview?.let { preview ->
        val fileName = state.pendingRestoreFileName ?: "backup.json"
        ConfirmDestructiveDialog(
            title = "Replace local data with backup?",
            message = buildString {
                appendLine("File: $fileName")
                appendLine("Exported at: ${preview.exportedAt}")
                appendLine("• Semesters: ${preview.semesterCount}")
                preview.activeSemesterName?.let { appendLine("• Active semester: $it") }
                appendLine("• Subjects: ${preview.subjectCount}")
                appendLine("• Attendance records: ${preview.attendanceRecordCount}")
                appendLine()
                append("Restoring this backup will completely replace your current local semesters, subjects, and attendance records in one atomic transaction.")
            },
            confirmLabel = "Replace & Restore",
            onConfirm = viewModel::confirmAndExecuteRestore,
            onDismiss = viewModel::cancelPendingRestore,
            isDestructive = true
        )
    }
}

private suspend fun writeTextToUri(context: Context, uri: Uri, content: String): Result<Unit> =
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: error("Unable to open output stream for selected document.")
        }
    }

private fun readTextFromUri(context: Context, uri: Uri): Result<String> =
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
        } ?: error("Unable to open input stream for selected document.")
    }

private fun resolveDisplayName(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        }
    }.getOrNull()
}

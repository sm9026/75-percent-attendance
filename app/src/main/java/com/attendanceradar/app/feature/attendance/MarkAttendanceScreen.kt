package com.attendanceradar.app.feature.attendance

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendanceradar.app.core.design.AppDimensions
import com.attendanceradar.app.core.design.ConfirmDestructiveDialog
import com.attendanceradar.app.core.design.LocalSemanticColors
import com.attendanceradar.app.domain.calculator.AttendanceCalculator
import com.attendanceradar.app.domain.model.AttendanceEntryStatus
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MarkAttendanceScreen(
    viewModel: MarkAttendanceViewModel,
    onNavigateBack: () -> Unit,
    onSavedSuccess: (createdRecordId: String?, message: String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDiscardDialog by remember { mutableStateOf(false) }

    val handleBack = {
        if (state.isDirty) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler(onBack = handleBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Edit Attendance Entry" else "Mark Attendance") },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
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
            // 1. Subject Selector
            Text(
                text = "Subject *",
                style = MaterialTheme.typography.titleSmall
            )
            if (state.availableSubjects.isEmpty()) {
                Text(
                    text = "No active subjects available. Add a subject first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.availableSubjects.forEach { subject ->
                        val selected = state.selectedSubjectId == subject.subjectId
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.selectSubject(subject.subjectId) },
                            label = {
                                Text(
                                    text = subject.shortName?.takeIf { it.isNotBlank() } ?: subject.name
                                )
                            }
                        )
                    }
                }
            }

            // 2. Date Selector (with Today & Yesterday quick shortcuts + ISO input for backdating)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Class date *",
                    style = MaterialTheme.typography.titleSmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { viewModel.updateDateInput(LocalDate.now().minusDays(1).toString()) }) {
                        Text("Yesterday")
                    }
                    TextButton(onClick = viewModel::setTodayDate) {
                        Text("Today")
                    }
                }
            }
            OutlinedTextField(
                value = state.dateInput,
                onValueChange = viewModel::updateDateInput,
                label = { Text("Date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Period Count Stepper (1..20)
            Text(
                text = "Period count (1–20) *",
                style = MaterialTheme.typography.titleSmall
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = viewModel::decrementPeriodCount,
                        enabled = state.periodCount > AttendanceCalculator.MIN_PERIOD_COUNT
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease period count")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${state.periodCount}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (state.periodCount == 1) "conducted period" else "conducted periods",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = viewModel::incrementPeriodCount,
                        enabled = state.periodCount < AttendanceCalculator.MAX_PERIOD_COUNT
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase period count")
                    }
                }
            }

            // 4. Three explicit status choices: Present, Absent, Cancelled (NO preselection)
            Text(
                text = "Attendance status * (select one)",
                style = MaterialTheme.typography.titleSmall
            )
            val semantic = LocalSemanticColors.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceXS)
            ) {
                StatusSelectionButton(
                    label = "Present",
                    selected = state.selectedStatus == AttendanceEntryStatus.PRESENT,
                    activeColor = semantic.onTrackForeground,
                    activeContainer = semantic.onTrackContainer,
                    icon = Icons.Default.CheckCircle,
                    onClick = { viewModel.selectStatus(AttendanceEntryStatus.PRESENT) },
                    modifier = Modifier.weight(1f)
                )
                StatusSelectionButton(
                    label = "Absent",
                    selected = state.selectedStatus == AttendanceEntryStatus.ABSENT,
                    activeColor = semantic.belowTargetForeground,
                    activeContainer = semantic.belowTargetContainer,
                    icon = Icons.Default.Cancel,
                    onClick = { viewModel.selectStatus(AttendanceEntryStatus.ABSENT) },
                    modifier = Modifier.weight(1f)
                )
                StatusSelectionButton(
                    label = "Cancelled",
                    selected = state.selectedStatus == AttendanceEntryStatus.CANCELLED,
                    activeColor = semantic.neutralForeground,
                    activeContainer = semantic.neutralContainer,
                    icon = Icons.Default.RemoveCircleOutline,
                    onClick = { viewModel.selectStatus(AttendanceEntryStatus.CANCELLED) },
                    modifier = Modifier.weight(1f)
                )
            }
            if (state.selectedStatus == AttendanceEntryStatus.CANCELLED) {
                Text(
                    text = "Cancelled sessions are preserved in history and calendar but excluded from your attendance denominator.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 5. Optional Session Label
            OutlinedTextField(
                value = state.sessionLabel,
                onValueChange = viewModel::updateSessionLabel,
                label = { Text("Session label (optional)") },
                placeholder = { Text("e.g., Lecture 1, Morning Lab") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 6. Optional Notes (max 500 chars)
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Notes (optional, ${state.notes.length}/500)") },
                placeholder = { Text("Topic covered, medical note, etc.") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            if (!state.errorMessage.isNullOrBlank()) {
                Text(
                    text = state.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(AppDimensions.SpaceXS))
            Button(
                onClick = {
                    viewModel.attemptSave(
                        ignoreWarnings = false,
                        onSaved = onSavedSuccess
                    )
                },
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isEditMode) "Save changes" else "Save attendance")
            }
            Spacer(modifier = Modifier.height(AppDimensions.SpaceXL))
        }
    }

    // Non-blocking warning for date outside semester range
    state.outOfRangeWarningPrompt?.let { warningText ->
        ConfirmDestructiveDialog(
            title = "Date outside semester range",
            message = warningText,
            confirmLabel = "Save anyway",
            onConfirm = {
                viewModel.attemptSave(
                    ignoreWarnings = true,
                    onSaved = onSavedSuccess
                )
            },
            onDismiss = viewModel::dismissWarnings,
            isDestructive = false
        )
    }

    // Non-blocking warning for possible duplicate session on the same date
    state.duplicateWarningPrompt?.let { dupText ->
        ConfirmDestructiveDialog(
            title = "Possible duplicate session",
            message = dupText,
            confirmLabel = "Save second session",
            onConfirm = {
                viewModel.attemptSave(
                    ignoreWarnings = true,
                    onSaved = onSavedSuccess
                )
            },
            onDismiss = viewModel::dismissWarnings,
            isDestructive = false
        )
    }

    if (showDiscardDialog) {
        ConfirmDestructiveDialog(
            title = "Discard unsaved entry?",
            message = "Your attendance entry has not been saved yet.",
            confirmLabel = "Discard",
            onConfirm = {
                showDiscardDialog = false
                onNavigateBack()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }
}

@Composable
private fun StatusSelectionButton(
    label: String,
    selected: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    activeContainer: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) activeColor else MaterialTheme.colorScheme.outline
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) activeContainer else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) activeColor else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

package com.attendanceradar.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendanceradar.app.core.common.DateFormatters
import com.attendanceradar.app.core.design.AppDimensions
import com.attendanceradar.app.core.design.ConfirmDestructiveDialog
import com.attendanceradar.app.core.design.SectionHeader
import com.attendanceradar.app.domain.model.Semester
import com.attendanceradar.app.domain.model.SemesterStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterManagementScreen(
    viewModel: SemesterManagementViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var semesterToActivate by remember { mutableStateOf<Semester?>(null) }

    val activeSemesters = state.semesters.filter { it.status == SemesterStatus.ACTIVE }
    val archivedSemesters = state.semesters.filter { it.status == SemesterStatus.ARCHIVED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Semesters") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::openCreateSemesterDialog,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Semester") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = AppDimensions.SpaceMD,
                end = AppDimensions.SpaceMD,
                top = AppDimensions.SpaceXS,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceMD)
        ) {
            state.statusMessage?.let { msg ->
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = viewModel::clearStatusMessage) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Active semester")
            }

            items(items = activeSemesters, key = { it.semesterId }) { sem ->
                SemesterItemCard(
                    semester = sem,
                    onEdit = { viewModel.openEditSemesterDialog(sem) },
                    onMakeActive = null
                )
            }

            item {
                SectionHeader(title = "Archived semesters (${archivedSemesters.size})")
            }

            if (archivedSemesters.isEmpty()) {
                item {
                    Text(
                        text = "No archived semesters. When you create or switch to a new semester, the previous semester is archived automatically with all its records intact.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(items = archivedSemesters, key = { it.semesterId }) { sem ->
                    SemesterItemCard(
                        semester = sem,
                        onEdit = { viewModel.openEditSemesterDialog(sem) },
                        onMakeActive = { semesterToActivate = sem }
                    )
                }
            }
        }
    }

    // Confirmation dialog naming both the semester being archived and the semester becoming active
    semesterToActivate?.let { targetSem ->
        val currentActiveName = state.activeSemester?.name ?: "current active semester"
        ConfirmDestructiveDialog(
            title = "Switch active semester?",
            message = "Making \"${targetSem.name}\" active will archive \"$currentActiveName\" in the same transaction. All historical records in both semesters remain preserved.",
            confirmLabel = "Reopen ${targetSem.name}",
            onConfirm = {
                viewModel.switchActiveSemesterConfirmed(targetSem)
                semesterToActivate = null
            },
            onDismiss = { semesterToActivate = null },
            isDestructive = false
        )
    }

    if (state.showCreateOrEditDialog) {
        val form = state.formState
        val currentActiveName = state.activeSemester?.name
        AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = {
                Text(
                    if (form.editingSemesterId != null) "Edit Semester" else "Create New Semester"
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (form.editingSemesterId == null && currentActiveName != null) {
                        Text(
                            text = "Creating a new active semester will archive \"$currentActiveName\" while preserving its history.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = form.name,
                        onValueChange = viewModel::updateFormName,
                        label = { Text("Semester name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = form.academicYearLabel,
                        onValueChange = viewModel::updateFormAcademicYear,
                        label = { Text("Academic year (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = form.startDateInput,
                        onValueChange = viewModel::updateFormStartDate,
                        label = { Text("Start date (YYYY-MM-DD) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = form.endDateInput,
                        onValueChange = viewModel::updateFormEndDate,
                        label = { Text("End date (YYYY-MM-DD, optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = form.targetPercentInput,
                        onValueChange = viewModel::updateFormTargetPercent,
                        label = { Text("Default target (%) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!form.errorMessage.isNullOrBlank()) {
                        Text(
                            text = form.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = viewModel::submitSemesterForm) {
                    Text(if (form.editingSemesterId != null) "Save" else "Create & Activate")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDialog) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SemesterItemCard(
    semester: Semester,
    onEdit: () -> Unit,
    onMakeActive: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = semester.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (semester.isActive) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active semester",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    val dateRange = buildString {
                        semester.academicYearLabel?.let { append("$it • ") }
                        append(DateFormatters.formatDisplayDate(semester.startDate))
                        semester.endDate?.let { append(" – ${DateFormatters.formatDisplayDate(it)}") }
                    }
                    Text(
                        text = dateRange,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Default target: ${DateFormatters.formatBasisPoints(semester.defaultTargetBp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit ${semester.name}")
                }
            }

            if (onMakeActive != null) {
                OutlinedButton(
                    onClick = onMakeActive,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reopen / make active")
                }
            }
        }
    }
}

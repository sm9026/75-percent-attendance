package com.attendanceradar.app.feature.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendanceradar.app.core.design.AppDimensions
import com.attendanceradar.app.core.design.ConfirmDestructiveDialog
import com.attendanceradar.app.core.design.EmptyStateCard
import com.attendanceradar.app.core.design.SectionHeader
import com.attendanceradar.app.core.design.SubjectAttendanceCard
import com.attendanceradar.app.domain.model.SubjectWithAttendance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    viewModel: SubjectsViewModel,
    onOpenSubjectDetail: (String) -> Unit,
    onOpenSubjectForm: (String?) -> Unit,
    onOpenMarkAttendance: (String) -> Unit,
    onOpenCalculator: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var subjectPendingDelete by remember { mutableStateOf<SubjectWithAttendance?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subjects") }
            )
        },
        floatingActionButton = {
            if (state.overview != null) {
                ExtendedFloatingActionButton(
                    onClick = { onOpenSubjectForm(null) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Subject") }
                )
            }
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val overview = state.overview
        if (overview == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(AppDimensions.SpaceMD),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    title = "No active semester",
                    message = "Create or select a semester in Settings first."
                )
            }
            return@Scaffold
        }

        val query = state.searchQuery.trim().lowercase()
        val filteredActive = overview.activeSubjects.filter {
            query.isEmpty() ||
                it.subject.name.lowercase().contains(query) ||
                (it.subject.code?.lowercase()?.contains(query) == true)
        }
        val filteredArchived = overview.archivedSubjects.filter {
            query.isEmpty() ||
                it.subject.name.lowercase().contains(query) ||
                (it.subject.code?.lowercase()?.contains(query) == true)
        }

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
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    label = { Text("Search subjects by name or code") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                SectionHeader(title = "Active subjects (${filteredActive.size})")
            }

            if (filteredActive.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = if (query.isEmpty()) "No active subjects" else "No matching active subjects",
                        message = if (query.isEmpty()) {
                            "Add a subject to start tracking attendance for ${overview.semester.name}."
                        } else {
                            "Try clearing the search query."
                        },
                        actionLabel = if (query.isEmpty()) "Add Subject" else null,
                        onAction = if (query.isEmpty()) ({ onOpenSubjectForm(null) }) else null
                    )
                }
            } else {
                items(
                    items = filteredActive,
                    key = { it.subject.subjectId }
                ) { item ->
                    SubjectAttendanceCard(
                        item = item,
                        displayPrecision = state.preferences.displayPrecision,
                        onClick = { onOpenSubjectDetail(item.subject.subjectId) },
                        onMarkAttendance = { onOpenMarkAttendance(item.subject.subjectId) },
                        onOpenCalculator = { onOpenCalculator(item.subject.subjectId) },
                        onEditSubject = { onOpenSubjectForm(item.subject.subjectId) },
                        onToggleArchive = { viewModel.toggleArchive(item) },
                        onDeleteSubject = { subjectPendingDelete = item }
                    )
                }
            }

            if (overview.archivedSubjects.isNotEmpty()) {
                item {
                    OutlinedButton(
                        onClick = viewModel::toggleShowArchivedSection,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (state.showArchivedSection) {
                                "Hide archived subjects (${filteredArchived.size})"
                            } else {
                                "Show archived subjects (${filteredArchived.size})"
                            }
                        )
                    }
                }

                if (state.showArchivedSection) {
                    items(
                        items = filteredArchived,
                        key = { it.subject.subjectId }
                    ) { item ->
                        SubjectAttendanceCard(
                            item = item,
                            displayPrecision = state.preferences.displayPrecision,
                            onClick = { onOpenSubjectDetail(item.subject.subjectId) },
                            onMarkAttendance = { onOpenMarkAttendance(item.subject.subjectId) },
                            onOpenCalculator = { onOpenCalculator(item.subject.subjectId) },
                            onEditSubject = { onOpenSubjectForm(item.subject.subjectId) },
                            onToggleArchive = { viewModel.toggleArchive(item) },
                            onDeleteSubject = { subjectPendingDelete = item }
                        )
                    }
                }
            }
        }
    }

    subjectPendingDelete?.let { pending ->
        ConfirmDestructiveDialog(
            title = "Delete ${pending.subject.name}?",
            message = "Deleting \"${pending.subject.name}\" will permanently remove it and all ${pending.totalRecordCount} of its attendance record(s). Archive it instead if you want to keep its history.",
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.deleteSubjectConfirmed(pending.subject.subjectId)
                subjectPendingDelete = null
            },
            onDismiss = { subjectPendingDelete = null },
            alternativeLabel = if (!pending.subject.isArchived) "Archive instead" else null,
            onAlternative = if (!pending.subject.isArchived) {
                {
                    viewModel.toggleArchive(pending)
                    subjectPendingDelete = null
                }
            } else null
        )
    }
}

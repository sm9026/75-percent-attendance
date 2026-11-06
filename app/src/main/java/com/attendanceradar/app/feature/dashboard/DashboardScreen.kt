package com.attendanceradar.app.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendanceradar.app.core.design.AppDimensions
import com.attendanceradar.app.core.design.AttendanceHistoryRow
import com.attendanceradar.app.core.design.BrandLogoSymbol
import com.attendanceradar.app.core.design.ConfirmDestructiveDialog
import com.attendanceradar.app.core.design.EmptyStateCard
import com.attendanceradar.app.core.design.MetricTile
import com.attendanceradar.app.core.design.OverallAttendanceSummaryCard
import com.attendanceradar.app.core.design.SectionHeader
import com.attendanceradar.app.core.design.SubjectAttendanceCard
import com.attendanceradar.app.domain.calculator.RecoveryRequirement
import com.attendanceradar.app.domain.model.AttendanceRecord
import com.attendanceradar.app.domain.model.SubjectWithAttendance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenMarkAttendance: (subjectId: String?, recordId: String?) -> Unit,
    onOpenSubjectDetail: (String) -> Unit,
    onOpenSubjectForm: (String?) -> Unit,
    onOpenCalculator: (String?) -> Unit,
    onOpenSemesterManagement: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var subjectPendingDelete by remember { mutableStateOf<SubjectWithAttendance?>(null) }
    var recordPendingDelete by remember { mutableStateOf<AttendanceRecord?>(null) }

    when (val current = state) {
        DashboardUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        DashboardUiState.NoActiveSemester -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppDimensions.SpaceMD),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    title = "No active semester",
                    message = "Create or activate a semester to track your attendance in 75%.",
                    actionLabel = "Manage semesters",
                    onAction = onOpenSemesterManagement
                )
            }
        }
        is DashboardUiState.Active -> {
            val overview = current.overview
            val summary = overview.overallSummary
            val precision = current.preferences.displayPrecision
            val subjectNameMap = remember(overview.subjects) {
                overview.subjects.associate { it.subject.subjectId to it.subject.name }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                BrandLogoSymbol(size = 32.dp, strokeWidth = 4.dp)
                                Column {
                                    Text(
                                        text = overview.semester.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    overview.semester.academicYearLabel?.let { label ->
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            TextButton(onClick = onOpenSemesterManagement) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Switch semester"
                                )
                                Text(
                                    text = "Semesters",
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    )
                },
                floatingActionButton = {
                    if (overview.activeSubjects.isNotEmpty()) {
                        ExtendedFloatingActionButton(
                            onClick = { onOpenMarkAttendance(null, null) },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("Mark Attendance") }
                        )
                    }
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
                    item {
                        OverallAttendanceSummaryCard(
                            title = "Semester overall",
                            summary = summary,
                            displayPrecision = precision,
                            onOpenCalculator = { onOpenCalculator(null) }
                        )
                    }

                    item {
                        val bufferTileLabel: String
                        val bufferTileValue: String
                        val bufferTileSub: String

                        if (summary.isNoRecords) {
                            bufferTileLabel = "Buffer"
                            bufferTileValue = "N/A"
                            bufferTileSub = "No records yet"
                        } else if (summary.meetsTarget) {
                            bufferTileLabel = "Safe buffer"
                            bufferTileValue = "${summary.safeAdditionalAbsences ?: 0L}"
                            bufferTileSub = "periods you can miss"
                        } else {
                            bufferTileLabel = "To recover"
                            when (val rec = summary.recoveryRequirement) {
                                is RecoveryRequirement.ImpossibleAt100Percent -> {
                                    bufferTileValue = "N/A"
                                    bufferTileSub = "100% unreachable"
                                }
                                is RecoveryRequirement.Possible -> {
                                    bufferTileValue = "${rec.periods}"
                                    bufferTileSub = "present periods needed"
                                }
                                RecoveryRequirement.NotNeeded -> {
                                    bufferTileValue = "0"
                                    bufferTileSub = "On target"
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceXS)
                        ) {
                            MetricTile(
                                label = "Absent",
                                value = "${summary.absentPeriods}",
                                subtitle = "periods missed",
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Cancelled",
                                value = "${summary.cancelledPeriods}",
                                subtitle = "excluded",
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = bufferTileLabel,
                                value = bufferTileValue,
                                subtitle = bufferTileSub,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        SectionHeader(
                            title = "Your subjects (${overview.activeSubjects.size})",
                            actionLabel = "Add subject",
                            onAction = { onOpenSubjectForm(null) }
                        )
                    }

                    if (overview.activeSubjects.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "No active subjects yet",
                                message = "Add your semester subjects to begin recording period attendance.",
                                actionLabel = "Add Subject",
                                onAction = { onOpenSubjectForm(null) }
                            )
                        }
                    } else {
                        items(
                            items = overview.activeSubjects,
                            key = { it.subject.subjectId }
                        ) { item ->
                            SubjectAttendanceCard(
                                item = item,
                                displayPrecision = precision,
                                onClick = { onOpenSubjectDetail(item.subject.subjectId) },
                                onMarkAttendance = { onOpenMarkAttendance(item.subject.subjectId, null) },
                                onOpenCalculator = { onOpenCalculator(item.subject.subjectId) },
                                onEditSubject = { onOpenSubjectForm(item.subject.subjectId) },
                                onToggleArchive = { viewModel.toggleSubjectArchive(item) },
                                onDeleteSubject = { subjectPendingDelete = item }
                            )
                        }
                    }

                    if (overview.archivedSubjects.isNotEmpty()) {
                        item {
                            Text(
                                text = "${overview.archivedSubjects.size} archived subject(s) remain included in semester overall totals.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (current.recentRecords.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            SectionHeader(title = "Recent activity")
                        }
                        items(
                            items = current.recentRecords,
                            key = { it.recordId }
                        ) { rec ->
                            AttendanceHistoryRow(
                                record = rec,
                                subjectName = subjectNameMap[rec.subjectId] ?: "Subject",
                                onEdit = { onOpenMarkAttendance(rec.subjectId, rec.recordId) },
                                onDelete = { recordPendingDelete = rec }
                            )
                        }
                    }
                }
            }
        }
    }

    subjectPendingDelete?.let { pending ->
        ConfirmDestructiveDialog(
            title = "Delete ${pending.subject.name}?",
            message = "Deleting \"${pending.subject.name}\" will permanently remove it and its ${pending.totalRecordCount} attendance record(s) from this semester. If you want to hide this subject from daily entry while keeping its history, archive it instead.",
            confirmLabel = "Delete permanently",
            onConfirm = {
                viewModel.deleteSubjectConfirmed(pending.subject.subjectId)
                subjectPendingDelete = null
            },
            onDismiss = { subjectPendingDelete = null },
            alternativeLabel = if (!pending.subject.isArchived) "Archive instead" else null,
            onAlternative = if (!pending.subject.isArchived) {
                {
                    viewModel.toggleSubjectArchive(pending)
                    subjectPendingDelete = null
                }
            } else null
        )
    }

    recordPendingDelete?.let { rec ->
        ConfirmDestructiveDialog(
            title = "Delete attendance entry?",
            message = "Delete the ${rec.status.name.lowercase()} entry (${rec.periodCount} period(s)) on ${rec.classDate}?",
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.deleteRecordConfirmed(rec.recordId)
                recordPendingDelete = null
            },
            onDismiss = { recordPendingDelete = null }
        )
    }
}

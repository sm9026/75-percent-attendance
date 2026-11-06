package com.attendanceradar.app.feature.subjects

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendanceradar.app.core.common.DateFormatters
import com.attendanceradar.app.core.design.AppDimensions
import com.attendanceradar.app.core.design.AttendanceHistoryRow
import com.attendanceradar.app.core.design.AttendanceProgressBar
import com.attendanceradar.app.core.design.ConfirmDestructiveDialog
import com.attendanceradar.app.core.design.EmptyStateCard
import com.attendanceradar.app.core.design.HealthStatusChip
import com.attendanceradar.app.core.design.MetricTile
import com.attendanceradar.app.core.design.SectionHeader
import com.attendanceradar.app.core.design.SubjectPalette
import com.attendanceradar.app.core.design.formatGuidanceMessage
import com.attendanceradar.app.domain.model.AttendanceRecord
import com.attendanceradar.app.domain.model.CumulativeTrendPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    viewModel: SubjectDetailViewModel,
    onNavigateBack: () -> Unit,
    onOpenMarkAttendance: (subjectId: String, recordId: String?) -> Unit,
    onOpenEditSubject: (String) -> Unit,
    onOpenCalculator: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var recordPendingDelete by remember { mutableStateOf<AttendanceRecord?>(null) }
    var showDeleteSubjectDialog by remember { mutableStateOf(false) }

    val subjectItem = state.subjectWithAttendance

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(subjectItem?.subject?.name ?: "Subject Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (subjectItem != null) {
                        IconButton(onClick = { onOpenEditSubject(subjectItem.subject.subjectId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit subject")
                        }
                        IconButton(onClick = { showDeleteSubjectDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete subject",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
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

        if (subjectItem == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(AppDimensions.SpaceMD),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    title = "Subject not found",
                    message = "This subject may have been deleted."
                )
            }
            return@Scaffold
        }

        val subject = subjectItem.subject
        val summary = subjectItem.summary
        val precision = state.preferences.displayPrecision
        val accentColor = SubjectPalette.parseColorOrDefault(subject.colorHex)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(AppDimensions.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceMD)
        ) {
            item {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                                Text(
                                    text = buildString {
                                        subject.code?.takeIf { it.isNotBlank() }?.let { append("$it • ") }
                                        append(
                                            "Target ${DateFormatters.formatBasisPoints(subjectItem.effectiveTargetBp, precision)}"
                                        )
                                        append(if (subjectItem.isTargetOverridden) " (custom override)" else " (semester default)")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HealthStatusChip(status = summary.status)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = if (summary.isNoRecords) {
                                    "N/A"
                                } else {
                                    DateFormatters.formatPercentage(summary.attendancePercentage, precision)
                                },
                                style = MaterialTheme.typography.displayMedium
                            )
                            Text(
                                text = if (summary.isNoRecords) {
                                    "No classes recorded"
                                } else {
                                    "${summary.presentPeriods} / ${summary.conductedPeriods} conducted periods"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AttendanceProgressBar(summary = summary)

                        Text(
                            text = formatGuidanceMessage(summary, precision),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceSM)
                        ) {
                            if (!subject.isArchived) {
                                Button(
                                    onClick = { onOpenMarkAttendance(subject.subjectId, null) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Text("Mark Attendance", modifier = Modifier.padding(start = 6.dp))
                                }
                            }
                            OutlinedButton(
                                onClick = { onOpenCalculator(subject.subjectId) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Calculate, contentDescription = null)
                                Text("What-If", modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceXS)
                ) {
                    MetricTile(
                        label = "Present",
                        value = "${summary.presentPeriods}",
                        subtitle = "periods",
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Absent",
                        value = "${summary.absentPeriods}",
                        subtitle = "periods",
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Cancelled",
                        value = "${summary.cancelledPeriods}",
                        subtitle = "excluded",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                SectionHeader(title = "Cumulative attendance trend")
                if (state.trendPoints.size < 2) {
                    EmptyStateCard(
                        title = "Not enough sessions for trend chart",
                        message = "Record at least two conducted sessions for ${subject.name} to view its cumulative attendance curve over time."
                    )
                } else {
                    CumulativeTrendChartCard(
                        points = state.trendPoints,
                        targetBp = subjectItem.effectiveTargetBp,
                        lineColor = accentColor,
                        displayPrecision = precision
                    )
                }
            }

            item {
                SectionHeader(title = "Attendance history (${state.historyRecords.size})")
            }

            if (state.historyRecords.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No attendance recorded yet",
                        message = "Tap Mark Attendance to record your first session for ${subject.name}.",
                        actionLabel = if (!subject.isArchived) "Mark Attendance" else null,
                        onAction = if (!subject.isArchived) {
                            { onOpenMarkAttendance(subject.subjectId, null) }
                        } else null
                    )
                }
            } else {
                items(
                    items = state.historyRecords,
                    key = { it.recordId }
                ) { record ->
                    AttendanceHistoryRow(
                        record = record,
                        onEdit = { onOpenMarkAttendance(subject.subjectId, record.recordId) },
                        onDelete = { recordPendingDelete = record }
                    )
                }
            }
        }
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

    if (showDeleteSubjectDialog && subjectItem != null) {
        ConfirmDestructiveDialog(
            title = "Delete ${subjectItem.subject.name}?",
            message = "Deleting \"${subjectItem.subject.name}\" will permanently remove it and all ${subjectItem.totalRecordCount} of its attendance record(s).",
            confirmLabel = "Delete permanently",
            onConfirm = {
                showDeleteSubjectDialog = false
                viewModel.deleteSubjectConfirmed(subjectItem.subject.subjectId, onNavigateBack)
            },
            onDismiss = { showDeleteSubjectDialog = false }
        )
    }
}

@Composable
private fun CumulativeTrendChartCard(
    points: List<CumulativeTrendPoint>,
    targetBp: Int,
    lineColor: androidx.compose.ui.graphics.Color,
    displayPrecision: Int
) {
    val firstPoint = points.first()
    val lastPoint = points.last()
    val targetPct = targetBp.toFloat() / 100f
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val targetLineColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceSM)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Start: ${DateFormatters.formatShortDate(firstPoint.date)} (${DateFormatters.formatPercentage(firstPoint.cumulativePercentage, displayPrecision)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Latest: ${DateFormatters.formatShortDate(lastPoint.date)} (${DateFormatters.formatPercentage(lastPoint.cumulativePercentage, displayPrecision)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .semantics {
                        contentDescription = "Cumulative attendance trend chart with ${points.size} conducted sessions, latest ${DateFormatters.formatPercentage(lastPoint.cumulativePercentage, displayPrecision)}"
                    }
            ) {
                val w = size.width
                val h = size.height
                val padVertical = 12.dp.toPx()
                val usableH = h - 2 * padVertical

                // Draw horizontal reference grid lines (0%, 50%, 100%)
                listOf(0f, 50f, 100f).forEach { pct ->
                    val y = padVertical + usableH * (1f - pct / 100f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw target threshold dashed line
                val targetY = padVertical + usableH * (1f - (targetPct / 100f).coerceIn(0f, 1f))
                drawLine(
                    color = targetLineColor,
                    start = Offset(0f, targetY),
                    end = Offset(w, targetY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )

                // Draw cumulative trend path
                val path = Path()
                points.forEachIndexed { index, pt ->
                    val x = if (points.size == 1) w / 2f else (index.toFloat() / (points.size - 1).toFloat()) * w
                    val y = padVertical + usableH * (1f - (pt.cumulativePercentage.toFloat() / 100f).coerceIn(0f, 1f))
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw data points
                points.forEachIndexed { index, pt ->
                    val x = if (points.size == 1) w / 2f else (index.toFloat() / (points.size - 1).toFloat()) * w
                    val y = padVertical + usableH * (1f - (pt.cumulativePercentage.toFloat() / 100f).coerceIn(0f, 1f))
                    drawCircle(
                        color = lineColor,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }

            Text(
                text = "Dashed line marks target (${DateFormatters.formatBasisPoints(targetBp, displayPrecision)}). Cancelled sessions are excluded.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

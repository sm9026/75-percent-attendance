package com.attendanceradar.app.core.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.attendanceradar.app.core.common.DateFormatters
import com.attendanceradar.app.domain.calculator.AttendanceSummary
import com.attendanceradar.app.domain.calculator.RecoveryRequirement
import com.attendanceradar.app.domain.model.AttendanceEntryStatus
import com.attendanceradar.app.domain.model.AttendanceHealthStatus
import com.attendanceradar.app.domain.model.AttendanceRecord
import com.attendanceradar.app.domain.model.SubjectWithAttendance

/**
 * Minimalist symbol-only 75% logo: three-quarters (270 degrees) of a circular ring
 * with a clean 90-degree gap in the top-left quadrant and rounded endpoints.
 */
@Composable
fun BrandLogoSymbol(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    strokeWidth: Dp = 5.dp,
    backgroundColor: Color = Color(0xFF0B1221),
    trackColor: Color = Color(0xFF1C263B),
    accentColor: Color = Color(0xFF1EE6A8)
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.24f))
            .background(backgroundColor)
            .semantics { contentDescription = "75% app symbol" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.62f)) {
            val strokePx = strokeWidth.toPx()
            val arcDiameter = this.size.minDimension - strokePx
            val topLeft = Offset(
                (this.size.width - arcDiameter) / 2f,
                (this.size.height - arcDiameter) / 2f
            )
            val arcSize = Size(arcDiameter, arcDiameter)

            // Full subtle background ring track
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Butt)
            )
            // 270-degree ring starting from 12 o'clock (-90f) clockwise to 9 o'clock (180f)
            drawArc(
                color = accentColor,
                startAngle = -90f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun HealthStatusChip(
    status: AttendanceHealthStatus,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val (label, fgColor, bgColor, icon) = when (status) {
        AttendanceHealthStatus.ON_TRACK -> Quadruple(
            "On track",
            semantic.onTrackForeground,
            semantic.onTrackContainer,
            Icons.Default.CheckCircle
        )
        AttendanceHealthStatus.AT_RISK -> Quadruple(
            "Limited buffer",
            semantic.atRiskForeground,
            semantic.atRiskContainer,
            Icons.Default.Warning
        )
        AttendanceHealthStatus.BELOW_TARGET -> Quadruple(
            "Below target",
            semantic.belowTargetForeground,
            semantic.belowTargetContainer,
            Icons.Default.Error
        )
        AttendanceHealthStatus.NO_RECORDS -> Quadruple(
            "No records",
            semantic.neutralForeground,
            semantic.neutralContainer,
            Icons.Default.RemoveCircleOutline
        )
    }

    Surface(
        modifier = modifier.semantics { contentDescription = "Attendance status: $label" },
        color = bgColor,
        shape = RoundedCornerShape(AppDimensions.ChipCornerRadius)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = fgColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun EntryStatusChip(
    status: AttendanceEntryStatus,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val (label, fgColor, bgColor, icon) = when (status) {
        AttendanceEntryStatus.PRESENT -> Quadruple(
            "Present",
            semantic.onTrackForeground,
            semantic.onTrackContainer,
            Icons.Default.CheckCircle
        )
        AttendanceEntryStatus.ABSENT -> Quadruple(
            "Absent",
            semantic.belowTargetForeground,
            semantic.belowTargetContainer,
            Icons.Default.Cancel
        )
        AttendanceEntryStatus.CANCELLED -> Quadruple(
            "Cancelled",
            semantic.neutralForeground,
            semantic.neutralContainer,
            Icons.Default.RemoveCircleOutline
        )
    }

    Surface(
        modifier = modifier.semantics { contentDescription = "Entry status: $label" },
        color = bgColor,
        shape = RoundedCornerShape(AppDimensions.ChipCornerRadius)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = fgColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun AttendanceProgressBar(
    summary: AttendanceSummary,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val barColor = when (summary.status) {
        AttendanceHealthStatus.ON_TRACK -> semantic.onTrackForeground
        AttendanceHealthStatus.AT_RISK -> semantic.atRiskForeground
        AttendanceHealthStatus.BELOW_TARGET -> semantic.belowTargetForeground
        AttendanceHealthStatus.NO_RECORDS -> semantic.neutralForeground
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val markerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)

    val progressFraction = if (summary.conductedPeriods > 0L) {
        (summary.presentPeriods.toFloat() / summary.conductedPeriods.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val targetFraction = (summary.targetBp.toFloat() / 10000f).coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimensions.ProgressBarHeight)
            .clip(RoundedCornerShape(4.dp))
            .semantics {
                contentDescription = if (summary.conductedPeriods == 0L) {
                    "No classes recorded"
                } else {
                    "Attendance progress ${DateFormatters.formatPercentage(summary.attendancePercentage, 1)} against target ${DateFormatters.formatBasisPoints(summary.targetBp)}"
                }
            }
    ) {
        drawRect(color = trackColor, size = size)
        if (summary.conductedPeriods > 0L && progressFraction > 0f) {
            drawRect(
                color = barColor,
                size = Size(width = size.width * progressFraction, height = size.height)
            )
        }
        val targetX = (size.width * targetFraction).coerceIn(1f, size.width - 1f)
        drawLine(
            color = markerColor,
            start = Offset(targetX, 0f),
            end = Offset(targetX, size.height),
            strokeWidth = 2.dp.toPx()
        )
    }
}

/**
 * Formats an unambiguous human-readable buffer or recovery message from an [AttendanceSummary].
 */
fun formatGuidanceMessage(summary: AttendanceSummary, precision: Int = 1): String {
    if (summary.conductedPeriods == 0L) {
        return "No attendance recorded yet."
    }
    val targetLabel = DateFormatters.formatBasisPoints(summary.targetBp, precision)
    return if (summary.meetsTarget) {
        val safe = summary.safeAdditionalAbsences ?: 0L
        if (safe == 0L) {
            "At target ($targetLabel). You cannot miss the next period without falling below $targetLabel."
        } else {
            "You can miss up to ${DateFormatters.pluralizeMorePeriods(safe)} and remain at/above $targetLabel."
        }
    } else {
        when (val rec = summary.recoveryRequirement) {
            is RecoveryRequirement.ImpossibleAt100Percent ->
                "Currently below 100% target. Cannot reach 100% through future attendance alone."
            is RecoveryRequirement.Possible ->
                "Below target. Attend ${DateFormatters.pluralizePeriods(rec.periods.toInt())} consecutively to reach $targetLabel."
            RecoveryRequirement.NotNeeded ->
                "No attendance recorded yet."
        }
    }
}

@Composable
fun OverallAttendanceSummaryCard(
    title: String,
    summary: AttendanceSummary,
    displayPrecision: Int,
    onOpenCalculator: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HealthStatusChip(status = summary.status)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = if (summary.isNoRecords) {
                            "N/A"
                        } else {
                            DateFormatters.formatPercentage(summary.attendancePercentage, displayPrecision)
                        },
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface
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

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Target: ${DateFormatters.formatBasisPoints(summary.targetBp, displayPrecision)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onOpenCalculator) {
                        Text("What-if calculator")
                    }
                }
            }

            AttendanceProgressBar(summary = summary)

            Text(
                text = formatGuidanceMessage(summary, displayPrecision),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SubjectAttendanceCard(
    item: SubjectWithAttendance,
    displayPrecision: Int,
    onClick: () -> Unit,
    onMarkAttendance: () -> Unit,
    onOpenCalculator: () -> Unit,
    onEditSubject: () -> Unit,
    onToggleArchive: () -> Unit,
    onDeleteSubject: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val subject = item.subject
    val summary = item.summary
    val accentColor = SubjectPalette.parseColorOrDefault(subject.colorHex)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceXS)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val metaParts = buildList {
                            subject.code?.takeIf { it.isNotBlank() }?.let { add(it) }
                            add(
                                "Target ${DateFormatters.formatBasisPoints(item.effectiveTargetBp, displayPrecision)}" +
                                    if (item.isTargetOverridden) " (custom)" else ""
                            )
                            if (subject.isArchived) add("Archived")
                        }
                        Text(
                            text = metaParts.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HealthStatusChip(status = summary.status)
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options for ${subject.name}"
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (!subject.isArchived) {
                                DropdownMenuItem(
                                    text = { Text("Mark attendance") },
                                    onClick = {
                                        menuExpanded = false
                                        onMarkAttendance()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Calculator & What-if") },
                                onClick = {
                                    menuExpanded = false
                                    onOpenCalculator()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit subject") },
                                onClick = {
                                    menuExpanded = false
                                    onEditSubject()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (subject.isArchived) "Unarchive subject" else "Archive subject") },
                                onClick = {
                                    menuExpanded = false
                                    onToggleArchive()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Delete subject",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteSubject()
                                }
                            )
                        }
                    }
                }
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
                        DateFormatters.formatPercentage(summary.attendancePercentage, displayPrecision)
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (summary.isNoRecords) {
                        "No classes recorded"
                    } else {
                        "${summary.presentPeriods} / ${summary.conductedPeriods} periods" +
                            if (summary.cancelledPeriods > 0L) " (+${summary.cancelledPeriods} cancelled)" else ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AttendanceProgressBar(summary = summary)

            Text(
                text = formatGuidanceMessage(summary, displayPrecision),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AttendanceHistoryRow(
    record: AttendanceRecord,
    subjectName: String? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = subjectName ?: DateFormatters.formatDisplayDate(record.classDate),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    EntryStatusChip(status = record.status)
                }

                val detailParts = buildList {
                    if (subjectName != null) {
                        add(DateFormatters.formatDisplayDate(record.classDate))
                    }
                    add(DateFormatters.pluralizePeriods(record.periodCount))
                    record.sessionLabel?.takeIf { it.isNotBlank() }?.let { add(it) }
                }
                Text(
                    text = detailParts.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!record.notes.isNullOrBlank()) {
                    Text(
                        text = record.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit attendance entry on ${record.classDate}"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete attendance entry on ${record.classDate}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.SpaceXL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceSM)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = onAction) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
    }
}

@Composable
fun ConfirmDestructiveDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    alternativeLabel: String? = null,
    onAlternative: (() -> Unit)? = null,
    isDestructive: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(text = confirmLabel)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (alternativeLabel != null && onAlternative != null) {
                    OutlinedButton(onClick = onAlternative) {
                        Text(text = alternativeLabel)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun ColorSwatchRow(
    selectedHex: String,
    onSelectHex: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SubjectPalette.Colors.forEach { hex ->
            val color = SubjectPalette.parseColorOrDefault(hex)
            val isSelected = selectedHex.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (isSelected) {
                            Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelectHex(hex) }
                    .semantics { contentDescription = "Select colour $hex" }
            )
        }
    }
}

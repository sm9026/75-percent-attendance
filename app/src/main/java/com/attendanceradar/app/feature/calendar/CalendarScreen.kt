package com.attendanceradar.app.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendanceradar.app.core.common.DateFormatters
import com.attendanceradar.app.core.design.AppDimensions
import com.attendanceradar.app.core.design.AttendanceHistoryRow
import com.attendanceradar.app.core.design.ConfirmDestructiveDialog
import com.attendanceradar.app.core.design.EmptyStateCard
import com.attendanceradar.app.core.design.LocalSemanticColors
import com.attendanceradar.app.core.design.SectionHeader
import com.attendanceradar.app.domain.model.AttendanceRecord
import com.attendanceradar.app.domain.model.CalendarDayMarker
import com.attendanceradar.app.domain.model.WeekStartDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onOpenMarkAttendanceForDate: (dateIso: String, recordId: String?, subjectId: String?) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var recordPendingDelete by remember { mutableStateOf<AttendanceRecord?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                actions = {
                    TextButton(onClick = viewModel::jumpToToday) {
                        Text("Today")
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
                    message = "Set up or activate a semester first to view calendar history."
                )
            }
            return@Scaffold
        }

        val subjectMap = remember(overview.subjects) {
            overview.subjects.associate { it.subject.subjectId to it.subject.name }
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
                        // Month Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = viewModel::previousMonth) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous month"
                                )
                            }
                            Text(
                                text = DateFormatters.formatMonthYear(state.displayedMonth),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(onClick = viewModel::nextMonth) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next month"
                                )
                            }
                        }

                        MonthGrid(
                            yearMonth = state.displayedMonth,
                            weekStartsOn = state.preferences.weekStartsOn,
                            selectedDate = state.selectedDate,
                            dayMarkers = state.dayMarkers,
                            onSelectDate = viewModel::selectDate
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        CalendarLegendRow()
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        title = DateFormatters.formatDisplayDate(state.selectedDate),
                        modifier = Modifier.weight(1f)
                    )
                    if (overview.activeSubjects.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                onOpenMarkAttendanceForDate(state.selectedDate.toString(), null, null)
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("Add entry", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }

            if (state.selectedDateRecords.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No records entered for this date",
                        message = "Unmarked dates are neutral and are never assumed to be Present or Absent."
                    )
                }
            } else {
                items(
                    items = state.selectedDateRecords,
                    key = { it.recordId }
                ) { record ->
                    AttendanceHistoryRow(
                        record = record,
                        subjectName = subjectMap[record.subjectId] ?: "Subject",
                        onEdit = {
                            onOpenMarkAttendanceForDate(
                                record.classDate.toString(),
                                record.recordId,
                                record.subjectId
                            )
                        },
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
}

@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    weekStartsOn: WeekStartDay,
    selectedDate: LocalDate,
    dayMarkers: Map<LocalDate, CalendarDayMarker>,
    onSelectDate: (LocalDate) -> Unit
) {
    val weekdayOrder = remember(weekStartsOn) {
        if (weekStartsOn == WeekStartDay.MONDAY) {
            listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
            )
        } else {
            listOf(
                DayOfWeek.SUNDAY,
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY
            )
        }
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        weekdayOrder.forEach { dow ->
            Text(
                text = dow.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }

    val firstDayOfMonth = yearMonth.atDay(1)
    val leadingEmptyCount = weekdayOrder.indexOf(firstDayOfMonth.dayOfWeek).coerceAtLeast(0)
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalCells = ((leadingEmptyCount + daysInMonth + 6) / 7) * 7
    val today = LocalDate.now()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (weekIndex in 0 until (totalCells / 7)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (dayIndex in 0 until 7) {
                    val cellPosition = weekIndex * 7 + dayIndex
                    val dayNumber = cellPosition - leadingEmptyCount + 1
                    if (dayNumber !in 1..daysInMonth) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    } else {
                        val cellDate = yearMonth.atDay(dayNumber)
                        val marker = dayMarkers[cellDate] ?: CalendarDayMarker.NONE
                        val isSelected = cellDate == selectedDate
                        val isToday = cellDate == today

                        CalendarDayCell(
                            date = cellDate,
                            marker = marker,
                            isSelected = isSelected,
                            isToday = isToday,
                            onClick = { onSelectDate(cellDate) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    marker: CalendarDayMarker,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val markerDesc = when (marker) {
        CalendarDayMarker.ALL_PRESENT -> "only Present recorded"
        CalendarDayMarker.ALL_ABSENT -> "only Absent recorded"
        CalendarDayMarker.MIXED_PRESENT_ABSENT -> "both Present and Absent recorded"
        CalendarDayMarker.ALL_CANCELLED -> "all Cancelled"
        CalendarDayMarker.NONE -> "no records"
    }

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${DateFormatters.formatDisplayDate(date)}: $markerDesc"
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${date.dayOfMonth}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.height(3.dp))
            DayMarkerIndicator(marker = marker, semantic = semantic)
        }
    }
}

@Composable
private fun DayMarkerIndicator(
    marker: CalendarDayMarker,
    semantic: com.attendanceradar.app.core.design.SemanticAttendanceColors
) {
    when (marker) {
        CalendarDayMarker.NONE -> {
            Spacer(modifier = Modifier.size(8.dp))
        }
        CalendarDayMarker.ALL_PRESENT -> {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(semantic.onTrackForeground)
            )
        }
        CalendarDayMarker.ALL_ABSENT -> {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(semantic.belowTargetForeground)
            )
        }
        CalendarDayMarker.MIXED_PRESENT_ABSENT -> {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(semantic.onTrackForeground)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(semantic.belowTargetForeground)
                )
            }
        }
        CalendarDayMarker.ALL_CANCELLED -> {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .border(1.5.dp, semantic.neutralForeground, CircleShape)
            )
        }
    }
}

@Composable
private fun CalendarLegendRow() {
    val semantic = LocalSemanticColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(label = "Present", marker = CalendarDayMarker.ALL_PRESENT, semantic = semantic)
        LegendItem(label = "Absent", marker = CalendarDayMarker.ALL_ABSENT, semantic = semantic)
        LegendItem(label = "Mixed", marker = CalendarDayMarker.MIXED_PRESENT_ABSENT, semantic = semantic)
        LegendItem(label = "Cancelled", marker = CalendarDayMarker.ALL_CANCELLED, semantic = semantic)
    }
}

@Composable
private fun LegendItem(
    label: String,
    marker: CalendarDayMarker,
    semantic: com.attendanceradar.app.core.design.SemanticAttendanceColors
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DayMarkerIndicator(marker = marker, semantic = semantic)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

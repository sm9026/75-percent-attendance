package com.attendanceradar.app.feature.insights

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendanceradar.app.core.common.DateFormatters
import com.attendanceradar.app.core.design.AppDimensions
import com.attendanceradar.app.core.design.AttendanceProgressBar
import com.attendanceradar.app.core.design.EmptyStateCard
import com.attendanceradar.app.core.design.MetricTile
import com.attendanceradar.app.core.design.SectionHeader
import com.attendanceradar.app.core.design.SubjectPalette
import com.attendanceradar.app.domain.model.DateBucketAttendance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insights") }
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
                    message = "Create or select a semester first to view attendance insights."
                )
            }
            return@Scaffold
        }

        val overall = overview.overallSummary
        val precision = state.preferences.displayPrecision

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
            // 1. Period Totals Row
            item {
                SectionHeader(title = "${overview.semester.name} totals")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceXS)
                ) {
                    MetricTile(
                        label = "Overall rate",
                        value = DateFormatters.formatPercentage(overall.attendancePercentage, precision),
                        subtitle = "${overall.presentPeriods}/${overall.conductedPeriods} periods",
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Absent",
                        value = "${overall.absentPeriods}",
                        subtitle = "periods",
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Cancelled",
                        value = "${overall.cancelledPeriods}",
                        subtitle = "excluded",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Date-range statistics (Current month & Last 7 days)
            item {
                SectionHeader(title = "Date-range rates")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceSM)
                ) {
                    val monthSum = state.currentMonthSummary
                    val weekSum = state.recentSevenDaysSummary
                    MetricTile(
                        label = "This month",
                        value = if (monthSum == null || monthSum.isNoRecords) {
                            "N/A"
                        } else {
                            DateFormatters.formatPercentage(monthSum.attendancePercentage, precision)
                        },
                        subtitle = if (monthSum == null || monthSum.isNoRecords) {
                            "No sessions recorded"
                        } else {
                            "${monthSum.presentPeriods}/${monthSum.conductedPeriods} periods"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Last 7 days",
                        value = if (weekSum == null || weekSum.isNoRecords) {
                            "N/A"
                        } else {
                            DateFormatters.formatPercentage(weekSum.attendancePercentage, precision)
                        },
                        subtitle = if (weekSum == null || weekSum.isNoRecords) {
                            "No sessions recorded"
                        } else {
                            "${weekSum.presentPeriods}/${weekSum.conductedPeriods} periods"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Best and Lowest-Attendance Subjects (only among subjects with conducted periods)
            item {
                SectionHeader(title = "Highlights")
                val best = state.bestSubject
                val lowest = state.lowestSubject
                if (best == null || lowest == null) {
                    EmptyStateCard(
                        title = "No conducted classes yet",
                        message = "Record Present or Absent sessions to compare best and lowest-attendance subjects.",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceSM)
                    ) {
                        MetricTile(
                            label = "Highest attendance",
                            value = DateFormatters.formatPercentage(best.summary.attendancePercentage, precision),
                            subtitle = "${best.subject.name} (${best.summary.presentPeriods}/${best.summary.conductedPeriods})",
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Lowest attendance",
                            value = DateFormatters.formatPercentage(lowest.summary.attendancePercentage, precision),
                            subtitle = "${lowest.subject.name} (${lowest.summary.presentPeriods}/${lowest.summary.conductedPeriods})",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Subject-wise Comparison Chart
            item {
                SectionHeader(title = "Subject comparison")
                if (overview.subjects.isEmpty()) {
                    EmptyStateCard(
                        title = "No subjects in semester",
                        message = "Add subjects to see a comparison chart.",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(AppDimensions.SpaceMD),
                            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceSM)
                        ) {
                            overview.subjects.forEach { item ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = item.subject.name + if (item.subject.isArchived) " (Archived)" else "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = if (item.summary.isNoRecords) {
                                                "N/A (0/0)"
                                            } else {
                                                "${DateFormatters.formatPercentage(item.summary.attendancePercentage, precision)} (${item.summary.presentPeriods}/${item.summary.conductedPeriods})"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    AttendanceProgressBar(summary = item.summary)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Weekly / Monthly Trend Buckets (showing explicit gaps for buckets with 0 conducted periods)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Period trend",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.bucketMode == InsightsBucketMode.WEEKLY,
                            onClick = { viewModel.selectBucketMode(InsightsBucketMode.WEEKLY) },
                            label = { Text("Weekly") }
                        )
                        FilterChip(
                            selected = state.bucketMode == InsightsBucketMode.MONTHLY,
                            onClick = { viewModel.selectBucketMode(InsightsBucketMode.MONTHLY) },
                            label = { Text("Monthly") }
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(AppDimensions.SpaceMD),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        state.buckets.forEach { bucket ->
                            BucketRow(bucket = bucket, displayPrecision = precision)
                        }
                        Text(
                            text = "Periods with no conducted classes are shown as 'No sessions' rather than 0%.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BucketRow(
    bucket: DateBucketAttendance,
    displayPrecision: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = bucket.label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (bucket.conductedPeriods == 0L || bucket.percentage == null) {
                    "No sessions"
                } else {
                    "${DateFormatters.formatPercentage(bucket.percentage, displayPrecision)} (${bucket.presentPeriods}/${bucket.conductedPeriods})"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (bucket.conductedPeriods > 0L && bucket.percentage != null) {
                val fraction = (bucket.percentage / 100.0).toFloat().coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

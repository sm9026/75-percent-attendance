package com.attendanceradar.app.feature.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendanceradar.app.core.common.DateFormatters
import com.attendanceradar.app.core.design.AppDimensions
import com.attendanceradar.app.core.design.AttendanceProgressBar
import com.attendanceradar.app.core.design.EmptyStateCard
import com.attendanceradar.app.core.design.HealthStatusChip
import com.attendanceradar.app.core.design.formatGuidanceMessage
import com.attendanceradar.app.domain.calculator.RecoveryRequirement

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculator & What-If") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.futurePresent > 0L || state.futureAbsent > 0L) {
                        TextButton(onClick = viewModel::resetScenario) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Text("Reset", modifier = Modifier.padding(start = 4.dp))
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

        val overview = state.overview
        val projection = state.projection
        if (overview == null || projection == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(AppDimensions.SpaceMD),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    title = "No active semester",
                    message = "Create or select a semester first to use the attendance calculator."
                )
            }
            return@Scaffold
        }

        val precision = state.preferences.displayPrecision
        val baseline = projection.baseline
        val projected = projection.projected

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AppDimensions.SpaceMD)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceMD)
        ) {
            Text(
                text = "Select scope",
                style = MaterialTheme.typography.titleSmall
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selectedSubjectId == null,
                    onClick = { viewModel.selectScope(null) },
                    label = { Text("Semester overall (${overview.semester.name})") }
                )
                overview.subjects.forEach { item ->
                    FilterChip(
                        selected = state.selectedSubjectId == item.subject.subjectId,
                        onClick = { viewModel.selectScope(item.subject.subjectId) },
                        label = {
                            Text(item.subject.shortName?.takeIf { it.isNotBlank() } ?: item.subject.name)
                        }
                    )
                }
            }

            // 1. Actual Current Attendance Card
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
                        Text(
                            text = "Current attendance (Actual)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        HealthStatusChip(status = baseline.status)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = if (baseline.isNoRecords) {
                                "N/A"
                            } else {
                                DateFormatters.formatPercentage(baseline.attendancePercentage, precision)
                            },
                            style = MaterialTheme.typography.displayMedium
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "P: ${baseline.presentPeriods} • A: ${baseline.absentPeriods} • C: ${baseline.conductedPeriods}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Target: ${DateFormatters.formatBasisPoints(baseline.targetBp, precision)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AttendanceProgressBar(summary = baseline)

                    // Primary guidance state per UI_UX_SPEC.md section 9
                    val primaryGuidance = when {
                        baseline.isNoRecords ->
                            "Record your first class to calculate your buffer."
                        baseline.meetsTarget -> {
                            val safe = baseline.safeAdditionalAbsences ?: 0L
                            "You can miss up to $safe more period(s) and remain at/above ${DateFormatters.formatBasisPoints(baseline.targetBp, precision)}."
                        }
                        else -> when (val rec = baseline.recoveryRequirement) {
                            is RecoveryRequirement.ImpossibleAt100Percent ->
                                "Cannot reach 100% through future attendance alone."
                            is RecoveryRequirement.Possible ->
                                "Attend ${rec.periods} consecutive period(s) to reach ${DateFormatters.formatBasisPoints(baseline.targetBp, precision)}."
                            RecoveryRequirement.NotNeeded ->
                                "Record your first class to calculate your buffer."
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = primaryGuidance,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // 2. What-If Hypothetical Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(AppDimensions.SpaceMD),
                    verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceMD)
                ) {
                    Text(
                        text = "What-If Scenario Inputs",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Simulate hypothetical future periods. These numbers do not modify your saved attendance history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ScenarioCounterRow(
                        label = "Hypothetical future Present periods",
                        value = state.futurePresent,
                        onDecrement = viewModel::decrementFuturePresent,
                        onIncrement = viewModel::incrementFuturePresent,
                        onTextChange = viewModel::updateFuturePresentText
                    )

                    ScenarioCounterRow(
                        label = "Hypothetical future Absent periods",
                        value = state.futureAbsent,
                        onDecrement = viewModel::decrementFutureAbsent,
                        onIncrement = viewModel::incrementFutureAbsent,
                        onTextChange = viewModel::updateFutureAbsentText
                    )
                }
            }

            // 3. Projected Output Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
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
                            text = "Projected attendance (What-if)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        HealthStatusChip(status = projected.status)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = if (projected.isNoRecords) {
                                "N/A"
                            } else {
                                DateFormatters.formatPercentage(projected.attendancePercentage, precision)
                            },
                            style = MaterialTheme.typography.displayMedium
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Projected P: ${projected.presentPeriods} (+${state.futurePresent})",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Projected A: ${projected.absentPeriods} (+${state.futureAbsent})",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Projected C: ${projected.conductedPeriods} periods",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    AttendanceProgressBar(summary = projected)

                    Text(
                        text = "Projected outcome: ${formatGuidanceMessage(projected, precision)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimensions.SpaceXL))
        }
    }
}

@Composable
private fun ScenarioCounterRow(
    label: String,
    value: Long,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onTextChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDecrement,
                enabled = value > 0L
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease $label")
            }
            OutlinedTextField(
                value = value.toString(),
                onValueChange = onTextChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = onIncrement) {
                Icon(Icons.Default.Add, contentDescription = "Increase $label")
            }
        }
    }
}

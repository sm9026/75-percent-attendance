package com.attendanceradar.app.feature.settings

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendanceradar.app.R
import com.attendanceradar.app.core.common.DateFormatters
import com.attendanceradar.app.core.design.AppDimensions
import com.attendanceradar.app.core.design.BrandLogoSymbol
import com.attendanceradar.app.domain.model.ThemeMode
import com.attendanceradar.app.domain.model.WeekStartDay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenSemesterManagement: () -> Unit,
    onOpenBackupRestore: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.preferences

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
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
            // 1. Semesters Section
            SettingsGroupCard(title = "Semesters") {
                Text(
                    text = state.activeSemester?.let {
                        "Active: ${it.name} (Target ${DateFormatters.formatBasisPoints(it.defaultTargetBp, prefs.displayPrecision)})"
                    } ?: "No active semester",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onOpenSemesterManagement,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.School, contentDescription = null)
                    Text(
                        text = "Manage Semesters (${state.totalSemestersCount})",
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    )
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }

            // 2. Data Backup, Restore & CSV Export
            SettingsGroupCard(title = "Data & Portability") {
                Text(
                    text = "Export a full JSON backup to restore later, or export CSV records for spreadsheet analysis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onOpenBackupRestore,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Text(
                        text = "Backup, Restore & CSV Export",
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    )
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }

            // 3. Appearance
            SettingsGroupCard(title = "Appearance") {
                Text(
                    text = "Theme mode",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = prefs.themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                        label = { Text("Follow system") }
                    )
                    FilterChip(
                        selected = prefs.themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                        label = { Text("Light") }
                    )
                    FilterChip(
                        selected = prefs.themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                        label = { Text("Dark") }
                    )
                }
            }

            // 4. Attendance Display & Defaults
            SettingsGroupCard(title = "Attendance Display") {
                Text(
                    text = "Percentage decimal precision",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 1, 2).forEach { dec ->
                        FilterChip(
                            selected = prefs.displayPrecision == dec,
                            onClick = { viewModel.setDisplayPrecision(dec) },
                            label = {
                                Text(
                                    when (dec) {
                                        0 -> "75%"
                                        1 -> "75.0%"
                                        else -> "75.00%"
                                    }
                                )
                            }
                        )
                    }
                }
                Text(
                    text = "Target checks always use exact integer basis points regardless of display rounding.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 5. Calendar Preferences
            SettingsGroupCard(title = "Calendar") {
                Text(
                    text = "First day of week",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = prefs.weekStartsOn == WeekStartDay.MONDAY,
                        onClick = { viewModel.setWeekStartsOn(WeekStartDay.MONDAY) },
                        label = { Text("Monday") }
                    )
                    FilterChip(
                        selected = prefs.weekStartsOn == WeekStartDay.SUNDAY,
                        onClick = { viewModel.setWeekStartsOn(WeekStartDay.SUNDAY) },
                        label = { Text("Sunday") }
                    )
                }
            }

            // 6. About & Privacy
            SettingsGroupCard(title = "About & Privacy") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BrandLogoSymbol(size = 44.dp, strokeWidth = 5.dp)
                    Column {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Version 1.0.0 • Offline Local Utility",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = stringResource(id = R.string.privacy_statement),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    content: @Composable () -> Unit
) {
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

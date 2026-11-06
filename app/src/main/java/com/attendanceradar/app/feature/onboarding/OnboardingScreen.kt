package com.attendanceradar.app.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendanceradar.app.core.design.AppDimensions
import com.attendanceradar.app.core.design.BrandLogoSymbol
import com.attendanceradar.app.core.design.ColorSwatchRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onCompleted: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (state.step) {
                            OnboardingStep.WELCOME -> "Welcome to 75%"
                            OnboardingStep.SEMESTER_DETAILS -> "Set up your semester"
                            OnboardingStep.ADD_SUBJECTS -> "Add your subjects"
                        }
                    )
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
            when (state.step) {
                OnboardingStep.WELCOME -> {
                    Spacer(modifier = Modifier.height(AppDimensions.SpaceMD))
                    BrandLogoSymbol(size = 84.dp, strokeWidth = 8.dp)
                    Text(
                        text = "75%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Track subject-wise period attendance, calculate your exact safe-absence buffer, and stay at or above your requirement.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(AppDimensions.SpaceMD),
                            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceSM),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "100% Local & Private",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Your attendance records stay on this device only. No account, no cloud sync, and no internet connection required.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AppDimensions.SpaceSM))
                    Button(
                        onClick = viewModel::proceedToSemesterDetails,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Set up semester")
                    }
                }

                OnboardingStep.SEMESTER_DETAILS -> {
                    Text(
                        text = "Create your active semester. You can add or switch semesters at any time in Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = state.semesterName,
                        onValueChange = viewModel::updateSemesterName,
                        label = { Text("Semester name *") },
                        placeholder = { Text("e.g., Semester 5") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = state.academicYearLabel,
                        onValueChange = viewModel::updateAcademicYearLabel,
                        label = { Text("Academic year (optional)") },
                        placeholder = { Text("e.g., 2026–27") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = state.startDateInput,
                        onValueChange = viewModel::updateStartDateInput,
                        label = { Text("Start date (YYYY-MM-DD) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = state.endDateInput,
                        onValueChange = viewModel::updateEndDateInput,
                        label = { Text("End date (YYYY-MM-DD, optional)") },
                        placeholder = { Text("Leave blank if open-ended") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = state.targetPercentInput,
                        onValueChange = viewModel::updateTargetPercentInput,
                        label = { Text("Default attendance target (%) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!state.errorMessage.isNullOrBlank()) {
                        Text(
                            text = state.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = viewModel::submitSemesterDetails,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue to subjects")
                    }
                }

                OnboardingStep.ADD_SUBJECTS -> {
                    Text(
                        text = "Add subjects for this semester now, or skip and add them later from the dashboard.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = state.subjectNameInput,
                        onValueChange = viewModel::updateSubjectName,
                        label = { Text("Subject name *") },
                        placeholder = { Text("e.g., Operating Systems") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceSM)
                    ) {
                        OutlinedTextField(
                            value = state.subjectCodeInput,
                            onValueChange = viewModel::updateSubjectCode,
                            label = { Text("Code (optional)") },
                            placeholder = { Text("CS301") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.subjectShortNameInput,
                            onValueChange = viewModel::updateSubjectShortName,
                            label = { Text("Short name (opt)") },
                            placeholder = { Text("OS") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "Accent colour",
                        style = MaterialTheme.typography.labelLarge
                    )
                    ColorSwatchRow(
                        selectedHex = state.subjectColorHex,
                        onSelectHex = viewModel::updateSubjectColorHex
                    )

                    if (!state.errorMessage.isNullOrBlank()) {
                        Text(
                            text = state.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    OutlinedButton(
                        onClick = viewModel::addSubjectDuringOnboarding,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add subject to list")
                    }

                    if (state.addedSubjects.isNotEmpty()) {
                        Text(
                            text = "Added subjects (${state.addedSubjects.size})",
                            style = MaterialTheme.typography.titleSmall
                        )
                        state.addedSubjects.forEach { sub ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = sub.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    sub.code?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AppDimensions.SpaceSM))
                    Button(
                        onClick = { viewModel.finishOnboarding(onCompleted) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (state.addedSubjects.isEmpty()) "Do this later" else "Finish setup"
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(AppDimensions.SpaceXL))
        }
    }
}

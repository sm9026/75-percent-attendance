package com.attendanceradar.app.feature.subjects

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendanceradar.app.core.common.DateFormatters
import com.attendanceradar.app.core.design.AppDimensions
import com.attendanceradar.app.core.design.ColorSwatchRow
import com.attendanceradar.app.core.design.ConfirmDestructiveDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectFormScreen(
    viewModel: SubjectFormViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDiscardDialog by remember { mutableStateOf(false) }

    val handleBack = {
        if (state.isDirty) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler(onBack = handleBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Edit Subject" else "Add Subject") },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
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
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateName,
                label = { Text("Subject name *") },
                placeholder = { Text("e.g., Computer Networks") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceSM)
            ) {
                OutlinedTextField(
                    value = state.code,
                    onValueChange = viewModel::updateCode,
                    label = { Text("Course code (optional)") },
                    placeholder = { Text("CS302") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.shortName,
                    onValueChange = viewModel::updateShortName,
                    label = { Text("Short name (optional)") },
                    placeholder = { Text("CN") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Subject accent colour",
                style = MaterialTheme.typography.titleSmall
            )
            ColorSwatchRow(
                selectedHex = state.colorHex,
                onSelectHex = viewModel::updateColorHex
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Custom target override",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = if (state.useCustomTarget) {
                            "Overriding semester default"
                        } else {
                            "Using semester default (${DateFormatters.formatBasisPoints(state.semesterDefaultTargetBp)})"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.useCustomTarget,
                    onCheckedChange = viewModel::updateUseCustomTarget
                )
            }

            if (state.useCustomTarget) {
                OutlinedTextField(
                    value = state.customTargetPercentInput,
                    onValueChange = viewModel::updateCustomTargetInput,
                    label = { Text("Custom target (%) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.isEditMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Archived subject",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Hides from quick-entry selector while preserving history and semester totals.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.isArchived,
                        onCheckedChange = viewModel::updateArchived
                    )
                }
            }

            if (!state.errorMessage.isNullOrBlank()) {
                Text(
                    text = state.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(AppDimensions.SpaceXS))
            Button(
                onClick = { viewModel.saveSubject(onNavigateBack) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isEditMode) "Save changes" else "Create subject")
            }
            Spacer(modifier = Modifier.height(AppDimensions.SpaceXL))
        }
    }

    if (showDiscardDialog) {
        ConfirmDestructiveDialog(
            title = "Discard unsaved changes?",
            message = "You have unsaved changes on this subject form.",
            confirmLabel = "Discard",
            onConfirm = {
                showDiscardDialog = false
                onNavigateBack()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }
}

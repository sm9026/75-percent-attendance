package com.attendanceradar.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendanceradar.app.core.common.AppResult
import com.attendanceradar.app.data.backup.BackupPreviewSummary
import com.attendanceradar.app.data.backup.BackupService
import com.attendanceradar.app.data.backup.CsvExportScope
import com.attendanceradar.app.data.backup.CsvExporter
import com.attendanceradar.app.domain.model.Semester
import com.attendanceradar.app.domain.repository.SemesterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupRestoreUiState(
    val semesters: List<Semester> = emptyList(),
    val selectedCsvScope: CsvExportScope = CsvExportScope.ACTIVE_SEMESTER,
    val selectedCsvSemesterId: String? = null,
    val pendingRestorePreview: BackupPreviewSummary? = null,
    val pendingRestoreFileName: String? = null,
    val isBusy: Boolean = false,
    val statusBannerMessage: String? = null,
    val isStatusError: Boolean = false
)

class BackupRestoreViewModel(
    semesterRepository: SemesterRepository,
    private val backupService: BackupService,
    private val csvExporter: CsvExporter
) : ViewModel() {

    private val internalState = MutableStateFlow(BackupRestoreUiState())

    val uiState: StateFlow<BackupRestoreUiState> = combine(
        semesterRepository.observeAllSemesters(),
        internalState
    ) { allSems, state ->
        state.copy(
            semesters = allSems,
            selectedCsvSemesterId = state.selectedCsvSemesterId ?: allSems.firstOrNull()?.semesterId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BackupRestoreUiState()
    )

    fun selectCsvScope(scope: CsvExportScope) {
        internalState.update { it.copy(selectedCsvScope = scope) }
    }

    fun selectCsvSemester(semesterId: String) {
        internalState.update { it.copy(selectedCsvSemesterId = semesterId) }
    }

    fun exportBackupJson(
        fileNameLabel: String,
        writeToUri: suspend (String) -> Result<Unit>
    ) {
        if (internalState.value.isBusy) return
        internalState.update { it.copy(isBusy = true, statusBannerMessage = null) }

        viewModelScope.launch {
            when (val res = backupService.createBackupJson()) {
                is AppResult.Success -> {
                    val writeRes = writeToUri(res.data)
                    writeRes.fold(
                        onSuccess = {
                            internalState.update {
                                it.copy(
                                    isBusy = false,
                                    statusBannerMessage = "Saved JSON backup to $fileNameLabel.",
                                    isStatusError = false
                                )
                            }
                        },
                        onFailure = { err ->
                            internalState.update {
                                it.copy(
                                    isBusy = false,
                                    statusBannerMessage = "Failed to write backup file: ${err.message ?: "I/O error"}",
                                    isStatusError = true
                                )
                            }
                        }
                    )
                }
                is AppResult.Error -> {
                    internalState.update {
                        it.copy(
                            isBusy = false,
                            statusBannerMessage = res.message,
                            isStatusError = true
                        )
                    }
                }
            }
        }
    }

    fun validateSelectedBackupFile(
        fileNameLabel: String,
        rawJsonResult: Result<String>
    ) {
        if (internalState.value.isBusy) return
        rawJsonResult.fold(
            onSuccess = { rawJson ->
                when (val validation = BackupService.parseAndValidateBackupJson(rawJson)) {
                    is AppResult.Success -> {
                        internalState.update {
                            it.copy(
                                pendingRestorePreview = validation.data,
                                pendingRestoreFileName = fileNameLabel,
                                statusBannerMessage = null,
                                isStatusError = false
                            )
                        }
                    }
                    is AppResult.Error -> {
                        internalState.update {
                            it.copy(
                                pendingRestorePreview = null,
                                pendingRestoreFileName = null,
                                statusBannerMessage = "Restore rejected ($fileNameLabel): ${validation.message}",
                                isStatusError = true
                            )
                        }
                    }
                }
            },
            onFailure = { err ->
                internalState.update {
                    it.copy(
                        pendingRestorePreview = null,
                        pendingRestoreFileName = null,
                        statusBannerMessage = "Could not read selected file: ${err.message ?: "I/O error"}",
                        isStatusError = true
                    )
                }
            }
        )
    }

    fun cancelPendingRestore() {
        internalState.update {
            it.copy(
                pendingRestorePreview = null,
                pendingRestoreFileName = null
            )
        }
    }

    fun confirmAndExecuteRestore() {
        val preview = internalState.value.pendingRestorePreview ?: return
        val fileName = internalState.value.pendingRestoreFileName ?: "backup.json"
        if (internalState.value.isBusy) return

        internalState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            when (val res = backupService.executeConfirmedRestore(preview)) {
                is AppResult.Success -> {
                    internalState.update {
                        it.copy(
                            isBusy = false,
                            pendingRestorePreview = null,
                            pendingRestoreFileName = null,
                            statusBannerMessage = "Restored ${preview.semesterCount} semester(s), ${preview.subjectCount} subject(s), and ${preview.attendanceRecordCount} record(s) from $fileName.",
                            isStatusError = false
                        )
                    }
                }
                is AppResult.Error -> {
                    internalState.update {
                        it.copy(
                            isBusy = false,
                            pendingRestorePreview = null,
                            pendingRestoreFileName = null,
                            statusBannerMessage = res.message,
                            isStatusError = true
                        )
                    }
                }
            }
        }
    }

    fun exportCsvFile(
        fileNameLabel: String,
        writeToUri: suspend (String) -> Result<Unit>
    ) {
        val state = uiState.value
        if (state.isBusy) return
        internalState.update { it.copy(isBusy = true, statusBannerMessage = null) }

        viewModelScope.launch {
            when (
                val res = csvExporter.exportCsv(
                    scope = state.selectedCsvScope,
                    selectedSemesterId = state.selectedCsvSemesterId
                )
            ) {
                is AppResult.Success -> {
                    val writeRes = writeToUri(res.data)
                    writeRes.fold(
                        onSuccess = {
                            internalState.update {
                                it.copy(
                                    isBusy = false,
                                    statusBannerMessage = "Exported CSV records to $fileNameLabel.",
                                    isStatusError = false
                                )
                            }
                        },
                        onFailure = { err ->
                            internalState.update {
                                it.copy(
                                    isBusy = false,
                                    statusBannerMessage = "Failed to write CSV file: ${err.message ?: "I/O error"}",
                                    isStatusError = true
                                )
                            }
                        }
                    )
                }
                is AppResult.Error -> {
                    internalState.update {
                        it.copy(
                            isBusy = false,
                            statusBannerMessage = res.message,
                            isStatusError = true
                        )
                    }
                }
            }
        }
    }

    fun reportPickerCancelled(operationName: String) {
        internalState.update {
            it.copy(
                statusBannerMessage = "$operationName cancelled.",
                isStatusError = false
            )
        }
    }
}

package com.attendanceradar.app.feature.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendanceradar.app.core.common.AppResult
import com.attendanceradar.app.core.common.DateFormatters
import com.attendanceradar.app.domain.calculator.AttendanceCalculator
import com.attendanceradar.app.domain.model.AttendanceEntryStatus
import com.attendanceradar.app.domain.model.Semester
import com.attendanceradar.app.domain.model.Subject
import com.attendanceradar.app.domain.repository.AttendanceRepository
import com.attendanceradar.app.domain.repository.SemesterRepository
import com.attendanceradar.app.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MarkAttendanceUiState(
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val editingRecordId: String? = null,
    val activeSemester: Semester? = null,
    val availableSubjects: List<Subject> = emptyList(),
    val selectedSubjectId: String? = null,
    val dateInput: String = LocalDate.now().toString(),
    val periodCount: Int = 1,
    /** Explicit null default: NO attendance status is preselected for new entries. */
    val selectedStatus: AttendanceEntryStatus? = null,
    val sessionLabel: String = "",
    val notes: String = "",
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    val duplicateWarningPrompt: String? = null,
    val outOfRangeWarningPrompt: String? = null,
    val errorMessage: String? = null
) {
    val parsedDate: LocalDate?
        get() = DateFormatters.parseIsoOrNull(dateInput)

    val canSubmit: Boolean
        get() = !isSaving &&
            !selectedSubjectId.isNullOrBlank() &&
            parsedDate != null &&
            periodCount in AttendanceCalculator.MIN_PERIOD_COUNT..AttendanceCalculator.MAX_PERIOD_COUNT &&
            selectedStatus != null &&
            activeSemester?.isActive == true
}

class MarkAttendanceViewModel(
    initialSubjectId: String?,
    editingRecordId: String?,
    initialDateIso: String?,
    private val semesterRepository: SemesterRepository,
    private val subjectRepository: SubjectRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MarkAttendanceUiState(
            isEditMode = !editingRecordId.isNullOrBlank(),
            editingRecordId = editingRecordId,
            dateInput = initialDateIso?.takeIf { DateFormatters.parseIsoOrNull(it) != null }
                ?: LocalDate.now().toString()
        )
    )
    val uiState: StateFlow<MarkAttendanceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val activeSemester = semesterRepository.getActiveSemester()
            val allSubjects = if (activeSemester != null) {
                subjectRepository.observeSubjectsForSemester(activeSemester.semesterId).first()
            } else {
                emptyList()
            }
            val activeSubjects = allSubjects.filter { !it.isArchived }

            if (!editingRecordId.isNullOrBlank()) {
                val existing = attendanceRepository.getRecordById(editingRecordId)
                if (existing != null) {
                    // Include the record's subject even if it was later archived so editing still works
                    val subjectChoices = if (activeSubjects.any { it.subjectId == existing.subjectId }) {
                        activeSubjects
                    } else {
                        allSubjects.filter { !it.isArchived || it.subjectId == existing.subjectId }
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditMode = true,
                            editingRecordId = existing.recordId,
                            activeSemester = activeSemester,
                            availableSubjects = subjectChoices,
                            selectedSubjectId = existing.subjectId,
                            dateInput = existing.classDate.toString(),
                            periodCount = existing.periodCount,
                            selectedStatus = existing.status,
                            sessionLabel = existing.sessionLabel ?: "",
                            notes = existing.notes ?: "",
                            isDirty = false
                        )
                    }
                    return@launch
                }
            }

            val resolvedSubjectId = initialSubjectId?.takeIf { id ->
                activeSubjects.any { it.subjectId == id }
            } ?: activeSubjects.firstOrNull()?.subjectId

            _uiState.update {
                it.copy(
                    isLoading = false,
                    activeSemester = activeSemester,
                    availableSubjects = activeSubjects,
                    selectedSubjectId = resolvedSubjectId,
                    selectedStatus = null // Never preselect a status on a new entry
                )
            }
        }
    }

    fun selectSubject(subjectId: String) {
        _uiState.update {
            it.copy(
                selectedSubjectId = subjectId,
                isDirty = true,
                errorMessage = null,
                duplicateWarningPrompt = null
            )
        }
    }

    fun updateDateInput(dateIso: String) {
        _uiState.update {
            it.copy(
                dateInput = dateIso,
                isDirty = true,
                errorMessage = null,
                duplicateWarningPrompt = null,
                outOfRangeWarningPrompt = null
            )
        }
    }

    fun setTodayDate() {
        updateDateInput(LocalDate.now().toString())
    }

    fun incrementPeriodCount() {
        _uiState.update {
            val next = (it.periodCount + 1).coerceAtMost(AttendanceCalculator.MAX_PERIOD_COUNT)
            it.copy(periodCount = next, isDirty = true, errorMessage = null)
        }
    }

    fun decrementPeriodCount() {
        _uiState.update {
            val prev = (it.periodCount - 1).coerceAtLeast(AttendanceCalculator.MIN_PERIOD_COUNT)
            it.copy(periodCount = prev, isDirty = true, errorMessage = null)
        }
    }

    fun updatePeriodCountText(value: String) {
        val parsed = value.trim().toIntOrNull() ?: return
        if (parsed in AttendanceCalculator.MIN_PERIOD_COUNT..AttendanceCalculator.MAX_PERIOD_COUNT) {
            _uiState.update { it.copy(periodCount = parsed, isDirty = true, errorMessage = null) }
        }
    }

    fun selectStatus(status: AttendanceEntryStatus) {
        _uiState.update { it.copy(selectedStatus = status, isDirty = true, errorMessage = null) }
    }

    fun updateSessionLabel(value: String) {
        if (value.length <= 80) {
            _uiState.update {
                it.copy(
                    sessionLabel = value,
                    isDirty = true,
                    errorMessage = null,
                    duplicateWarningPrompt = null
                )
            }
        }
    }

    fun updateNotes(value: String) {
        if (value.length <= 500) {
            _uiState.update { it.copy(notes = value, isDirty = true, errorMessage = null) }
        }
    }

    fun dismissWarnings() {
        _uiState.update {
            it.copy(
                duplicateWarningPrompt = null,
                outOfRangeWarningPrompt = null
            )
        }
    }

    fun attemptSave(
        ignoreWarnings: Boolean = false,
        onSaved: (createdRecordId: String?, summaryMessage: String) -> Unit
    ) {
        val state = _uiState.value
        if (state.isSaving) return

        val subjectId = state.selectedSubjectId
        if (subjectId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Please select a subject.") }
            return
        }

        val date = state.parsedDate
        if (date == null) {
            _uiState.update { it.copy(errorMessage = "Enter a valid date in YYYY-MM-DD format.") }
            return
        }

        val status = state.selectedStatus
        if (status == null) {
            _uiState.update { it.copy(errorMessage = "Select Present, Absent, or Cancelled.") }
            return
        }

        if (state.periodCount !in AttendanceCalculator.MIN_PERIOD_COUNT..AttendanceCalculator.MAX_PERIOD_COUNT) {
            _uiState.update { it.copy(errorMessage = "Period count must be between 1 and 20.") }
            return
        }

        val semester = state.activeSemester
        if (semester == null || !semester.isActive) {
            _uiState.update { it.copy(errorMessage = "Active semester required to record attendance.") }
            return
        }

        viewModelScope.launch {
            if (!ignoreWarnings) {
                // Check date range against semester startDate / endDate
                val beforeStart = date.isBefore(semester.startDate)
                val afterEnd = semester.endDate != null && date.isAfter(semester.endDate)
                if (beforeStart || afterEnd) {
                    val rangeDesc = if (semester.endDate != null) {
                        "${semester.startDate} to ${semester.endDate}"
                    } else {
                        "starting ${semester.startDate}"
                    }
                    _uiState.update {
                        it.copy(
                            outOfRangeWarningPrompt = "The selected date ($date) is outside ${semester.name}'s configured date range ($rangeDesc). Do you want to save this entry anyway?"
                        )
                    }
                    return@launch
                }

                // Check possible duplicate session on the same subject & date
                val dupCheck = attendanceRepository.checkPotentialDuplicate(
                    subjectId = subjectId,
                    classDate = date,
                    sessionLabel = state.sessionLabel,
                    excludeRecordId = state.editingRecordId
                )
                if (dupCheck.warningMessage != null) {
                    _uiState.update { it.copy(duplicateWarningPrompt = dupCheck.warningMessage) }
                    return@launch
                }
            }

            _uiState.update {
                it.copy(
                    isSaving = true,
                    errorMessage = null,
                    duplicateWarningPrompt = null,
                    outOfRangeWarningPrompt = null
                )
            }

            val result = if (state.isEditMode && state.editingRecordId != null) {
                attendanceRepository.updateAttendanceRecord(
                    recordId = state.editingRecordId,
                    subjectId = subjectId,
                    classDate = date,
                    periodCount = state.periodCount,
                    status = status,
                    sessionLabel = state.sessionLabel,
                    notes = state.notes
                )
            } else {
                attendanceRepository.addAttendanceRecord(
                    subjectId = subjectId,
                    classDate = date,
                    periodCount = state.periodCount,
                    status = status,
                    sessionLabel = state.sessionLabel,
                    notes = state.notes
                )
            }

            when (result) {
                is AppResult.Success -> {
                    val subjectName = state.availableSubjects.find { it.subjectId == subjectId }?.name ?: "Subject"
                    val msg = if (state.isEditMode) {
                        "Updated ${status.name.lowercase()} entry for $subjectName"
                    } else {
                        "Saved ${DateFormatters.pluralizePeriods(state.periodCount)} (${status.name.lowercase()}) for $subjectName"
                    }
                    val createdId = if (state.isEditMode) null else result.data.recordId
                    _uiState.update { it.copy(isSaving = false, isDirty = false) }
                    onSaved(createdId, msg)
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = result.message) }
                }
            }
        }
    }
}

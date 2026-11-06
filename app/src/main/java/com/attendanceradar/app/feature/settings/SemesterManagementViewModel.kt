package com.attendanceradar.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendanceradar.app.core.common.AppResult
import com.attendanceradar.app.core.common.DateFormatters
import com.attendanceradar.app.domain.model.Semester
import com.attendanceradar.app.domain.model.SemesterStatus
import com.attendanceradar.app.domain.repository.SemesterRepository
import com.attendanceradar.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

data class SemesterFormState(
    val editingSemesterId: String? = null,
    val name: String = "",
    val academicYearLabel: String = "",
    val startDateInput: String = LocalDate.now().toString(),
    val endDateInput: String = "",
    val targetPercentInput: String = "75",
    val errorMessage: String? = null
)

data class SemesterManagementUiState(
    val semesters: List<Semester> = emptyList(),
    val activeSemester: Semester? = null,
    val showCreateOrEditDialog: Boolean = false,
    val formState: SemesterFormState = SemesterFormState(),
    val statusMessage: String? = null
)

class SemesterManagementViewModel(
    private val semesterRepository: SemesterRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val dialogVisibleFlow = MutableStateFlow(false)
    private val formStateFlow = MutableStateFlow(SemesterFormState())
    private val statusMessageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SemesterManagementUiState> = combine(
        semesterRepository.observeAllSemesters(),
        dialogVisibleFlow,
        formStateFlow,
        statusMessageFlow
    ) { allSems, showDialog, form, msg ->
        SemesterManagementUiState(
            semesters = allSems,
            activeSemester = allSems.find { it.status == SemesterStatus.ACTIVE },
            showCreateOrEditDialog = showDialog,
            formState = form,
            statusMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SemesterManagementUiState()
    )

    fun openCreateSemesterDialog() {
        viewModelScope.launch {
            val prefs = preferencesRepository.getSnapshot()
            val defaultPct = (prefs.defaultNewSemesterTargetBp / 100).toString()
            formStateFlow.value = SemesterFormState(
                editingSemesterId = null,
                name = "",
                academicYearLabel = "",
                startDateInput = LocalDate.now().toString(),
                endDateInput = "",
                targetPercentInput = defaultPct,
                errorMessage = null
            )
            dialogVisibleFlow.value = true
        }
    }

    fun openEditSemesterDialog(semester: Semester) {
        formStateFlow.value = SemesterFormState(
            editingSemesterId = semester.semesterId,
            name = semester.name,
            academicYearLabel = semester.academicYearLabel ?: "",
            startDateInput = semester.startDate.toString(),
            endDateInput = semester.endDate?.toString() ?: "",
            targetPercentInput = (semester.defaultTargetBp.toDouble() / 100.0).let {
                if (semester.defaultTargetBp % 100 == 0) (semester.defaultTargetBp / 100).toString() else it.toString()
            },
            errorMessage = null
        )
        dialogVisibleFlow.value = true
    }

    fun dismissDialog() {
        dialogVisibleFlow.value = false
    }

    fun updateFormName(value: String) {
        formStateFlow.update { it.copy(name = value, errorMessage = null) }
    }

    fun updateFormAcademicYear(value: String) {
        formStateFlow.update { it.copy(academicYearLabel = value, errorMessage = null) }
    }

    fun updateFormStartDate(value: String) {
        formStateFlow.update { it.copy(startDateInput = value, errorMessage = null) }
    }

    fun updateFormEndDate(value: String) {
        formStateFlow.update { it.copy(endDateInput = value, errorMessage = null) }
    }

    fun updateFormTargetPercent(value: String) {
        formStateFlow.update { it.copy(targetPercentInput = value, errorMessage = null) }
    }

    fun submitSemesterForm() {
        val form = formStateFlow.value
        val trimmedName = form.name.trim()
        if (trimmedName.isEmpty()) {
            formStateFlow.update { it.copy(errorMessage = "Semester name is required.") }
            return
        }
        val startDate = DateFormatters.parseIsoOrNull(form.startDateInput)
        if (startDate == null) {
            formStateFlow.update { it.copy(errorMessage = "Enter a valid start date (YYYY-MM-DD).") }
            return
        }
        val endDate = if (form.endDateInput.isBlank()) {
            null
        } else {
            val parsed = DateFormatters.parseIsoOrNull(form.endDateInput)
            if (parsed == null) {
                formStateFlow.update { it.copy(errorMessage = "Enter a valid end date (YYYY-MM-DD) or leave blank.") }
                return
            }
            if (parsed.isBefore(startDate)) {
                formStateFlow.update { it.copy(errorMessage = "End date cannot precede start date.") }
                return
            }
            parsed
        }

        val targetPct = form.targetPercentInput.trim().toDoubleOrNull()
        if (targetPct == null || targetPct <= 0.0 || targetPct > 100.0) {
            formStateFlow.update { it.copy(errorMessage = "Enter a valid target between 1% and 100%.") }
            return
        }
        val targetBp = (targetPct * 100.0).roundToInt().coerceIn(1, 10_000)

        viewModelScope.launch {
            val result = if (form.editingSemesterId != null) {
                semesterRepository.updateSemester(
                    semesterId = form.editingSemesterId,
                    name = trimmedName,
                    academicYearLabel = form.academicYearLabel,
                    startDate = startDate,
                    endDate = endDate,
                    defaultTargetBp = targetBp
                )
            } else {
                semesterRepository.createAndActivateSemester(
                    name = trimmedName,
                    academicYearLabel = form.academicYearLabel,
                    startDate = startDate,
                    endDate = endDate,
                    defaultTargetBp = targetBp
                )
            }

            when (result) {
                is AppResult.Success -> {
                    dialogVisibleFlow.value = false
                    statusMessageFlow.value = if (form.editingSemesterId != null) {
                        "Updated ${result.data.name}."
                    } else {
                        "Created and activated ${result.data.name}."
                    }
                }
                is AppResult.Error -> {
                    formStateFlow.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun switchActiveSemesterConfirmed(targetSemester: Semester) {
        viewModelScope.launch {
            when (val res = semesterRepository.switchActiveSemester(targetSemester.semesterId)) {
                is AppResult.Success -> {
                    statusMessageFlow.value = "${res.data.name} is now the active semester."
                }
                is AppResult.Error -> {
                    statusMessageFlow.value = res.message
                }
            }
        }
    }

    fun clearStatusMessage() {
        statusMessageFlow.value = null
    }
}

package com.attendanceradar.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendanceradar.app.core.common.AppResult
import com.attendanceradar.app.core.common.DateFormatters
import com.attendanceradar.app.core.design.SubjectPalette
import com.attendanceradar.app.domain.model.Subject
import com.attendanceradar.app.domain.repository.SemesterRepository
import com.attendanceradar.app.domain.repository.SubjectRepository
import com.attendanceradar.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

enum class OnboardingStep {
    WELCOME,
    SEMESTER_DETAILS,
    ADD_SUBJECTS
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val semesterName: String = "",
    val academicYearLabel: String = "",
    val startDateInput: String = LocalDate.now().toString(),
    val endDateInput: String = "",
    val targetPercentInput: String = "75",
    val createdSemesterId: String? = null,
    val addedSubjects: List<Subject> = emptyList(),
    val subjectNameInput: String = "",
    val subjectCodeInput: String = "",
    val subjectShortNameInput: String = "",
    val subjectColorHex: String = SubjectPalette.DEFAULT_HEX,
    val errorMessage: String? = null,
    val isSaving: Boolean = false
)

class OnboardingViewModel(
    private val semesterRepository: SemesterRepository,
    private val subjectRepository: SubjectRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun proceedToSemesterDetails() {
        _uiState.update { it.copy(step = OnboardingStep.SEMESTER_DETAILS, errorMessage = null) }
    }

    fun updateSemesterName(value: String) {
        _uiState.update { it.copy(semesterName = value, errorMessage = null) }
    }

    fun updateAcademicYearLabel(value: String) {
        _uiState.update { it.copy(academicYearLabel = value, errorMessage = null) }
    }

    fun updateStartDateInput(value: String) {
        _uiState.update { it.copy(startDateInput = value, errorMessage = null) }
    }

    fun updateEndDateInput(value: String) {
        _uiState.update { it.copy(endDateInput = value, errorMessage = null) }
    }

    fun updateTargetPercentInput(value: String) {
        _uiState.update { it.copy(targetPercentInput = value, errorMessage = null) }
    }

    fun submitSemesterDetails() {
        val state = _uiState.value
        if (state.isSaving) return

        val trimmedName = state.semesterName.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Enter a semester name (e.g., Semester 1 or Autumn 2026).") }
            return
        }

        val startDate = DateFormatters.parseIsoOrNull(state.startDateInput)
        if (startDate == null) {
            _uiState.update { it.copy(errorMessage = "Enter a valid start date in YYYY-MM-DD format.") }
            return
        }

        val endDate = if (state.endDateInput.isBlank()) {
            null
        } else {
            val parsedEnd = DateFormatters.parseIsoOrNull(state.endDateInput)
            if (parsedEnd == null) {
                _uiState.update { it.copy(errorMessage = "Enter a valid end date in YYYY-MM-DD format, or leave it blank.") }
                return
            }
            if (parsedEnd.isBefore(startDate)) {
                _uiState.update { it.copy(errorMessage = "End date cannot be earlier than start date.") }
                return
            }
            parsedEnd
        }

        val targetPct = state.targetPercentInput.trim().toDoubleOrNull()
        if (targetPct == null || targetPct <= 0.0 || targetPct > 100.0) {
            _uiState.update { it.copy(errorMessage = "Enter a valid attendance target between 1% and 100%.") }
            return
        }
        val targetBp = (targetPct * 100.0).roundToInt().coerceIn(1, 10_000)

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (
                val result = semesterRepository.createAndActivateSemester(
                    name = trimmedName,
                    academicYearLabel = state.academicYearLabel.trim().takeIf { it.isNotEmpty() },
                    startDate = startDate,
                    endDate = endDate,
                    defaultTargetBp = targetBp
                )
            ) {
                is AppResult.Success -> {
                    preferencesRepository.setDefaultNewSemesterTargetBp(targetBp)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            createdSemesterId = result.data.semesterId,
                            step = OnboardingStep.ADD_SUBJECTS,
                            errorMessage = null
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun updateSubjectName(value: String) {
        _uiState.update { it.copy(subjectNameInput = value, errorMessage = null) }
    }

    fun updateSubjectCode(value: String) {
        _uiState.update { it.copy(subjectCodeInput = value, errorMessage = null) }
    }

    fun updateSubjectShortName(value: String) {
        _uiState.update { it.copy(subjectShortNameInput = value, errorMessage = null) }
    }

    fun updateSubjectColorHex(hex: String) {
        _uiState.update { it.copy(subjectColorHex = hex, errorMessage = null) }
    }

    fun addSubjectDuringOnboarding() {
        val state = _uiState.value
        val semesterId = state.createdSemesterId ?: return
        if (state.isSaving) return

        val name = state.subjectNameInput.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Enter a subject name before adding.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (
                val result = subjectRepository.createSubject(
                    semesterId = semesterId,
                    name = name,
                    code = state.subjectCodeInput.trim().takeIf { it.isNotEmpty() },
                    shortName = state.subjectShortNameInput.trim().takeIf { it.isNotEmpty() },
                    targetOverrideBp = null,
                    colorHex = state.subjectColorHex
                )
            ) {
                is AppResult.Success -> {
                    val nextColorIndex = (state.addedSubjects.size + 1) % SubjectPalette.Colors.size
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            addedSubjects = it.addedSubjects + result.data,
                            subjectNameInput = "",
                            subjectCodeInput = "",
                            subjectShortNameInput = "",
                            subjectColorHex = SubjectPalette.Colors[nextColorIndex],
                            errorMessage = null
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun finishOnboarding(onFinished: () -> Unit) {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
            onFinished()
        }
    }
}

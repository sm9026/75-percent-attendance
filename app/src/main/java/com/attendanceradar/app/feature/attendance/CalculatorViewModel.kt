package com.attendanceradar.app.feature.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendanceradar.app.domain.calculator.AttendanceCalculator
import com.attendanceradar.app.domain.calculator.WhatIfProjection
import com.attendanceradar.app.domain.model.SemesterOverview
import com.attendanceradar.app.domain.model.SubjectWithAttendance
import com.attendanceradar.app.domain.model.UserPreferences
import com.attendanceradar.app.domain.repository.SemesterRepository
import com.attendanceradar.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CalculatorUiState(
    val isLoading: Boolean = true,
    val overview: SemesterOverview? = null,
    /** null means Semester Overall; non-null means specific Subject. */
    val selectedSubjectId: String? = null,
    val selectedSubject: SubjectWithAttendance? = null,
    val futurePresent: Long = 0L,
    val futureAbsent: Long = 0L,
    val projection: WhatIfProjection? = null,
    val preferences: UserPreferences = UserPreferences()
)

class CalculatorViewModel(
    initialSubjectId: String?,
    semesterRepository: SemesterRepository,
    preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val selectedSubjectIdFlow = MutableStateFlow(initialSubjectId)
    private val futurePresentFlow = MutableStateFlow(0L)
    private val futureAbsentFlow = MutableStateFlow(0L)

    val uiState: StateFlow<CalculatorUiState> = combine(
        semesterRepository.observeActiveSemesterOverview(),
        selectedSubjectIdFlow,
        futurePresentFlow,
        futureAbsentFlow,
        preferencesRepository.preferencesFlow
    ) { overview, subId, futP, futA, prefs ->
        if (overview == null) {
            CalculatorUiState(isLoading = false, overview = null, preferences = prefs)
        } else {
            val matchedSubject = subId?.let { id ->
                overview.subjects.find { it.subject.subjectId == id }
            }
            val baseSummary = matchedSubject?.summary ?: overview.overallSummary
            val projection = AttendanceCalculator.projectWhatIf(
                presentPeriods = baseSummary.presentPeriods,
                absentPeriods = baseSummary.absentPeriods,
                cancelledPeriods = baseSummary.cancelledPeriods,
                targetBp = baseSummary.targetBp,
                futurePresentPeriods = futP,
                futureAbsentPeriods = futA
            )
            CalculatorUiState(
                isLoading = false,
                overview = overview,
                selectedSubjectId = matchedSubject?.subject?.subjectId,
                selectedSubject = matchedSubject,
                futurePresent = futP,
                futureAbsent = futA,
                projection = projection,
                preferences = prefs
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalculatorUiState()
    )

    fun selectScope(subjectId: String?) {
        selectedSubjectIdFlow.value = subjectId
    }

    fun incrementFuturePresent() {
        futurePresentFlow.value = (futurePresentFlow.value + 1L).coerceAtMost(500L)
    }

    fun decrementFuturePresent() {
        futurePresentFlow.value = (futurePresentFlow.value - 1L).coerceAtLeast(0L)
    }

    fun updateFuturePresentText(text: String) {
        val parsed = text.trim().toLongOrNull() ?: if (text.isBlank()) 0L else return
        if (parsed in 0L..500L) {
            futurePresentFlow.value = parsed
        }
    }

    fun incrementFutureAbsent() {
        futureAbsentFlow.value = (futureAbsentFlow.value + 1L).coerceAtMost(500L)
    }

    fun decrementFutureAbsent() {
        futureAbsentFlow.value = (futureAbsentFlow.value - 1L).coerceAtLeast(0L)
    }

    fun updateFutureAbsentText(text: String) {
        val parsed = text.trim().toLongOrNull() ?: if (text.isBlank()) 0L else return
        if (parsed in 0L..500L) {
            futureAbsentFlow.value = parsed
        }
    }

    fun resetScenario() {
        futurePresentFlow.value = 0L
        futureAbsentFlow.value = 0L
    }
}

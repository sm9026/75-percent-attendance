package com.attendanceradar.app.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendanceradar.app.domain.model.AttendanceRecord
import com.attendanceradar.app.domain.model.CalendarDayMarker
import com.attendanceradar.app.domain.model.SemesterOverview
import com.attendanceradar.app.domain.model.UserPreferences
import com.attendanceradar.app.domain.repository.AttendanceRepository
import com.attendanceradar.app.domain.repository.SemesterRepository
import com.attendanceradar.app.domain.repository.UserPreferencesRepository
import com.attendanceradar.app.domain.usecase.AttendanceAnalyticsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val isLoading: Boolean = true,
    val overview: SemesterOverview? = null,
    val displayedMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val dayMarkers: Map<LocalDate, CalendarDayMarker> = emptyMap(),
    val selectedDateRecords: List<AttendanceRecord> = emptyList(),
    val preferences: UserPreferences = UserPreferences()
)

class CalendarViewModel(
    semesterRepository: SemesterRepository,
    private val attendanceRepository: AttendanceRepository,
    preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val displayedMonthFlow = MutableStateFlow(YearMonth.now())
    private val selectedDateFlow = MutableStateFlow(LocalDate.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val semesterRecordsFlow = semesterRepository.observeActiveSemester().flatMapLatest { sem ->
        if (sem == null) flowOf(emptyList()) else attendanceRepository.observeRecordsForSemester(sem.semesterId)
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        semesterRepository.observeActiveSemesterOverview(),
        semesterRecordsFlow,
        displayedMonthFlow,
        selectedDateFlow,
        preferencesRepository.preferencesFlow
    ) { overview, allRecords, month, selectedDate, prefs ->
        val markers = AttendanceAnalyticsUseCase.computeCalendarMarkers(allRecords)
        val recordsForSelectedDate = allRecords.filter { it.classDate == selectedDate }
        CalendarUiState(
            isLoading = false,
            overview = overview,
            displayedMonth = month,
            selectedDate = selectedDate,
            dayMarkers = markers,
            selectedDateRecords = recordsForSelectedDate,
            preferences = prefs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState()
    )

    fun previousMonth() {
        displayedMonthFlow.value = displayedMonthFlow.value.minusMonths(1)
    }

    fun nextMonth() {
        displayedMonthFlow.value = displayedMonthFlow.value.plusMonths(1)
    }

    fun jumpToToday() {
        val today = LocalDate.now()
        displayedMonthFlow.value = YearMonth.from(today)
        selectedDateFlow.value = today
    }

    fun selectDate(date: LocalDate) {
        selectedDateFlow.value = date
        if (YearMonth.from(date) != displayedMonthFlow.value) {
            displayedMonthFlow.value = YearMonth.from(date)
        }
    }

    fun deleteRecordConfirmed(recordId: String) {
        viewModelScope.launch {
            attendanceRepository.deleteAttendanceRecord(recordId)
        }
    }
}

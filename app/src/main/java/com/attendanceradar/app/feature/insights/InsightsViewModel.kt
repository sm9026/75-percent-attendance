package com.attendanceradar.app.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendanceradar.app.domain.calculator.AttendanceSummary
import com.attendanceradar.app.domain.model.DateBucketAttendance
import com.attendanceradar.app.domain.model.SemesterOverview
import com.attendanceradar.app.domain.model.SubjectWithAttendance
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
import java.time.LocalDate
import java.time.YearMonth

enum class InsightsBucketMode {
    WEEKLY,
    MONTHLY
}

data class InsightsUiState(
    val isLoading: Boolean = true,
    val overview: SemesterOverview? = null,
    val bestSubject: SubjectWithAttendance? = null,
    val lowestSubject: SubjectWithAttendance? = null,
    val currentMonthSummary: AttendanceSummary? = null,
    val recentSevenDaysSummary: AttendanceSummary? = null,
    val bucketMode: InsightsBucketMode = InsightsBucketMode.WEEKLY,
    val buckets: List<DateBucketAttendance> = emptyList(),
    val preferences: UserPreferences = UserPreferences()
)

class InsightsViewModel(
    semesterRepository: SemesterRepository,
    attendanceRepository: AttendanceRepository,
    preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val bucketModeFlow = MutableStateFlow(InsightsBucketMode.WEEKLY)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val semesterRecordsFlow = semesterRepository.observeActiveSemester().flatMapLatest { sem ->
        if (sem == null) flowOf(emptyList()) else attendanceRepository.observeRecordsForSemester(sem.semesterId)
    }

    val uiState: StateFlow<InsightsUiState> = combine(
        semesterRepository.observeActiveSemesterOverview(),
        semesterRecordsFlow,
        bucketModeFlow,
        preferencesRepository.preferencesFlow
    ) { overview, records, mode, prefs ->
        if (overview == null) {
            InsightsUiState(isLoading = false, overview = null, preferences = prefs)
        } else {
            val conductedSubjects = overview.subjects.filter { it.summary.conductedPeriods > 0L }
            val best = conductedSubjects.maxWithOrNull(
                compareBy<SubjectWithAttendance> { it.summary.attendancePercentage ?: 0.0 }
                    .thenBy { it.summary.conductedPeriods }
            )
            val lowest = conductedSubjects.minWithOrNull(
                compareBy<SubjectWithAttendance> { it.summary.attendancePercentage ?: 0.0 }
                    .thenByDescending { it.summary.conductedPeriods }
            )

            val today = LocalDate.now()
            val ym = YearMonth.from(today)
            val currentMonthSummary = AttendanceAnalyticsUseCase.computeDateRangeSummary(
                records = records,
                startDate = ym.atDay(1),
                endDate = ym.atEndOfMonth(),
                targetBp = overview.semester.defaultTargetBp
            )
            val recentWeekSummary = AttendanceAnalyticsUseCase.computeDateRangeSummary(
                records = records,
                startDate = today.minusDays(6),
                endDate = today,
                targetBp = overview.semester.defaultTargetBp
            )

            val buckets = when (mode) {
                InsightsBucketMode.WEEKLY ->
                    AttendanceAnalyticsUseCase.computeWeeklyBuckets(records, today, prefs.weekStartsOn, 6)
                InsightsBucketMode.MONTHLY ->
                    AttendanceAnalyticsUseCase.computeMonthlyBuckets(records, today, 6)
            }

            InsightsUiState(
                isLoading = false,
                overview = overview,
                bestSubject = best,
                lowestSubject = lowest,
                currentMonthSummary = currentMonthSummary,
                recentSevenDaysSummary = recentWeekSummary,
                bucketMode = mode,
                buckets = buckets,
                preferences = prefs
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InsightsUiState()
    )

    fun selectBucketMode(mode: InsightsBucketMode) {
        bucketModeFlow.value = mode
    }
}

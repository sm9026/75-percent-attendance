package com.attendanceradar.app.feature.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendanceradar.app.domain.model.AttendanceRecord
import com.attendanceradar.app.domain.model.CumulativeTrendPoint
import com.attendanceradar.app.domain.model.SubjectWithAttendance
import com.attendanceradar.app.domain.model.UserPreferences
import com.attendanceradar.app.domain.repository.AttendanceRepository
import com.attendanceradar.app.domain.repository.SubjectRepository
import com.attendanceradar.app.domain.repository.UserPreferencesRepository
import com.attendanceradar.app.domain.usecase.AttendanceAnalyticsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SubjectDetailUiState(
    val isLoading: Boolean = true,
    val subjectWithAttendance: SubjectWithAttendance? = null,
    val historyRecords: List<AttendanceRecord> = emptyList(),
    val trendPoints: List<CumulativeTrendPoint> = emptyList(),
    val preferences: UserPreferences = UserPreferences()
)

class SubjectDetailViewModel(
    subjectId: String,
    private val subjectRepository: SubjectRepository,
    private val attendanceRepository: AttendanceRepository,
    preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<SubjectDetailUiState> = combine(
        subjectRepository.observeSubjectWithAttendance(subjectId),
        attendanceRepository.observeRecordsForSubject(subjectId),
        preferencesRepository.preferencesFlow
    ) { subjectItem, records, prefs ->
        SubjectDetailUiState(
            isLoading = false,
            subjectWithAttendance = subjectItem,
            historyRecords = records,
            trendPoints = AttendanceAnalyticsUseCase.computeCumulativeTrend(records),
            preferences = prefs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SubjectDetailUiState()
    )

    fun deleteRecordConfirmed(recordId: String) {
        viewModelScope.launch {
            attendanceRepository.deleteAttendanceRecord(recordId)
        }
    }

    fun deleteSubjectConfirmed(subjectId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            subjectRepository.deleteSubjectAndRecords(subjectId)
            onDeleted()
        }
    }
}

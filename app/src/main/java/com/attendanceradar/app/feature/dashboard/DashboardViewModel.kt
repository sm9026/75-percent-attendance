package com.attendanceradar.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendanceradar.app.domain.model.AttendanceRecord
import com.attendanceradar.app.domain.model.SemesterOverview
import com.attendanceradar.app.domain.model.SubjectWithAttendance
import com.attendanceradar.app.domain.model.UserPreferences
import com.attendanceradar.app.domain.repository.AttendanceRepository
import com.attendanceradar.app.domain.repository.SemesterRepository
import com.attendanceradar.app.domain.repository.SubjectRepository
import com.attendanceradar.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data object NoActiveSemester : DashboardUiState
    data class Active(
        val overview: SemesterOverview,
        val recentRecords: List<AttendanceRecord>,
        val preferences: UserPreferences
    ) : DashboardUiState
}

class DashboardViewModel(
    private val semesterRepository: SemesterRepository,
    private val subjectRepository: SubjectRepository,
    private val attendanceRepository: AttendanceRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val recentRecordsFlow = semesterRepository.observeActiveSemester().flatMapLatest { sem ->
        if (sem == null) {
            flowOf(emptyList())
        } else {
            attendanceRepository.observeRecordsForSemester(sem.semesterId)
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        semesterRepository.observeActiveSemesterOverview(),
        recentRecordsFlow,
        preferencesRepository.preferencesFlow
    ) { overview, records, prefs ->
        if (overview == null) {
            DashboardUiState.NoActiveSemester
        } else {
            DashboardUiState.Active(
                overview = overview,
                recentRecords = records.take(5),
                preferences = prefs
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState.Loading
    )

    fun toggleSubjectArchive(item: SubjectWithAttendance) {
        viewModelScope.launch {
            subjectRepository.setSubjectArchived(
                subjectId = item.subject.subjectId,
                archived = !item.subject.isArchived
            )
        }
    }

    fun deleteSubjectConfirmed(subjectId: String) {
        viewModelScope.launch {
            subjectRepository.deleteSubjectAndRecords(subjectId)
        }
    }

    fun deleteRecordConfirmed(recordId: String) {
        viewModelScope.launch {
            attendanceRepository.deleteAttendanceRecord(recordId)
        }
    }
}

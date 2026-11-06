package com.attendanceradar.app.feature.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendanceradar.app.domain.model.SemesterOverview
import com.attendanceradar.app.domain.model.SubjectWithAttendance
import com.attendanceradar.app.domain.model.UserPreferences
import com.attendanceradar.app.domain.repository.SemesterRepository
import com.attendanceradar.app.domain.repository.SubjectRepository
import com.attendanceradar.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SubjectsScreenUiState(
    val isLoading: Boolean = true,
    val overview: SemesterOverview? = null,
    val searchQuery: String = "",
    val showArchivedSection: Boolean = false,
    val preferences: UserPreferences = UserPreferences()
)

class SubjectsViewModel(
    semesterRepository: SemesterRepository,
    private val subjectRepository: SubjectRepository,
    preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow("")
    private val showArchivedFlow = MutableStateFlow(false)

    val uiState: StateFlow<SubjectsScreenUiState> = combine(
        semesterRepository.observeActiveSemesterOverview(),
        searchQueryFlow,
        showArchivedFlow,
        preferencesRepository.preferencesFlow
    ) { overview, query, showArchived, prefs ->
        SubjectsScreenUiState(
            isLoading = false,
            overview = overview,
            searchQuery = query,
            showArchivedSection = showArchived,
            preferences = prefs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SubjectsScreenUiState()
    )

    fun updateSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun toggleShowArchivedSection() {
        showArchivedFlow.value = !showArchivedFlow.value
    }

    fun toggleArchive(item: SubjectWithAttendance) {
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
}

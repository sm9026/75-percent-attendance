package com.attendanceradar.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendanceradar.app.domain.model.Semester
import com.attendanceradar.app.domain.model.ThemeMode
import com.attendanceradar.app.domain.model.UserPreferences
import com.attendanceradar.app.domain.model.WeekStartDay
import com.attendanceradar.app.domain.repository.SemesterRepository
import com.attendanceradar.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val activeSemester: Semester? = null,
    val totalSemestersCount: Int = 0
)

class SettingsViewModel(
    semesterRepository: SemesterRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferencesFlow,
        semesterRepository.observeActiveSemester(),
        semesterRepository.observeAllSemesters()
    ) { prefs, activeSem, allSems ->
        SettingsUiState(
            preferences = prefs,
            activeSemester = activeSem,
            totalSemestersCount = allSems.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setDisplayPrecision(precision: Int) {
        viewModelScope.launch {
            preferencesRepository.setDisplayPrecision(precision)
        }
    }

    fun setWeekStartsOn(day: WeekStartDay) {
        viewModelScope.launch {
            preferencesRepository.setWeekStartsOn(day)
        }
    }

    fun setDefaultNewSemesterTargetPercent(percentText: String) {
        val pct = percentText.trim().toDoubleOrNull() ?: return
        if (pct in 1.0..100.0) {
            val bp = (pct * 100.0).roundToInt().coerceIn(1, 10_000)
            viewModelScope.launch {
                preferencesRepository.setDefaultNewSemesterTargetBp(bp)
            }
        }
    }
}

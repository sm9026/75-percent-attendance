package com.attendanceradar.app.feature.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendanceradar.app.core.common.AppResult
import com.attendanceradar.app.core.common.DateFormatters
import com.attendanceradar.app.core.design.SubjectPalette
import com.attendanceradar.app.domain.repository.SemesterRepository
import com.attendanceradar.app.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class SubjectFormUiState(
    val isEditMode: Boolean = false,
    val subjectId: String? = null,
    val activeSemesterId: String? = null,
    val semesterDefaultTargetBp: Int = 7500,
    val name: String = "",
    val code: String = "",
    val shortName: String = "",
    val useCustomTarget: Boolean = false,
    val customTargetPercentInput: String = "75",
    val colorHex: String = SubjectPalette.DEFAULT_HEX,
    val isArchived: Boolean = false,
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class SubjectFormViewModel(
    private val editingSubjectId: String?,
    private val semesterRepository: SemesterRepository,
    private val subjectRepository: SubjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectFormUiState(isEditMode = !editingSubjectId.isNullOrBlank()))
    val uiState: StateFlow<SubjectFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val activeSem = semesterRepository.getActiveSemester()
            val defaultBp = activeSem?.defaultTargetBp ?: 7500

            if (!editingSubjectId.isNullOrBlank()) {
                val existing = subjectRepository.getSubjectById(editingSubjectId)
                if (existing != null) {
                    val overridePct = existing.targetOverrideBp?.let { bp ->
                        if (bp % 100 == 0) (bp / 100).toString() else (bp.toDouble() / 100.0).toString()
                    } ?: DateFormatters.formatBasisPoints(defaultBp).removeSuffix("%")
                    _uiState.update {
                        it.copy(
                            isEditMode = true,
                            subjectId = existing.subjectId,
                            activeSemesterId = existing.semesterId,
                            semesterDefaultTargetBp = defaultBp,
                            name = existing.name,
                            code = existing.code ?: "",
                            shortName = existing.shortName ?: "",
                            useCustomTarget = existing.targetOverrideBp != null,
                            customTargetPercentInput = overridePct,
                            colorHex = existing.colorHex,
                            isArchived = existing.isArchived,
                            isDirty = false
                        )
                    }
                    return@launch
                }
            }

            _uiState.update {
                it.copy(
                    activeSemesterId = activeSem?.semesterId,
                    semesterDefaultTargetBp = defaultBp,
                    customTargetPercentInput = (defaultBp / 100).toString()
                )
            }
        }
    }

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value, isDirty = true, errorMessage = null) }
    }

    fun updateCode(value: String) {
        _uiState.update { it.copy(code = value, isDirty = true, errorMessage = null) }
    }

    fun updateShortName(value: String) {
        _uiState.update { it.copy(shortName = value, isDirty = true, errorMessage = null) }
    }

    fun updateUseCustomTarget(enabled: Boolean) {
        _uiState.update { it.copy(useCustomTarget = enabled, isDirty = true, errorMessage = null) }
    }

    fun updateCustomTargetInput(value: String) {
        _uiState.update { it.copy(customTargetPercentInput = value, isDirty = true, errorMessage = null) }
    }

    fun updateColorHex(hex: String) {
        _uiState.update { it.copy(colorHex = hex, isDirty = true, errorMessage = null) }
    }

    fun updateArchived(archived: Boolean) {
        _uiState.update { it.copy(isArchived = archived, isDirty = true, errorMessage = null) }
    }

    fun saveSubject(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.isSaving) return

        val trimmedName = state.name.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Subject name is required.") }
            return
        }

        val overrideBp: Int? = if (state.useCustomTarget) {
            val pct = state.customTargetPercentInput.trim().toDoubleOrNull()
            if (pct == null || pct <= 0.0 || pct > 100.0) {
                _uiState.update { it.copy(errorMessage = "Enter a valid custom target percentage between 1% and 100%.") }
                return
            }
            (pct * 100.0).roundToInt().coerceIn(1, 10_000)
        } else {
            null
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val result = if (state.isEditMode && state.subjectId != null) {
                subjectRepository.updateSubject(
                    subjectId = state.subjectId,
                    name = trimmedName,
                    code = state.code.trim().takeIf { it.isNotEmpty() },
                    shortName = state.shortName.trim().takeIf { it.isNotEmpty() },
                    targetOverrideBp = overrideBp,
                    colorHex = state.colorHex,
                    isArchived = state.isArchived
                )
            } else {
                val semId = state.activeSemesterId
                if (semId == null) {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "No active semester found.") }
                    return@launch
                }
                subjectRepository.createSubject(
                    semesterId = semId,
                    name = trimmedName,
                    code = state.code.trim().takeIf { it.isNotEmpty() },
                    shortName = state.shortName.trim().takeIf { it.isNotEmpty() },
                    targetOverrideBp = overrideBp,
                    colorHex = state.colorHex
                )
            }

            when (result) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, isDirty = false) }
                    onSuccess()
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = result.message) }
                }
            }
        }
    }
}

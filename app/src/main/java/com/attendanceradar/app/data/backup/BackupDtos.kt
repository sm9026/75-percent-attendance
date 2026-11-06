package com.attendanceradar.app.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupPayloadDto(
    val format: String,
    val backupVersion: Int,
    val appVersion: String,
    val exportedAt: String,
    val preferences: BackupPreferencesDto,
    val semesters: List<BackupSemesterDto>,
    val subjects: List<BackupSubjectDto>,
    val attendanceRecords: List<BackupAttendanceRecordDto>
)

@Serializable
data class BackupPreferencesDto(
    val themeMode: String = "SYSTEM",
    val displayPrecision: Int = 1,
    val weekStartsOn: String = "MONDAY"
)

@Serializable
data class BackupSemesterDto(
    val semesterId: String,
    val name: String,
    val academicYearLabel: String? = null,
    val startDate: String,
    val endDate: String? = null,
    val defaultTargetBp: Int,
    val status: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Serializable
data class BackupSubjectDto(
    val subjectId: String,
    val semesterId: String,
    val name: String,
    val code: String? = null,
    val shortName: String? = null,
    val targetOverrideBp: Int? = null,
    val colorHex: String,
    val isArchived: Boolean = false,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Serializable
data class BackupAttendanceRecordDto(
    val recordId: String,
    val subjectId: String,
    val classDate: String,
    val periodCount: Int,
    val status: String,
    val sessionLabel: String? = null,
    val notes: String? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

data class BackupPreviewSummary(
    val exportedAt: String,
    val appVersion: String,
    val semesterCount: Int,
    val activeSemesterName: String?,
    val subjectCount: Int,
    val attendanceRecordCount: Int,
    val validatedPayload: BackupPayloadDto
)

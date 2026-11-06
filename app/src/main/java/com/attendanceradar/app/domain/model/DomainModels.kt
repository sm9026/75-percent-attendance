package com.attendanceradar.app.domain.model

import com.attendanceradar.app.domain.calculator.AttendanceSummary
import java.time.LocalDate

data class Semester(
    val semesterId: String,
    val name: String,
    val academicYearLabel: String?,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val defaultTargetBp: Int,
    val status: SemesterStatus,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    val isActive: Boolean get() = status == SemesterStatus.ACTIVE
}

data class Subject(
    val subjectId: String,
    val semesterId: String,
    val name: String,
    val code: String?,
    val shortName: String?,
    val targetOverrideBp: Int?,
    val colorHex: String,
    val isArchived: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    fun effectiveTargetBp(semesterDefaultBp: Int): Int =
        targetOverrideBp ?: semesterDefaultBp
}

data class AttendanceRecord(
    val recordId: String,
    val subjectId: String,
    val classDate: LocalDate,
    val periodCount: Int,
    val status: AttendanceEntryStatus,
    val sessionLabel: String?,
    val notes: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val displayPrecision: Int = 1,
    val weekStartsOn: WeekStartDay = WeekStartDay.MONDAY,
    val defaultNewSemesterTargetBp: Int = 7500,
    val onboardingCompleted: Boolean = false
)

data class SubjectWithAttendance(
    val subject: Subject,
    val effectiveTargetBp: Int,
    val isTargetOverridden: Boolean,
    val summary: AttendanceSummary,
    val totalRecordCount: Int
)

data class SemesterOverview(
    val semester: Semester,
    val overallSummary: AttendanceSummary,
    val subjects: List<SubjectWithAttendance>
) {
    val activeSubjects: List<SubjectWithAttendance>
        get() = subjects.filter { !it.subject.isArchived }

    val archivedSubjects: List<SubjectWithAttendance>
        get() = subjects.filter { it.subject.isArchived }
}

data class CumulativeTrendPoint(
    val recordId: String,
    val date: LocalDate,
    val cumulativePresent: Long,
    val cumulativeConducted: Long,
    val cumulativePercentage: Double
)

data class DateBucketAttendance(
    val label: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val presentPeriods: Long,
    val absentPeriods: Long,
    val cancelledPeriods: Long,
    val conductedPeriods: Long,
    val percentage: Double?
)

data class DuplicateCheckResult(
    val hasExistingSessionOnDate: Boolean,
    val hasMatchingSessionLabel: Boolean,
    val existingRecordCount: Int,
    val warningMessage: String?
)

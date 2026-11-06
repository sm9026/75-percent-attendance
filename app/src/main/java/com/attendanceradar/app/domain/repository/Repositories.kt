package com.attendanceradar.app.domain.repository

import com.attendanceradar.app.core.common.AppResult
import com.attendanceradar.app.domain.model.AttendanceEntryStatus
import com.attendanceradar.app.domain.model.AttendanceRecord
import com.attendanceradar.app.domain.model.DuplicateCheckResult
import com.attendanceradar.app.domain.model.Semester
import com.attendanceradar.app.domain.model.SemesterOverview
import com.attendanceradar.app.domain.model.Subject
import com.attendanceradar.app.domain.model.SubjectWithAttendance
import com.attendanceradar.app.domain.model.ThemeMode
import com.attendanceradar.app.domain.model.UserPreferences
import com.attendanceradar.app.domain.model.WeekStartDay
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface SemesterRepository {
    fun observeAllSemesters(): Flow<List<Semester>>
    fun observeActiveSemester(): Flow<Semester?>
    fun observeActiveSemesterOverview(): Flow<SemesterOverview?>
    fun observeSemesterOverview(semesterId: String): Flow<SemesterOverview?>
    suspend fun getActiveSemester(): Semester?
    suspend fun getSemesterById(semesterId: String): Semester?
    suspend fun createAndActivateSemester(
        name: String,
        academicYearLabel: String?,
        startDate: LocalDate,
        endDate: LocalDate?,
        defaultTargetBp: Int
    ): AppResult<Semester>
    suspend fun updateSemester(
        semesterId: String,
        name: String,
        academicYearLabel: String?,
        startDate: LocalDate,
        endDate: LocalDate?,
        defaultTargetBp: Int
    ): AppResult<Semester>
    suspend fun switchActiveSemester(targetSemesterId: String): AppResult<Semester>
    suspend fun archiveSemester(semesterId: String): AppResult<Unit>
}

interface SubjectRepository {
    fun observeSubjectsForSemester(semesterId: String): Flow<List<Subject>>
    fun observeSubjectsWithAttendance(semesterId: String): Flow<List<SubjectWithAttendance>>
    fun observeSubjectWithAttendance(subjectId: String): Flow<SubjectWithAttendance?>
    suspend fun getSubjectById(subjectId: String): Subject?
    suspend fun createSubject(
        semesterId: String,
        name: String,
        code: String?,
        shortName: String?,
        targetOverrideBp: Int?,
        colorHex: String
    ): AppResult<Subject>
    suspend fun updateSubject(
        subjectId: String,
        name: String,
        code: String?,
        shortName: String?,
        targetOverrideBp: Int?,
        colorHex: String,
        isArchived: Boolean
    ): AppResult<Subject>
    suspend fun setSubjectArchived(subjectId: String, archived: Boolean): AppResult<Unit>
    suspend fun deleteSubjectAndRecords(subjectId: String): AppResult<Int>
}

interface AttendanceRepository {
    fun observeRecordsForSubject(subjectId: String): Flow<List<AttendanceRecord>>
    fun observeRecordsForSemester(semesterId: String): Flow<List<AttendanceRecord>>
    fun observeRecordsForSemesterInDateRange(
        semesterId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<AttendanceRecord>>
    fun observeRecordsForDate(semesterId: String, date: LocalDate): Flow<List<AttendanceRecord>>
    suspend fun getRecordById(recordId: String): AttendanceRecord?
    suspend fun checkPotentialDuplicate(
        subjectId: String,
        classDate: LocalDate,
        sessionLabel: String?,
        excludeRecordId: String? = null
    ): DuplicateCheckResult
    suspend fun addAttendanceRecord(
        subjectId: String,
        classDate: LocalDate,
        periodCount: Int,
        status: AttendanceEntryStatus,
        sessionLabel: String?,
        notes: String?
    ): AppResult<AttendanceRecord>
    suspend fun updateAttendanceRecord(
        recordId: String,
        subjectId: String,
        classDate: LocalDate,
        periodCount: Int,
        status: AttendanceEntryStatus,
        sessionLabel: String?,
        notes: String?
    ): AppResult<AttendanceRecord>
    suspend fun deleteAttendanceRecord(recordId: String): AppResult<Unit>
}

interface UserPreferencesRepository {
    val preferencesFlow: Flow<UserPreferences>
    suspend fun getSnapshot(): UserPreferences
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setDisplayPrecision(precision: Int)
    suspend fun setWeekStartsOn(weekStartDay: WeekStartDay)
    suspend fun setDefaultNewSemesterTargetBp(targetBp: Int)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun restorePreferences(
        themeMode: ThemeMode,
        displayPrecision: Int,
        weekStartsOn: WeekStartDay
    )
}

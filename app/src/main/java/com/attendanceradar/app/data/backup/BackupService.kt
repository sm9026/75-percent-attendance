package com.attendanceradar.app.data.backup

import androidx.room.withTransaction
import com.attendanceradar.app.core.common.AppResult
import com.attendanceradar.app.core.common.DateFormatters
import com.attendanceradar.app.core.design.SubjectPalette
import com.attendanceradar.app.data.local.AttendanceDatabase
import com.attendanceradar.app.data.local.dao.AttendanceRecordDao
import com.attendanceradar.app.data.local.dao.SemesterDao
import com.attendanceradar.app.data.local.dao.SubjectDao
import com.attendanceradar.app.data.local.entity.AttendanceRecordEntity
import com.attendanceradar.app.data.local.entity.SemesterEntity
import com.attendanceradar.app.data.local.entity.SubjectEntity
import com.attendanceradar.app.domain.calculator.AttendanceCalculator
import com.attendanceradar.app.domain.model.AttendanceEntryStatus
import com.attendanceradar.app.domain.model.SemesterStatus
import com.attendanceradar.app.domain.model.ThemeMode
import com.attendanceradar.app.domain.model.WeekStartDay
import com.attendanceradar.app.domain.repository.UserPreferencesRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * Handles versioned JSON backup generation, strict offline validation before mutation,
 * and single-transaction restore replacement.
 */
class BackupService(
    private val database: AttendanceDatabase,
    private val semesterDao: SemesterDao,
    private val subjectDao: SubjectDao,
    private val attendanceRecordDao: AttendanceRecordDao,
    private val preferencesRepository: UserPreferencesRepository
) {

    companion object {
        const val FORMAT_IDENTIFIER = "attendance-radar-backup"
        const val SUPPORTED_BACKUP_VERSION = 1
        const val APP_VERSION = "1.0.0"

        private val jsonCodec = Json {
            prettyPrint = true
            ignoreUnknownKeys = false
            encodeDefaults = true
        }

        /**
         * Pure validation function that validates the entire JSON backup string before any database mutation.
         * Can be unit-tested without Room or Android Context.
         */
        fun parseAndValidateBackupJson(rawJson: String): AppResult<BackupPreviewSummary> {
            if (rawJson.isBlank()) {
                return AppResult.Error("The selected backup file is empty.")
            }

            val payload = try {
                jsonCodec.decodeFromString<BackupPayloadDto>(rawJson)
            } catch (e: Exception) {
                return AppResult.Error("Invalid JSON backup format: ${e.message ?: "Malformed JSON"}", e)
            }

            if (payload.format != FORMAT_IDENTIFIER) {
                return AppResult.Error("Unsupported backup format identifier: '${payload.format}'. Expected '$FORMAT_IDENTIFIER'.")
            }

            if (payload.backupVersion != SUPPORTED_BACKUP_VERSION) {
                return AppResult.Error("Unsupported backup version: ${payload.backupVersion}. Supported version is $SUPPORTED_BACKUP_VERSION.")
            }

            if (payload.exportedAt.isBlank()) {
                return AppResult.Error("Backup export timestamp is missing.")
            }

            // Validate preferences
            if (runCatching { ThemeMode.valueOf(payload.preferences.themeMode) }.isFailure) {
                return AppResult.Error("Invalid themeMode in backup preferences: '${payload.preferences.themeMode}'.")
            }
            if (payload.preferences.displayPrecision !in 0..2) {
                return AppResult.Error("Invalid displayPrecision in backup preferences: ${payload.preferences.displayPrecision}.")
            }
            if (runCatching { WeekStartDay.valueOf(payload.preferences.weekStartsOn) }.isFailure) {
                return AppResult.Error("Invalid weekStartsOn in backup preferences: '${payload.preferences.weekStartsOn}'.")
            }

            // Validate semesters
            val semesterIds = mutableSetOf<String>()
            var activeSemesterCount = 0
            var activeSemesterName: String? = null

            for ((index, sem) in payload.semesters.withIndex()) {
                if (sem.semesterId.isBlank()) {
                    return AppResult.Error("Semester at index $index has an empty semesterId.")
                }
                if (!semesterIds.add(sem.semesterId)) {
                    return AppResult.Error("Duplicate semesterId found in backup: '${sem.semesterId}'.")
                }
                if (sem.name.trim().isEmpty()) {
                    return AppResult.Error("Semester '${sem.semesterId}' has an empty name.")
                }
                val startDate = DateFormatters.parseIsoOrNull(sem.startDate)
                    ?: return AppResult.Error("Semester '${sem.name}' has an invalid startDate: '${sem.startDate}'.")
                if (sem.endDate != null) {
                    val endDate = DateFormatters.parseIsoOrNull(sem.endDate)
                        ?: return AppResult.Error("Semester '${sem.name}' has an invalid endDate: '${sem.endDate}'.")
                    if (endDate.isBefore(startDate)) {
                        return AppResult.Error("Semester '${sem.name}' has endDate (${sem.endDate}) before startDate (${sem.startDate}).")
                    }
                }
                if (sem.defaultTargetBp !in AttendanceCalculator.MIN_TARGET_BP..AttendanceCalculator.MAX_TARGET_BP) {
                    return AppResult.Error("Semester '${sem.name}' has invalid defaultTargetBp: ${sem.defaultTargetBp}.")
                }
                val status = runCatching { SemesterStatus.valueOf(sem.status) }.getOrNull()
                    ?: return AppResult.Error("Semester '${sem.name}' has invalid status: '${sem.status}'.")
                if (status == SemesterStatus.ACTIVE) {
                    activeSemesterCount++
                    activeSemesterName = sem.name.trim()
                }
            }

            if (payload.semesters.isNotEmpty() && activeSemesterCount > 1) {
                return AppResult.Error("Backup contains $activeSemesterCount ACTIVE semesters; at most 1 ACTIVE semester is allowed.")
            }

            // Validate subjects and foreign keys -> semesters
            val subjectIds = mutableSetOf<String>()
            for ((index, sub) in payload.subjects.withIndex()) {
                if (sub.subjectId.isBlank()) {
                    return AppResult.Error("Subject at index $index has an empty subjectId.")
                }
                if (!subjectIds.add(sub.subjectId)) {
                    return AppResult.Error("Duplicate subjectId found in backup: '${sub.subjectId}'.")
                }
                if (sub.semesterId !in semesterIds) {
                    return AppResult.Error("Subject '${sub.name}' references missing semesterId '${sub.semesterId}'.")
                }
                if (sub.name.trim().isEmpty()) {
                    return AppResult.Error("Subject '${sub.subjectId}' has an empty name.")
                }
                if (sub.targetOverrideBp != null &&
                    sub.targetOverrideBp !in AttendanceCalculator.MIN_TARGET_BP..AttendanceCalculator.MAX_TARGET_BP
                ) {
                    return AppResult.Error("Subject '${sub.name}' has invalid targetOverrideBp: ${sub.targetOverrideBp}.")
                }
                if (!SubjectPalette.isValidHex(sub.colorHex)) {
                    return AppResult.Error("Subject '${sub.name}' has invalid colorHex: '${sub.colorHex}'.")
                }
            }

            // Validate attendance records and foreign keys -> subjects
            val recordIds = mutableSetOf<String>()
            for ((index, rec) in payload.attendanceRecords.withIndex()) {
                if (rec.recordId.isBlank()) {
                    return AppResult.Error("Attendance record at index $index has an empty recordId.")
                }
                if (!recordIds.add(rec.recordId)) {
                    return AppResult.Error("Duplicate recordId found in backup: '${rec.recordId}'.")
                }
                if (rec.subjectId !in subjectIds) {
                    return AppResult.Error("Attendance record '${rec.recordId}' references missing subjectId '${rec.subjectId}'.")
                }
                if (DateFormatters.parseIsoOrNull(rec.classDate) == null) {
                    return AppResult.Error("Attendance record '${rec.recordId}' has invalid classDate: '${rec.classDate}'.")
                }
                if (rec.periodCount !in AttendanceCalculator.MIN_PERIOD_COUNT..AttendanceCalculator.MAX_PERIOD_COUNT) {
                    return AppResult.Error("Attendance record '${rec.recordId}' has invalid periodCount: ${rec.periodCount} (expected 1..20).")
                }
                if (runCatching { AttendanceEntryStatus.valueOf(rec.status) }.isFailure) {
                    return AppResult.Error("Attendance record '${rec.recordId}' has invalid status: '${rec.status}'.")
                }
                if (rec.notes != null && rec.notes.length > 500) {
                    return AppResult.Error("Attendance record '${rec.recordId}' has notes exceeding 500 characters.")
                }
            }

            return AppResult.Success(
                BackupPreviewSummary(
                    exportedAt = payload.exportedAt,
                    appVersion = payload.appVersion,
                    semesterCount = payload.semesters.size,
                    activeSemesterName = activeSemesterName,
                    subjectCount = payload.subjects.size,
                    attendanceRecordCount = payload.attendanceRecords.size,
                    validatedPayload = payload
                )
            )
        }
    }

    suspend fun createBackupJson(): AppResult<String> {
        return try {
            val prefs = preferencesRepository.getSnapshot()
            val semesters = semesterDao.getAllSemestersSnapshot()
            val subjects = subjectDao.getAllSubjectsSnapshot()
            val records = attendanceRecordDao.getAllRecordsSnapshot()

            val payload = BackupPayloadDto(
                format = FORMAT_IDENTIFIER,
                backupVersion = SUPPORTED_BACKUP_VERSION,
                appVersion = APP_VERSION,
                exportedAt = Instant.now().toString(),
                preferences = BackupPreferencesDto(
                    themeMode = prefs.themeMode.name,
                    displayPrecision = prefs.displayPrecision,
                    weekStartsOn = prefs.weekStartsOn.name
                ),
                semesters = semesters.map {
                    BackupSemesterDto(
                        semesterId = it.semesterId,
                        name = it.name,
                        academicYearLabel = it.academicYearLabel,
                        startDate = it.startDate,
                        endDate = it.endDate,
                        defaultTargetBp = it.defaultTargetBp,
                        status = it.status.name,
                        createdAtEpochMs = it.createdAtEpochMs,
                        updatedAtEpochMs = it.updatedAtEpochMs
                    )
                },
                subjects = subjects.map {
                    BackupSubjectDto(
                        subjectId = it.subjectId,
                        semesterId = it.semesterId,
                        name = it.name,
                        code = it.code,
                        shortName = it.shortName,
                        targetOverrideBp = it.targetOverrideBp,
                        colorHex = it.colorHex,
                        isArchived = it.isArchived,
                        createdAtEpochMs = it.createdAtEpochMs,
                        updatedAtEpochMs = it.updatedAtEpochMs
                    )
                },
                attendanceRecords = records.map {
                    BackupAttendanceRecordDto(
                        recordId = it.recordId,
                        subjectId = it.subjectId,
                        classDate = it.classDate,
                        periodCount = it.periodCount,
                        status = it.status.name,
                        sessionLabel = it.sessionLabel,
                        notes = it.notes,
                        createdAtEpochMs = it.createdAtEpochMs,
                        updatedAtEpochMs = it.updatedAtEpochMs
                    )
                }
            )

            AppResult.Success(jsonCodec.encodeToString(payload))
        } catch (e: Exception) {
            AppResult.Error("Failed to export backup JSON: ${e.message ?: "Unexpected error"}", e)
        }
    }

    /**
     * Executes an atomic full replacement inside a single Room transaction after validation succeeds.
     * Preferences are restored only after the database transaction succeeds.
     */
    suspend fun executeConfirmedRestore(preview: BackupPreviewSummary): AppResult<Unit> {
        val payload = preview.validatedPayload

        // If the backup has semesters but none is marked ACTIVE, promote the most recent semester to ACTIVE
        // so the app satisfies the post-onboarding invariant of having one active semester.
        val hasActive = payload.semesters.any { it.status == SemesterStatus.ACTIVE.name }
        val firstIdToActivate = if (!hasActive && payload.semesters.isNotEmpty()) {
            payload.semesters.maxByOrNull { it.startDate }?.semesterId
        } else {
            null
        }

        val semesterEntities = payload.semesters.map { dto ->
            SemesterEntity(
                semesterId = dto.semesterId,
                name = dto.name.trim(),
                academicYearLabel = dto.academicYearLabel?.trim()?.takeIf { it.isNotEmpty() },
                startDate = dto.startDate.trim(),
                endDate = dto.endDate?.trim()?.takeIf { it.isNotEmpty() },
                defaultTargetBp = dto.defaultTargetBp,
                status = if (dto.semesterId == firstIdToActivate) {
                    SemesterStatus.ACTIVE
                } else {
                    SemesterStatus.valueOf(dto.status)
                },
                createdAtEpochMs = dto.createdAtEpochMs,
                updatedAtEpochMs = dto.updatedAtEpochMs
            )
        }

        val subjectEntities = payload.subjects.map { dto ->
            SubjectEntity(
                subjectId = dto.subjectId,
                semesterId = dto.semesterId,
                name = dto.name.trim(),
                code = dto.code?.trim()?.takeIf { it.isNotEmpty() },
                shortName = dto.shortName?.trim()?.takeIf { it.isNotEmpty() },
                targetOverrideBp = dto.targetOverrideBp,
                colorHex = dto.colorHex.trim().uppercase(),
                isArchived = dto.isArchived,
                createdAtEpochMs = dto.createdAtEpochMs,
                updatedAtEpochMs = dto.updatedAtEpochMs
            )
        }

        val recordEntities = payload.attendanceRecords.map { dto ->
            AttendanceRecordEntity(
                recordId = dto.recordId,
                subjectId = dto.subjectId,
                classDate = dto.classDate.trim(),
                periodCount = dto.periodCount,
                status = AttendanceEntryStatus.valueOf(dto.status),
                sessionLabel = dto.sessionLabel?.trim()?.takeIf { it.isNotEmpty() },
                notes = dto.notes?.trim()?.takeIf { it.isNotEmpty() },
                createdAtEpochMs = dto.createdAtEpochMs,
                updatedAtEpochMs = dto.updatedAtEpochMs
            )
        }

        return try {
            database.withTransaction {
                attendanceRecordDao.deleteAllRecords()
                subjectDao.deleteAllSubjects()
                semesterDao.deleteAllSemesters()

                semesterDao.insertSemesters(semesterEntities)
                subjectDao.insertSubjects(subjectEntities)
                attendanceRecordDao.insertRecords(recordEntities)
            }

            preferencesRepository.restorePreferences(
                themeMode = ThemeMode.valueOf(payload.preferences.themeMode),
                displayPrecision = payload.preferences.displayPrecision,
                weekStartsOn = WeekStartDay.valueOf(payload.preferences.weekStartsOn)
            )
            preferencesRepository.setOnboardingCompleted(semesterEntities.isNotEmpty())

            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Restore transaction failed; existing records were preserved: ${e.message ?: "Database error"}", e)
        }
    }
}

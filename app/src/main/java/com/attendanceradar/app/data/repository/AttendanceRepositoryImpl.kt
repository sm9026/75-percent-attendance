package com.attendanceradar.app.data.repository

import com.attendanceradar.app.core.common.AppResult
import com.attendanceradar.app.data.local.dao.AttendanceRecordDao
import com.attendanceradar.app.data.local.dao.SemesterDao
import com.attendanceradar.app.data.local.dao.SubjectDao
import com.attendanceradar.app.data.local.entity.AttendanceRecordEntity
import com.attendanceradar.app.data.local.mapper.toDomain
import com.attendanceradar.app.domain.calculator.AttendanceCalculator
import com.attendanceradar.app.domain.model.AttendanceEntryStatus
import com.attendanceradar.app.domain.model.AttendanceRecord
import com.attendanceradar.app.domain.model.DuplicateCheckResult
import com.attendanceradar.app.domain.model.SemesterStatus
import com.attendanceradar.app.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

class AttendanceRepositoryImpl(
    private val semesterDao: SemesterDao,
    private val subjectDao: SubjectDao,
    private val attendanceRecordDao: AttendanceRecordDao
) : AttendanceRepository {

    companion object {
        const val MAX_NOTES_LENGTH = 500
        const val MAX_SESSION_LABEL_LENGTH = 80
    }

    override fun observeRecordsForSubject(subjectId: String): Flow<List<AttendanceRecord>> =
        attendanceRecordDao.observeRecordsForSubject(subjectId).map { list -> list.map { it.toDomain() } }

    override fun observeRecordsForSemester(semesterId: String): Flow<List<AttendanceRecord>> =
        attendanceRecordDao.observeRecordsForSemester(semesterId).map { list -> list.map { it.toDomain() } }

    override fun observeRecordsForSemesterInDateRange(
        semesterId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<AttendanceRecord>> =
        attendanceRecordDao.observeRecordsForSemesterInDateRange(
            semesterId = semesterId,
            startDate = startDate.toString(),
            endDate = endDate.toString()
        ).map { list -> list.map { it.toDomain() } }

    override fun observeRecordsForDate(semesterId: String, date: LocalDate): Flow<List<AttendanceRecord>> =
        attendanceRecordDao.observeRecordsForDate(semesterId, date.toString())
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getRecordById(recordId: String): AttendanceRecord? =
        attendanceRecordDao.getRecordById(recordId)?.toDomain()

    override suspend fun checkPotentialDuplicate(
        subjectId: String,
        classDate: LocalDate,
        sessionLabel: String?,
        excludeRecordId: String?
    ): DuplicateCheckResult {
        val existing = attendanceRecordDao.getRecordsForSubjectAndDate(subjectId, classDate.toString())
            .filter { it.recordId != excludeRecordId }

        if (existing.isEmpty()) {
            return DuplicateCheckResult(
                hasExistingSessionOnDate = false,
                hasMatchingSessionLabel = false,
                existingRecordCount = 0,
                warningMessage = null
            )
        }

        val normalizedInputLabel = normalizeLabel(sessionLabel)
        if (normalizedInputLabel != null) {
            val labelMatches = existing.any { normalizeLabel(it.sessionLabel) == normalizedInputLabel }
            if (labelMatches) {
                return DuplicateCheckResult(
                    hasExistingSessionOnDate = true,
                    hasMatchingSessionLabel = true,
                    existingRecordCount = existing.size,
                    warningMessage = "An entry with session label \"${sessionLabel?.trim()}\" already exists for this subject on $classDate. Save as an additional session anyway?"
                )
            }
            return DuplicateCheckResult(
                hasExistingSessionOnDate = true,
                hasMatchingSessionLabel = false,
                existingRecordCount = existing.size,
                warningMessage = null
            )
        } else {
            return DuplicateCheckResult(
                hasExistingSessionOnDate = true,
                hasMatchingSessionLabel = false,
                existingRecordCount = existing.size,
                warningMessage = "${existing.size} session(s) already recorded for this subject on $classDate. Save an additional session?"
            )
        }
    }

    private fun normalizeLabel(label: String?): String? =
        label?.trim()?.replace(Regex("\\s+"), " ")?.lowercase(Locale.ROOT)?.takeIf { it.isNotEmpty() }

    override suspend fun addAttendanceRecord(
        subjectId: String,
        classDate: LocalDate,
        periodCount: Int,
        status: AttendanceEntryStatus,
        sessionLabel: String?,
        notes: String?
    ): AppResult<AttendanceRecord> {
        if (periodCount !in AttendanceCalculator.MIN_PERIOD_COUNT..AttendanceCalculator.MAX_PERIOD_COUNT) {
            return AppResult.Error("Period count must be between ${AttendanceCalculator.MIN_PERIOD_COUNT} and ${AttendanceCalculator.MAX_PERIOD_COUNT}.")
        }

        val subject = subjectDao.getSubjectById(subjectId)
            ?: return AppResult.Error("Selected subject does not exist.")
        val semester = semesterDao.getSemesterById(subject.semesterId)
            ?: return AppResult.Error("Semester for selected subject does not exist.")

        if (semester.status != SemesterStatus.ACTIVE) {
            return AppResult.Error("Cannot add attendance to an archived semester. Reopen the semester first.")
        }

        val cleanedLabel = sessionLabel?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanedLabel != null && cleanedLabel.length > MAX_SESSION_LABEL_LENGTH) {
            return AppResult.Error("Session label cannot exceed $MAX_SESSION_LABEL_LENGTH characters.")
        }

        val cleanedNotes = notes?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanedNotes != null && cleanedNotes.length > MAX_NOTES_LENGTH) {
            return AppResult.Error("Notes cannot exceed $MAX_NOTES_LENGTH characters.")
        }

        val now = System.currentTimeMillis()
        val entity = AttendanceRecordEntity(
            recordId = UUID.randomUUID().toString(),
            subjectId = subject.subjectId,
            classDate = classDate.toString(),
            periodCount = periodCount,
            status = status,
            sessionLabel = cleanedLabel,
            notes = cleanedNotes,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )

        return try {
            attendanceRecordDao.insertRecord(entity)
            AppResult.Success(entity.toDomain())
        } catch (e: Exception) {
            AppResult.Error("Failed to save attendance record: ${e.message ?: "Database error"}", e)
        }
    }

    override suspend fun updateAttendanceRecord(
        recordId: String,
        subjectId: String,
        classDate: LocalDate,
        periodCount: Int,
        status: AttendanceEntryStatus,
        sessionLabel: String?,
        notes: String?
    ): AppResult<AttendanceRecord> {
        val existing = attendanceRecordDao.getRecordById(recordId)
            ?: return AppResult.Error("Attendance record not found.")

        if (periodCount !in AttendanceCalculator.MIN_PERIOD_COUNT..AttendanceCalculator.MAX_PERIOD_COUNT) {
            return AppResult.Error("Period count must be between ${AttendanceCalculator.MIN_PERIOD_COUNT} and ${AttendanceCalculator.MAX_PERIOD_COUNT}.")
        }

        val subject = subjectDao.getSubjectById(subjectId)
            ?: return AppResult.Error("Selected subject does not exist.")
        val semester = semesterDao.getSemesterById(subject.semesterId)
            ?: return AppResult.Error("Semester for selected subject does not exist.")

        if (semester.status != SemesterStatus.ACTIVE) {
            return AppResult.Error("Cannot edit records in an archived semester. Reopen the semester first.")
        }

        val cleanedLabel = sessionLabel?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanedLabel != null && cleanedLabel.length > MAX_SESSION_LABEL_LENGTH) {
            return AppResult.Error("Session label cannot exceed $MAX_SESSION_LABEL_LENGTH characters.")
        }

        val cleanedNotes = notes?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanedNotes != null && cleanedNotes.length > MAX_NOTES_LENGTH) {
            return AppResult.Error("Notes cannot exceed $MAX_NOTES_LENGTH characters.")
        }

        val updated = existing.copy(
            subjectId = subject.subjectId,
            classDate = classDate.toString(),
            periodCount = periodCount,
            status = status,
            sessionLabel = cleanedLabel,
            notes = cleanedNotes,
            updatedAtEpochMs = System.currentTimeMillis()
        )

        return try {
            attendanceRecordDao.updateRecord(updated)
            AppResult.Success(updated.toDomain())
        } catch (e: Exception) {
            AppResult.Error("Failed to update attendance record: ${e.message ?: "Database error"}", e)
        }
    }

    override suspend fun deleteAttendanceRecord(recordId: String): AppResult<Unit> {
        val existing = attendanceRecordDao.getRecordById(recordId)
            ?: return AppResult.Error("Attendance record not found.")
        return try {
            attendanceRecordDao.deleteRecordById(existing.recordId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Failed to delete record: ${e.message ?: "Database error"}", e)
        }
    }
}

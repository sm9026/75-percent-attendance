package com.attendanceradar.app.data.repository

import com.attendanceradar.app.core.common.AppResult
import com.attendanceradar.app.core.design.SubjectPalette
import com.attendanceradar.app.data.local.dao.AttendanceRecordDao
import com.attendanceradar.app.data.local.dao.SemesterDao
import com.attendanceradar.app.data.local.dao.SubjectDao
import com.attendanceradar.app.data.local.entity.SubjectEntity
import com.attendanceradar.app.data.local.mapper.toDomain
import com.attendanceradar.app.domain.calculator.AttendanceCalculator
import com.attendanceradar.app.domain.calculator.AttendanceRecordSlice
import com.attendanceradar.app.domain.model.Subject
import com.attendanceradar.app.domain.model.SubjectWithAttendance
import com.attendanceradar.app.domain.repository.SubjectRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID

class SubjectRepositoryImpl(
    private val semesterDao: SemesterDao,
    private val subjectDao: SubjectDao,
    private val attendanceRecordDao: AttendanceRecordDao
) : SubjectRepository {

    override fun observeSubjectsForSemester(semesterId: String): Flow<List<Subject>> =
        subjectDao.observeSubjectsForSemester(semesterId).map { list -> list.map { it.toDomain() } }

    override fun observeSubjectsWithAttendance(semesterId: String): Flow<List<SubjectWithAttendance>> {
        return combine(
            semesterDao.observeSemesterById(semesterId),
            subjectDao.observeSubjectsForSemester(semesterId),
            attendanceRecordDao.observeRecordsForSemester(semesterId)
        ) { semesterEntity, subjectEntities, recordEntities ->
            val defaultTargetBp = semesterEntity?.defaultTargetBp ?: AttendanceCalculator.DEFAULT_TARGET_BP
            val recordsBySubject = recordEntities.groupBy { it.subjectId }
            subjectEntities.map { subjectEntity ->
                val subject = subjectEntity.toDomain()
                val subjectRecords = recordsBySubject[subject.subjectId].orEmpty()
                val counts = AttendanceCalculator.aggregateSlices(
                    subjectRecords.map { AttendanceRecordSlice(it.periodCount, it.status) }
                )
                val effectiveTarget = subject.effectiveTargetBp(defaultTargetBp)
                val summary = AttendanceCalculator.calculateSummary(counts, effectiveTarget)
                SubjectWithAttendance(
                    subject = subject,
                    effectiveTargetBp = effectiveTarget,
                    isTargetOverridden = subject.targetOverrideBp != null,
                    summary = summary,
                    totalRecordCount = subjectRecords.size
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSubjectWithAttendance(subjectId: String): Flow<SubjectWithAttendance?> {
        return subjectDao.observeSubjectById(subjectId).flatMapLatest { subjectEntity ->
            if (subjectEntity == null) {
                flowOf(null)
            } else {
                combine(
                    semesterDao.observeSemesterById(subjectEntity.semesterId),
                    attendanceRecordDao.observeRecordsForSubject(subjectId)
                ) { semesterEntity, recordEntities ->
                    val subject = subjectEntity.toDomain()
                    val defaultTargetBp = semesterEntity?.defaultTargetBp ?: AttendanceCalculator.DEFAULT_TARGET_BP
                    val effectiveTarget = subject.effectiveTargetBp(defaultTargetBp)
                    val counts = AttendanceCalculator.aggregateSlices(
                        recordEntities.map { AttendanceRecordSlice(it.periodCount, it.status) }
                    )
                    val summary = AttendanceCalculator.calculateSummary(counts, effectiveTarget)
                    SubjectWithAttendance(
                        subject = subject,
                        effectiveTargetBp = effectiveTarget,
                        isTargetOverridden = subject.targetOverrideBp != null,
                        summary = summary,
                        totalRecordCount = recordEntities.size
                    )
                }
            }
        }
    }

    override suspend fun getSubjectById(subjectId: String): Subject? =
        subjectDao.getSubjectById(subjectId)?.toDomain()

    override suspend fun createSubject(
        semesterId: String,
        name: String,
        code: String?,
        shortName: String?,
        targetOverrideBp: Int?,
        colorHex: String
    ): AppResult<Subject> {
        val semester = semesterDao.getSemesterById(semesterId)
            ?: return AppResult.Error("Cannot add subject: semester not found.")

        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return AppResult.Error("Subject name is required.")
        }
        if (targetOverrideBp != null &&
            targetOverrideBp !in AttendanceCalculator.MIN_TARGET_BP..AttendanceCalculator.MAX_TARGET_BP
        ) {
            return AppResult.Error("Subject target override must be between 0.01% and 100.00%.")
        }

        val resolvedColor = if (SubjectPalette.isValidHex(colorHex)) {
            colorHex.trim().uppercase()
        } else {
            SubjectPalette.DEFAULT_HEX
        }

        val now = System.currentTimeMillis()
        val entity = SubjectEntity(
            subjectId = UUID.randomUUID().toString(),
            semesterId = semester.semesterId,
            name = trimmedName,
            code = code?.trim()?.takeIf { it.isNotEmpty() },
            shortName = shortName?.trim()?.takeIf { it.isNotEmpty() },
            targetOverrideBp = targetOverrideBp,
            colorHex = resolvedColor,
            isArchived = false,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )

        return try {
            subjectDao.insertSubject(entity)
            AppResult.Success(entity.toDomain())
        } catch (e: Exception) {
            AppResult.Error("Failed to create subject: ${e.message ?: "Database error"}", e)
        }
    }

    override suspend fun updateSubject(
        subjectId: String,
        name: String,
        code: String?,
        shortName: String?,
        targetOverrideBp: Int?,
        colorHex: String,
        isArchived: Boolean
    ): AppResult<Subject> {
        val existing = subjectDao.getSubjectById(subjectId)
            ?: return AppResult.Error("Subject not found.")

        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return AppResult.Error("Subject name is required.")
        }
        if (targetOverrideBp != null &&
            targetOverrideBp !in AttendanceCalculator.MIN_TARGET_BP..AttendanceCalculator.MAX_TARGET_BP
        ) {
            return AppResult.Error("Subject target override must be between 0.01% and 100.00%.")
        }

        val resolvedColor = if (SubjectPalette.isValidHex(colorHex)) {
            colorHex.trim().uppercase()
        } else {
            existing.colorHex
        }

        val updated = existing.copy(
            name = trimmedName,
            code = code?.trim()?.takeIf { it.isNotEmpty() },
            shortName = shortName?.trim()?.takeIf { it.isNotEmpty() },
            targetOverrideBp = targetOverrideBp,
            colorHex = resolvedColor,
            isArchived = isArchived,
            updatedAtEpochMs = System.currentTimeMillis()
        )

        return try {
            subjectDao.updateSubject(updated)
            AppResult.Success(updated.toDomain())
        } catch (e: Exception) {
            AppResult.Error("Failed to update subject: ${e.message ?: "Database error"}", e)
        }
    }

    override suspend fun setSubjectArchived(subjectId: String, archived: Boolean): AppResult<Unit> {
        val existing = subjectDao.getSubjectById(subjectId)
            ?: return AppResult.Error("Subject not found.")
        return try {
            subjectDao.setSubjectArchived(
                subjectId = existing.subjectId,
                archived = archived,
                updatedAt = System.currentTimeMillis()
            )
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Failed to update subject archive state: ${e.message ?: "Database error"}", e)
        }
    }

    override suspend fun deleteSubjectAndRecords(subjectId: String): AppResult<Int> {
        val existing = subjectDao.getSubjectById(subjectId)
            ?: return AppResult.Error("Subject not found.")
        return try {
            val deletedRecordCount = subjectDao.deleteSubjectAndRecordsTransactional(existing.subjectId)
            AppResult.Success(deletedRecordCount)
        } catch (e: Exception) {
            AppResult.Error("Failed to delete subject: ${e.message ?: "Database error"}", e)
        }
    }
}

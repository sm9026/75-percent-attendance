package com.attendanceradar.app.data.repository

import com.attendanceradar.app.core.common.AppResult
import com.attendanceradar.app.data.local.dao.AttendanceRecordDao
import com.attendanceradar.app.data.local.dao.SemesterDao
import com.attendanceradar.app.data.local.dao.SubjectDao
import com.attendanceradar.app.data.local.entity.SemesterEntity
import com.attendanceradar.app.data.local.mapper.toDomain
import com.attendanceradar.app.domain.calculator.AttendanceCalculator
import com.attendanceradar.app.domain.calculator.AttendanceRecordSlice
import com.attendanceradar.app.domain.calculator.PeriodCounts
import com.attendanceradar.app.domain.model.Semester
import com.attendanceradar.app.domain.model.SemesterOverview
import com.attendanceradar.app.domain.model.SemesterStatus
import com.attendanceradar.app.domain.model.SubjectWithAttendance
import com.attendanceradar.app.domain.repository.SemesterRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class SemesterRepositoryImpl(
    private val semesterDao: SemesterDao,
    private val subjectDao: SubjectDao,
    private val attendanceRecordDao: AttendanceRecordDao
) : SemesterRepository {

    override fun observeAllSemesters(): Flow<List<Semester>> =
        semesterDao.observeAllSemesters().map { list -> list.map { it.toDomain() } }

    override fun observeActiveSemester(): Flow<Semester?> =
        semesterDao.observeActiveSemester().map { it?.toDomain() }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActiveSemesterOverview(): Flow<SemesterOverview?> =
        semesterDao.observeActiveSemester().flatMapLatest { activeEntity ->
            if (activeEntity == null) {
                flowOf(null)
            } else {
                observeSemesterOverview(activeEntity.semesterId)
            }
        }

    override fun observeSemesterOverview(semesterId: String): Flow<SemesterOverview?> {
        return combine(
            semesterDao.observeSemesterById(semesterId),
            subjectDao.observeSubjectsForSemester(semesterId),
            attendanceRecordDao.observeRecordsForSemester(semesterId)
        ) { semesterEntity, subjectEntities, recordEntities ->
            if (semesterEntity == null) return@combine null
            val semester = semesterEntity.toDomain()
            val recordsBySubject = recordEntities.groupBy { it.subjectId }

            val subjectItems = subjectEntities.map { subjectEntity ->
                val subject = subjectEntity.toDomain()
                val subjectRecords = recordsBySubject[subject.subjectId].orEmpty()
                val counts = AttendanceCalculator.aggregateSlices(
                    subjectRecords.map { AttendanceRecordSlice(it.periodCount, it.status) }
                )
                val effectiveTarget = subject.effectiveTargetBp(semester.defaultTargetBp)
                val summary = AttendanceCalculator.calculateSummary(counts, effectiveTarget)
                SubjectWithAttendance(
                    subject = subject,
                    effectiveTargetBp = effectiveTarget,
                    isTargetOverridden = subject.targetOverrideBp != null,
                    summary = summary,
                    totalRecordCount = subjectRecords.size
                )
            }

            // CRITICAL DOMAIN RULE:
            // Semester overall = sum(P) / sum(C) across ALL subjects in the semester (including archived subjects).
            // Never average subject percentages!
            // Overall semester status uses the semester default target.
            val allCounts = subjectItems.map {
                PeriodCounts(
                    presentPeriods = it.summary.presentPeriods,
                    absentPeriods = it.summary.absentPeriods,
                    cancelledPeriods = it.summary.cancelledPeriods
                )
            }
            val aggregatedCounts = AttendanceCalculator.aggregatePeriodCounts(allCounts)
            val overallSummary = AttendanceCalculator.calculateSummary(
                counts = aggregatedCounts,
                targetBp = semester.defaultTargetBp
            )

            SemesterOverview(
                semester = semester,
                overallSummary = overallSummary,
                subjects = subjectItems
            )
        }
    }

    override suspend fun getActiveSemester(): Semester? =
        semesterDao.getActiveSemester()?.toDomain()

    override suspend fun getSemesterById(semesterId: String): Semester? =
        semesterDao.getSemesterById(semesterId)?.toDomain()

    override suspend fun createAndActivateSemester(
        name: String,
        academicYearLabel: String?,
        startDate: LocalDate,
        endDate: LocalDate?,
        defaultTargetBp: Int
    ): AppResult<Semester> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return AppResult.Error("Semester name is required.")
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            return AppResult.Error("Semester end date cannot be earlier than the start date.")
        }
        if (defaultTargetBp !in AttendanceCalculator.MIN_TARGET_BP..AttendanceCalculator.MAX_TARGET_BP) {
            return AppResult.Error("Target percentage must be between 0.01% and 100.00%.")
        }

        val now = System.currentTimeMillis()
        val entity = SemesterEntity(
            semesterId = UUID.randomUUID().toString(),
            name = trimmedName,
            academicYearLabel = academicYearLabel?.trim()?.takeIf { it.isNotEmpty() },
            startDate = startDate.toString(),
            endDate = endDate?.toString(),
            defaultTargetBp = defaultTargetBp,
            status = SemesterStatus.ACTIVE,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )

        return try {
            semesterDao.insertAndActivateSemesterTransactional(entity, now)
            AppResult.Success(entity.toDomain())
        } catch (e: Exception) {
            AppResult.Error("Failed to create semester: ${e.message ?: "Database error"}", e)
        }
    }

    override suspend fun updateSemester(
        semesterId: String,
        name: String,
        academicYearLabel: String?,
        startDate: LocalDate,
        endDate: LocalDate?,
        defaultTargetBp: Int
    ): AppResult<Semester> {
        val existing = semesterDao.getSemesterById(semesterId)
            ?: return AppResult.Error("Semester not found.")

        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return AppResult.Error("Semester name is required.")
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            return AppResult.Error("Semester end date cannot be earlier than the start date.")
        }
        if (defaultTargetBp !in AttendanceCalculator.MIN_TARGET_BP..AttendanceCalculator.MAX_TARGET_BP) {
            return AppResult.Error("Target percentage must be between 0.01% and 100.00%.")
        }

        val updated = existing.copy(
            name = trimmedName,
            academicYearLabel = academicYearLabel?.trim()?.takeIf { it.isNotEmpty() },
            startDate = startDate.toString(),
            endDate = endDate?.toString(),
            defaultTargetBp = defaultTargetBp,
            updatedAtEpochMs = System.currentTimeMillis()
        )

        return try {
            semesterDao.updateSemester(updated)
            AppResult.Success(updated.toDomain())
        } catch (e: Exception) {
            AppResult.Error("Failed to update semester: ${e.message ?: "Database error"}", e)
        }
    }

    override suspend fun switchActiveSemester(targetSemesterId: String): AppResult<Semester> {
        val existing = semesterDao.getSemesterById(targetSemesterId)
            ?: return AppResult.Error("Target semester not found.")

        return try {
            val now = System.currentTimeMillis()
            semesterDao.switchActiveSemesterTransactional(targetSemesterId, now)
            val refreshed = semesterDao.getSemesterById(targetSemesterId) ?: existing
            AppResult.Success(refreshed.toDomain())
        } catch (e: Exception) {
            AppResult.Error("Failed to switch semester: ${e.message ?: "Database error"}", e)
        }
    }

    override suspend fun archiveSemester(semesterId: String): AppResult<Unit> {
        val existing = semesterDao.getSemesterById(semesterId)
            ?: return AppResult.Error("Semester not found.")
        return try {
            semesterDao.setSemesterStatus(
                semesterId = existing.semesterId,
                status = SemesterStatus.ARCHIVED,
                updatedAt = System.currentTimeMillis()
            )
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Failed to archive semester: ${e.message ?: "Database error"}", e)
        }
    }
}

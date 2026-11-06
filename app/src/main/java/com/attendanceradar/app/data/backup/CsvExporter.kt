package com.attendanceradar.app.data.backup

import com.attendanceradar.app.core.common.AppResult
import com.attendanceradar.app.data.local.dao.AttendanceRecordDao
import com.attendanceradar.app.data.local.dao.SemesterDao
import com.attendanceradar.app.data.local.dao.SubjectDao
import com.attendanceradar.app.data.local.entity.AttendanceRecordEntity
import com.attendanceradar.app.data.local.entity.SemesterEntity
import com.attendanceradar.app.data.local.entity.SubjectEntity
import java.time.Instant

enum class CsvExportScope {
    ACTIVE_SEMESTER,
    SELECTED_SEMESTER,
    ALL_SEMESTERS
}

/**
 * Generates RFC-4180 compliant UTF-8 CSV exports for attendance analysis.
 * Includes PRESENT, ABSENT, and CANCELLED rows.
 */
class CsvExporter(
    private val semesterDao: SemesterDao,
    private val subjectDao: SubjectDao,
    private val attendanceRecordDao: AttendanceRecordDao
) {

    companion object {
        const val CSV_HEADER =
            "semester_name,subject_name,subject_code,class_date,status,period_count,session_label,notes,created_at,updated_at"

        fun escapeCsvField(value: String?): String {
            if (value == null) return ""
            val needsQuotes = value.contains(',') ||
                value.contains('"') ||
                value.contains('\n') ||
                value.contains('\r')
            return if (needsQuotes) {
                "\"${value.replace("\"", "\"\"")}\""
            } else {
                value
            }
        }

        fun buildCsvString(
            semesters: List<SemesterEntity>,
            subjects: List<SubjectEntity>,
            records: List<AttendanceRecordEntity>
        ): String {
            val semesterMap = semesters.associateBy { it.semesterId }
            val subjectMap = subjects.associateBy { it.subjectId }

            val sb = StringBuilder()
            sb.append(CSV_HEADER).append("\n")

            val sortedRecords = records.sortedWith(
                compareBy<AttendanceRecordEntity> { it.classDate }
                    .thenBy { it.createdAtEpochMs }
                    .thenBy { it.recordId }
            )

            for (record in sortedRecords) {
                val subject = subjectMap[record.subjectId] ?: continue
                val semester = semesterMap[subject.semesterId] ?: continue

                val row = listOf(
                    escapeCsvField(semester.name),
                    escapeCsvField(subject.name),
                    escapeCsvField(subject.code ?: ""),
                    escapeCsvField(record.classDate),
                    escapeCsvField(record.status.name),
                    record.periodCount.toString(),
                    escapeCsvField(record.sessionLabel ?: ""),
                    escapeCsvField(record.notes ?: ""),
                    escapeCsvField(Instant.ofEpochMilli(record.createdAtEpochMs).toString()),
                    escapeCsvField(Instant.ofEpochMilli(record.updatedAtEpochMs).toString())
                ).joinToString(",")

                sb.append(row).append("\n")
            }

            return sb.toString()
        }
    }

    suspend fun exportCsv(
        scope: CsvExportScope,
        selectedSemesterId: String? = null
    ): AppResult<String> {
        return try {
            val semesters: List<SemesterEntity>
            val subjects: List<SubjectEntity>
            val records: List<AttendanceRecordEntity>

            when (scope) {
                CsvExportScope.ACTIVE_SEMESTER -> {
                    val active = semesterDao.getActiveSemester()
                        ?: return AppResult.Error("No active semester found to export.")
                    semesters = listOf(active)
                    subjects = subjectDao.getSubjectsForSemesterSnapshot(active.semesterId)
                    records = attendanceRecordDao.getRecordsForSemesterSnapshot(active.semesterId)
                }
                CsvExportScope.SELECTED_SEMESTER -> {
                    val semId = selectedSemesterId
                        ?: return AppResult.Error("Please select a semester to export.")
                    val sem = semesterDao.getSemesterById(semId)
                        ?: return AppResult.Error("Selected semester not found.")
                    semesters = listOf(sem)
                    subjects = subjectDao.getSubjectsForSemesterSnapshot(sem.semesterId)
                    records = attendanceRecordDao.getRecordsForSemesterSnapshot(sem.semesterId)
                }
                CsvExportScope.ALL_SEMESTERS -> {
                    semesters = semesterDao.getAllSemestersSnapshot()
                    subjects = subjectDao.getAllSubjectsSnapshot()
                    records = attendanceRecordDao.getAllRecordsSnapshot()
                }
            }

            AppResult.Success(buildCsvString(semesters, subjects, records))
        } catch (e: Exception) {
            AppResult.Error("Failed to generate CSV export: ${e.message ?: "Unexpected error"}", e)
        }
    }
}

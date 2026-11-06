package com.attendanceradar.app.data.backup

import com.attendanceradar.app.core.common.AppResult
import com.attendanceradar.app.data.local.entity.AttendanceRecordEntity
import com.attendanceradar.app.data.local.entity.SemesterEntity
import com.attendanceradar.app.data.local.entity.SubjectEntity
import com.attendanceradar.app.domain.model.AttendanceEntryStatus
import com.attendanceradar.app.domain.model.SemesterStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupAndCsvTest {

    @Test
    fun validBackupJson_passesValidationAndProducesCorrectPreviewCounts() {
        val validJson = """
        {
          "format": "attendance-radar-backup",
          "backupVersion": 1,
          "appVersion": "1.0.0",
          "exportedAt": "2026-11-05T12:00:00Z",
          "preferences": {
            "themeMode": "DARK",
            "displayPrecision": 1,
            "weekStartsOn": "MONDAY"
          },
          "semesters": [
            {
              "semesterId": "sem-1",
              "name": "Semester 5",
              "academicYearLabel": "2026-27",
              "startDate": "2026-08-01",
              "endDate": "2026-12-15",
              "defaultTargetBp": 7500,
              "status": "ACTIVE",
              "createdAtEpochMs": 1000,
              "updatedAtEpochMs": 1000
            }
          ],
          "subjects": [
            {
              "subjectId": "sub-1",
              "semesterId": "sem-1",
              "name": "Operating Systems",
              "code": "CS301",
              "shortName": "OS",
              "targetOverrideBp": null,
              "colorHex": "#1B4FD8",
              "isArchived": false,
              "createdAtEpochMs": 1000,
              "updatedAtEpochMs": 1000
            }
          ],
          "attendanceRecords": [
            {
              "recordId": "rec-1",
              "subjectId": "sub-1",
              "classDate": "2026-11-01",
              "periodCount": 2,
              "status": "PRESENT",
              "sessionLabel": "Lecture 1",
              "notes": "Scheduling",
              "createdAtEpochMs": 1000,
              "updatedAtEpochMs": 1000
            }
          ]
        }
        """.trimIndent()

        val result = BackupService.parseAndValidateBackupJson(validJson)
        assertTrue(result is AppResult.Success)
        val preview = (result as AppResult.Success).data
        assertEquals(1, preview.semesterCount)
        assertEquals(1, preview.subjectCount)
        assertEquals(1, preview.attendanceRecordCount)
        assertEquals("Semester 5", preview.activeSemesterName)
    }

    @Test
    fun backupWithBrokenSubjectForeignKey_isRejectedBeforeAnyMutation() {
        val brokenFkJson = """
        {
          "format": "attendance-radar-backup",
          "backupVersion": 1,
          "appVersion": "1.0.0",
          "exportedAt": "2026-11-05T12:00:00Z",
          "preferences": {
            "themeMode": "SYSTEM",
            "displayPrecision": 1,
            "weekStartsOn": "MONDAY"
          },
          "semesters": [],
          "subjects": [
            {
              "subjectId": "sub-1",
              "semesterId": "missing-semester-id",
              "name": "Operating Systems",
              "colorHex": "#1B4FD8",
              "isArchived": false,
              "createdAtEpochMs": 1000,
              "updatedAtEpochMs": 1000
            }
          ],
          "attendanceRecords": []
        }
        """.trimIndent()

        val result = BackupService.parseAndValidateBackupJson(brokenFkJson)
        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message.contains("missing semesterId"))
    }

    @Test
    fun csvExporter_escapesCommasQuotesNewlinesAndIncludesCancelledRows() {
        val sem = SemesterEntity(
            semesterId = "sem-1",
            name = "Semester 5, Honours",
            academicYearLabel = "2026-27",
            startDate = "2026-08-01",
            endDate = null,
            defaultTargetBp = 7500,
            status = SemesterStatus.ACTIVE,
            createdAtEpochMs = 1000L,
            updatedAtEpochMs = 1000L
        )
        val sub = SubjectEntity(
            subjectId = "sub-1",
            semesterId = "sem-1",
            name = "Systems \"Kernel\" Lab",
            code = "CS301",
            shortName = "OS",
            targetOverrideBp = null,
            colorHex = "#1B4FD8",
            isArchived = false,
            createdAtEpochMs = 1000L,
            updatedAtEpochMs = 1000L
        )
        val recPresent = AttendanceRecordEntity(
            recordId = "rec-1",
            subjectId = "sub-1",
            classDate = "2026-11-01",
            periodCount = 2,
            status = AttendanceEntryStatus.PRESENT,
            sessionLabel = "Lab, Group A",
            notes = "Line 1\nLine 2 with \"quote\"",
            createdAtEpochMs = 1000L,
            updatedAtEpochMs = 1000L
        )
        val recCancelled = AttendanceRecordEntity(
            recordId = "rec-2",
            subjectId = "sub-1",
            classDate = "2026-11-02",
            periodCount = 1,
            status = AttendanceEntryStatus.CANCELLED,
            sessionLabel = null,
            notes = "Public holiday",
            createdAtEpochMs = 2000L,
            updatedAtEpochMs = 2000L
        )

        val csv = CsvExporter.buildCsvString(
            semesters = listOf(sem),
            subjects = listOf(sub),
            records = listOf(recPresent, recCancelled)
        )

        assertTrue(csv.startsWith(CsvExporter.CSV_HEADER))
        assertTrue(csv.contains("\"Semester 5, Honours\""))
        assertTrue(csv.contains("\"Systems \"\"Kernel\"\" Lab\""))
        assertTrue(csv.contains("\"Lab, Group A\""))
        assertTrue(csv.contains("\"Line 1\nLine 2 with \"\"quote\"\"\""))
        assertTrue(csv.contains(",CANCELLED,1,"))
    }
}

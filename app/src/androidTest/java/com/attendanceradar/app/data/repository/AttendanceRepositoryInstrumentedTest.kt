package com.attendanceradar.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.attendanceradar.app.core.common.AppResult
import com.attendanceradar.app.data.local.AttendanceDatabase
import com.attendanceradar.app.domain.model.AttendanceEntryStatus
import com.attendanceradar.app.domain.model.SemesterStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class AttendanceRepositoryInstrumentedTest {

    private lateinit var database: AttendanceDatabase
    private lateinit var semesterRepository: SemesterRepositoryImpl
    private lateinit var subjectRepository: SubjectRepositoryImpl
    private lateinit var attendanceRepository: AttendanceRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AttendanceDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        semesterRepository = SemesterRepositoryImpl(
            semesterDao = database.semesterDao(),
            subjectDao = database.subjectDao(),
            attendanceRecordDao = database.attendanceRecordDao()
        )
        subjectRepository = SubjectRepositoryImpl(
            semesterDao = database.semesterDao(),
            subjectDao = database.subjectDao(),
            attendanceRecordDao = database.attendanceRecordDao()
        )
        attendanceRepository = AttendanceRepositoryImpl(
            semesterDao = database.semesterDao(),
            subjectDao = database.subjectDao(),
            attendanceRecordDao = database.attendanceRecordDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun semesterSwitch_archivesPreviousActiveAndGuaranteesSingleActiveSemester() = runBlocking {
        val sem1 = (semesterRepository.createAndActivateSemester(
            name = "Semester 1",
            academicYearLabel = "2025-26",
            startDate = LocalDate.of(2025, 8, 1),
            endDate = LocalDate.of(2025, 12, 15),
            defaultTargetBp = 7500
        ) as AppResult.Success).data

        val sem2 = (semesterRepository.createAndActivateSemester(
            name = "Semester 2",
            academicYearLabel = "2026-27",
            startDate = LocalDate.of(2026, 1, 10),
            endDate = null,
            defaultTargetBp = 7500
        ) as AppResult.Success).data

        val allAfterCreate = semesterRepository.observeAllSemesters().first()
        assertEquals(1, allAfterCreate.count { it.status == SemesterStatus.ACTIVE })
        assertEquals(sem2.semesterId, semesterRepository.getActiveSemester()?.semesterId)

        // Switch back to sem1
        semesterRepository.switchActiveSemester(sem1.semesterId)
        val allAfterSwitch = semesterRepository.observeAllSemesters().first()
        assertEquals(1, allAfterSwitch.count { it.status == SemesterStatus.ACTIVE })
        assertEquals(sem1.semesterId, semesterRepository.getActiveSemester()?.semesterId)
    }

    @Test
    fun attendanceCrudDuplicateWarningArchiveAndDelete_updateDerivedSummariesAccurately() = runBlocking {
        val sem = (semesterRepository.createAndActivateSemester(
            name = "Semester 5",
            academicYearLabel = null,
            startDate = LocalDate.of(2026, 8, 1),
            endDate = null,
            defaultTargetBp = 7500
        ) as AppResult.Success).data

        val subA = (subjectRepository.createSubject(
            semesterId = sem.semesterId,
            name = "Operating Systems",
            code = "CS301",
            shortName = "OS",
            targetOverrideBp = null,
            colorHex = "#1B4FD8"
        ) as AppResult.Success).data

        val subB = (subjectRepository.createSubject(
            semesterId = sem.semesterId,
            name = "Database Systems",
            code = "CS302",
            shortName = "DB",
            targetOverrideBp = 8000,
            colorHex = "#0A8765"
        ) as AppResult.Success).data

        val date = LocalDate.of(2026, 10, 12)
        attendanceRepository.addAttendanceRecord(
            subjectId = subA.subjectId,
            classDate = date,
            periodCount = 3,
            status = AttendanceEntryStatus.PRESENT,
            sessionLabel = "Lecture 1",
            notes = null
        )

        // Duplicate check warns on same label or same date, but allows saving a second session
        val dupCheck = attendanceRepository.checkPotentialDuplicate(
            subjectId = subA.subjectId,
            classDate = date,
            sessionLabel = "lecture 1"
        )
        assertTrue(dupCheck.hasMatchingSessionLabel)
        assertNotNull(dupCheck.warningMessage)

        attendanceRepository.addAttendanceRecord(
            subjectId = subA.subjectId,
            classDate = date,
            periodCount = 1,
            status = AttendanceEntryStatus.ABSENT,
            sessionLabel = "Lab",
            notes = null
        )
        attendanceRepository.addAttendanceRecord(
            subjectId = subA.subjectId,
            classDate = date,
            periodCount = 2,
            status = AttendanceEntryStatus.CANCELLED,
            sessionLabel = "Extra",
            notes = null
        )

        attendanceRepository.addAttendanceRecord(
            subjectId = subB.subjectId,
            classDate = date,
            periodCount = 4,
            status = AttendanceEntryStatus.PRESENT,
            sessionLabel = null,
            notes = null
        )

        // Archive subB: must remain included in semester overall totals
        subjectRepository.setSubjectArchived(subB.subjectId, true)

        val overview = semesterRepository.observeActiveSemesterOverview().first()!!
        // Total Present = 3 (subA) + 4 (archived subB) = 7; Total Conducted = 4 (subA) + 4 (subB) = 8; Cancelled = 2
        assertEquals(7L, overview.overallSummary.presentPeriods)
        assertEquals(8L, overview.overallSummary.conductedPeriods)
        assertEquals(2L, overview.overallSummary.cancelledPeriods)
        assertEquals(1, overview.activeSubjects.size)
        assertEquals(1, overview.archivedSubjects.size)

        // Confirmed deletion of subA removes its 3 attendance records transactionally
        val deleteResult = subjectRepository.deleteSubjectAndRecords(subA.subjectId)
        assertEquals(3, (deleteResult as AppResult.Success).data)
        val afterDeleteOverview = semesterRepository.observeActiveSemesterOverview().first()!!
        assertEquals(4L, afterDeleteOverview.overallSummary.presentPeriods)
        assertEquals(4L, afterDeleteOverview.overallSummary.conductedPeriods)
    }
}

package com.attendanceradar.app.data.local.mapper

import com.attendanceradar.app.data.local.entity.AttendanceRecordEntity
import com.attendanceradar.app.data.local.entity.SemesterEntity
import com.attendanceradar.app.data.local.entity.SubjectEntity
import com.attendanceradar.app.domain.model.AttendanceRecord
import com.attendanceradar.app.domain.model.Semester
import com.attendanceradar.app.domain.model.Subject
import java.time.LocalDate

fun SemesterEntity.toDomain(): Semester = Semester(
    semesterId = semesterId,
    name = name,
    academicYearLabel = academicYearLabel,
    startDate = LocalDate.parse(startDate),
    endDate = endDate?.let { LocalDate.parse(it) },
    defaultTargetBp = defaultTargetBp,
    status = status,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun Semester.toEntity(): SemesterEntity = SemesterEntity(
    semesterId = semesterId,
    name = name,
    academicYearLabel = academicYearLabel,
    startDate = startDate.toString(),
    endDate = endDate?.toString(),
    defaultTargetBp = defaultTargetBp,
    status = status,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun SubjectEntity.toDomain(): Subject = Subject(
    subjectId = subjectId,
    semesterId = semesterId,
    name = name,
    code = code,
    shortName = shortName,
    targetOverrideBp = targetOverrideBp,
    colorHex = colorHex,
    isArchived = isArchived,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun Subject.toEntity(): SubjectEntity = SubjectEntity(
    subjectId = subjectId,
    semesterId = semesterId,
    name = name,
    code = code,
    shortName = shortName,
    targetOverrideBp = targetOverrideBp,
    colorHex = colorHex,
    isArchived = isArchived,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun AttendanceRecordEntity.toDomain(): AttendanceRecord = AttendanceRecord(
    recordId = recordId,
    subjectId = subjectId,
    classDate = LocalDate.parse(classDate),
    periodCount = periodCount,
    status = status,
    sessionLabel = sessionLabel,
    notes = notes,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun AttendanceRecord.toEntity(): AttendanceRecordEntity = AttendanceRecordEntity(
    recordId = recordId,
    subjectId = subjectId,
    classDate = classDate.toString(),
    periodCount = periodCount,
    status = status,
    sessionLabel = sessionLabel,
    notes = notes,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

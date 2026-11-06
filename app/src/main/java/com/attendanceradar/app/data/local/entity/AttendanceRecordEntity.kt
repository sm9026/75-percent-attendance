package com.attendanceradar.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.attendanceradar.app.domain.model.AttendanceEntryStatus

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["subjectId"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["subjectId"], name = "index_attendance_records_subjectId"),
        Index(value = ["classDate"], name = "index_attendance_records_classDate"),
        Index(value = ["subjectId", "classDate"], name = "index_attendance_records_subjectId_classDate")
    ]
)
data class AttendanceRecordEntity(
    @PrimaryKey
    val recordId: String,
    val subjectId: String,
    val classDate: String,
    val periodCount: Int,
    val status: AttendanceEntryStatus,
    val sessionLabel: String?,
    val notes: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

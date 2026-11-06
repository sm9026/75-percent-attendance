package com.attendanceradar.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.attendanceradar.app.domain.model.SemesterStatus

@Entity(
    tableName = "semesters",
    indices = [
        Index(value = ["status"], name = "index_semesters_status")
    ]
)
data class SemesterEntity(
    @PrimaryKey
    val semesterId: String,
    val name: String,
    val academicYearLabel: String?,
    val startDate: String,
    val endDate: String?,
    val defaultTargetBp: Int,
    val status: SemesterStatus,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

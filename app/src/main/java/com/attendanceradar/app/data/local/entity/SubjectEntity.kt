package com.attendanceradar.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subjects",
    foreignKeys = [
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["semesterId"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["semesterId"], name = "index_subjects_semesterId"),
        Index(value = ["semesterId", "isArchived"], name = "index_subjects_semesterId_isArchived")
    ]
)
data class SubjectEntity(
    @PrimaryKey
    val subjectId: String,
    val semesterId: String,
    val name: String,
    val code: String?,
    val shortName: String?,
    val targetOverrideBp: Int?,
    val colorHex: String,
    val isArchived: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

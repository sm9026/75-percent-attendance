package com.attendanceradar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.attendanceradar.app.data.local.entity.SemesterEntity
import com.attendanceradar.app.domain.model.SemesterStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {

    @Query("SELECT * FROM semesters ORDER BY CASE WHEN status = 'ACTIVE' THEN 0 ELSE 1 END, startDate DESC, createdAtEpochMs DESC")
    fun observeAllSemesters(): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters ORDER BY createdAtEpochMs ASC")
    suspend fun getAllSemestersSnapshot(): List<SemesterEntity>

    @Query("SELECT * FROM semesters WHERE status = 'ACTIVE' LIMIT 1")
    fun observeActiveSemester(): Flow<SemesterEntity?>

    @Query("SELECT * FROM semesters WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveSemester(): SemesterEntity?

    @Query("SELECT * FROM semesters WHERE semesterId = :semesterId LIMIT 1")
    fun observeSemesterById(semesterId: String): Flow<SemesterEntity?>

    @Query("SELECT * FROM semesters WHERE semesterId = :semesterId LIMIT 1")
    suspend fun getSemesterById(semesterId: String): SemesterEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSemester(semester: SemesterEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSemesters(semesters: List<SemesterEntity>)

    @Update
    suspend fun updateSemester(semester: SemesterEntity)

    @Query("UPDATE semesters SET status = :status, updatedAtEpochMs = :updatedAt WHERE status = 'ACTIVE'")
    suspend fun archiveAllActiveSemesters(
        status: SemesterStatus = SemesterStatus.ARCHIVED,
        updatedAt: Long
    )

    @Query("UPDATE semesters SET status = :status, updatedAtEpochMs = :updatedAt WHERE semesterId = :semesterId")
    suspend fun setSemesterStatus(
        semesterId: String,
        status: SemesterStatus,
        updatedAt: Long
    )

    @Transaction
    suspend fun insertAndActivateSemesterTransactional(
        newSemester: SemesterEntity,
        updatedAt: Long
    ) {
        archiveAllActiveSemesters(SemesterStatus.ARCHIVED, updatedAt)
        insertSemester(newSemester.copy(status = SemesterStatus.ACTIVE))
    }

    @Transaction
    suspend fun switchActiveSemesterTransactional(
        targetSemesterId: String,
        updatedAt: Long
    ) {
        archiveAllActiveSemesters(SemesterStatus.ARCHIVED, updatedAt)
        setSemesterStatus(targetSemesterId, SemesterStatus.ACTIVE, updatedAt)
    }

    @Query("DELETE FROM semesters")
    suspend fun deleteAllSemesters()
}

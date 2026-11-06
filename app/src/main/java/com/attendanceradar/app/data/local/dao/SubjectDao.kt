package com.attendanceradar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.attendanceradar.app.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Query("SELECT * FROM subjects WHERE semesterId = :semesterId ORDER BY isArchived ASC, createdAtEpochMs ASC, subjectId ASC")
    fun observeSubjectsForSemester(semesterId: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE semesterId = :semesterId ORDER BY createdAtEpochMs ASC, subjectId ASC")
    suspend fun getSubjectsForSemesterSnapshot(semesterId: String): List<SubjectEntity>

    @Query("SELECT * FROM subjects ORDER BY createdAtEpochMs ASC, subjectId ASC")
    suspend fun getAllSubjectsSnapshot(): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE subjectId = :subjectId LIMIT 1")
    fun observeSubjectById(subjectId: String): Flow<SubjectEntity?>

    @Query("SELECT * FROM subjects WHERE subjectId = :subjectId LIMIT 1")
    suspend fun getSubjectById(subjectId: String): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSubject(subject: SubjectEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSubjects(subjects: List<SubjectEntity>)

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Query("UPDATE subjects SET isArchived = :archived, updatedAtEpochMs = :updatedAt WHERE subjectId = :subjectId")
    suspend fun setSubjectArchived(subjectId: String, archived: Boolean, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM attendance_records WHERE subjectId = :subjectId")
    suspend fun countRecordsForSubject(subjectId: String): Int

    @Query("DELETE FROM attendance_records WHERE subjectId = :subjectId")
    suspend fun deleteRecordsForSubject(subjectId: String)

    @Query("DELETE FROM subjects WHERE subjectId = :subjectId")
    suspend fun deleteSubjectById(subjectId: String)

    @Transaction
    suspend fun deleteSubjectAndRecordsTransactional(subjectId: String): Int {
        val count = countRecordsForSubject(subjectId)
        deleteRecordsForSubject(subjectId)
        deleteSubjectById(subjectId)
        return count
    }

    @Query("DELETE FROM subjects")
    suspend fun deleteAllSubjects()
}

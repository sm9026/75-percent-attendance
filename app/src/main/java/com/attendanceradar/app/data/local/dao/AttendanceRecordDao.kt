package com.attendanceradar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.attendanceradar.app.data.local.entity.AttendanceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceRecordDao {

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId ORDER BY classDate DESC, createdAtEpochMs DESC, recordId DESC")
    fun observeRecordsForSubject(subjectId: String): Flow<List<AttendanceRecordEntity>>

    @Query(
        """
        SELECT ar.* FROM attendance_records ar
        INNER JOIN subjects s ON ar.subjectId = s.subjectId
        WHERE s.semesterId = :semesterId
        ORDER BY ar.classDate DESC, ar.createdAtEpochMs DESC, ar.recordId DESC
        """
    )
    fun observeRecordsForSemester(semesterId: String): Flow<List<AttendanceRecordEntity>>

    @Query(
        """
        SELECT ar.* FROM attendance_records ar
        INNER JOIN subjects s ON ar.subjectId = s.subjectId
        WHERE s.semesterId = :semesterId
          AND ar.classDate >= :startDate
          AND ar.classDate <= :endDate
        ORDER BY ar.classDate DESC, ar.createdAtEpochMs DESC, ar.recordId DESC
        """
    )
    fun observeRecordsForSemesterInDateRange(
        semesterId: String,
        startDate: String,
        endDate: String
    ): Flow<List<AttendanceRecordEntity>>

    @Query(
        """
        SELECT ar.* FROM attendance_records ar
        INNER JOIN subjects s ON ar.subjectId = s.subjectId
        WHERE s.semesterId = :semesterId
          AND ar.classDate = :date
        ORDER BY s.name ASC, ar.createdAtEpochMs ASC, ar.recordId ASC
        """
    )
    fun observeRecordsForDate(semesterId: String, date: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId AND classDate = :classDate ORDER BY createdAtEpochMs ASC")
    suspend fun getRecordsForSubjectAndDate(
        subjectId: String,
        classDate: String
    ): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE recordId = :recordId LIMIT 1")
    suspend fun getRecordById(recordId: String): AttendanceRecordEntity?

    @Query("SELECT * FROM attendance_records ORDER BY classDate ASC, createdAtEpochMs ASC, recordId ASC")
    suspend fun getAllRecordsSnapshot(): List<AttendanceRecordEntity>

    @Query(
        """
        SELECT ar.* FROM attendance_records ar
        INNER JOIN subjects s ON ar.subjectId = s.subjectId
        WHERE s.semesterId = :semesterId
        ORDER BY ar.classDate ASC, ar.createdAtEpochMs ASC, ar.recordId ASC
        """
    )
    suspend fun getRecordsForSemesterSnapshot(semesterId: String): List<AttendanceRecordEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecord(record: AttendanceRecordEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecords(records: List<AttendanceRecordEntity>)

    @Update
    suspend fun updateRecord(record: AttendanceRecordEntity)

    @Query("DELETE FROM attendance_records WHERE recordId = :recordId")
    suspend fun deleteRecordById(recordId: String)

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAllRecords()
}

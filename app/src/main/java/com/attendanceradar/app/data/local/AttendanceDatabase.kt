package com.attendanceradar.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.attendanceradar.app.data.local.converter.RoomConverters
import com.attendanceradar.app.data.local.dao.AttendanceRecordDao
import com.attendanceradar.app.data.local.dao.SemesterDao
import com.attendanceradar.app.data.local.dao.SubjectDao
import com.attendanceradar.app.data.local.entity.AttendanceRecordEntity
import com.attendanceradar.app.data.local.entity.SemesterEntity
import com.attendanceradar.app.data.local.entity.SubjectEntity

@Database(
    entities = [
        SemesterEntity::class,
        SubjectEntity::class,
        AttendanceRecordEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(RoomConverters::class)
abstract class AttendanceDatabase : RoomDatabase() {

    abstract fun semesterDao(): SemesterDao
    abstract fun subjectDao(): SubjectDao
    abstract fun attendanceRecordDao(): AttendanceRecordDao

    companion object {
        const val DATABASE_NAME = "attendance_radar.db"

        /**
         * Explicit forward migration example for future schema changes.
         * Destructive migrations are intentionally NEVER enabled in release or production builds.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Schema v1 is current; future column/table migrations belong here.
            }
        }

        @Volatile
        private var INSTANCE: AttendanceDatabase? = null

        fun getInstance(context: Context): AttendanceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AttendanceDatabase {
            return Room.databaseBuilder(
                context,
                AttendanceDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}

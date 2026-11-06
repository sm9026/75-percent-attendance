package com.attendanceradar.app.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.attendanceradar.app.data.backup.BackupService
import com.attendanceradar.app.data.backup.CsvExporter
import com.attendanceradar.app.data.local.AttendanceDatabase
import com.attendanceradar.app.data.preferences.PreferencesRepositoryImpl
import com.attendanceradar.app.data.repository.AttendanceRepositoryImpl
import com.attendanceradar.app.data.repository.SemesterRepositoryImpl
import com.attendanceradar.app.data.repository.SubjectRepositoryImpl
import com.attendanceradar.app.domain.repository.AttendanceRepository
import com.attendanceradar.app.domain.repository.SemesterRepository
import com.attendanceradar.app.domain.repository.SubjectRepository
import com.attendanceradar.app.domain.repository.UserPreferencesRepository

interface AppContainer {
    val database: AttendanceDatabase
    val semesterRepository: SemesterRepository
    val subjectRepository: SubjectRepository
    val attendanceRepository: AttendanceRepository
    val preferencesRepository: UserPreferencesRepository
    val backupService: BackupService
    val csvExporter: CsvExporter
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext

    override val database: AttendanceDatabase by lazy {
        AttendanceDatabase.getInstance(appContext)
    }

    override val preferencesRepository: UserPreferencesRepository by lazy {
        PreferencesRepositoryImpl(appContext)
    }

    override val semesterRepository: SemesterRepository by lazy {
        SemesterRepositoryImpl(
            semesterDao = database.semesterDao(),
            subjectDao = database.subjectDao(),
            attendanceRecordDao = database.attendanceRecordDao()
        )
    }

    override val subjectRepository: SubjectRepository by lazy {
        SubjectRepositoryImpl(
            semesterDao = database.semesterDao(),
            subjectDao = database.subjectDao(),
            attendanceRecordDao = database.attendanceRecordDao()
        )
    }

    override val attendanceRepository: AttendanceRepository by lazy {
        AttendanceRepositoryImpl(
            semesterDao = database.semesterDao(),
            subjectDao = database.subjectDao(),
            attendanceRecordDao = database.attendanceRecordDao()
        )
    }

    override val backupService: BackupService by lazy {
        BackupService(
            database = database,
            semesterDao = database.semesterDao(),
            subjectDao = database.subjectDao(),
            attendanceRecordDao = database.attendanceRecordDao(),
            preferencesRepository = preferencesRepository
        )
    }

    override val csvExporter: CsvExporter by lazy {
        CsvExporter(
            semesterDao = database.semesterDao(),
            subjectDao = database.subjectDao(),
            attendanceRecordDao = database.attendanceRecordDao()
        )
    }
}

inline fun <reified VM : ViewModel> singleViewModelFactory(
    crossinline initializer: () -> VM
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return initializer() as T
    }
}

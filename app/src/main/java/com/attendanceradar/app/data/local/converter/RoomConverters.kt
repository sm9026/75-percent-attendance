package com.attendanceradar.app.data.local.converter

import androidx.room.TypeConverter
import com.attendanceradar.app.domain.model.AttendanceEntryStatus
import com.attendanceradar.app.domain.model.SemesterStatus
import java.time.LocalDate

class RoomConverters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromSemesterStatus(status: SemesterStatus?): String? = status?.name

    @TypeConverter
    fun toSemesterStatus(value: String?): SemesterStatus? =
        value?.let { SemesterStatus.valueOf(it) }

    @TypeConverter
    fun fromAttendanceEntryStatus(status: AttendanceEntryStatus?): String? = status?.name

    @TypeConverter
    fun toAttendanceEntryStatus(value: String?): AttendanceEntryStatus? =
        value?.let { AttendanceEntryStatus.valueOf(it) }
}

package com.attendanceradar.app.domain.model

enum class SemesterStatus {
    ACTIVE,
    ARCHIVED
}

enum class AttendanceEntryStatus {
    PRESENT,
    ABSENT,
    CANCELLED
}

enum class AttendanceHealthStatus {
    NO_RECORDS,
    BELOW_TARGET,
    AT_RISK,
    ON_TRACK
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class WeekStartDay {
    MONDAY,
    SUNDAY
}

enum class CalendarDayMarker {
    NONE,
    ALL_PRESENT,
    ALL_ABSENT,
    MIXED_PRESENT_ABSENT,
    ALL_CANCELLED
}

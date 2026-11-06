package com.attendanceradar.app.domain.calculator

import com.attendanceradar.app.domain.model.AttendanceEntryStatus
import com.attendanceradar.app.domain.model.AttendanceHealthStatus

data class PeriodCounts(
    val presentPeriods: Long = 0L,
    val absentPeriods: Long = 0L,
    val cancelledPeriods: Long = 0L
) {
    init {
        require(presentPeriods >= 0L) { "Present periods cannot be negative: $presentPeriods" }
        require(absentPeriods >= 0L) { "Absent periods cannot be negative: $absentPeriods" }
        require(cancelledPeriods >= 0L) { "Cancelled periods cannot be negative: $cancelledPeriods" }
    }

    val conductedPeriods: Long
        get() = Math.addExact(presentPeriods, absentPeriods)
}

data class AttendanceRecordSlice(
    val periodCount: Int,
    val status: AttendanceEntryStatus
) {
    init {
        require(periodCount > 0) { "Period count must be positive: $periodCount" }
    }
}

sealed interface RecoveryRequirement {
    /** Already at or above target, or no records yet. */
    data object NotNeeded : RecoveryRequirement

    /** Requires attending [periods] consecutive future Present periods with 0 absences. */
    data class Possible(val periods: Long) : RecoveryRequirement

    /** Target is 100% (10000 bp) and at least one absence already exists. */
    data object ImpossibleAt100Percent : RecoveryRequirement
}

data class AttendanceSummary(
    val presentPeriods: Long,
    val absentPeriods: Long,
    val cancelledPeriods: Long,
    val conductedPeriods: Long,
    val targetBp: Int,
    /** Exact percentage in 0.0..100.0 when conductedPeriods > 0, or null when conductedPeriods == 0. */
    val attendancePercentage: Double?,
    val meetsTarget: Boolean,
    val status: AttendanceHealthStatus,
    /** Non-null when conductedPeriods > 0; 0 when below target. */
    val safeAdditionalAbsences: Long?,
    val recoveryRequirement: RecoveryRequirement
) {
    val isNoRecords: Boolean get() = conductedPeriods == 0L
}

data class WhatIfProjection(
    val baseline: AttendanceSummary,
    val futurePresentPeriods: Long,
    val futureAbsentPeriods: Long,
    val futureCancelledPeriods: Long = 0L,
    val projected: AttendanceSummary
)

package com.attendanceradar.app.domain.usecase

import com.attendanceradar.app.core.common.DateFormatters
import com.attendanceradar.app.domain.calculator.AttendanceCalculator
import com.attendanceradar.app.domain.calculator.AttendanceRecordSlice
import com.attendanceradar.app.domain.calculator.AttendanceSummary
import com.attendanceradar.app.domain.model.AttendanceEntryStatus
import com.attendanceradar.app.domain.model.AttendanceRecord
import com.attendanceradar.app.domain.model.CalendarDayMarker
import com.attendanceradar.app.domain.model.CumulativeTrendPoint
import com.attendanceradar.app.domain.model.DateBucketAttendance
import com.attendanceradar.app.domain.model.WeekStartDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * Pure domain analytics functions for cumulative subject trends, calendar day markers,
 * and weekly/monthly date-range insights.
 */
object AttendanceAnalyticsUseCase {

    /**
     * Computes deterministic cumulative attendance trend points for a subject.
     * Ordered by classDate ascending, then createdAtEpochMs ascending, then recordId ascending.
     * Cancelled records do not alter cumulative Present or Conducted counts and are omitted from trend points.
     */
    fun computeCumulativeTrend(records: List<AttendanceRecord>): List<CumulativeTrendPoint> {
        val sorted = records.sortedWith(
            compareBy<AttendanceRecord> { it.classDate }
                .thenBy { it.createdAtEpochMs }
                .thenBy { it.recordId }
        )
        var cumP = 0L
        var cumC = 0L
        val points = mutableListOf<CumulativeTrendPoint>()
        for (record in sorted) {
            if (record.status == AttendanceEntryStatus.CANCELLED) continue
            val count = record.periodCount.toLong()
            if (record.status == AttendanceEntryStatus.PRESENT) {
                cumP = Math.addExact(cumP, count)
            }
            cumC = Math.addExact(cumC, count)
            if (cumC > 0L) {
                val pct = (cumP.toDouble() * 100.0) / cumC.toDouble()
                points.add(
                    CumulativeTrendPoint(
                        recordId = record.recordId,
                        date = record.classDate,
                        cumulativePresent = cumP,
                        cumulativeConducted = cumC,
                        cumulativePercentage = pct
                    )
                )
            }
        }
        return points
    }

    /**
     * Computes the daily status marker for every date that has at least one record.
     * - ALL_PRESENT: only Present periods recorded that day (Cancelled may or may not exist, wait:
     *   per PRODUCT_SPEC.md 3.7:
     *   - Green indicator: only Present periods recorded that day.
     *   - Red indicator: only Absent periods recorded that day.
     *   - Mixed indicator: both Present and Absent periods recorded that day.
     *   - Neutral indicator: records exist but all are Cancelled.
     *   - No indicator: no records.)
     */
    fun computeCalendarMarkers(records: List<AttendanceRecord>): Map<LocalDate, CalendarDayMarker> {
        if (records.isEmpty()) return emptyMap()
        return records.groupBy { it.classDate }.mapValues { (_, dayRecords) ->
            var hasPresent = false
            var hasAbsent = false
            var hasCancelled = false
            for (r in dayRecords) {
                when (r.status) {
                    AttendanceEntryStatus.PRESENT -> hasPresent = true
                    AttendanceEntryStatus.ABSENT -> hasAbsent = true
                    AttendanceEntryStatus.CANCELLED -> hasCancelled = true
                }
            }
            when {
                hasPresent && hasAbsent -> CalendarDayMarker.MIXED_PRESENT_ABSENT
                hasPresent && !hasAbsent -> CalendarDayMarker.ALL_PRESENT
                hasAbsent && !hasPresent -> CalendarDayMarker.ALL_ABSENT
                hasCancelled -> CalendarDayMarker.ALL_CANCELLED
                else -> CalendarDayMarker.NONE
            }
        }
    }

    /**
     * Computes attendance summary for records within [startDate, endDate] inclusive.
     */
    fun computeDateRangeSummary(
        records: List<AttendanceRecord>,
        startDate: LocalDate,
        endDate: LocalDate,
        targetBp: Int
    ): AttendanceSummary {
        val inRange = records.filter { !it.classDate.isBefore(startDate) && !it.classDate.isAfter(endDate) }
        val counts = AttendanceCalculator.aggregateSlices(
            inRange.map { AttendanceRecordSlice(it.periodCount, it.status) }
        )
        return AttendanceCalculator.calculateSummary(counts, targetBp)
    }

    /**
     * Computes recent weekly buckets (last [weekCount] weeks ending at [referenceDate]'s week).
     * Buckets with zero conducted periods have percentage = null so charts show explicit gaps rather than 0%.
     */
    fun computeWeeklyBuckets(
        records: List<AttendanceRecord>,
        referenceDate: LocalDate,
        weekStartsOn: WeekStartDay,
        weekCount: Int = 6
    ): List<DateBucketAttendance> {
        val firstDayOfWeek = if (weekStartsOn == WeekStartDay.MONDAY) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
        val currentWeekStart = referenceDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        return ((weekCount - 1) downTo 0).map { offset ->
            val start = currentWeekStart.minusWeeks(offset.toLong())
            val end = start.plusDays(6)
            val weekRecords = records.filter { !it.classDate.isBefore(start) && !it.classDate.isAfter(end) }
            val counts = AttendanceCalculator.aggregateSlices(
                weekRecords.map { AttendanceRecordSlice(it.periodCount, it.status) }
            )
            val pct = if (counts.conductedPeriods > 0L) {
                (counts.presentPeriods.toDouble() * 100.0) / counts.conductedPeriods.toDouble()
            } else {
                null
            }
            DateBucketAttendance(
                label = "${DateFormatters.formatShortDate(start)}–${DateFormatters.formatShortDate(end)}",
                startDate = start,
                endDate = end,
                presentPeriods = counts.presentPeriods,
                absentPeriods = counts.absentPeriods,
                cancelledPeriods = counts.cancelledPeriods,
                conductedPeriods = counts.conductedPeriods,
                percentage = pct
            )
        }
    }

    /**
     * Computes monthly buckets across the semester's recorded history (or recent 6 months).
     * Buckets with zero conducted periods have percentage = null so charts never plot misleading 0%.
     */
    fun computeMonthlyBuckets(
        records: List<AttendanceRecord>,
        referenceDate: LocalDate,
        monthCount: Int = 6
    ): List<DateBucketAttendance> {
        val currentYm = YearMonth.from(referenceDate)
        return ((monthCount - 1) downTo 0).map { offset ->
            val ym = currentYm.minusMonths(offset.toLong())
            val start = ym.atDay(1)
            val end = ym.atEndOfMonth()
            val monthRecords = records.filter { !it.classDate.isBefore(start) && !it.classDate.isAfter(end) }
            val counts = AttendanceCalculator.aggregateSlices(
                monthRecords.map { AttendanceRecordSlice(it.periodCount, it.status) }
            )
            val pct = if (counts.conductedPeriods > 0L) {
                (counts.presentPeriods.toDouble() * 100.0) / counts.conductedPeriods.toDouble()
            } else {
                null
            }
            DateBucketAttendance(
                label = ym.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                startDate = start,
                endDate = end,
                presentPeriods = counts.presentPeriods,
                absentPeriods = counts.absentPeriods,
                cancelledPeriods = counts.cancelledPeriods,
                conductedPeriods = counts.conductedPeriods,
                percentage = pct
            )
        }
    }
}

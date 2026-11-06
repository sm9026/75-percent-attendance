package com.attendanceradar.app.domain.calculator

import com.attendanceradar.app.core.common.DateFormatters
import com.attendanceradar.app.domain.model.AttendanceEntryStatus
import com.attendanceradar.app.domain.model.AttendanceRecord
import com.attendanceradar.app.domain.model.CalendarDayMarker
import com.attendanceradar.app.domain.usecase.AttendanceAnalyticsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate

class SemesterAggregationTest {

    @Test
    fun semesterOverallAggregation_isWeightedByPeriods_neverSimpleAverage() {
        // Subject A: 90 Present / 10 Absent = 90.0%
        val subjectACounts = PeriodCounts(presentPeriods = 90L, absentPeriods = 10L)
        // Subject B (archived or active): 30 Present / 10 Absent = 75.0%
        val subjectBCounts = PeriodCounts(presentPeriods = 30L, absentPeriods = 10L)

        val aggregated = AttendanceCalculator.aggregatePeriodCounts(
            listOf(subjectACounts, subjectBCounts)
        )
        val overallSummary = AttendanceCalculator.calculateSummary(aggregated, targetBp = 7500)

        assertEquals(120L, overallSummary.presentPeriods)
        assertEquals(20L, overallSummary.absentPeriods)
        assertEquals(140L, overallSummary.conductedPeriods)

        // 120 / 140 = 85.7142857...% (displays as 85.7%), NOT (90% + 75%) / 2 = 82.5%
        assertEquals(120.0 * 100.0 / 140.0, overallSummary.attendancePercentage!!, 1e-9)
        assertNotEquals(82.5, overallSummary.attendancePercentage!!, 1e-4)
        assertEquals("85.7%", DateFormatters.formatPercentage(overallSummary.attendancePercentage, 1))
    }

    @Test
    fun calendarMarkers_distinguishPresentAbsentMixedAndCancelledDays() {
        val d1 = LocalDate.of(2026, 11, 1)
        val d2 = LocalDate.of(2026, 11, 2)
        val d3 = LocalDate.of(2026, 11, 3)
        val d4 = LocalDate.of(2026, 11, 4)

        val records = listOf(
            AttendanceRecord("r1", "s1", d1, 2, AttendanceEntryStatus.PRESENT, null, null, 1L, 1L),
            AttendanceRecord("r2", "s2", d2, 1, AttendanceEntryStatus.ABSENT, null, null, 2L, 2L),
            AttendanceRecord("r3", "s1", d3, 1, AttendanceEntryStatus.PRESENT, null, null, 3L, 3L),
            AttendanceRecord("r4", "s2", d3, 1, AttendanceEntryStatus.ABSENT, null, null, 4L, 4L),
            AttendanceRecord("r5", "s1", d4, 2, AttendanceEntryStatus.CANCELLED, null, null, 5L, 5L)
        )

        val markers = AttendanceAnalyticsUseCase.computeCalendarMarkers(records)
        assertEquals(CalendarDayMarker.ALL_PRESENT, markers[d1])
        assertEquals(CalendarDayMarker.ALL_ABSENT, markers[d2])
        assertEquals(CalendarDayMarker.MIXED_PRESENT_ABSENT, markers[d3])
        assertEquals(CalendarDayMarker.ALL_CANCELLED, markers[d4])
    }

    @Test
    fun cumulativeTrend_ordersDeterministicallyAndSkipsCancelledSessions() {
        val d1 = LocalDate.of(2026, 11, 1)
        val d2 = LocalDate.of(2026, 11, 2)
        val d3 = LocalDate.of(2026, 11, 3)

        val records = listOf(
            AttendanceRecord("r3", "s1", d3, 1, AttendanceEntryStatus.ABSENT, null, null, 30L, 30L),
            AttendanceRecord("r2", "s1", d2, 2, AttendanceEntryStatus.CANCELLED, null, null, 20L, 20L),
            AttendanceRecord("r1", "s1", d1, 3, AttendanceEntryStatus.PRESENT, null, null, 10L, 10L)
        )

        val trend = AttendanceAnalyticsUseCase.computeCumulativeTrend(records)
        assertEquals(2, trend.size)
        assertEquals("r1", trend[0].recordId)
        assertEquals(100.0, trend[0].cumulativePercentage, 1e-9)
        assertEquals("r3", trend[1].recordId)
        assertEquals(75.0, trend[1].cumulativePercentage, 1e-9)
    }
}

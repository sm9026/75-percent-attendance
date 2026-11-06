package com.attendanceradar.app.domain.calculator

import com.attendanceradar.app.core.common.DateFormatters
import com.attendanceradar.app.domain.model.AttendanceEntryStatus
import com.attendanceradar.app.domain.model.AttendanceHealthStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceCalculatorTest {

    @Test
    fun noRecords_returnsUndefinedPercentageAndNoRecordsStatus() {
        val summary = AttendanceCalculator.calculateSummary(
            presentPeriods = 0L,
            absentPeriods = 0L,
            cancelledPeriods = 0L,
            targetBp = 7500
        )

        assertEquals(0L, summary.conductedPeriods)
        assertNull(summary.attendancePercentage)
        assertEquals("N/A", DateFormatters.formatPercentage(summary.attendancePercentage, 1))
        assertFalse(summary.meetsTarget)
        assertEquals(AttendanceHealthStatus.NO_RECORDS, summary.status)
        assertNull(summary.safeAdditionalAbsences)
        assertEquals(RecoveryRequirement.NotNeeded, summary.recoveryRequirement)
    }

    @Test
    fun exactlyAtTarget_30Present10Absent_hasZeroSafeAbsencesAndAtRiskStatus() {
        val summary = AttendanceCalculator.calculateSummary(
            presentPeriods = 30L,
            absentPeriods = 10L,
            targetBp = 7500
        )

        assertEquals(40L, summary.conductedPeriods)
        assertEquals(75.0, summary.attendancePercentage!!, 1e-9)
        assertTrue(summary.meetsTarget)
        assertEquals(0L, summary.safeAdditionalAbsences)
        assertEquals(AttendanceHealthStatus.AT_RISK, summary.status)
        assertEquals(RecoveryRequirement.NotNeeded, summary.recoveryRequirement)

        // Boundary check: 1 additional absence drops below 75%
        val afterOneAbsence = AttendanceCalculator.calculateSummary(
            presentPeriods = 30L,
            absentPeriods = 11L,
            targetBp = 7500
        )
        assertFalse(afterOneAbsence.meetsTarget)
        assertEquals(AttendanceHealthStatus.BELOW_TARGET, afterOneAbsence.status)
    }

    @Test
    fun aboveTarget_32Present8Absent_allows2AbsencesNot3() {
        val summary = AttendanceCalculator.calculateSummary(
            presentPeriods = 32L,
            absentPeriods = 8L,
            targetBp = 7500
        )

        assertEquals(40L, summary.conductedPeriods)
        assertEquals(80.0, summary.attendancePercentage!!, 1e-9)
        assertTrue(summary.meetsTarget)
        assertEquals(2L, summary.safeAdditionalAbsences)
        assertEquals(AttendanceHealthStatus.AT_RISK, summary.status)

        // +2 absences -> 32/42 = 76.19% >= 75%
        val withTwoMore = AttendanceCalculator.calculateSummary(32L, 10L, targetBp = 7500)
        assertTrue(withTwoMore.meetsTarget)

        // +3 absences -> 32/43 = 74.418% < 75%
        val withThreeMore = AttendanceCalculator.calculateSummary(32L, 11L, targetBp = 7500)
        assertFalse(withThreeMore.meetsTarget)
        assertEquals(AttendanceHealthStatus.BELOW_TARGET, withThreeMore.status)
    }

    @Test
    fun smallRecord_9Present1Absent_allows2AbsencesNot3() {
        val summary = AttendanceCalculator.calculateSummary(
            presentPeriods = 9L,
            absentPeriods = 1L,
            targetBp = 7500
        )

        assertEquals(90.0, summary.attendancePercentage!!, 1e-9)
        assertEquals(2L, summary.safeAdditionalAbsences)
        assertEquals(AttendanceHealthStatus.AT_RISK, summary.status)

        // 9/12 = 75% meets target
        assertTrue(AttendanceCalculator.calculateSummary(9L, 3L, targetBp = 7500).meetsTarget)
        // 9/13 < 75% fails target
        assertFalse(AttendanceCalculator.calculateSummary(9L, 4L, targetBp = 7500).meetsTarget)
    }

    @Test
    fun statusBoundaries_useSafeAbsenceBufferFor75And100PercentTargets() {
        val fourOfFour = AttendanceCalculator.calculateSummary(4L, 0L, targetBp = 7500)
        assertEquals(1L, fourOfFour.safeAdditionalAbsences)
        assertEquals(AttendanceHealthStatus.AT_RISK, fourOfFour.status)

        val eightOfEight = AttendanceCalculator.calculateSummary(8L, 0L, targetBp = 7500)
        assertEquals(2L, eightOfEight.safeAdditionalAbsences)
        assertEquals(AttendanceHealthStatus.AT_RISK, eightOfEight.status)

        val nineOfNine = AttendanceCalculator.calculateSummary(9L, 0L, targetBp = 7500)
        assertEquals(3L, nineOfNine.safeAdditionalAbsences)
        assertEquals(AttendanceHealthStatus.ON_TRACK, nineOfNine.status)

        val tenOfTenAt100Percent = AttendanceCalculator.calculateSummary(10L, 0L, targetBp = 10_000)
        assertEquals(0L, tenOfTenAt100Percent.safeAdditionalAbsences)
        assertEquals(AttendanceHealthStatus.AT_RISK, tenOfTenAt100Percent.status)

        val nineOfTenAt100Percent = AttendanceCalculator.calculateSummary(9L, 1L, targetBp = 10_000)
        assertEquals(0L, nineOfTenAt100Percent.safeAdditionalAbsences)
        assertEquals(AttendanceHealthStatus.BELOW_TARGET, nineOfTenAt100Percent.status)
    }

    @Test
    fun comfortableBuffer_36Present4Absent_isOnTrack() {
        val summary = AttendanceCalculator.calculateSummary(
            presentPeriods = 36L,
            absentPeriods = 4L,
            targetBp = 7500
        )

        assertEquals(8L, summary.safeAdditionalAbsences)
        assertEquals(AttendanceHealthStatus.ON_TRACK, summary.status)
    }

    @Test
    fun belowTarget_28Present12Absent_requires8RecoveryPeriodsNot7() {
        val summary = AttendanceCalculator.calculateSummary(
            presentPeriods = 28L,
            absentPeriods = 12L,
            targetBp = 7500
        )

        assertEquals(40L, summary.conductedPeriods)
        assertEquals(70.0, summary.attendancePercentage!!, 1e-9)
        assertFalse(summary.meetsTarget)
        assertEquals(0L, summary.safeAdditionalAbsences)
        assertEquals(AttendanceHealthStatus.BELOW_TARGET, summary.status)
        assertEquals(RecoveryRequirement.Possible(8L), summary.recoveryRequirement)

        // +7 present -> 35/47 = 74.468% (still below 75%)
        assertFalse(AttendanceCalculator.calculateSummary(35L, 12L, targetBp = 7500).meetsTarget)
        // +8 present -> 36/48 = 75.00% (reaches target)
        assertTrue(AttendanceCalculator.calculateSummary(36L, 12L, targetBp = 7500).meetsTarget)
    }

    @Test
    fun belowTarget_20Present20Absent_requires40RecoveryPeriodsNot39() {
        val summary = AttendanceCalculator.calculateSummary(
            presentPeriods = 20L,
            absentPeriods = 20L,
            targetBp = 7500
        )

        assertEquals(RecoveryRequirement.Possible(40L), summary.recoveryRequirement)
        assertFalse(AttendanceCalculator.calculateSummary(59L, 20L, targetBp = 7500).meetsTarget)
        assertTrue(AttendanceCalculator.calculateSummary(60L, 20L, targetBp = 7500).meetsTarget)
    }

    @Test
    fun target100Percent_perfectRecord_hasZeroSafeAbsencesAndZeroRecovery() {
        val summary = AttendanceCalculator.calculateSummary(
            presentPeriods = 10L,
            absentPeriods = 0L,
            targetBp = 10_000
        )

        assertEquals(100.0, summary.attendancePercentage!!, 1e-9)
        assertTrue(summary.meetsTarget)
        assertEquals(0L, summary.safeAdditionalAbsences)
        assertEquals(RecoveryRequirement.NotNeeded, summary.recoveryRequirement)
    }

    @Test
    fun target100Percent_withAnyAbsence_isImpossibleToRecover() {
        val summary = AttendanceCalculator.calculateSummary(
            presentPeriods = 9L,
            absentPeriods = 1L,
            targetBp = 10_000
        )

        assertEquals(90.0, summary.attendancePercentage!!, 1e-9)
        assertFalse(summary.meetsTarget)
        assertEquals(AttendanceHealthStatus.BELOW_TARGET, summary.status)
        assertEquals(0L, summary.safeAdditionalAbsences)
        assertEquals(RecoveryRequirement.ImpossibleAt100Percent, summary.recoveryRequirement)
    }

    @Test
    fun cancelledPeriods_areExcludedFromNumeratorAndDenominator() {
        val counts = AttendanceCalculator.aggregateSlices(
            listOf(
                AttendanceRecordSlice(periodCount = 8, status = AttendanceEntryStatus.PRESENT),
                AttendanceRecordSlice(periodCount = 2, status = AttendanceEntryStatus.ABSENT),
                AttendanceRecordSlice(periodCount = 5, status = AttendanceEntryStatus.CANCELLED)
            )
        )
        val summary = AttendanceCalculator.calculateSummary(counts, targetBp = 7500)

        assertEquals(8L, summary.presentPeriods)
        assertEquals(2L, summary.absentPeriods)
        assertEquals(5L, summary.cancelledPeriods)
        assertEquals(10L, summary.conductedPeriods) // 10, NOT 15
        assertEquals(80.0, summary.attendancePercentage!!, 1e-9)
    }

    @Test
    fun whatIfProjection_computesProjectedValuesWithoutChangingBaseline() {
        val projection = AttendanceCalculator.projectWhatIf(
            presentPeriods = 32L,
            absentPeriods = 8L,
            cancelledPeriods = 2L,
            targetBp = 7500,
            futurePresentPeriods = 3L,
            futureAbsentPeriods = 2L
        )

        assertEquals(32L, projection.baseline.presentPeriods)
        assertEquals(40L, projection.baseline.conductedPeriods)

        assertEquals(35L, projection.projected.presentPeriods)
        assertEquals(10L, projection.projected.absentPeriods)
        assertEquals(45L, projection.projected.conductedPeriods)
        assertEquals(77.77777777777779, projection.projected.attendancePercentage!!, 1e-9)
        assertTrue(projection.projected.meetsTarget)
    }

    @Test
    fun exactThresholdCheck_1499Present501Absent_displays75Point0ButRemainsBelowTarget() {
        // 1499 / 2000 = 74.95%, which rounds to "75.0%" at 1 decimal place.
        // Exact comparison: 1499 * 10000 = 14,990,000 < 7500 * 2000 = 15,000,000.
        val summary = AttendanceCalculator.calculateSummary(
            presentPeriods = 1499L,
            absentPeriods = 501L,
            targetBp = 7500
        )

        assertEquals("75.0%", DateFormatters.formatPercentage(summary.attendancePercentage, 1))
        assertFalse(summary.meetsTarget)
        assertEquals(AttendanceHealthStatus.BELOW_TARGET, summary.status)
        assertEquals(0L, summary.safeAdditionalAbsences)
        assertEquals(RecoveryRequirement.Possible(4L), summary.recoveryRequirement)
    }

    @Test
    fun wideIntegerArithmetic_largeValidCountsDoNotOverflow32BitOr64Bit() {
        // 300,000,000 * 10,000 = 3,000,000,000,000 (exceeds Int.MAX_VALUE = 2,147,483,647 by >1396x)
        val exactAtLargeScale = AttendanceCalculator.calculateSummary(
            presentPeriods = 300_000_000L,
            absentPeriods = 100_000_000L,
            targetBp = 7500
        )
        assertTrue(exactAtLargeScale.meetsTarget)
        assertEquals(0L, exactAtLargeScale.safeAdditionalAbsences)

        val oneShortAtLargeScale = AttendanceCalculator.calculateSummary(
            presentPeriods = 299_999_999L,
            absentPeriods = 100_000_001L,
            targetBp = 7500
        )
        assertFalse(oneShortAtLargeScale.meetsTarget)
        assertEquals(AttendanceHealthStatus.BELOW_TARGET, oneShortAtLargeScale.status)
        assertEquals(RecoveryRequirement.Possible(4L), oneShortAtLargeScale.recoveryRequirement)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidTargetZero_throwsIllegalArgumentException() {
        AttendanceCalculator.calculateSummary(10L, 2L, targetBp = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidTargetAbove10000_throwsIllegalArgumentException() {
        AttendanceCalculator.calculateSummary(10L, 2L, targetBp = 10_001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativePeriodCount_throwsIllegalArgumentException() {
        AttendanceCalculator.calculateSummary(-1L, 5L, targetBp = 7500)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroSlicePeriodCount_throwsIllegalArgumentException() {
        AttendanceRecordSlice(periodCount = 0, status = AttendanceEntryStatus.PRESENT)
    }
}

package com.attendanceradar.app.domain.calculator

import com.attendanceradar.app.domain.model.AttendanceEntryStatus
import com.attendanceradar.app.domain.model.AttendanceHealthStatus
import java.math.BigInteger

/**
 * Pure Kotlin authoritative domain calculator implementing DOMAIN_RULES.md.
 *
 * Key invariants:
 * - Never uses rounded display percentages for target or status comparisons.
 * - Uses integer counts and integer basis points (1..10000, where 7500 = 75.00%).
 * - Uses checked Long / BigInteger intermediate arithmetic so large histories cannot overflow 32-bit or 64-bit bounds.
 * - Excludes CANCELLED periods from both numerator (P) and denominator (C = P + A).
 * - Semester overall aggregates raw period counts across all subjects (including archived) before dividing.
 */
object AttendanceCalculator {

    const val MIN_TARGET_BP: Int = 1
    const val MAX_TARGET_BP: Int = 10_000
    const val DEFAULT_TARGET_BP: Int = 7_500
    const val LOW_BUFFER_THRESHOLD: Long = 2L
    const val MIN_PERIOD_COUNT: Int = 1
    const val MAX_PERIOD_COUNT: Int = 20

    private val TEN_THOUSAND_BI: BigInteger = BigInteger.valueOf(10_000L)

    fun validateTargetBp(targetBp: Int) {
        require(targetBp in MIN_TARGET_BP..MAX_TARGET_BP) {
            "Target basis points must be between $MIN_TARGET_BP and $MAX_TARGET_BP, got $targetBp."
        }
    }

    fun validatePeriodCount(periodCount: Int, enforceUiMax: Boolean = false) {
        require(periodCount >= MIN_PERIOD_COUNT) {
            "Period count must be at least $MIN_PERIOD_COUNT, got $periodCount."
        }
        if (enforceUiMax) {
            require(periodCount <= MAX_PERIOD_COUNT) {
                "Period count cannot exceed $MAX_PERIOD_COUNT, got $periodCount."
            }
        }
    }

    /**
     * Aggregates a collection of attendance record slices into [PeriodCounts].
     */
    fun aggregateSlices(slices: Iterable<AttendanceRecordSlice>): PeriodCounts {
        var p = 0L
        var a = 0L
        var x = 0L
        for (slice in slices) {
            validatePeriodCount(slice.periodCount)
            val count = slice.periodCount.toLong()
            when (slice.status) {
                AttendanceEntryStatus.PRESENT -> p = Math.addExact(p, count)
                AttendanceEntryStatus.ABSENT -> a = Math.addExact(a, count)
                AttendanceEntryStatus.CANCELLED -> x = Math.addExact(x, count)
            }
        }
        return PeriodCounts(presentPeriods = p, absentPeriods = a, cancelledPeriods = x)
    }

    /**
     * Aggregates multiple [PeriodCounts] (e.g., across all subjects in a semester, including archived subjects).
     * Never averages percentages.
     */
    fun aggregatePeriodCounts(countsList: Iterable<PeriodCounts>): PeriodCounts {
        var totalP = 0L
        var totalA = 0L
        var totalX = 0L
        for (item in countsList) {
            totalP = Math.addExact(totalP, item.presentPeriods)
            totalA = Math.addExact(totalA, item.absentPeriods)
            totalX = Math.addExact(totalX, item.cancelledPeriods)
        }
        return PeriodCounts(
            presentPeriods = totalP,
            absentPeriods = totalA,
            cancelledPeriods = totalX
        )
    }

    /**
     * Evaluates complete attendance status, exact comparison, safe additional absences,
     * and minimum consecutive recovery Present periods.
     */
    fun calculateSummary(
        presentPeriods: Long,
        absentPeriods: Long,
        cancelledPeriods: Long = 0L,
        targetBp: Int
    ): AttendanceSummary {
        require(presentPeriods >= 0L) { "Present periods must be non-negative: $presentPeriods" }
        require(absentPeriods >= 0L) { "Absent periods must be non-negative: $absentPeriods" }
        require(cancelledPeriods >= 0L) { "Cancelled periods must be non-negative: $cancelledPeriods" }
        validateTargetBp(targetBp)

        val conductedPeriods = Math.addExact(presentPeriods, absentPeriods)
        if (conductedPeriods == 0L) {
            return AttendanceSummary(
                presentPeriods = 0L,
                absentPeriods = 0L,
                cancelledPeriods = cancelledPeriods,
                conductedPeriods = 0L,
                targetBp = targetBp,
                attendancePercentage = null,
                meetsTarget = false,
                status = AttendanceHealthStatus.NO_RECORDS,
                safeAdditionalAbsences = null,
                recoveryRequirement = RecoveryRequirement.NotNeeded
            )
        }

        val pBi = BigInteger.valueOf(presentPeriods)
        val cBi = BigInteger.valueOf(conductedPeriods)
        val bBi = BigInteger.valueOf(targetBp.toLong())

        // Exact comparison: P * 10000 >= B * C
        val lhs = pBi.multiply(TEN_THOUSAND_BI)
        val rhs = bBi.multiply(cBi)
        val meetsTarget = lhs >= rhs

        val percentage = (presentPeriods.toDouble() * 100.0) / conductedPeriods.toDouble()

        if (meetsTarget) {
            // Safe additional absences = floor((P * 10000 - B * C) / B)
            val surplus = lhs.subtract(rhs)
            val safeAbsencesBi = surplus.divide(bBi)
            val safeAbsences = safeAbsencesBi.longValueExact()

            val status = if (safeAbsences <= LOW_BUFFER_THRESHOLD) {
                AttendanceHealthStatus.AT_RISK
            } else {
                AttendanceHealthStatus.ON_TRACK
            }

            return AttendanceSummary(
                presentPeriods = presentPeriods,
                absentPeriods = absentPeriods,
                cancelledPeriods = cancelledPeriods,
                conductedPeriods = conductedPeriods,
                targetBp = targetBp,
                attendancePercentage = percentage,
                meetsTarget = true,
                status = status,
                safeAdditionalAbsences = safeAbsences,
                recoveryRequirement = RecoveryRequirement.NotNeeded
            )
        } else {
            // Currently below target: safe additional absences = 0
            val recovery = if (targetBp == MAX_TARGET_BP) {
                RecoveryRequirement.ImpossibleAt100Percent
            } else {
                // ceil((B * C - P * 10000) / (10000 - B))
                val deficit = rhs.subtract(lhs)
                val denom = TEN_THOUSAND_BI.subtract(bBi)
                val recoveryBi = ceilDiv(deficit, denom)
                RecoveryRequirement.Possible(recoveryBi.longValueExact())
            }

            return AttendanceSummary(
                presentPeriods = presentPeriods,
                absentPeriods = absentPeriods,
                cancelledPeriods = cancelledPeriods,
                conductedPeriods = conductedPeriods,
                targetBp = targetBp,
                attendancePercentage = percentage,
                meetsTarget = false,
                status = AttendanceHealthStatus.BELOW_TARGET,
                safeAdditionalAbsences = 0L,
                recoveryRequirement = recovery
            )
        }
    }

    fun calculateSummary(counts: PeriodCounts, targetBp: Int): AttendanceSummary =
        calculateSummary(
            presentPeriods = counts.presentPeriods,
            absentPeriods = counts.absentPeriods,
            cancelledPeriods = counts.cancelledPeriods,
            targetBp = targetBp
        )

    /**
     * Computes a non-persistent What-If projection from baseline counts and hypothetical future counts.
     */
    fun projectWhatIf(
        presentPeriods: Long,
        absentPeriods: Long,
        cancelledPeriods: Long = 0L,
        targetBp: Int,
        futurePresentPeriods: Long,
        futureAbsentPeriods: Long,
        futureCancelledPeriods: Long = 0L
    ): WhatIfProjection {
        require(futurePresentPeriods >= 0L) { "Future present periods cannot be negative: $futurePresentPeriods" }
        require(futureAbsentPeriods >= 0L) { "Future absent periods cannot be negative: $futureAbsentPeriods" }
        require(futureCancelledPeriods >= 0L) { "Future cancelled periods cannot be negative: $futureCancelledPeriods" }

        val baseline = calculateSummary(
            presentPeriods = presentPeriods,
            absentPeriods = absentPeriods,
            cancelledPeriods = cancelledPeriods,
            targetBp = targetBp
        )

        val projectedP = Math.addExact(presentPeriods, futurePresentPeriods)
        val projectedA = Math.addExact(absentPeriods, futureAbsentPeriods)
        val projectedX = Math.addExact(cancelledPeriods, futureCancelledPeriods)

        val projected = calculateSummary(
            presentPeriods = projectedP,
            absentPeriods = projectedA,
            cancelledPeriods = projectedX,
            targetBp = targetBp
        )

        return WhatIfProjection(
            baseline = baseline,
            futurePresentPeriods = futurePresentPeriods,
            futureAbsentPeriods = futureAbsentPeriods,
            futureCancelledPeriods = futureCancelledPeriods,
            projected = projected
        )
    }

    /**
     * Safe integer ceiling division: ceil(n / d) = (n + d - 1) / d for non-negative n and d > 0.
     */
    fun ceilDiv(numerator: BigInteger, denominator: BigInteger): BigInteger {
        require(numerator >= BigInteger.ZERO) { "Numerator must be non-negative: $numerator" }
        require(denominator > BigInteger.ZERO) { "Denominator must be positive: $denominator" }
        if (numerator == BigInteger.ZERO) return BigInteger.ZERO
        return numerator.add(denominator).subtract(BigInteger.ONE).divide(denominator)
    }

    fun ceilDivLong(numerator: Long, denominator: Long): Long {
        require(numerator >= 0L) { "Numerator must be non-negative: $numerator" }
        require(denominator > 0L) { "Denominator must be positive: $denominator" }
        return ceilDiv(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator)).longValueExact()
    }
}

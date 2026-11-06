package com.attendanceradar.app.core.common

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object DateFormatters {
    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val displayDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    private val shortDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val monthYearFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    fun toIsoString(date: LocalDate): String = date.format(isoFormatter)

    fun parseIsoOrNull(iso: String?): LocalDate? {
        if (iso.isNullOrBlank()) return null
        return try {
            LocalDate.parse(iso.trim(), isoFormatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    fun formatDisplayDate(date: LocalDate): String = date.format(displayDateFormatter)

    fun formatDisplayDate(iso: String): String =
        parseIsoOrNull(iso)?.let(::formatDisplayDate) ?: iso

    fun formatShortDate(date: LocalDate): String = date.format(shortDateFormatter)

    fun formatMonthYear(yearMonth: YearMonth): String = yearMonth.format(monthYearFormatter)

    fun formatPercentage(
        percentage: Double?,
        precision: Int = 1
    ): String {
        if (percentage == null || percentage.isNaN() || percentage.isInfinite()) {
            return "N/A"
        }
        val clampedPrecision = precision.coerceIn(0, 2)
        return String.format(Locale.getDefault(), "%.${clampedPrecision}f%%", percentage)
    }

    fun formatBasisPoints(targetBp: Int, precision: Int = 1): String {
        val pct = targetBp.toDouble() / 100.0
        val clampedPrecision = if (targetBp % 100 == 0 && precision == 0) 0 else precision.coerceIn(0, 2)
        return if (targetBp % 100 == 0) {
            String.format(Locale.getDefault(), "%d%%", targetBp / 100)
        } else {
            String.format(Locale.getDefault(), "%.${clampedPrecision.coerceAtLeast(1)}f%%", pct)
        }
    }

    fun pluralizePeriods(count: Int): String =
        if (count == 1) "1 period" else "$count periods"

    fun pluralizeMorePeriods(count: Long): String =
        if (count == 1L) "1 more period" else "$count more periods"
}

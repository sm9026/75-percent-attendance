package com.attendanceradar.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.attendanceradar.app.core.design.SeventyFivePercentTheme
import com.attendanceradar.app.core.design.OverallAttendanceSummaryCard
import com.attendanceradar.app.domain.calculator.AttendanceCalculator
import org.junit.Rule
import org.junit.Test

class AppUiSmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun overallSummaryCard_withNoRecords_displaysNaAndNeverFakeZeroPercent() {
        val noRecordsSummary = AttendanceCalculator.calculateSummary(
            presentPeriods = 0L,
            absentPeriods = 0L,
            cancelledPeriods = 0L,
            targetBp = 7500
        )

        composeTestRule.setContent {
            SeventyFivePercentTheme {
                OverallAttendanceSummaryCard(
                    title = "Semester overall",
                    summary = noRecordsSummary,
                    displayPrecision = 1,
                    onOpenCalculator = {}
                )
            }
        }

        composeTestRule.onNodeWithText("N/A").assertIsDisplayed()
        composeTestRule.onNodeWithText("No classes recorded").assertIsDisplayed()
        composeTestRule.onNodeWithText("No records").assertIsDisplayed()
    }

    @Test
    fun overallSummaryCard_belowTarget_displaysExactRecoveryPeriods() {
        val belowSummary = AttendanceCalculator.calculateSummary(
            presentPeriods = 28L,
            absentPeriods = 12L,
            cancelledPeriods = 0L,
            targetBp = 7500
        )

        composeTestRule.setContent {
            SeventyFivePercentTheme {
                OverallAttendanceSummaryCard(
                    title = "Semester overall",
                    summary = belowSummary,
                    displayPrecision = 1,
                    onOpenCalculator = {}
                )
            }
        }

        composeTestRule.onNodeWithText("70.0%").assertIsDisplayed()
        composeTestRule.onNodeWithText("Below target").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Below target. Attend 8 periods consecutively to reach 75%."
        ).assertIsDisplayed()
    }
}

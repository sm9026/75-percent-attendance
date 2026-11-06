package com.attendanceradar.app.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(
        route = "home",
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    SUBJECTS(
        route = "subjects",
        label = "Subjects",
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook
    ),
    CALENDAR(
        route = "calendar",
        label = "Calendar",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    ),
    INSIGHTS(
        route = "insights",
        label = "Insights",
        selectedIcon = Icons.Filled.Assessment,
        unselectedIcon = Icons.Outlined.Assessment
    ),
    SETTINGS(
        route = "settings",
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

object AppRoutes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SUBJECTS = "subjects"
    const val CALENDAR = "calendar"
    const val INSIGHTS = "insights"
    const val SETTINGS = "settings"

    const val SUBJECT_FORM = "subject_form?subjectId={subjectId}"
    fun subjectFormRoute(subjectId: String? = null): String =
        if (subjectId.isNullOrBlank()) "subject_form" else "subject_form?subjectId=$subjectId"

    const val SUBJECT_DETAIL = "subject_detail/{subjectId}"
    fun subjectDetailRoute(subjectId: String): String = "subject_detail/$subjectId"

    const val MARK_ATTENDANCE = "mark_attendance?subjectId={subjectId}&recordId={recordId}&date={date}"
    fun markAttendanceRoute(
        subjectId: String? = null,
        recordId: String? = null,
        dateIso: String? = null
    ): String {
        val params = mutableListOf<String>()
        if (!subjectId.isNullOrBlank()) params.add("subjectId=$subjectId")
        if (!recordId.isNullOrBlank()) params.add("recordId=$recordId")
        if (!dateIso.isNullOrBlank()) params.add("date=$dateIso")
        return if (params.isEmpty()) "mark_attendance" else "mark_attendance?${params.joinToString("&")}"
    }

    const val CALCULATOR = "calculator?subjectId={subjectId}"
    fun calculatorRoute(subjectId: String? = null): String =
        if (subjectId.isNullOrBlank()) "calculator" else "calculator?subjectId=$subjectId"

    const val SEMESTER_MANAGEMENT = "semester_management"
    const val BACKUP_RESTORE = "backup_restore"
}

package com.attendanceradar.app.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.attendanceradar.app.di.AppContainer
import com.attendanceradar.app.di.singleViewModelFactory
import com.attendanceradar.app.feature.attendance.CalculatorScreen
import com.attendanceradar.app.feature.attendance.CalculatorViewModel
import com.attendanceradar.app.feature.attendance.MarkAttendanceScreen
import com.attendanceradar.app.feature.attendance.MarkAttendanceViewModel
import com.attendanceradar.app.feature.calendar.CalendarScreen
import com.attendanceradar.app.feature.calendar.CalendarViewModel
import com.attendanceradar.app.feature.dashboard.DashboardScreen
import com.attendanceradar.app.feature.dashboard.DashboardViewModel
import com.attendanceradar.app.feature.insights.InsightsScreen
import com.attendanceradar.app.feature.insights.InsightsViewModel
import com.attendanceradar.app.feature.onboarding.OnboardingScreen
import com.attendanceradar.app.feature.onboarding.OnboardingViewModel
import com.attendanceradar.app.feature.settings.BackupRestoreScreen
import com.attendanceradar.app.feature.settings.BackupRestoreViewModel
import com.attendanceradar.app.feature.settings.SemesterManagementScreen
import com.attendanceradar.app.feature.settings.SemesterManagementViewModel
import com.attendanceradar.app.feature.settings.SettingsScreen
import com.attendanceradar.app.feature.settings.SettingsViewModel
import com.attendanceradar.app.feature.subjects.SubjectDetailScreen
import com.attendanceradar.app.feature.subjects.SubjectDetailViewModel
import com.attendanceradar.app.feature.subjects.SubjectFormScreen
import com.attendanceradar.app.feature.subjects.SubjectFormViewModel
import com.attendanceradar.app.feature.subjects.SubjectsScreen
import com.attendanceradar.app.feature.subjects.SubjectsViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph(
    appContainer: AppContainer,
    startInOnboarding: Boolean
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topLevelRoutes = remember { TopLevelDestination.entries.map { it.route }.toSet() }
    val showBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (startInOnboarding) AppRoutes.ONBOARDING else AppRoutes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppRoutes.ONBOARDING) {
                val vm: OnboardingViewModel = viewModel(
                    factory = singleViewModelFactory {
                        OnboardingViewModel(
                            semesterRepository = appContainer.semesterRepository,
                            subjectRepository = appContainer.subjectRepository,
                            preferencesRepository = appContainer.preferencesRepository
                        )
                    }
                )
                OnboardingScreen(
                    viewModel = vm,
                    onCompleted = {
                        navController.navigate(AppRoutes.HOME) {
                            popUpTo(AppRoutes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            composable(AppRoutes.HOME) {
                val vm: DashboardViewModel = viewModel(
                    factory = singleViewModelFactory {
                        DashboardViewModel(
                            semesterRepository = appContainer.semesterRepository,
                            subjectRepository = appContainer.subjectRepository,
                            attendanceRepository = appContainer.attendanceRepository,
                            preferencesRepository = appContainer.preferencesRepository
                        )
                    }
                )
                DashboardScreen(
                    viewModel = vm,
                    onOpenMarkAttendance = { subjectId, recordId ->
                        navController.navigate(
                            AppRoutes.markAttendanceRoute(subjectId = subjectId, recordId = recordId)
                        )
                    },
                    onOpenSubjectDetail = { subjectId ->
                        navController.navigate(AppRoutes.subjectDetailRoute(subjectId))
                    },
                    onOpenSubjectForm = { subjectId ->
                        navController.navigate(AppRoutes.subjectFormRoute(subjectId))
                    },
                    onOpenCalculator = { subjectId ->
                        navController.navigate(AppRoutes.calculatorRoute(subjectId))
                    },
                    onOpenSemesterManagement = {
                        navController.navigate(AppRoutes.SEMESTER_MANAGEMENT)
                    }
                )
            }

            composable(AppRoutes.SUBJECTS) {
                val vm: SubjectsViewModel = viewModel(
                    factory = singleViewModelFactory {
                        SubjectsViewModel(
                            semesterRepository = appContainer.semesterRepository,
                            subjectRepository = appContainer.subjectRepository,
                            preferencesRepository = appContainer.preferencesRepository
                        )
                    }
                )
                SubjectsScreen(
                    viewModel = vm,
                    onOpenSubjectDetail = { subjectId ->
                        navController.navigate(AppRoutes.subjectDetailRoute(subjectId))
                    },
                    onOpenSubjectForm = { subjectId ->
                        navController.navigate(AppRoutes.subjectFormRoute(subjectId))
                    },
                    onOpenMarkAttendance = { subjectId ->
                        navController.navigate(AppRoutes.markAttendanceRoute(subjectId = subjectId))
                    },
                    onOpenCalculator = { subjectId ->
                        navController.navigate(AppRoutes.calculatorRoute(subjectId))
                    }
                )
            }

            composable(AppRoutes.CALENDAR) {
                val vm: CalendarViewModel = viewModel(
                    factory = singleViewModelFactory {
                        CalendarViewModel(
                            semesterRepository = appContainer.semesterRepository,
                            attendanceRepository = appContainer.attendanceRepository,
                            preferencesRepository = appContainer.preferencesRepository
                        )
                    }
                )
                CalendarScreen(
                    viewModel = vm,
                    onOpenMarkAttendanceForDate = { dateIso, recordId, subjectId ->
                        navController.navigate(
                            AppRoutes.markAttendanceRoute(
                                subjectId = subjectId,
                                recordId = recordId,
                                dateIso = dateIso
                            )
                        )
                    }
                )
            }

            composable(AppRoutes.INSIGHTS) {
                val vm: InsightsViewModel = viewModel(
                    factory = singleViewModelFactory {
                        InsightsViewModel(
                            semesterRepository = appContainer.semesterRepository,
                            attendanceRepository = appContainer.attendanceRepository,
                            preferencesRepository = appContainer.preferencesRepository
                        )
                    }
                )
                InsightsScreen(viewModel = vm)
            }

            composable(AppRoutes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(
                    factory = singleViewModelFactory {
                        SettingsViewModel(
                            semesterRepository = appContainer.semesterRepository,
                            preferencesRepository = appContainer.preferencesRepository
                        )
                    }
                )
                SettingsScreen(
                    viewModel = vm,
                    onOpenSemesterManagement = {
                        navController.navigate(AppRoutes.SEMESTER_MANAGEMENT)
                    },
                    onOpenBackupRestore = {
                        navController.navigate(AppRoutes.BACKUP_RESTORE)
                    }
                )
            }

            composable(
                route = AppRoutes.SUBJECT_FORM,
                arguments = listOf(
                    navArgument("subjectId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val editingId = entry.arguments?.getString("subjectId")
                val vm: SubjectFormViewModel = viewModel(
                    factory = singleViewModelFactory {
                        SubjectFormViewModel(
                            editingSubjectId = editingId,
                            semesterRepository = appContainer.semesterRepository,
                            subjectRepository = appContainer.subjectRepository
                        )
                    }
                )
                SubjectFormScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = AppRoutes.SUBJECT_DETAIL,
                arguments = listOf(
                    navArgument("subjectId") { type = NavType.StringType }
                )
            ) { entry ->
                val subjectId = entry.arguments?.getString("subjectId") ?: return@composable
                val vm: SubjectDetailViewModel = viewModel(
                    factory = singleViewModelFactory {
                        SubjectDetailViewModel(
                            subjectId = subjectId,
                            subjectRepository = appContainer.subjectRepository,
                            attendanceRepository = appContainer.attendanceRepository,
                            preferencesRepository = appContainer.preferencesRepository
                        )
                    }
                )
                SubjectDetailScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenMarkAttendance = { subId, recId ->
                        navController.navigate(
                            AppRoutes.markAttendanceRoute(subjectId = subId, recordId = recId)
                        )
                    },
                    onOpenEditSubject = { subId ->
                        navController.navigate(AppRoutes.subjectFormRoute(subId))
                    },
                    onOpenCalculator = { subId ->
                        navController.navigate(AppRoutes.calculatorRoute(subId))
                    }
                )
            }

            composable(
                route = AppRoutes.MARK_ATTENDANCE,
                arguments = listOf(
                    navArgument("subjectId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("recordId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("date") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val initialSubjectId = entry.arguments?.getString("subjectId")
                val editingRecordId = entry.arguments?.getString("recordId")
                val initialDate = entry.arguments?.getString("date")

                val vm: MarkAttendanceViewModel = viewModel(
                    factory = singleViewModelFactory {
                        MarkAttendanceViewModel(
                            initialSubjectId = initialSubjectId,
                            editingRecordId = editingRecordId,
                            initialDateIso = initialDate,
                            semesterRepository = appContainer.semesterRepository,
                            subjectRepository = appContainer.subjectRepository,
                            attendanceRepository = appContainer.attendanceRepository
                        )
                    }
                )
                MarkAttendanceScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onSavedSuccess = { createdRecordId, summaryMessage ->
                        navController.popBackStack()
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = summaryMessage,
                                actionLabel = if (createdRecordId != null) "Undo" else null,
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed && createdRecordId != null) {
                                // Undo removes ONLY that exact newly created record
                                appContainer.attendanceRepository.deleteAttendanceRecord(createdRecordId)
                            }
                        }
                    }
                )
            }

            composable(
                route = AppRoutes.CALCULATOR,
                arguments = listOf(
                    navArgument("subjectId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val initialSubjectId = entry.arguments?.getString("subjectId")
                val vm: CalculatorViewModel = viewModel(
                    factory = singleViewModelFactory {
                        CalculatorViewModel(
                            initialSubjectId = initialSubjectId,
                            semesterRepository = appContainer.semesterRepository,
                            preferencesRepository = appContainer.preferencesRepository
                        )
                    }
                )
                CalculatorScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.SEMESTER_MANAGEMENT) {
                val vm: SemesterManagementViewModel = viewModel(
                    factory = singleViewModelFactory {
                        SemesterManagementViewModel(
                            semesterRepository = appContainer.semesterRepository,
                            preferencesRepository = appContainer.preferencesRepository
                        )
                    }
                )
                SemesterManagementScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.BACKUP_RESTORE) {
                val vm: BackupRestoreViewModel = viewModel(
                    factory = singleViewModelFactory {
                        BackupRestoreViewModel(
                            semesterRepository = appContainer.semesterRepository,
                            backupService = appContainer.backupService,
                            csvExporter = appContainer.csvExporter
                        )
                    }
                )
                BackupRestoreScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

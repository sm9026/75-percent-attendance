package com.attendanceradar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendanceradar.app.core.design.SeventyFivePercentTheme
import com.attendanceradar.app.core.navigation.AppNavGraph
import com.attendanceradar.app.domain.model.UserPreferences

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as AttendanceRadarApplication).container

        setContent {
            val preferences by appContainer.preferencesRepository.preferencesFlow
                .collectAsStateWithLifecycle(initialValue = null)

            var initialCheckDone by remember { mutableStateOf(false) }
            var needsOnboarding by remember { mutableStateOf(false) }

            LaunchedEffect(preferences) {
                if (preferences != null && !initialCheckDone) {
                    val activeSem = appContainer.semesterRepository.getActiveSemester()
                    needsOnboarding = !(preferences!!.onboardingCompleted) && activeSem == null
                    initialCheckDone = true
                }
            }

            splashScreen.setKeepOnScreenCondition { !initialCheckDone }

            val activePrefs = preferences ?: UserPreferences()
            SeventyFivePercentTheme(themeMode = activePrefs.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!initialCheckDone) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        AppNavGraph(
                            appContainer = appContainer,
                            startInOnboarding = needsOnboarding
                        )
                    }
                }
            }
        }
    }
}

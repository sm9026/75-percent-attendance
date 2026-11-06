package com.attendanceradar.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.attendanceradar.app.domain.calculator.AttendanceCalculator
import com.attendanceradar.app.domain.model.ThemeMode
import com.attendanceradar.app.domain.model.UserPreferences
import com.attendanceradar.app.domain.model.WeekStartDay
import com.attendanceradar.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "seventy_five_percent_preferences"
)

class PreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    constructor(context: Context) : this(context.applicationContext.dataStore)

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DISPLAY_PRECISION = intPreferencesKey("display_precision")
        val WEEK_STARTS_ON = stringPreferencesKey("week_starts_on")
        val DEFAULT_NEW_SEMESTER_TARGET_BP = intPreferencesKey("default_new_semester_target_bp")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    override val preferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrNull()
            } ?: ThemeMode.SYSTEM

            val precision = (prefs[Keys.DISPLAY_PRECISION] ?: 1).coerceIn(0, 2)

            val weekStartsOn = prefs[Keys.WEEK_STARTS_ON]?.let {
                runCatching { WeekStartDay.valueOf(it) }.getOrNull()
            } ?: WeekStartDay.MONDAY

            val defaultTargetBp = (prefs[Keys.DEFAULT_NEW_SEMESTER_TARGET_BP]
                ?: AttendanceCalculator.DEFAULT_TARGET_BP)
                .coerceIn(AttendanceCalculator.MIN_TARGET_BP, AttendanceCalculator.MAX_TARGET_BP)

            val onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false

            UserPreferences(
                themeMode = themeMode,
                displayPrecision = precision,
                weekStartsOn = weekStartsOn,
                defaultNewSemesterTargetBp = defaultTargetBp,
                onboardingCompleted = onboardingCompleted
            )
        }

    override suspend fun getSnapshot(): UserPreferences = preferencesFlow.first()

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
    }

    override suspend fun setDisplayPrecision(precision: Int) {
        dataStore.edit { it[Keys.DISPLAY_PRECISION] = precision.coerceIn(0, 2) }
    }

    override suspend fun setWeekStartsOn(weekStartDay: WeekStartDay) {
        dataStore.edit { it[Keys.WEEK_STARTS_ON] = weekStartDay.name }
    }

    override suspend fun setDefaultNewSemesterTargetBp(targetBp: Int) {
        AttendanceCalculator.validateTargetBp(targetBp)
        dataStore.edit { it[Keys.DEFAULT_NEW_SEMESTER_TARGET_BP] = targetBp }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun restorePreferences(
        themeMode: ThemeMode,
        displayPrecision: Int,
        weekStartsOn: WeekStartDay
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode.name
            prefs[Keys.DISPLAY_PRECISION] = displayPrecision.coerceIn(0, 2)
            prefs[Keys.WEEK_STARTS_ON] = weekStartsOn.name
            prefs[Keys.ONBOARDING_COMPLETED] = true
        }
    }
}

package com.attendanceradar.app

import android.app.Application
import com.attendanceradar.app.di.AppContainer
import com.attendanceradar.app.di.DefaultAppContainer

class AttendanceRadarApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        if (!::container.isInitialized) {
            container = DefaultAppContainer(this)
        }
    }
}

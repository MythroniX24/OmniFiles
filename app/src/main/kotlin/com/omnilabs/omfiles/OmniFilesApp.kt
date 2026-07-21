package com.omnilabs.omfiles

import android.app.Application
import com.omnilabs.omfiles.core.GlobalExceptionHandler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OmniFilesApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Register global exception handler to catch crashes and display them in-app
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(
            GlobalExceptionHandler(applicationContext, defaultHandler)
        )
    }
}

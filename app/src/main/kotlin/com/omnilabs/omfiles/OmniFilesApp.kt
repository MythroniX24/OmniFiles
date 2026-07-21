package com.omnilabs.omfiles

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OmniFilesApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}

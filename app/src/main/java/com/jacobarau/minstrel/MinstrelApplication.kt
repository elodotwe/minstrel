package com.jacobarau.minstrel

import android.app.Application
import android.content.Intent
import com.jacobarau.minstrel.media.MinstrelService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MinstrelApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startService(Intent(this, MinstrelService::class.java))
    }
}

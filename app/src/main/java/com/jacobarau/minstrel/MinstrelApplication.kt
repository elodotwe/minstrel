package com.jacobarau.minstrel

import android.app.Application
import com.jacobarau.minstrel.media.MediaSessionManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MinstrelApplication : Application() {
    @Inject
    lateinit var mediaSessionManager: MediaSessionManager

    override fun onTerminate() {
        mediaSessionManager.release()
        super.onTerminate()
    }
}

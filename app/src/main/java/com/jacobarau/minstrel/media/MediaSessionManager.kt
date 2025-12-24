package com.jacobarau.minstrel.media

import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.jacobarau.minstrel.player.Player
import com.jacobarau.minstrel.player.PlaybackState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val player: Player
) {
    private val mediaSession: MediaSessionCompat
    private val job: Job

    private val sessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            player.togglePlayPause()
        }

        override fun onPause() {
            player.togglePlayPause()
        }

        override fun onStop() {
            player.stop()
        }
    }

    init {
        mediaSession = MediaSessionCompat(context, "Minstrel").apply {
            setCallback(sessionCallback)
            isActive = true
        }

        job = CoroutineScope(Dispatchers.Main).launch {
            player.playbackState.collect { state ->
                val playbackState = PlaybackStateCompat.Builder()
                    .setActions(
                        PlaybackStateCompat.ACTION_PLAY or
                                PlaybackStateCompat.ACTION_PAUSE or
                                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                                PlaybackStateCompat.ACTION_STOP
                    )
                    .setState(
                        state.toPlaybackStateCompat(),
                        0,
                        1.0f
                    )
                    .build()
                mediaSession.setPlaybackState(playbackState)
            }
        }
    }

    fun release() {
        job.cancel()
        mediaSession.release()
    }
}

private fun PlaybackState.toPlaybackStateCompat(): Int {
    return when (this) {
        is PlaybackState.Playing -> PlaybackStateCompat.STATE_PLAYING
        is PlaybackState.Paused -> PlaybackStateCompat.STATE_PAUSED
        is PlaybackState.Stopped -> PlaybackStateCompat.STATE_STOPPED
    }
}

package com.jacobarau.minstrel.media

import android.content.Context
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.jacobarau.minstrel.data.Track
import com.jacobarau.minstrel.player.Player
import com.jacobarau.minstrel.player.PlaybackState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
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

        override fun onSkipToNext() {
            player.skipToNext()
        }

        override fun onSkipToPrevious() {
            player.skipToPrevious()
        }
    }

    init {
        mediaSession = MediaSessionCompat(context, "Minstrel").apply {
            setCallback(sessionCallback)
            isActive = true
        }

        job = CoroutineScope(Dispatchers.Main).launch {
            player.playbackState.combine(player.currentTrack) { state, track ->
                val playbackStateBuilder = PlaybackStateCompat.Builder()
                    .setActions(
                        PlaybackStateCompat.ACTION_PLAY or
                                PlaybackStateCompat.ACTION_PAUSE or
                                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                                PlaybackStateCompat.ACTION_STOP or
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    )
                    .setState(
                        state.toPlaybackStateCompat(),
                        0,
                        1.0f
                    )
                mediaSession.setPlaybackState(playbackStateBuilder.build())

                val metadataBuilder = MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track?.filename)
                mediaSession.setMetadata(metadataBuilder.build())
            }.collect {}
        }

        CoroutineScope(Dispatchers.Main).launch {
            player.tracks.collect { tracks ->
                val queue = tracks.mapIndexed { index, track -> track.toQueueItem(index.toLong()) }
                mediaSession.setQueue(queue)
                mediaSession.setQueueTitle("Up Next")
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

private fun Track.toQueueItem(id: Long): MediaSessionCompat.QueueItem {
    val description = MediaDescriptionCompat.Builder()
        .setMediaId(uri.toString())
        .setTitle(filename)
        .build()
    return MediaSessionCompat.QueueItem(description, id)
}
